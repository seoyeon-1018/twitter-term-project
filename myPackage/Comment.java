package myPackage;

import java.sql.*;

public class Comment {

    public static void write(int postId, String writer, String content) throws SQLException {
        Connection con = DBConn.getConnection();

        String sql = "insert into comment(content,writer_id,post_id) values(?,?,?)";
        PreparedStatement ps = con.prepareStatement(sql);

        ps.setString(1, content);
        ps.setString(2, writer);
        ps.setInt(3, postId);
        ps.executeUpdate();
    }
}
