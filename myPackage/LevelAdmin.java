/*
 * 파일명: LevelAdmin.java
 * 목적: 사용자 경험치(Exp) 및 레벨(Level) 상승 로직을 중앙에서 관리하는 유틸리티 클래스
 *
 * 주요 기능
 * - info(Connection, String, int): 특정 사용자(userId)에게 경험치(userExp)를 부여하고,
 *   누적치가 레벨업 기준을 넘으면 레벨을 증가시키고 DB에 반영.
 * - 만렙(기본 20레벨) 도달 시 배지(badge) 자동 지급.
 *
 * 동작 개요
 * 1) 현재 사용자 레벨/경험치 조회 (user 테이블의 level, exp 컬럼)
 * 2) 전달받은 경험치를 누적 (음수 무시)
 * 3) 누적 경험치가 요구 경험치 이상이면 레벨업 루프 수행
 *    - 요구 경험치 공식: floor(level * BASE_EXP * MULT)
 *    - 레벨이 MAX_LEVEL 이상이면 남는 경험치는 버리고(정책) exp=0으로 고정
 * 4) 만렙이면 badge 컬럼을 TRUE로 업데이트(이미 TRUE면 무시)
 * 5) 변경된 level/exp를 user 테이블에 UPDATE
 *
 * 요구/전제
 * - DB 스키마: user(user_id PK, level INT, exp INT, badge BOOLEAN, ...)
 * - 호출자는 트랜잭션 경계를 관리한다(autocommit, rollback 등은 호출측 책임).
 *   (ex. 여러 업무 이벤트와 함께 원자성을 보장하려면 같은 Connection에서 호출)
 *
 * 사용 예
 * - 팔로우를 받았을 때: info(con, targetUserId, 50);
 * - 게시글 좋아요를 받았을 때: info(con, writerId, 10);
 * - 댓글 좋아요를 받았을 때: info(con, commentWriterId, 5);
 * - 댓글을 받았을 때(게시글 작성자 보상): info(con, postWriterId, 5);
 *
 * 주의 사항
 * - 스레드/프로세스 동시 호출 시 최신 레벨/경험치 반영을 위해 같은 커넥션-트랜잭션 내에서 호출 권장.
 * - userExp <= 0이면 무시(가산하지 않음).
 * - 레벨 상한은 MAX_LEVEL(기본 20). 상한 도달 시 exp는 0으로 맞추며 초과분 저장하지 않음(정책).
 */

package myPackage;

import java.sql.*;

public class LevelAdmin {

    // 정책 상수
    private static final int MAX_LEVEL = 20;  // 레벨 상한
    private static final int BASE_EXP  = 100; // 필요 경험치 계산의 베이스(레벨에 곱해짐)
    private static final double MULT   = 1.5; // 레벨 증가에 따른 필요 경험치 가중치

    /** 
     * userId에게 userExp만큼 경험치를 부여하고, 필요 시 레벨업/배지 지급 후 DB에 반영.
     * - 트랜잭션은 호출측(Connection) 설정을 따름(autocommit 여부는 건드리지 않음).
     * - userExp가 0 이하이면 가산하지 않음.
     */
    public static void info(Connection con, String userId, int userExp) throws SQLException {
        if (userId == null || userId.isBlank()) return;

        int level = 1;
        int exp   = 0;

        // 1) 현재 레벨/경험치 조회
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT level, exp FROM user WHERE user_id=?")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // 존재하지 않는 사용자
                    System.out.println("User not found");
                    return;
                }
                level = rs.getInt("level");
                exp   = rs.getInt("exp");
            }
        }

        // 2) 경험치 가산(음수 방지)
        if (userExp > 0) {
            exp += userExp;
        }

        // 3) 레벨업 루프
        boolean leveledUp = false;
        int required = requiredExp(level); // 현재 레벨에서 다음 레벨로 가기 위한 필요치
        while (exp >= required) {
            exp   -= required; // 필요치 차감
            level += 1;        // 레벨 상승
            leveledUp = true;

            // 상한 도달 시 정책에 따라 exp는 0으로, 루프 종료
            if (level >= MAX_LEVEL) {
                level = MAX_LEVEL;
                exp   = 0;
                break;
            }
            // 다음 레벨에 필요한 경험치를 재계산
            required = requiredExp(level);
        }

        // 4) 만렙 배지 지급(이미 배지 있으면 쿼리는 영향 없음)
        if (level == MAX_LEVEL) {
            try (PreparedStatement bq = con.prepareStatement(
                    "UPDATE user SET badge=TRUE WHERE user_id=? AND badge=FALSE")) {
                bq.setString(1, userId);
                bq.executeUpdate();
            }
        }

        // 5) 최종 레벨/경험치 저장
        try (PreparedStatement up = con.prepareStatement(
                "UPDATE user SET level=?, exp=? WHERE user_id=?")) {
            up.setInt(1, level);
            up.setInt(2, Math.max(0, exp)); // 방어적: 음수 방지
            up.setString(3, userId);
            up.executeUpdate();
        }

        // 6) 로깅(디버깅/모니터링용)
        if (leveledUp) System.out.println("New Level: " + level);
        System.out.println("Gain exp: " + userExp);
        System.out.println("Now Level: " + level + ", Exp: " + exp);
    }

    /**
     * 현재 level에서 다음 레벨로 가기 위한 필요 경험치 계산식.
     * 필요 경험치 = floor(level * BASE_EXP * MULT)
     * 예) BASE_EXP=100, MULT=1.5일 때
     *     L1→L2: 150, L2→L3: 300, L3→L4: 450, ...
     */
    private static int requiredExp(int level) {
        return (int) (level * BASE_EXP * MULT);
    }
}
