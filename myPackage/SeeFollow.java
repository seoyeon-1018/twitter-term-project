package myPackage;
import java.sql.*;

public class SeeFollow {

    public static void seefollow(String userId) throws SQLException {
        Connection con = DBConn.getConnection();

        String sql = "select follower_id from following where user_id=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, userId);

        ResultSet rs = ps.executeQuery();

        System.out.println("Followers list:");

        boolean exist = false;
        while (rs.next()) {
            System.out.println(rs.getString("follower_id"));
            exist = true;
        }

        if (!exist) {
            System.out.println("No followers yet.");
        }
    }
}
