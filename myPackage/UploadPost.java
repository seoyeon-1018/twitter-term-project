package myPackage;

import java.sql.*;

public class UploadPost {

    public static int uploadPost(String writer, String content) throws SQLException {
        Connection con = DBConn.getConnection();

        String postQuery = "insert into posts(content, writer_id) values(?,?)";
        PreparedStatement ps = con.prepareStatement(postQuery, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, content);
        ps.setString(2, writer);
        ps.executeUpdate();

        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) return keys.getInt(1);

        return -1;
    }

    public static void createTag(int postId, String tags) throws SQLException {
        if (tags.isEmpty()) return; //When there's no tag

        String[] str = tags.split(",");
        Connection con = DBConn.getConnection();

        for (String t : str) {
            String insert = "insert into post_tag(post_id, tag) values(?,?)";
            PreparedStatement ps = con.prepareStatement(insert);
            ps.setInt(1, postId);
            ps.setString(2, t.trim());
            ps.executeUpdate();
        }
    }
}
