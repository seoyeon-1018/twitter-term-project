package myPackage;

import java.sql.*;

public class ReservedPost {

    public static void reserve(String writer, String content, String time) throws SQLException {
        Connection con = DBConn.getConnection();

        String sql = "insert into reserved_post(writer_id, content, scheduled_time) values(?,?,?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, writer);
        ps.setString(2, content);
        ps.setString(3, time);
        ps.executeUpdate();

        System.out.println("Reservation created successfully.");
    }

    public static void showReservedPosts(String writer) throws SQLException {
        Connection con = DBConn.getConnection();

        String sql = "select * from reserved_post where writer_id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, writer);

        ResultSet rs = ps.executeQuery();

        System.out.println("=== Reserved Posts for " + writer + " ===");
        boolean exist = false;

        while (rs.next()) {
            int id = rs.getInt("s_id");
            String content = rs.getString("content");
            Timestamp t = rs.getTimestamp("scheduled_time");

            System.out.println("[" + id + "] " + t + " : " + content);
            exist = true;
        }

        if (!exist) {
            System.out.println("No reserved posts found.");
        }

        rs.close();
        ps.close();
    }
}
