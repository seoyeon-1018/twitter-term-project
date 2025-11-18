CREATE DATABASE IF NOT EXISTS twitter;
USE twitter;

create table user(
   user_id varchar(20) not null primary key,
   pwd varchar(20) not null,
   level int default 1,   -- 시작 레벨 1 
   exp int default 0,   -- 시작 경험치 0
   badge boolean default false, -- badge 소지 여부
   followers int default 0, -- 본인을 팔로우하는 사람 수
   followings int default 0 -- 본인이 팔로우하는 사람 수
);
--  혹자가 본인을 팔로우 및 본인 포스트에 좋아요를 받을 시 경험치 부여 **팔로우:50의 경험치 | 좋아요: 10의 경험치;
-- 누적 경험치 달성 시 레벨 업++; (but, 레벨 증가에 따라 필요 경험치도 비례하여 증가
--  레벨은 20레벨 까지 --> 최고 레벨 달성 시 파란베찌 부여

CREATE TABLE posts (
  post_id INT AUTO_INCREMENT PRIMARY KEY,     -- 게시글 고유 번호 | AUTO_INCREMENT을 통해 데이터베이스 자동관리 설정(1++) 
  content TEXT,                               -- 게시글 내용
  writer_id VARCHAR(20) NOT NULL,             -- 작성자 ID
  num_of_likes INT DEFAULT 0,                 -- 좋아요 수
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 작성 시간 자동 저장
  FOREIGN KEY (writer_id) REFERENCES user(user_id) -- 작성자 → user 테이블 참조
);

CREATE TABLE reserved_post (
  s_id INT AUTO_INCREMENT PRIMARY KEY,       -- 예약 게시글 ID
  writer_id VARCHAR(20) NOT NULL,            -- 작성자 ID
  content TEXT NOT NULL,                     -- 예약할 게시글 내용
  scheduled_time DATETIME NOT NULL,          -- 게시 예정 시간
  is_posted BOOLEAN DEFAULT FALSE,           -- 실제 업로드 여부 (FALSE: 미게시, TRUE: 게시됨)
  FOREIGN KEY (writer_id) REFERENCES user(user_id) -- 작성자 참조
);

CREATE TABLE comment (
  comment_id INT AUTO_INCREMENT PRIMARY KEY,     -- 댓글 고유 번호
  content TEXT,                                  -- 댓글 내용
  writer_id VARCHAR(20) NOT NULL,                -- 댓글 작성자
  post_id INT NOT NULL,                          -- 어떤 게시글에 달렸는지
  num_of_likes INT DEFAULT 0,                    -- 댓글 좋아요 수
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,-- 댓글 작성 시각
  FOREIGN KEY (writer_id) REFERENCES user(user_id),
  FOREIGN KEY (post_id) REFERENCES posts(post_id)
);

CREATE TABLE post_like (
  l_id INT AUTO_INCREMENT PRIMARY KEY,       -- 좋아요 고유 ID
  post_id INT NOT NULL,                      -- 어떤 게시글을 좋아요했는지
  liker_id VARCHAR(20) NOT NULL,             -- 좋아요 누른 사용자
  FOREIGN KEY (post_id) REFERENCES posts(post_id),
  FOREIGN KEY (liker_id) REFERENCES user(user_id)
);

CREATE TABLE comment_like (
  l_id INT AUTO_INCREMENT PRIMARY KEY,       -- 댓글 좋아요 고유 ID
  comment_id INT NOT NULL,                   -- 어떤 댓글을 좋아요했는지
  liker_id VARCHAR(20) NOT NULL,             -- 좋아요 누른 사용자
  FOREIGN KEY (comment_id) REFERENCES comment(comment_id),
  FOREIGN KEY (liker_id) REFERENCES user(user_id)
);

CREATE TABLE following (
  f_id INT AUTO_INCREMENT PRIMARY KEY,       -- 팔로잉 고유 ID
  user_id VARCHAR(20) NOT NULL,              -- 팔로우 당하는 사람
  follower_id VARCHAR(20) NOT NULL,          -- 팔로우 하는 사람
  FOREIGN KEY (user_id) REFERENCES user(user_id),
  FOREIGN KEY (follower_id) REFERENCES user(user_id)
);

CREATE TABLE follower (
  f_id INT AUTO_INCREMENT PRIMARY KEY,       -- 팔로워 고유 ID
  user_id VARCHAR(20) NOT NULL,              -- 나(팔로우 당하는 사람)
  follower_id VARCHAR(20) NOT NULL,          -- 나를 팔로우한 사람
  FOREIGN KEY (user_id) REFERENCES user(user_id),
  FOREIGN KEY (follower_id) REFERENCES user(user_id)
);


CREATE TABLE block (
  block_id VARCHAR(20) NOT NULL,             -- 차단된 사람
  by_block_id VARCHAR(20) NOT NULL,          -- 차단한 사람
  PRIMARY KEY (block_id, by_block_id),       -- 복합키로 한 번만 차단되도록
  FOREIGN KEY (block_id) REFERENCES user(user_id),
  FOREIGN KEY (by_block_id) REFERENCES user(user_id)
);

CREATE TABLE message ( 
	m_id INT AUTO_INCREMENT PRIMARY KEY,  --  메시지 고유 ID
	send_id VARCHAR(20) NOT NULL,    --  송신자
	receive_id VARCHAR(20) NOT NULL,   --  수신자
	content TEXT,  -- 내용 
	send_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	is_read BOOLEAN DEFAULT FALSE,  --  읽음 여부
	FOREIGN KEY (send_id) REFERENCES user(user_id),
	FOREIGN KEY (receive_id) REFERENCES user(user_id)
    );

CREATE TABLE post_tag (
  t_id INT AUTO_INCREMENT PRIMARY KEY,       -- 태그 고유 ID
  post_id INT NOT NULL,                      -- 어떤 게시글에 달렸는지
  tag VARCHAR(50) NOT NULL,                  -- 해시태그 이름 (예: 여행, 제주도)
  FOREIGN KEY (post_id) REFERENCES posts(post_id),
  UNIQUE (post_id, tag)                      -- 한 게시글에 같은 태그 중복 저장 방지
);

insert into user values('kim','12345',1,0,false,0,0);