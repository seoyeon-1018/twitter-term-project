package myPackage;

import java.sql.*;

public class FollowRecommend {

    public static void showRecommend(String userId) throws SQLException {

        Connection con = DBConn.getConnection();

        String recommend =
            "select distinct f2.user_id as recommend " +
            "from following f1 " +
            "join following f2 on f1.user_id = f2.follower_id " +
            "where f1.follower_id=? and f2.user_id<>? and " +
            "f2.user_id not in (select user_id from following where follower_id=?)";

        PreparedStatement ps = con.prepareStatement(recommend);
        ps.setString(1, userId);
        ps.setString(2, userId);
        ps.setString(3, userId);

        ResultSet rs = ps.executeQuery();

        System.out.println("=== Follow Recommendations ===");
        boolean exist = false;

        while (rs.next()) {
            System.out.println("- " + rs.getString("recommend"));
            exist = true;
        }

        if (!exist) {
            System.out.println("No user to recommend.");
        }

        rs.close();
        ps.close();
    }
}
