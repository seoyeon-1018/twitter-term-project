# twitter-term-project
login, signup,비밀번호 구현

메인페이지
피드 구현 (최근 작성된 10개 피드가 보여짐)
피드 안 like와 comment구현 완료, comment안에 comment like 구현완료, 받은 좋아요 수 및 작성자 시간 확인 가능
팔로우 추천 구현 유저가 있으면 뜨고 해당 유저 팔로우 및 보드로 이동 가능
post 구현(내용 작성후 post버튼 눌러 post, 예약 기능 체크 및 시간을 통해 예약 기능 구현)
search 앞 textfield를 통해 다른 사용자를 찾을 수 있음(다른 이용자의 보드가 보임), #을 통해 특정 해쉬태그가 있는 피드 찾을 수 있음
myboard를 통해 보드로 이동가능

보드
사용자id, 레벨, 팔로워 팔로잉 수 확인가능, 팔로우 팔로잉 목록 확인가능, 간단한 상태메세지 작성가능
해당 사용자가 작성한 피드가 보임(like comment등 메인페이지 피드의 모든 기능 구현)

메세지와 차단은 구현x

twitter.sql 및 myPackage 다운로드
-----------------------------------------------------------------------------------
mysql twitter 한번 실행 필요 만약 미리 존재한 데이터가 있을 경우 

USE twitter;

-- FK 잠시 비활성화
SET FOREIGN_KEY_CHECKS = 0;

-- 자식 테이블부터 비우기
TRUNCATE TABLE comment_like;
TRUNCATE TABLE post_like;
TRUNCATE TABLE comment;
TRUNCATE TABLE post_tag;
TRUNCATE TABLE reserved_post;
TRUNCATE TABLE message;
TRUNCATE TABLE following;
TRUNCATE TABLE block;
TRUNCATE TABLE user_profile;

-- 부모 테이블
TRUNCATE TABLE posts;
TRUNCATE TABLE `user`;

-- FK 다시 활성화
SET FOREIGN_KEY_CHECKS = 1;

-- 확인용
SELECT 
  (SELECT COUNT(*) FROM `user`)          AS cnt_user,
  (SELECT COUNT(*) FROM posts)           AS cnt_posts,
  (SELECT COUNT(*) FROM comment)         AS cnt_comment,
  (SELECT COUNT(*) FROM post_like)       AS cnt_post_like,
  (SELECT COUNT(*) FROM comment_like)    AS cnt_comment_like,
  (SELECT COUNT(*) FROM following)       AS cnt_following,
  (SELECT COUNT(*) FROM block)           AS cnt_block,
  (SELECT COUNT(*) FROM message)         AS cnt_message,
  (SELECT COUNT(*) FROM post_tag)        AS cnt_post_tag,
  (SELECT COUNT(*) FROM reserved_post)   AS cnt_reserved,
  (SELECT COUNT(*) FROM user_profile)    AS cnt_profile;

을 통해 모든 데이터를 삭제하고 삭제된 것 확인 후
다시 mysql에서 twitter 실행 (한번만 실행)
DBConn.java파일을 본인 환경에 맞게 수정
TwitterApp 실행 후 아이디: kim, 비밀번호:12345 (테스트아이디)로 로그인 되나 확인

