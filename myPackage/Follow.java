/*
 * 파일명: Follow.java
 * 목적: 팔로우/언팔로우 관계 관리 및 조회(JDBC)
 *
 * 개요
 * - 팔로우 여부 확인(isFollowing)
 * - 팔로우(follow): 관계 생성, 양측 카운트 갱신, 대상 사용자 경험치(+50) 부여
 * - 언팔로우(unfollow): 관계 삭제, 양측 카운트 감소
 *
 * 트랜잭션 처리
 * - follow/unfollow는 자동커밋을 끄고(setAutoCommit(false)) 다단계 변경을 하나의 트랜잭션으로 처리
 * - 중간 실패 시 rollback, 성공 시 commit 후 원래 자동커밋 상태 복구
 *
 * 무결성/중복 방지
 * - following(user_id, follower_id)에 대한 UNIQUE 제약(uq_follow) 전제
 * - 자기 자신을 팔로우하는 행위 방지
 *
 * 경험치 정책
 * - 팔로우 성공 시: 팔로우를 받은 사용자(target)에게 +50 EXP (LevelAdmin.info)
 *
 * 예외 처리
 * - SQLIntegrityConstraintViolationException: 중복 팔로우 시 false 반환
 * - 기타 예외: 롤백 후 SQLException으로 래핑/전파
 */

package myPackage;

import java.sql.*;

public class Follow {

    /** 이미 팔로우 중인지 확인 */
    public static boolean isFollowing(String me, String target) throws SQLException {
        // 입력 검증(널/공백)
        if (me == null || target == null || me.isBlank() || target.isBlank()) return false;

        // me(팔로워)가 target(피팔로우)을 팔로우 하고 있는지 단순 조회
        String sql = "SELECT 1 FROM following WHERE user_id=? AND follower_id=? LIMIT 1";
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, target);   // target: 팔로우 당하는 사람
            ps.setString(2, me);       // me: 팔로우 하는 사람
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();      // 존재하면 팔로우 중
            }
        }
    }

    /** 팔로우 수행: 성공 시 true, (자기자신/중복 등) 변화 없으면 false */
    public static boolean follow(String follower, String target) throws SQLException {
        // 입력 검증 및 자기 자신 팔로우 금지
        if (follower == null || target == null || follower.isBlank() || target.isBlank()) return false;
        if (follower.equals(target)) return false;

        Connection con = DBConn.getConnection();
        boolean oldAuto = con.getAutoCommit();
        con.setAutoCommit(false); // 트랜잭션 시작
        try {
            // 1) 중복 관계 검사
            String check = "SELECT 1 FROM following WHERE user_id=? AND follower_id=? LIMIT 1";
            try (PreparedStatement ps = con.prepareStatement(check)) {
                ps.setString(1, target);
                ps.setString(2, follower);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // 이미 팔로우 중 → 롤백 후 false
                        con.rollback();
                        con.setAutoCommit(oldAuto);
                        return false;
                    }
                }
            }

            // 2) following 관계 입력
            String insert = "INSERT INTO following(user_id, follower_id) VALUES(?, ?)";
            try (PreparedStatement ps = con.prepareStatement(insert)) {
                ps.setString(1, target);
                ps.setString(2, follower);
                ps.executeUpdate();
            }

            // 3) 카운트 갱신: target의 followers +1, follower의 followings +1
            try (PreparedStatement ps1 = con.prepareStatement(
                         "UPDATE user SET followers = followers + 1 WHERE user_id=?");
                 PreparedStatement ps2 = con.prepareStatement(
                         "UPDATE user SET followings = followings + 1 WHERE user_id=?")) {
                ps1.setString(1, target);
                ps1.executeUpdate();
                ps2.setString(1, follower);
                ps2.executeUpdate();
            }

            // 4) 경험치 부여: 팔로우 받은 사용자(target)에게 +50
            LevelAdmin.info(con, target, 50);

            // 커밋 및 자동커밋 복원
            con.commit();
            con.setAutoCommit(oldAuto);
            System.out.println("Follow successfully");
            return true;
        } catch (SQLIntegrityConstraintViolationException dup) {
            // UNIQUE 제약으로 인한 중복 삽입 등
            con.rollback();
            con.setAutoCommit(oldAuto);
            return false;
        } catch (Exception e) {
            // 기타 예외: 롤백 후 SQLException으로 래핑
            con.rollback();
            con.setAutoCommit(oldAuto);
            throw e instanceof SQLException ? (SQLException)e : new SQLException(e);
        }
    }

    /** 언팔로우: 성공 시 true, (관계가 없던 경우 등) 변화 없으면 false */
    public static boolean unfollow(String follower, String target) throws SQLException {
        // 입력 검증 및 자기 자신 보호
        if (follower == null || target == null || follower.isBlank() || target.isBlank()) return false;
        if (follower.equals(target)) return false;

        Connection con = DBConn.getConnection();
        boolean oldAuto = con.getAutoCommit();
        con.setAutoCommit(false); // 트랜잭션 시작
        try {
            // 1) 관계 삭제
            int affected;
            String del = "DELETE FROM following WHERE user_id=? AND follower_id=?";
            try (PreparedStatement ps = con.prepareStatement(del)) {
                ps.setString(1, target);
                ps.setString(2, follower);
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                // 원래 팔로우가 아니었음 → 롤백 후 false
                con.rollback();
                con.setAutoCommit(oldAuto);
                return false;
            }

            // 2) 카운트 감소 (최소 0 보장)
            try (PreparedStatement ps1 = con.prepareStatement(
                         "UPDATE user SET followers = GREATEST(0, followers - 1) WHERE user_id=?");
                 PreparedStatement ps2 = con.prepareStatement(
                         "UPDATE user SET followings = GREATEST(0, followings - 1) WHERE user_id=?")) {
                ps1.setString(1, target);
                ps1.executeUpdate();
                ps2.setString(1, follower);
                ps2.executeUpdate();
            }

            // 커밋 및 자동커밋 복원
            con.commit();
            con.setAutoCommit(oldAuto);
            System.out.println("Unfollow successfully");
            return true;
        } catch (Exception e) {
            con.rollback();
            con.setAutoCommit(oldAuto);
            throw e instanceof SQLException ? (SQLException)e : new SQLException(e);
        }
    }
}
