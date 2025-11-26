/*
 * 파일명: FollowRecommendPanel.java
 * 역할: 메인 화면의 좌측 사이드에 "당신을 위한 추천" 사용자 목록을 보여주는 패널
 *
 * 동작 개요
 * 1) 데이터 소스
 *    - following 테이블을 기반으로 2-홉 추천(내가 팔로우한 사람이 팔로우하는 사람) 우선 노출
 *    - 2-홉 결과가 없을 때 → 팔로워 수 상위 사용자(인기) → 무작위 사용자 순으로 대체 추천
 *    - 항상 본인과 이미 팔로우한 사용자는 제외
 *
 * 2) UI 구성
 *    - 상단 제목 라벨("당신을 위한 추천")
 *    - 스크롤 가능한 목록(listPanel)
 *    - 각 항목: 사용자 아이디 라벨 + [Open] (개인 보드 열기) + [Follow/Unfollow] 토글 버튼
 *
 * 3) 상호작용
 *    - [Open]: TwitterApp.openPersonalBoard(targetId) 호출로 해당 유저의 보드 전환
 *    - [Follow]/[Unfollow]:
 *         · Follow.isFollowing(...)로 현재 관계 확인
 *         · Follow.follow(...) 또는 Follow.unfollow(...) 실행
 *         · 성공 시: 내 보드/상대 보드의 상단 헤더(팔로워/팔로잉, 레벨 등) 즉시 갱신
 *           → app.refreshPersonalBoardHeader(내아이디/상대아이디) 호출
 *         · 버튼 라벨 토글 및 추천 목록을 reload()로 재구성
 *
 * 4) 예외/빈 상태 처리
 *    - 로그인 전: "로그인 후 이용하세요." 안내
 *    - 추천 결과 없음: 친절한 빈 상태 뷰 + [새로고침] 버튼
 *
 */

package myPackage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 왼쪽 사이드 “추천 사용자” 패널
 * - 스크롤 목록
 * - 각 항목: 아이디 라벨, [Open] 보드 이동, [Follow]/[Unfollow] 토글
 * - 팔로우/언팔 성공 시 즉시 UI 반영 + 추천목록 재갱신
 * - 비어 있을 때 인기/랜덤 대체 추천 및 새로고침 버튼 제공
 */
public class FollowRecommendPanel extends JPanel {

    private final TwitterApp app;
    private final JPanel listPanel = new JPanel();
    private final JLabel title;

    public FollowRecommendPanel(TwitterApp app) {
        this.app = app;
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220,220,220)),
                new EmptyBorder(8, 8, 8, 8)
        ));

        // 상단 제목
        title = new JLabel("당신을 위한 추천");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        add(title, BorderLayout.NORTH);

        // 스크롤 가능한 목록 컨테이너
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane sp = new JScrollPane(
                listPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        add(sp, BorderLayout.CENTER);
    }

    /** 추천목록 새로고침 */
    public void reload() {
        listPanel.removeAll();
        String me = app.getCurrentUserId();
        if (me == null || me.isBlank()) {
            // 로그인 안 된 상태
            listPanel.add(new JLabel("로그인 후 이용하세요."));
            refreshUI();
            return;
        }

        // 추천 데이터 취합 (2-홉 → 인기 → 랜덤)
        List<String> recs = fetchRecommendationsTiered(me);

        if (recs.isEmpty()) {
            // 빈 상태 뷰 + [새로고침]
            JPanel empty = new JPanel(new BorderLayout(6, 6));
            empty.add(new JLabel("지금은 추천할 사용자가 없어요."), BorderLayout.CENTER);
            JButton refresh = new JButton("새로고침");
            refresh.addActionListener(e -> reload());
            JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            south.add(refresh);
            empty.add(south, BorderLayout.SOUTH);
            listPanel.add(empty);
        } else {
            // 추천 유저 행 구성
            for (String uid : recs) {
                listPanel.add(buildRow(uid));
                listPanel.add(Box.createVerticalStrut(6));
            }
        }
        refreshUI();
    }

    /** 1) 2-홉 추천 → 2) 인기 사용자 → 3) 랜덤 사용자 (본인/이미 팔로우 제외) */
    private List<String> fetchRecommendationsTiered(String userId) {
        // 내가 팔로우한 사람이 팔로우하는 사람을 먼저 추천
        List<String> out = fetchTwoHop(userId, 10);
        if (!out.isEmpty()) return out;

        // 없으면 팔로워 수 많은 사용자
        out = fetchPopularUsers(userId, 10);
        if (!out.isEmpty()) return out;

        // 마지막으로 무작위 사용자
        return fetchRandomUsers(userId, 10);
    }

    /** 기존 2-홉 추천 */
    private List<String> fetchTwoHop(String userId, int limit) {
        List<String> out = new ArrayList<>();
        String sql =
            "SELECT DISTINCT f2.user_id AS recommend " +
            "FROM following f1 " +
            "JOIN following f2 ON f1.user_id = f2.follower_id " +
            "WHERE f1.follower_id = ? " +
            "  AND f2.user_id <> ? " +
            "  AND f2.user_id NOT IN (SELECT user_id FROM following WHERE follower_id = ?) " +
            "LIMIT ?";
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            ps.setString(3, userId);
            ps.setInt(4, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString("recommend"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    /** 팔로워 수 많은 사용자(본인/이미 팔로우 제외) */
    private List<String> fetchPopularUsers(String userId, int limit) {
        List<String> out = new ArrayList<>();
        String sql =
            "SELECT u.user_id " +
            "FROM user u " +
            "WHERE u.user_id <> ? " +
            "  AND u.user_id NOT IN (SELECT user_id FROM following WHERE follower_id = ?) " +
            "ORDER BY u.followers DESC " +
            "LIMIT ?";
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    /** 무작위 사용자(본인/이미 팔로우 제외) – MySQL */
    private List<String> fetchRandomUsers(String userId, int limit) {
        List<String> out = new ArrayList<>();
        String sql =
            "SELECT u.user_id " +
            "FROM user u " +
            "WHERE u.user_id <> ? " +
            "  AND u.user_id NOT IN (SELECT user_id FROM following WHERE follower_id = ?) " +
            "ORDER BY RAND() " +
            "LIMIT ?";
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    /** 추천 한 줄 UI: [아이디]  [Open]  [Follow/Unfollow] */
    private JComponent buildRow(String targetId) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(235,235,235)),
                new EmptyBorder(6,6,6,6)
        ));

        // 사용자 아이디 라벨
        JLabel name = new JLabel(targetId);
        row.add(name, BorderLayout.CENTER);

        // 우측 액션 영역
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton openBtn = new JButton("Open");
        JButton followBtn = new JButton("Follow");

        // 현재 팔로우 상태에 맞춰 초기 라벨 설정
        String me = app.getCurrentUserId();
        boolean isFollowing = false;
        try { isFollowing = Follow.isFollowing(me, targetId); } catch (Exception ignored) {}
        followBtn.setText(isFollowing ? "Unfollow" : "Follow");

        actions.add(openBtn);
        actions.add(followBtn);
        row.add(actions, BorderLayout.EAST);

        // [Open] 클릭 시 개인 보드 열기
        openBtn.addActionListener(e -> app.openPersonalBoard(targetId));

        // [Follow]/[Unfollow] 토글
        followBtn.addActionListener(e -> {
            String cur = app.getCurrentUserId();
            if (cur == null || cur.isBlank()) {
                JOptionPane.showMessageDialog(this, "로그인이 필요합니다.", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (cur.equals(targetId)) {
                JOptionPane.showMessageDialog(this, "자기 자신은 팔로우할 수 없습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // 현재 상태 조회
            boolean nowFollowing = false;
            try { nowFollowing = Follow.isFollowing(cur, targetId); } catch (Exception ignored) {}

            boolean ok = false;
            try {
                ok = nowFollowing ? Follow.unfollow(cur, targetId)
                                  : Follow.follow(cur, targetId);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (!ok) {
                JOptionPane.showMessageDialog(this,
                        nowFollowing ? "언팔로우 실패" : "팔로우 실패",
                        "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 성공 시: 내 보드/상대 보드 숫자·레벨 등 상단 헤더 즉시 갱신
            app.refreshPersonalBoardHeader(cur);       // 내 보드(팔로잉 수)
            app.refreshPersonalBoardHeader(targetId);  // 상대 보드(팔로워 수)

            // 버튼 라벨 토글
            followBtn.setText(nowFollowing ? "Follow" : "Unfollow");
            // 추천 목록 재구성(변경 반영)
            reload();
        });

        return row;
    }

    /** 스크롤 영역 리레이아웃/리페인트 */
    private void refreshUI() {
        listPanel.revalidate();
        listPanel.repaint();
    }
}
