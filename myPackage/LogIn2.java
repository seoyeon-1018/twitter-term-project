package myPackage;

import java.sql.*;

public class LogIn2 {

    public static boolean login(String id, String pwd) throws SQLException {
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM user WHERE user_id=? AND pwd=?")) {
            ps.setString(1, id);
            ps.setString(2, pwd);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Log in failed");
                    return false;
                }
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
    }

    /** 현재 비밀번호가 일치할 때만 새 비밀번호로 변경 (1건 업데이트되면 성공) */
    public static boolean changePassword(String userId, String currentPwd, String newPwd) throws SQLException {
        if (userId == null || userId.isBlank()) return false;
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE user SET pwd=? WHERE user_id=? AND pwd=?")) {
            ps.setString(1, newPwd);
            ps.setString(2, userId);
            ps.setString(3, currentPwd);
            int updated = ps.executeUpdate();
            return updated == 1;
        }
    }
}
