import java.sql.*;
import java.util.Scanner;

public class twitter_test {

	public static void main(String[] args) {
			Scanner s = new Scanner(System.in);
			Connection con =null;
			try {
				Class.forName("com.mysql.cj.jdbc.Driver");
				String url = "jdbc:mysql://localhost:3306/twitter";
				String user = "root";
				String passwd = "12345";
				con = DriverManager.getConnection(url,user,passwd);
			}
			catch(Exception e)
			{
				e.getStackTrace();
			}
			
			String plid = "NULL";
			String flid = "NULL";
			String f2id = "NULL";
			int check = 0;
			Statement stmt = null;
			ResultSet rs = null;
			PreparedStatement pstm = null;
			
			try (Statement st = con.createStatement();
				     ResultSet r = st.executeQuery("SELECT DATABASE(), @@hostname, @@port, @@version, @@socket, @@datadir")) {
				  if (r.next()) {
				    System.out.printf("[APP] db=%s host=%s port=%s ver=%s socket=%s datadir=%s%n",
				      r.getString(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5), r.getString(6));
				  }
				  System.out.println("autocommit = " + con.getAutoCommit()); 
				

				}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
			
			try {
		    	while(true) {
			    	stmt = 	con.createStatement();
			    	
			    	int opt = 0;;
			    	String id = null;
			    	String pwd = null;
			    	
			    	if(check == 0)
			    	{
			    		System.out.println("Hi, insert number");
				    	System.out.println("0 -> sign up, 1 -> log in");
				    	opt = s.nextInt();
				    	check++;
			    	}
			    	
			    	
			    	
			    	// sign up
			    	if(opt == 0) {
			    		String s1 = null;
			    		System.out.println("sign up");
			    		System.out.println("Enter user id");
			    		id = s.next();
			    		
			    		
			    		stmt = con.createStatement();
			            String s2 = "select user_id from users where user_id = \"" + id + "\"";
			            rs = stmt.executeQuery(s2);
			             
			             if(rs.next()) {
			                 
			                 System.out.println("User name already exists. Please try again!");
			                 
			             }
			             else {
			                 System.out.println("Enter user password");
			                 pwd = s.next();
			                 s1 = "insert into users values('" + id + "', '" + pwd+ "','0' , '0')";
			                 
			                 pstm = con.prepareStatement(s1);
			                 pstm.executeUpdate();
			                 check = 0;
			             }
			    	}
			    	
			    	// log in
			         else if(opt == 1){
			        	 check--;
			        	 System.out.println("Input userid / password");
			             id = s.next();
			             pwd = s.next();
			             
			             stmt = con.createStatement();
			             String s1 = "select user_id from users where user_id = \"" + id + "\"and pwd =\"" + pwd+ "\"";
			             rs = stmt.executeQuery(s1);
			             
			             if(rs.next()) {
			                 System.out.println("Logged in!!");
			                 
			                 System.out.println("choose your activity");
			                 //int count;
			                 System.out.println("2-> post, 3->post like, 4->following, 5->see followers, 0->terminate");
			                 opt = s.nextInt();
			                 while(opt!= 0)
			                 {
			                	// write post
			                	 if(opt == 2) {
			                		 	String pid = null;
			                		 	String text = null;
			                		 	s1 = "null";
						        	 	int plike = 0;
						        	 
						        	 	String rub = s.nextLine();
						        	 	System.out.println("Enter your post text!!");
						        	 	text = s.nextLine();
						        	 	
						        	    int postCount = 0;
						        	    String postCountsql = "SELECT COUNT(*) AS cnt from posts";
						        	    try (PreparedStatement ps = con.prepareStatement(postCountsql)) {
						        	      
						        	        try (ResultSet rs1 = ps.executeQuery()) {
						        	            if (rs1.next()) {
						        	                postCount = rs1.getInt("cnt"); 
						        	            }
						        	        }
						        	    }
						        	    System.out.println("count = " + postCount);
						        	  
						        	    pid = "p"+ (postCount) ;
						        	    
						        	    s1 =  "INSERT INTO posts (post_id, `content`, writer_id, num_of_likes) VALUES (?, ?, ?, ?)";
						        	    try (PreparedStatement  pstm1 = con.prepareStatement(s1)) {
						        	        pstm1.setString(1, pid);
						        	        pstm1.setString(2, text);   
						        	        pstm1.setString(3, id);
						        	        pstm1.setInt(4, plike);     
						        	        System.out.println("post complete!");
						        	        pstm1.executeUpdate();
						        	       
						        	    }
						        	    catch(Exception e)
						        	    {
						        	    	e.printStackTrace();
						        	    }
						        	   
						        	    
						         }
						    	
						    	// post like
						         else if(opt == 3) {
						        	    String rub = s.nextLine();
						        	    String postid = null;
						        	    System.out.println("Enter you liked post id");
						        	    postid = s.nextLine();
						        	    String s3 = null;
						        	    String s4 = null;
						        	    
						        	    stmt = con.createStatement();
						        	    String s2 = "select l_id from post_like where liker_id = \"" + id + "\" and post_id = \"" + postid + "\"";
						        	    rs = stmt.executeQuery(s2);
						        	    
						        	    if(rs.next()) {
						        	        
						        	        System.out.println("Already liked post. Please try again!");
						        	        
						        	    }
						        	    else {
						        	    	
						        	    	int postLikeCount = 0;
						        	    	String postLikeCountsql = "SELECT COUNT(*) AS cnt from post_like";
								            try (PreparedStatement ps = con.prepareStatement(postLikeCountsql)) {
								       	        try (ResultSet rs1 = ps.executeQuery()) {
								       	            if (rs1.next()) {
								       	            	postLikeCount = rs1.getInt("cnt"); 
								       	            }
								       	        }
								       	    }
							        	    plid =  "pl"+(postLikeCount);
						        	    	
						        	        s3 = "insert into post_like values ( '" + plid + "', '" + postid + "', '" + id + "')";
						        	        s4 = "update posts set num_of_likes = num_of_likes + 1 where post_id = \"" + postid + "\"";
						        	        
						        	        pstm = con.prepareStatement(s3);
						        	        pstm.executeUpdate();
						        	        
						        	        pstm = con.prepareStatement(s4);
						        	        pstm.executeUpdate();
						        	    }
						        	    
						         }
						    	
						    	// follow
						         else if(opt == 4) {
						        	 	String rub = s.nextLine();
						        	    String u_id = null;
						        	    System.out.println("Input user ID to follow");
						        	    u_id = s.nextLine();
						        	    
						        	    if(u_id.equals(id))
						        	        System.out.println("Can't follow yourself");
						        	    else {
						        	        stmt = con.createStatement();
						        	        String s2 = "select following_id from following where following_id = \"" + u_id + "\"and user_id = \""+id+"\""; 
						        	        rs = stmt.executeQuery(s2);
						        	        if(rs.next()) {
						        	            System.out.println("Already followed the user. Please try again!");
						        	        }
						        	        else {
						        	        	int followCount = 0;
							        	    	String followCountsql = "SELECT COUNT(*) AS cnt from following";
									            try (PreparedStatement ps = con.prepareStatement(followCountsql)) {
									       	        try (ResultSet rs1 = ps.executeQuery()) {
									       	            if (rs1.next()) {
									       	            	followCount = rs1.getInt("cnt"); 
									       	            }
									       	        }
									       	    }
								        	    flid =  "f"+(followCount+1);
								        	    f2id = flid;
								        	    
						        	            String s3 = "insert into following values ( '" + flid + "', '" + id + "', '" + u_id + "')";
						        	            String s4 = "insert into follower values ( '" + f2id + "', '" + u_id + "', '" + id + "')";
						        	            pstm = con.prepareStatement(s3);
						        	            pstm.executeUpdate();
						        	            pstm = con.prepareStatement(s4);
						        	            pstm.executeUpdate();
						        	            System.out.println(id+" following "+u_id);
						        	        }
						        	        
						        	    }
						         }
			                	 //see follower
						         else if(opt == 5)
						         {
						        	 String sqlCnt = "SELECT COUNT(*) AS cnt FROM follower WHERE user_id = ?";
						        	 try (PreparedStatement ps = con.prepareStatement(sqlCnt)) {
						        	     ps.setString(1, id);
						        	     try (ResultSet rs1 = ps.executeQuery()) {
						        	         if (rs1.next()) {
						        	             int followerCount = rs1.getInt("cnt");
						        	             System.out.println(id + " have " + followerCount + " follower(s)");
						        	         }
						        	     }
						        	 }
						        	 
						        	 String sqlList = "SELECT follower_id FROM follower WHERE user_id = ? ORDER BY follower_id";
						        	 try (PreparedStatement ps = con.prepareStatement(sqlList)) {
						        	     ps.setString(1, id);
						        	     try (ResultSet rs1 = ps.executeQuery()) {
						        	         while (rs1.next()) {
						        	             System.out.println(rs1.getString("follower_id"));
						        	         }
						        	     }
						        	 }


						         }
			                	 //see following
						         else if(opt == 6)
						         {
						        	 String sqlCnt = "SELECT COUNT(*) AS cnt FROM following WHERE user_id = ?";
						        	 try (PreparedStatement ps = con.prepareStatement(sqlCnt)) {
						        	     ps.setString(1, id);
						        	     try (ResultSet rs1 = ps.executeQuery()) {
						        	         if (rs1.next()) {
						        	             int followingCount = rs1.getInt("cnt");
						        	             System.out.println(id + " have " + followingCount + " following(s)");
						        	         }
						        	     }
						        	 }
						        	 
						        	 String sqlList = "SELECT following_id FROM following WHERE user_id = ? ORDER BY following_id";
						        	 try (PreparedStatement ps = con.prepareStatement(sqlList)) {
						        	     ps.setString(1, id);
						        	     try (ResultSet rs1 = ps.executeQuery()) {
						        	         while (rs1.next()) {
						        	             System.out.println(rs1.getString("following_id"));
						        	         }
						        	     }
						        	 }
						         }
			                	 System.out.println("2-> post, 3->post like, 4->following, 5->see followers, 0->terminate");
			                	 opt = s.nextInt();
			                 }//사용자의 활동 추적하는 while문 블록 닫기
			                  
			             }
			             else {
			                 
			                 System.out.println("wrong id/password. Please log in again. ");
					         }// 로그인 성공후 if문 블록 닫기
			    	
			         }  //로그인 if문 블록 닫기
			    	
			    } // 첫번쨰 while 블록 닫기
		    } // try 블록 닫기
		    catch (SQLException e) {
		        e.printStackTrace();
		    } // catch 블록 추가
		    finally {
		        // ResultSet 닫기
		        try {
		            if (rs != null) rs.close();
		        } catch (SQLException e) {
		            e.printStackTrace();
		        }
		        // Statement 닫기
		        try {
		            if (stmt != null) stmt.close();
		        } catch (SQLException e) {
		            e.printStackTrace();
		        }
		        // PreparedStatement 닫기
		        try {
		            if (pstm != null) pstm.close();
		        } catch (SQLException e) {
		            e.printStackTrace();
		        }
		        // Connection 닫기
		        try {
		            if (con != null) con.close();
		        } catch (SQLException e) {
		            e.printStackTrace();
		        }
		        // Scanner 닫기
		        s.close();
		    }


	}

}
