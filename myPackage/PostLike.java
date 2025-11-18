package myPackage;

import java.sql.*;

public class PostLike {

    public static boolean likePost(int postId, String liker) throws SQLException {

        Connection con = DBConn.getConnection();

        //Check already like it
        String check = "select * from post_like where post_id=? and liker_id=?";
        PreparedStatement ps = con.prepareStatement(check);
        ps.setInt(1, postId);
        ps.setString(2, liker);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) return false; //Already pressed "like"

        //Like, register
        String insert = "insert into post_like(post_id,liker_id) values(?,?)";
        ps = con.prepareStatement(insert);
        ps.setInt(1, postId);
        ps.setString(2, liker);
        ps.executeUpdate();

        //Find the author of the post you received like
        String writerQuery = "select writer_id from posts where post_id=?";
        ps = con.prepareStatement(writerQuery);
        ps.setInt(1, postId);
        rs = ps.executeQuery();

        if (rs.next()) {
            String writerId = rs.getString("writer_id");

            //Exp value +10
            LevelAdmin.info(con, writerId, 10);
        }

        System.out.println("Post Like successfully");
        return true;
    }
}
