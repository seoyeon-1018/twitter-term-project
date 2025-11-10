/*drop database twitter;*/
CREATE DATABASE IF NOT EXISTS twitter;
USE twitter;


CREATE TABLE IF NOT EXISTS users(
user_id varchar(10) not null primary key,
pwd varchar(15) not null,
/*token varchar(10) unique,*/
follower_num int,
following_num int
);


CREATE TABLE IF NOT EXISTS posts(
post_id varchar(15) not null primary key,
content text,
writer_id VARCHAR(10) NOT NULL,
num_of_likes int,
/*post_time date,*/
FOREIGN KEY (writer_id) REFERENCES users(user_id)
);
/*
ALTER TABLE posts MODIFY post_id INT NOT NULL AUTO_INCREMENT;
지우기 기능 추가시 사용 후 jdbc 코드 수정 필요
*/
CREATE TABLE IF NOT EXISTS comment(
comment_id varchar(15) not null primary key,
content text,
writer_id varchar(10) not null,
post_id varchar(15) not null,
num_of_likes int,
comment_time date,
foreign key (writer_id) references users(user_id),
foreign key (post_id) references posts(post_id) on delete cascade
);

CREATE TABLE IF NOT EXISTS post_like(
l_id varchar(15) not null primary key,
post_id varchar(15) not null,
liker_id varchar(10) not null, 
foreign key (post_id) references posts(post_id) on delete cascade,
foreign key (liker_id) references users(user_id)
);

CREATE TABLE IF NOT EXISTS comment_like(
l_id varchar(15) not null primary key,
comment_id varchar(15) not null,
liker_id varchar(10) not null,
foreign key (comment_id) references comment(comment_id),
foreign key (liker_id) references users(user_id)
);


CREATE TABLE IF NOT EXISTS follower(
f_id varchar(10) primary key not null,
user_id varchar(10) not null,
follower_id varchar(10) not null,
foreign key (user_id) references users(user_id),
foreign key (follower_id) references users(user_id)
);

CREATE TABLE IF NOT EXISTS following(
f_id varchar(10) not null primary key,
user_id varchar(10) not null,
following_id varchar(10) not null,
foreign key (user_id) references users(user_id),
foreign key (following_id) references users(user_id)
);


/*
insert into users values('harry','123@%',0,0);
insert into users values('hongsim','12345',0,0);
insert into posts values('p1','hello world','hongsim','10');
*/






SELECT DATABASE(), @@hostname, @@port, @@version, @@socket, @@datadir;

