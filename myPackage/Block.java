package myPackage;

import java.sql.*;

public class Block {

    public static void block(String blocker, String blocked) throws SQLException {
        Connection con = DBConn.getConnection();

        if (blocker.equals(blocked)) {
            System.out.println("You cannot block yourself.");
            return;
        }

        String check = "select * from block where block_id=? and by_block_id=?";
        PreparedStatement ps = con.prepareStatement(check);
        ps.setString(1, blocked);
        ps.setString(2, blocker);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            System.out.println("You already blocked this user.");
            return;
        }

        String insert = "insert into block(block_id, by_block_id) values(?,?)";
        ps = con.prepareStatement(insert);
        ps.setString(1, blocked);
        ps.setString(2, blocker);
        ps.executeUpdate();

        System.out.println("User '" + blocked + "' has been blocked.");
    }
}
