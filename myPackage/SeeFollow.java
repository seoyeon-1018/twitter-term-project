package myPackage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SeeFollow
 * -----------------------------------------------------------------------------
 * 목적
 *  - 팔로우 관계 테이블(following)을 조회하여 특정 사용자의 팔로워/팔로잉 목록과
 *    그 개수를 제공하는 순수 DAO 유틸 클래스.
 *
 * 동작 개요
 *  - followers:  following.user_id   = 대상 사용자를 팔로우하는 사람들(follower_id) 조회
 *  - followings: following.follower_id = 대상 사용자가 팔로우 중인 사람들(user_id) 조회
 *  - 각 조회는 오름차순 정렬, 목록/페이지네이션/카운트 버전 제공
 *
 * 사용처 예
 *  - PersonalBoardPanel 헤더 영역의 "Followers / Following" 숫자 및 목록 다이얼로그
 *  - FollowRecommendPanel 등에서 팔로우 상태 갱신 후 카운트/목록 재로드
 *
 * 주의
 *  - 스키마는 following 단일 테이블을 기준으로 하며,
 *    컬럼: (user_id, follower_id) 를 사용한다.
 *  - Connection은 DBConn.getConnection() (AutoCloseable)로 획득하며,
 *    본 클래스는 트랜잭션을 직접 다루지 않고 단순 조회만 수행한다.
 */
public class SeeFollow {

    /** 대상 사용자를 팔로우하는 모든 계정 ID 목록 (오름차순) */
    public static List<String> getFollowers(String userId) throws SQLException {
        String sql = "SELECT follower_id FROM following WHERE user_id=? ORDER BY follower_id ASC";
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) out.add(rs.getString(1));
                return out;
            }
        }
    }

    /** 대상 사용자의 팔로워 목록(페이지네이션) */
    public static List<String> getFollowers(String userId, int limit, int offset) throws SQLException {
        String sql = """
            SELECT follower_id
            FROM following
            WHERE user_id=?
            ORDER BY follower_id ASC
            LIMIT ? OFFSET ?
        """;
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setInt(2, Math.max(0, limit));
            ps.setInt(3, Math.max(0, offset));
            try (ResultSet rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) out.add(rs.getString(1));
                return out;
            }
        }
    }

    /** 대상 사용자가 팔로우 중인 모든 계정 ID 목록 (오름차순) */
    public static List<String> getFollowings(String userId) throws SQLException {
        String sql = "SELECT user_id FROM following WHERE follower_id=? ORDER BY user_id ASC";
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) out.add(rs.getString(1));
                return out;
            }
        }
    }

    /** 대상 사용자의 팔로잉 목록(페이지네이션) */
    public static List<String> getFollowings(String userId, int limit, int offset) throws SQLException {
        String sql = """
            SELECT user_id
            FROM following
            WHERE follower_id=?
            ORDER BY user_id ASC
            LIMIT ? OFFSET ?
        """;
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setInt(2, Math.max(0, limit));
            ps.setInt(3, Math.max(0, offset));
            try (ResultSet rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) out.add(rs.getString(1));
                return out;
            }
        }
    }

    /** 팔로워 수 카운트 */
    public static int countFollowers(String userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM following WHERE user_id=?";
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** 팔로잉 수 카운트 */
    public static int countFollowings(String userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM following WHERE follower_id=?";
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

}
