package myPackage;

import java.sql.*;

public class Message {

    public static void sendMessage(String sender, String receiver, String content) throws SQLException {
        Connection con = DBConn.getConnection();
        
        //Check you're blocked
        String block = "select * from block where block_id=? and by_block_id=?";
        PreparedStatement ps = con.prepareStatement(block);
        ps.setString(1, receiver);
        ps.setString(2, sender);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            System.out.println("You are blocked by this user. Message not sent.");
            return;
        }

        String sql = "insert into message(send_id, receive_id, content) values(?,?,?)";
        ps = con.prepareStatement(sql);
        ps.setString(1, sender);
        ps.setString(2, receiver);
        ps.setString(3, content);
        ps.executeUpdate();

        System.out.println("Message sent successfully.");
    }
}
