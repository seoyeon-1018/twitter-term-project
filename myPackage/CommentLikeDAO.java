/*
 * 파일명: CommentLikeDAO.java
 * 목적: 댓글 좋아요/좋아요 취소를 처리하고, '댓글 좋아요를 받은 작성자'에게 경험치(XP)를 부여하는 DAO 유틸리티.
 *
 * 동작 개요:
 *  1) like(con, likerId, commentId)
 *     - comment_like 테이블에 (commentId, likerId)를 INSERT 하여 '댓글 좋아요'를 기록합니다.
 *     - 이어서 해당 댓글의 작성자(writer_id)를 조회합니다.
 *     - 좋아요 누른 사람(likerId)과 댓글 작성자가 다르면, 댓글 작성자에게 경험치 +5를 부여합니다.
 *       (LevelAdmin.info(con, writer, 5) 호출)
 *
 *  2) unlike(con, likerId, commentId)
 *     - comment_like 테이블에서 (commentId, likerId) 행을 DELETE 하여 '댓글 좋아요 취소'를 처리합니다.
 *     - 취소 시에는 경험치 환원/감소를 하지 않습니다. (정책상 단순 취소)
 *
 * 파라미터:
 *  - Connection con   : 호출 측에서 생성/관리하는 DB 커넥션(트랜잭션 경계도 호출 측이 소유)
 *  - String likerId   : 좋아요를 누른 사용자 ID
 *  - int commentId    : 좋아요 대상 댓글 ID
 *
 * 반환값:
 *  - like(...)   : 성공 시 true. (예외 발생 시 SQLException 전파)
 *  - unlike(...) : 실제 삭제된 행이 1개 이상이면 true, 아니면 false
 *
 * 예외 처리 / 트랜잭션:
 *  - try-with-resources로 PreparedStatement/ResultSet 누수 방지.
 *  - 본 메서드들은 커넥션을 닫지 않습니다. (오토커밋/트랜잭션은 호출자가 관리)
 *  - 중복 좋아요 방지를 위해 DB 제약(UNIQUE 인덱스)이 없다면, 상위 레벨에서 중복 체크가 필요할 수 있습니다.
 *    (현재 코드는 중복 시 DB 오류가 발생할 수 있으며, 그 예외를 호출자에 전파합니다.)
 *
 * 보안/무결성:
 *  - 모든 SQL에 PreparedStatement 사용으로 SQL 인젝션 예방.
 *  - 자신이 쓴 댓글을 자신이 좋아요 눌러도 경험치는 부여되지 않습니다. (자기 보상 방지)
 *
 * 의존:
 *  - LevelAdmin.info(Connection, String, int) : 경험치 부여(레벨업 로직 포함)
 *
 * 사용 예:
 *  try (Connection con = DBConn.getConnection()) {
 *      con.setAutoCommit(false);
 *      CommentLikeDAO.like(con, "alice", 456);
 *      con.commit();
 *  }
 */

package myPackage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CommentLikeDAO {

    /**
     * 댓글 좋아요 등록 및 댓글 작성자에게 경험치(+5) 부여.
     *
     * @param con        호출 측에서 제공하는 커넥션
     * @param likerId    좋아요를 누른 사용자 ID
     * @param commentId  좋아요 대상 댓글 ID
     * @return           성공 시 true
     * @throws SQLException DB 오류 전파
     */
    public static boolean like(Connection con, String likerId, int commentId) throws SQLException {
        // 1) 좋아요 INSERT
        String ins = "INSERT INTO comment_like(comment_id, liker_id) VALUES(?, ?)";
        try (PreparedStatement ps = con.prepareStatement(ins)) {
            ps.setInt(1, commentId);
            ps.setString(2, likerId);
            ps.executeUpdate();
        }

        // 2) 댓글 작성자 조회
        String q = "SELECT writer_id FROM comment WHERE comment_id=?";
        String writer = null;
        try (PreparedStatement ps = con.prepareStatement(q)) {
            ps.setInt(1, commentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) writer = rs.getString(1);
            }
        }

        // 3) 자기 댓글이 아닌 경우에만 작성자에게 경험치 +5 부여
        if (writer != null && !writer.equals(likerId)) {
            LevelAdmin.info(con, writer, 5); // ★ 경험치
        }
        return true;
    }

    /**
     * 댓글 좋아요 취소.
     *
     * @param con        호출 측에서 제공하는 커넥션
     * @param likerId    좋아요를 취소하는 사용자 ID
     * @param commentId  목표 댓글 ID
     * @return           삭제된 행이 1개 이상이면 true
     * @throws SQLException DB 오류 전파
     */
    public static boolean unlike(Connection con, String likerId, int commentId) throws SQLException {
        String del = "DELETE FROM comment_like WHERE comment_id=? AND liker_id=?";
        try (PreparedStatement ps = con.prepareStatement(del)) {
            ps.setInt(1, commentId);
            ps.setString(2, likerId);
            return ps.executeUpdate() > 0;
        }
    }
}
