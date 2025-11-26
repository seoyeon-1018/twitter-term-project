/*
 * 파일명: ProfileDAO.java
 * 목적: 사용자 프로필 정보에 대한 DB 접근(DAO) 기능 제공.
 *
 * 제공 메서드
 * 1) getBio(userId): user_profile 테이블에서 사용자의 상태 메시지(bio) 조회
 * 2) upsertBio(userId, bio): user_profile에 bio를 삽입 또는 갱신(ON DUPLICATE KEY UPDATE)
 * 3) countFollowers(userId): 해당 사용자를 팔로우하는 사람 수 조회
 * 4) countFollowings(userId): 해당 사용자가 팔로우 중인 사람 수 조회
 *
 * 의존 전제
 * - DBConn.getConnection(): JDBC Connection 제공
 * - 스키마:
 *    user_profile(user_id PK, bio TEXT, updated_at TIMESTAMP …)
 *    following(f_id AI, user_id, follower_id, UNIQUE(user_id, follower_id) …)
 *
 * 주의 사항
 * - 모든 Connection/PreparedStatement/ResultSet은 try-with-resources로 안전하게 해제.
 */

package myPackage;

import java.sql.*;

public class ProfileDAO {

    /** user_profile에서 해당 사용자의 bio(상태 메시지) 조회 */
    public static String getBio(String userId) throws SQLException {
        String sql = "SELECT bio FROM user_profile WHERE user_id=?";
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null; // 결과 없으면 null
            }
        }
    }

    /**
     * user_profile에 bio 삽입/갱신
     * - PK(user_id) 충돌 시 bio만 갱신(ON DUPLICATE KEY UPDATE)
     */
    public static void upsertBio(String userId, String bio) throws SQLException {
        String sql = """
            INSERT INTO user_profile(user_id, bio)
            VALUES(?, ?)
            ON DUPLICATE KEY UPDATE bio=VALUES(bio)
        """;
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, bio);
            ps.executeUpdate();
        }
    }

    /**
     * 해당 사용자를 팔로우하는 사람 수
   
     */
    public static int countFollowers(String userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM following WHERE user_id=?"; 
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    /** 해당 사용자가 팔로우 중인 사람 수 */
    public static int countFollowings(String userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM following WHERE follower_id=?";
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }
}
