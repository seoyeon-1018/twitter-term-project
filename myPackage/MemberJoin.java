package myPackage;

import java.sql.*;

public class MemberJoin {

    public static boolean signUp(String id, String pwd) throws SQLException {
        Connection con = DBConn.getConnection();

        String checkId = "select * from user where user_id=?";
        PreparedStatement ps = con.prepareStatement(checkId);
        ps.setString(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) return false;  // The ID you want to join exists

        String insert = "insert into user(user_id, pwd) values(?,?)";
        ps = con.prepareStatement(insert);
        ps.setString(1, id);
        ps.setString(2, pwd);
        ps.executeUpdate();

        return true;
    }
}
