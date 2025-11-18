package myPackage;

import java.sql.*;

public class HashTag{

    public static void search(String tag) throws SQLException {
        Connection con = DBConn.getConnection();

        String tagQuery =
            "select p.post_id, p.writer_id, p.content " +
            "from posts p join post_tag t on p.post_id = t.post_id " +
            "where t.tag=?";

        PreparedStatement ps = con.prepareStatement(tagQuery);
        ps.setString(1, tag);

        ResultSet rs = ps.executeQuery();

        System.out.println("=== Posts with hashtag #" + tag + " ===");
        boolean exist = false;

        while (rs.next()) {
            int id = rs.getInt("post_id");
            String writer = rs.getString("writer_id");
            String content = rs.getString("content");

            System.out.println("[" + id + "] " + writer + ": " + content);
            exist = true;
        }

        if (!exist) {
            System.out.println("No posts found for this hashtag.");
        }

        rs.close();
        ps.close();
    }
}
