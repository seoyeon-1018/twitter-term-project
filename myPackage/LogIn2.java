package myPackage;

import java.sql.*;

public class LogIn2 {

    public static boolean login(String id, String pwd) throws SQLException {

        Connection con = DBConn.getConnection();

        String logQuery = "select * from user where user_id=? and pwd=?";
        PreparedStatement ps = con.prepareStatement(logQuery);
        ps.setString(1, id);
        ps.setString(2, pwd);

        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            System.out.println("Log in failed");
            return false;
        }

        //When log in successfully
        int level = rs.getInt("level");
        int exp = rs.getInt("exp");
        boolean badge = rs.getBoolean("badge");
        int followers = rs.getInt("followers");
        int followings = rs.getInt("followings");

        System.out.println("Log in completed");
        System.out.println("===== User Info =====");
        System.out.println("ID: " + id);
        System.out.println("Level: " + level + " | Exp: " + exp);
        System.out.println("Followers: " + followers + " | Followings: " + followings);
        System.out.println("Badge: " + (badge ? "Held" : "Not Held"));
        System.out.println("======================");

        return true;
    }
}
