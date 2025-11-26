/*
 * 파일명: CommentDAO.java
 * 목적: 댓글 등록(INSERT)과 댓글로 인해 원글 작성자에게 경험치(XP)를 부여하는 DAO 유틸리티.
 *
 * 동작 개요:
 *  1) add(con, writerId, postId, content)
 *     - comment 테이블에 새 댓글을 INSERT 합니다.
 *     - 이어서 해당 postId의 원글 작성자(writer_id)를 조회합니다.
 *     - 댓글 작성자(writerId)와 원글 작성자가 다르면, 원글 작성자에게 경험치 +5를 부여합니다.
 *       (LevelAdmin.info(con, postWriter, 5) 호출)
 *
 * 파라미터:
 *  - Connection con  : 호출 측에서 생성/관리하는 DB 커넥션(트랜잭션 경계도 호출 측이 소유)
 *  - String writerId : 댓글 작성자 ID
 *  - int postId      : 댓글이 달릴 게시글 ID
 *  - String content  : 댓글 내용
 *
 * 반환값:
 *  - boolean : 성공 시 true. (예외가 발생하면 SQLException 전파)
 *
 * 예외 처리 / 트랜잭션:
 *  - try-with-resources로 PreparedStatement/ResultSet 누수 방지.
 *  - 이 메서드는 커넥션을 닫지 않습니다. (트랜잭션 경계는 호출자 결정)
 *  - 댓글 INSERT 이후 XP 부여까지 하나의 논리 동작으로 보고,
 *    호출자가 오토커밋 OFF 인 경우 원자성을 원하면 상위에서 트랜잭션을 명시적으로 관리하세요.
 *
 * 보안/무결성:
 *  - 모든 SQL은 PreparedStatement 사용으로 SQL 인젝션 예방.
 *  - postId가 존재하지 않으면 postWriter는 null이 되어 XP 부여 스킵.
 *  - 댓글 작성자가 자기 자신의 글에 댓글을 단 경우 XP 부여 없음(자기 보상 방지).
 *
 * 의존:
 *  - LevelAdmin.info(Connection, String, int) : 경험치 부여(레벨업 로직 포함)
 *
 * 사용 예:
 *  try (Connection con = DBConn.getConnection()) {
 *      con.setAutoCommit(false);
 *      CommentDAO.add(con, "alice", 123, "Nice post!");
 *      con.commit();
 *  }
 */

package myPackage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CommentDAO {

    /**
     * 댓글을 등록하고, 필요 시 원글 작성자에게 경험치를 부여합니다.
     *
     * @param con       호출 측에서 제공하는 커넥션
     * @param writerId  댓글 작성자 ID
     * @param postId    댓글이 달릴 게시글 ID
     * @param content   댓글 내용
     * @return          성공 시 true
     * @throws SQLException DB 오류 시 전파
     */
    public static boolean add(Connection con, String writerId, int postId, String content) throws SQLException {

        // 1) 댓글 INSERT
        String ins = "INSERT INTO comment(content, writer_id, post_id) VALUES(?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(ins)) {
            ps.setString(1, content);
            ps.setString(2, writerId);
            ps.setInt(3, postId);
            ps.executeUpdate();
        }

        // 2) 원글 작성자 조회
        String q = "SELECT writer_id FROM posts WHERE post_id=?";
        String postWriter = null;
        try (PreparedStatement ps = con.prepareStatement(q)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) postWriter = rs.getString(1);
            }
        }

        // 3) 자기 글이 아닌 경우에만 원글 작성자에게 경험치 +5 부여
        if (postWriter != null && !postWriter.equals(writerId)) {
            // ★ 경험치: 댓글을 받았을 때 원글 작성자 보상(+5)
            LevelAdmin.info(con, postWriter, 5);
        }

        return true;
    }
}
