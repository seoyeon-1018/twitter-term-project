package myPackage;

import java.sql.*;

public class DBConn {
    //해당 유저 url,user,pwd로 바꾸기
    private static final String url = "jdbc:mysql://localhost:3306/twitter"; 
    private static final String user = "root";
    private static final String pwd = "12345"; 

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url,user,pwd);
    }
}