package myPackage;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ReservedPostWorker
 * -----------------------------------------------------------------------------
 * 목적
 *  - 예약 포스트(reserved_post) 중, 게시 시각이 도래했지만(is_posted=FALSE) 아직
 *    실제 업로드되지 않은 항목을 찾아 posts 테이블로 이동시키고 해시태그까지 저장한 뒤,
 *    해당 예약 레코드를 is_posted=TRUE 로 마킹한다.
 *
 * 동작 개요
 *  1) reserved_post에서 (scheduled_time <= NOW() AND is_posted=FALSE) 행을 조회(FOR UPDATE).
 *  2) 각 행에 대해 posts에 INSERT(생성된 post_id 회수) → post_tag에 해시태그 배치 INSERT.
 *  3) 처리 완료한 예약글은 is_posted=TRUE로 업데이트.
 *  4) 위 과정을 단일 트랜잭션으로 묶어 원자성 보장.
 *
 * 사용 방법
 *  - 주기 실행 타이머(예: javax.swing.Timer 또는 ScheduledExecutorService)에서
 *    정기적으로 runOnce()를 호출한다.
 *  - 독립적인 DB 연결을 매 호출마다 획득하므로, 장시간 유휴 커넥션 타임아웃 이슈를 회피한다.
 *
 * 주의
 *  - 동시에 여러 인스턴스가 실행될 수 있다면, SELECT … FOR UPDATE 구문과
 *    트랜잭션 범위가 충돌 없이 동작하도록 DB 격리수준을 확인할 것.
 *  - 해시태그 키는 소문자/길이 50 제한으로 정규화되어 post_tag에 저장한다.
 */
public class ReservedPostWorker {
    // PostWriterMain과 동일한 규칙 유지: #뒤에 영문/숫자/밑줄/한글
    private static final Pattern TAG = Pattern.compile("#([\\p{IsAlphabetic}\\p{IsDigit}_가-힣]+)");

    /**
     * 예약글을 한 번 처리한다.
     * - 기한 도달 & 미게시 → posts 이동 + 해시태그 저장 + 플래그 업데이트
     * - 호출 시마다 새 Connection을 열고 닫는다.
     */
    public static void runOnce() {
        // 실행 때마다 새 연결을 잡는 방식: 커넥션 타임아웃/끊김 대비
        try (Connection con = DBConn.getConnection()) {
            con.setAutoCommit(false); // 전체 처리 원자성 보장

            // 게시 시각 도달 + 미게시 건을 잠금 상태로 선점
            final String selectSql =
                "SELECT s_id, writer_id, content " +
                "FROM reserved_post " +
                "WHERE is_posted = FALSE AND scheduled_time <= NOW() " +
                "FOR UPDATE"; // 동시 실행 대비(단일 인스턴스면 없어도 됨)

            // posts에 실제 업로드(작성 시간은 NOW())
            final String insertPostSql =
                "INSERT INTO posts (content, writer_id, created_at) VALUES (?, ?, NOW())";

            // 처리 완료 표시
            final String updateReservedSql =
                "UPDATE reserved_post SET is_posted = TRUE WHERE s_id = ?";

            try (PreparedStatement psSel = con.prepareStatement(selectSql);
                 ResultSet rs = psSel.executeQuery()) {

                while (rs.next()) {
                    int sId = rs.getInt("s_id");
                    String writer = rs.getString("writer_id");
                    String content = rs.getString("content");

                    int newPostId = -1;

                    // 1) posts INSERT → 생성된 post_id 회수
                    try (PreparedStatement psIns =
                             con.prepareStatement(insertPostSql, Statement.RETURN_GENERATED_KEYS)) {
                        psIns.setString(1, content);
                        psIns.setString(2, writer);
                        psIns.executeUpdate();
                        try (ResultSet keys = psIns.getGeneratedKeys()) {
                            if (keys.next()) newPostId = keys.getInt(1);
                        }
                    }

                    // 1-1) 해시태그 저장(있다면)
                    if (newPostId > 0) {
                        saveHashtags(con, newPostId, content);
                    }

                    // 2) 예약글 처리 완료 마킹
                    try (PreparedStatement psUp = con.prepareStatement(updateReservedSql)) {
                        psUp.setInt(1, sId);
                        psUp.executeUpdate();
                    }
                }
            }

            con.commit(); // 모든 예약글 처리 성공 시 커밋
        } catch (SQLException e) {
            // 예약 워커는 콘솔 로그로 원인 파악
            e.printStackTrace();
        }
    }

    /**
     * 본문에서 해시태그를 추출하여 post_tag에 저장한다.
     * - 태그는 소문자 정규화, 최대 길이 50으로 절단.
     * - INSERT IGNORE: 중복 태그 삽입 시 무시(UNIQUE(post_id, tag) 가정).
     */
    private static void saveHashtags(Connection con, int postId, String content) throws SQLException {
        if (content == null || content.isBlank()) return;

        try (PreparedStatement ps = con.prepareStatement(
                "INSERT IGNORE INTO post_tag(post_id, tag) VALUES(?, ?)")) {

            Matcher m = TAG.matcher(content);
            while (m.find()) {
                String raw = m.group(1);
                if (raw == null || raw.isBlank()) continue;

                // 정규화: 소문자, 길이 제한
                String norm = raw.trim().toLowerCase();
                if (norm.length() > 50) norm = norm.substring(0, 50);

                ps.setInt(1, postId);
                ps.setString(2, norm);
                ps.addBatch();
            }
            ps.executeBatch(); // 배치로 한 번에 INSERT
        }
    }
}
