
/*
package myPackage;

import java.sql.SQLException;
import java.util.Scanner;

public class Main {

    private static String user = null; //The corresponding user ID

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("-------------------- OPTION --------------------");
            System.out.println("0. 회원가입 | 1. 로그인");
            System.out.println("2. 포스트 업로드(+해시태그) | 3. 포스트 좋아요");
            System.out.println("4. 댓글 작성 | 5. 댓글 좋아요");
            System.out.println("6. 팔로우 | 7. 팔로우 추천 | 8. 팔로워 보기");
            System.out.println("9. 해시태그 검색 | 10. 예약 포스트 등록");
            System.out.println("11. 메시지 | 12. 차단");
            System.out.println("13. 종료");
            System.out.println("---------------------------------------------------");

            System.out.print("Select option: ");
            int opt = sc.nextInt();
            sc.nextLine(); 

            try {
                switch (opt) {

                    //Member join
                    case 0: {
                        System.out.print("Enter ID: ");
                        String id = sc.next();
                        System.out.print("Enter password: ");
                        String pwd = sc.next();
                        MemberJoin.signUp(id, pwd);
                        break;
                    }

                    //Log in
                    case 1: {
                        System.out.print("Enter ID: ");
                        String id = sc.next();
                        System.out.print("Enter password: ");
                        String pwd = sc.next();

                        boolean isLogin = LogIn.login(id, pwd);
                        if (isLogin) {
                            user = id;
                        }
                        break;
                    }

                    // You need to log in to use it
                    case 2: case 3: case 4: case 5:
                    case 6: case 7: case 8: case 9:
                    case 10: case 11: case 12:

                        if (user == null) {
                            System.out.println("You need to log in");
                            break;
                        }

                        //Post
                        if (opt == 2) {
                            System.out.print("Content: ");
                            String content = sc.nextLine();
                            System.out.print("Hashtags (comma ,): ");
                            String tags = sc.nextLine();

                            int postId = UploadPost.uploadPost(user, content);
                            UploadPost.createTag(postId, tags);
                            System.out.println("Post & Tags saved.");
                        }

                        //Post Like
                        if (opt == 3) {
                            System.out.print("Post ID: ");
                            int postId = sc.nextInt();
                            PostLike.likePost(postId, user);
                        }

                        //Comment
                        if (opt == 4) {
                            System.out.print("Post ID: ");
                            int pId = sc.nextInt();
                            sc.nextLine(); 
                            System.out.print("Comment content: ");
                            String cmt = sc.nextLine();

                            // Comment 클래스 기준: write(int postId, String writer, String content)
                            Comment.write(pId, user, cmt);
                            System.out.println("Comment uploaded.");
                        }

                        //Comment Like
                        if (opt == 5) {
                            System.out.print("Comment ID: ");
                            int commentId = sc.nextInt();
                            CommentLike.likeComment(commentId, user);
                        }

                        //Follow
                        if (opt == 6) {
                            System.out.print("Follow target: ");
                            String target = sc.next();
                            Follow.follow(user, target);
                        }

                        //Follow Recommend
                        if (opt == 7) {
                            FollowRecommend.showRecommend(user); 
                        }

                        //See Follow
                        if (opt == 8) {
                            SeeFollow.seefollow(user); 
                        }

                        //Hash Tag
                        if (opt == 9) {
                            System.out.print("Tag: ");
                            String tagSearch = sc.next();
                            Hashtag.search(tagSearch); 
                        }

                        //Reserved Post
                        if (opt == 10) {
                            System.out.print("Content: ");
                            String rContent = sc.nextLine();
                            System.out.print("Time (YYYY-MM-DD HH:MM:SS): ");
                            String time = sc.nextLine();
                            ReservedPost.reserve(user, rContent, time);
                            ReservedPost.showReservedPosts(user);
                        }

                        //Message
                        if (opt == 11) {
                            System.out.print("Receiver ID: ");
                            String recv = sc.next();
                            sc.nextLine();
                            System.out.print("Message: ");
                            String msg = sc.nextLine();
                            Message.sendMessage(user, recv, msg);
                        }

                        //Block
                        if (opt == 12) {
                            System.out.print("Block target: ");
                            String blockTarget = sc.next();
                            Block.block(user, blockTarget);
                        }

                        break;

                    //Terminate
                    case 13:
                        System.out.println("Finished");
                        return;

                    default: //Invalid Parameter
                        System.out.println("Invalid option.");
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
*/