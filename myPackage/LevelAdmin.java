package myPackage;
import java.sql.*;

public class LevelAdmin { //User Level Management Classes
	public static void info(Connection con, String userId, int userExp) throws SQLException{
        String query = "select level, exp from user where user_id=?";
        int level = 1; //Initialize to Start Level 1
        int exp = 0; // Initialize to starting experience value 0

        PreparedStatement ps = null;
        ResultSet rs = null;

        try{
            ps = con.prepareStatement(query);
            ps.setString(1,userId);
            rs = ps.executeQuery();

            if(rs.next()){
                level = rs.getInt("level");
                exp = rs.getInt("exp");
            }
            else{
                System.out.println("User not found");
                return;
            }
        } finally{
            if(rs!=null) rs.close();
            if(ps!=null) ps.close();
        } 
        
        exp+=userExp; //Add a exp
        boolean up = false;
        int requiredExp = (int)(level*100*1.5); //Downcast to int
        while(exp>=requiredExp){
            exp-=requiredExp; //Exhaustion of experience for levelling up
            level++; //level up
            up = true;

            if(level>20){ //When a user reach level 20
                level = 20;
                exp = 0;
                break;
            }
            requiredExp = (int)(level*100*1.5); //1.5: Required experience weight as the level increases

        if(up){
            System.out.println("New Level: " + level);
        }

        if(level==20){ //Get a bedge
            String badgeQuery = "update user set badge=true where user_id=?";
            PreparedStatement bq = null;
            try{
                bq = con.prepareStatement(badgeQuery);
                bq.setString(1,userId);
                bq.executeUpdate();
            } finally{
                if(bq!=null) bq.close();
            }
        }
        
        String levelQuery = "update user set level=?,exp=? where user_id=?";
        PreparedStatement lq = null;

        try{
            lq = con.prepareStatement(levelQuery);
            lq.setInt(1,level);
            lq.setInt(2,exp);
            lq.setString(3,userId);
            lq.executeUpdate();
        } finally{
            if(lq!=null) lq.close();
        }
        System.out.println("Gain exp: "+userExp);
        System.out.println("Now Level: " +level+", Exp: "+exp);
        }
}

}