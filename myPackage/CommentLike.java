package myPackage;

import java.sql.*;

public class CommentLike {

    public static boolean likeComment(int commentId, String liker) throws SQLException {

        Connection con = DBConn.getConnection();

        //Check already pressed "like"
        String check = "select * from comment_like where comment_id=? and liker_id=?";
        PreparedStatement ps = con.prepareStatement(check);
        ps.setInt(1, commentId);
        ps.setString(2, liker);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) return false; //Already pressed "like"

        //Like, register
        String insert = "insert into comment_like(comment_id, liker_id) values(?,?)";
        ps = con.prepareStatement(insert);
        ps.setInt(1, commentId);
        ps.setString(2, liker);
        ps.executeUpdate();

        //Find a writer of comment
        String writerQuery = "select writer_id from comment where comment_id=?";
        ps = con.prepareStatement(writerQuery);
        ps.setInt(1, commentId);
        rs = ps.executeQuery();

        if (rs.next()) {
            String writerId = rs.getString("writer_id");

            //Exp values +5
            LevelAdmin.info(con, writerId, 5);
        }

        System.out.println("Comment Like successfully");
        return true;
    }
}
