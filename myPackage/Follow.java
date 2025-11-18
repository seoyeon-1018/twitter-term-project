package myPackage;

import java.sql.*;

public class Follow {

    public static boolean follow(String follower, String target) throws SQLException {

        Connection con = DBConn.getConnection();

        //You can't follow yourself
        if (follower.equals(target)) return false;

        //Check already follow the user
        String check = "select * from following where user_id=? and follower_id=?";
        PreparedStatement ps = con.prepareStatement(check);
        ps.setString(1, target);
        ps.setString(2, follower);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) return false; //Already followed the user

        String insert = "insert into following(user_id, follower_id) values(?, ?)";
        ps = con.prepareStatement(insert);
        ps.setString(1, target); 
        ps.setString(2, follower); 
        ps.executeUpdate();

        //Target's followers increase by 1
        String incFollower = "update user set followers = followers + 1 where user_id=?";
        PreparedStatement ps1 = con.prepareStatement(incFollower);
        ps1.setString(1, target);
        ps1.executeUpdate();

        //The user's followings increase by 1
        String incFollowing = "update user set followings = followings + 1 where user_id=?";
        PreparedStatement ps2 = con.prepareStatement(incFollowing);
        ps2.setString(1, follower);
        ps2.executeUpdate();

        //Exp values +50
        LevelAdmin.info(con, target, 50);

        System.out.println("Follow successfully");

        return true;
    }
}
