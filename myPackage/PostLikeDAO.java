/*
 * 파일명: PostLikeDAO.java
 * 목적: 게시글 좋아요/좋아요 취소를 DB에 반영하고, 필요 시 게시글 작성자에게 경험치를 부여.
 *
 * 동작 개요
 * - like(con, likerId, postId)
 *   1) post_like 테이블에 (post_id, liker_id) 한 건을 INSERT하여 '좋아요' 상태로 만듭니다.
 *   2) 해당 게시글의 작성자(writer_id)를 조회합니다.
 *   3) 좋아요를 누른 사람이 작성자 본인이 아닐 경우, 작성자에게 경험치(+10)를 지급합니다.
 *      (LevelAdmin.info 호출 — 레벨/경험치 계산 및 만렙 배지 반영은 해당 클래스 책임)
 *   4) 성공 시 true 반환. (중복 좋아요의 경우 DB 제약으로 예외 발생 가능)
 *
 *
 * DB/제약
 * - post_like(post_id, liker_id)에 UNIQUE 제약을 두는 것을 강력 권장합니다.
 *   → 중복 좋아요 시 SQLIntegrityConstraintViolationException 유발(호출측에서 처리)
 * - posts(post_id) ↔ post_like(post_id) FK, user(user_id) ↔ post_like(liker_id) FK 가정.
 *
 * 트랜잭션
 * - 트랜잭션 경계는 호출측(Connection)에서 관리합니다.
 *   여러 DAO 작업(예: 좋아요 + 알림 + 피드 카운트 갱신)을 묶을 경우, 호출측에서 수동 커밋을 사용하세요.
 *
 * 예외/오류 처리
 * - SQLException을 그대로 던집니다. UI/서비스 레이어에서 사용자 메시지/롤백 처리 권장.
 *
 * 보안/검증
 * - likerId, postId는 호출 전 유효성 검사를 마친 상태라고 가정합니다.
 * - 자기 게시글에 대한 좋아요는 경험치가 부여되지 않습니다.
 */

package myPackage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PostLikeDAO {
    // 좋아요
    public static boolean like(Connection con, String likerId, int postId) throws SQLException {
        // 중복 방지: post_like에 UNIQUE(post_id, liker_id) 권장
        String ins = "INSERT INTO post_like(post_id, liker_id) VALUES(?, ?)";
        try (PreparedStatement ps = con.prepareStatement(ins)) {
            ps.setInt(1, postId);
            ps.setString(2, likerId);
            ps.executeUpdate();
        }

        // 작성자 조회
        String q = "SELECT writer_id FROM posts WHERE post_id=?";
        String writer = null;
        try (PreparedStatement ps = con.prepareStatement(q)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) writer = rs.getString(1);
            }
        }

        // 자기 좋아요는 경험치 X
        if (writer != null && !writer.equals(likerId)) {
            LevelAdmin.info(con, writer, 10); // ★ 경험치
        }
        return true;
    }

  
}
