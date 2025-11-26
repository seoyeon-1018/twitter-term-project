/*
 * 파일명: PersonalBoardPanel.java
 * 목적: 특정 사용자(프로필 주인)의 개인 보드 화면을 구성/표시하는 패널.
 *
 * 화면 구성
 * - 상단 Header:
 *   · 좌측: [← Back] 버튼 (메인 페이지로 복귀)
 *   · 중앙: 아바타, 사용자명, 레벨/경험치(Label), 팔로워/팔로잉 수(Label), 목록 열기 버튼(팔로워/팔로잉)
 *   · 우측: 상태메시지(Bio) 편집 영역(프로필 주인 본인에게만 편집 허용)
 *   · (타 사용자 보드일 때) Follow/Unfollow 토글 버튼
 * - 중앙: 해당 사용자가 작성한 게시글 목록(스크롤) — FeedCardFactory를 재사용하여 카드 UI 표시
 *
 * 주요 기능
 * - 레벨/경험치 표시: user(level, exp)를 조회하여 "Lv. L • EXP e/r" 형식으로 표시 (requiredExp = level*100*1.5)
 * - 팔로워/팔로잉 표시 및 목록 다이얼로그: SeeFollow DAO(가정)로 count/목록 조회
 * - Follow/Unfollow: Follow DAO를 통해 팔로우 상태 토글, 성공 시 헤더 전체(레벨 포함) 갱신
 * - 상태 메시지(Bio) 저장: ProfileDAO.upsertBio 호출
 * - 게시글 목록: selectUserPosts() → FeedCardFactory.createPostCard()로 렌더링
 *
 * 상호작용/연동
 * - TwitterApp: 페이지 전환, 현재 로그인 사용자ID, 다른 보드 열기, 커넥션 등 외부 협력
 * - FeedCardFactory: 게시글 카드 UI(좋아요/댓글/댓글 좋아요), 경험치 변화 시 app.refreshPersonalBoardHeader(ownerId)로 헤더 갱신
 * - Follow/SeeFollow/ProfileDAO: 팔로우/카운트/프로필 텍스트 관련 DB 접근
 *
 * - 경험치 정책(팔로우/좋아요/댓글 등)은 LevelAdmin에서 처리되며, 변화 시 본 패널의 refreshSocialHeader()가 호출되어 최신 상태가 반영됨.
 */

package myPackage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PersonalBoardPanel extends JPanel {

    private final TwitterApp app;
    private final String ownerId;     // 프로필 주인
    
    // ---- 상태 메시지 ----
    private JTextArea bioArea;
    private JButton saveBioBtn;

    // ---- 레벨 ----
    private JLabel levelLabel;        // ← #1: 레벨 라벨 추가

    // ---- 팔로워/팔로잉 UI ----
    private JLabel socialLabel;         // "Followers: X • Followings: Y"
    private JButton followersBtn;       // 목록 보기
    private JButton followingsBtn;      // 목록 보기
    private JButton followToggleBtn;    // 상대 보드에서 Follow/Unfollow

    // ---- 피드 ----
    private final JPanel listPanel = new JPanel();

    public PersonalBoardPanel(TwitterApp app, String ownerId) {
        this.app = app;
        this.ownerId = ownerId;
        setLayout(new BorderLayout(0,10));

        // ===== Header (프로필) =====
        add(buildHeader(), BorderLayout.NORTH);

        // ===== User 전용 Feed =====
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(
                listPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        add(scroll, BorderLayout.CENTER);

        reloadPosts();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10,0));
        header.setBorder(new EmptyBorder(10,10,10,10));

        // ---------- 상단 라인: Back(좌) / Follow 토글(우) ----------
        JPanel northLine = new JPanel(new BorderLayout());
        JButton backBtn = new JButton("← Back");
        backBtn.setFocusable(false);
        backBtn.setToolTipText("메인으로 돌아가기");
        backBtn.addActionListener(e -> app.showPage(TwitterApp.PAGE_MAIN));
        northLine.add(backBtn, BorderLayout.WEST);

        followToggleBtn = new JButton("Follow");
        JPanel followWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        String current = app.getCurrentUserId();
        boolean isOwner = (current != null && current.equals(ownerId));
        if (!isOwner) {
            safeRefreshFollowToggle();
            followWrap.add(followToggleBtn);
        }
        northLine.add(followWrap, BorderLayout.EAST);
        header.add(northLine, BorderLayout.NORTH);

        // ESC로 Back
        InputMap im = header.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = header.getActionMap();
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "GO_BACK");
        am.put("GO_BACK", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                app.showPage(TwitterApp.PAGE_MAIN);
            }
        });

        // ---------- 아바타 ----------
        JLabel avatar = new JLabel("\uD83D\uDC64", SwingConstants.CENTER);
        avatar.setPreferredSize(new Dimension(64,64));
        avatar.setFont(avatar.getFont().deriveFont(48f));
        header.add(avatar, BorderLayout.WEST);

        // ---------- 가운데: 이름, 레벨, 팔로워/팔로잉 ----------
        JPanel mid = new JPanel();
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(ownerId + "  (profile)");
        name.setFont(name.getFont().deriveFont(Font.BOLD, 16f));
        mid.add(name);
        mid.add(Box.createVerticalStrut(4));

        // #1: 레벨 라벨 생성 & 추가
        levelLabel = new JLabel();
        mid.add(levelLabel);
        mid.add(Box.createVerticalStrut(4));

        // 상단 라벨 (숫자)
        socialLabel = new JLabel();
        mid.add(socialLabel);
        mid.add(Box.createVerticalStrut(6));

        // 목록 보기 버튼 2개
        JPanel socialBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        followersBtn = new JButton("Open Followers");
        followingsBtn = new JButton("Open Following");
        socialBtns.add(followersBtn);
        socialBtns.add(followingsBtn);
        mid.add(socialBtns);

        header.add(mid, BorderLayout.CENTER);

        // ---------- 오른쪽: 상태메시지(본인만 편집) ----------
        JPanel right = new JPanel(new BorderLayout(5,5));
        bioArea = new JTextArea(3, 28);
        bioArea.setLineWrap(true);
        bioArea.setWrapStyleWord(true);
        JScrollPane bioScroll = new JScrollPane(bioArea);
        saveBioBtn = new JButton("Save");

        bioArea.setEditable(isOwner);
        saveBioBtn.setEnabled(isOwner);

        try {
            String bio = ProfileDAO.getBio(ownerId);
            bioArea.setText(bio == null ? "" : bio);
        } catch (Exception e) {
            bioArea.setText("");
        }

        saveBioBtn.addActionListener(ev -> {
            try {
                ProfileDAO.upsertBio(ownerId, bioArea.getText());
                JOptionPane.showMessageDialog(this, "Saved.", "Info", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Save failed", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        right.add(new JLabel("Status message:"), BorderLayout.NORTH);
        right.add(bioScroll, BorderLayout.CENTER);
        right.add(saveBioBtn, BorderLayout.SOUTH);
        header.add(right, BorderLayout.EAST);

        // ---------- 이벤트: 팔로워/팔로잉 목록 ----------
        followersBtn.addActionListener(e -> openListDialog(
                "Followers of @" + ownerId,
                safeList(() -> SeeFollow.getFollowers(ownerId))
        ));
        followingsBtn.addActionListener(e -> openListDialog(
                "Following by @" + ownerId,
                safeList(() -> SeeFollow.getFollowings(ownerId))
        ));

        // ---------- 이벤트: 팔로우/언팔 토글 ----------
        followToggleBtn.addActionListener(e -> {
            String me = app.getCurrentUserId();
            if (me == null || me.isBlank()) {
                JOptionPane.showMessageDialog(this, "로그인이 필요합니다.", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (me.equals(ownerId)) {
                JOptionPane.showMessageDialog(this, "자기 자신은 팔로우할 수 없습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            boolean followingNow = false;
            try {
                followingNow = Follow.isFollowing(me, ownerId);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            boolean ok = false;
            try {
                ok = followingNow ? Follow.unfollow(me, ownerId)
                                  : Follow.follow(me, ownerId);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            if (!ok) {
                JOptionPane.showMessageDialog(this,
                        followingNow ? "언팔로우 실패" : "팔로우 실패",
                        "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 성공 → 헤더 전체 갱신(레벨 포함)
            refreshSocialHeader();
        });

        // 최초 헤더 갱신(레벨+팔로우)
        refreshSocialHeader();

        return header;
    }

    /** #2: 레벨/경험치 라벨 갱신 */
    private void refreshLevel() {
        String sql = "SELECT level, exp FROM user WHERE user_id=?";
        int level = 1, exp = 0;
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    level = rs.getInt("level");
                    exp   = rs.getInt("exp");
                }
            }
        } catch (SQLException ignored) {}
        int required = Math.max(0, (int) Math.round(level * 100 * 1.5));
        levelLabel.setText("Lv. " + level + "  •  EXP " + exp + "/" + required);
    }

    /** 숫자 라벨(Followers / Following) 갱신 */
    private void refreshSocialCounts() {
        int followers = 0, followings = 0;
        try { followers = SeeFollow.countFollowers(ownerId); } catch (Exception ignored) {}
        try { followings = SeeFollow.countFollowings(ownerId); } catch (Exception ignored) {}

        socialLabel.setText("Followers: " + followers + "   •   Followings: " + followings);
        followersBtn.setText("Open Followers (" + followers + ")");
        followingsBtn.setText("Open Following (" + followings + ")");
    }

    /** 외부/내부에서 호출하는 헤더 전체 리프레시(레벨+팔로우+토글) */
    public void refreshSocialHeader() {
        refreshLevel();           // ← 레벨
        refreshSocialCounts();    // ← 팔로워/팔로잉
        safeRefreshFollowToggle();
        revalidate(); repaint();
    }

    /** Follow/Unfollow 버튼 라벨만 안전하게 갱신 */
    private void safeRefreshFollowToggle() {
        String me = app.getCurrentUserId();
        if (me == null || me.isBlank() || me.equals(ownerId)) {
            followToggleBtn.setVisible(false);
            return;
        }
        boolean following = false;
        try { following = Follow.isFollowing(me, ownerId); } catch (Exception ignored) {}
        followToggleBtn.setText(following ? "Unfollow" : "Follow");
        followToggleBtn.setVisible(true);
    }

    /** 목록 다이얼로그: 아이디 리스트 표시 + 선택해서 보드 열기 */
    private void openListDialog(String title, List<String> users) {
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.MODELESS);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        d.setSize(360, 480);
        d.setLocationRelativeTo(this);

        DefaultListModel<String> model = new DefaultListModel<>();
        if (users != null) users.forEach(model::addElement);

        JList<String> list = new JList<>(model);
        d.add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton openBoard = new JButton("Open board");
        south.add(openBoard);
        d.add(south, BorderLayout.SOUTH);

        openBoard.addActionListener(e -> {
            String sel = list.getSelectedValue();
            if (sel != null) {
                try {
                    app.openPersonalBoard(sel);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "보드 열기 기능이 연결되어 있지 않습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        d.setVisible(true);
    }

    /** ownerId가 작성한 최근 글만 */
    private void reloadPosts() {
        listPanel.removeAll();
        try (Connection con = DBConn.getConnection()) {
            for (PostDTO p : selectUserPosts(con, ownerId, 50)) {
                listPanel.add(FeedCardFactory.createPostCard(app, p));
                listPanel.add(Box.createVerticalStrut(8));
            }
        } catch (Exception e) {
            e.printStackTrace();
            listPanel.add(new JLabel("피드를 불러오지 못했습니다."));
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    // ====== 게시글 조회(작성자 필터) – likeCnt는 post_like COUNT 방식 ======
    private List<PostDTO> selectUserPosts(Connection con, String userId, int limit) throws SQLException {
        String sql = """
            SELECT p.post_id,
                   p.writer_id,
                   p.content,
                   (SELECT COUNT(*) FROM post_like pl WHERE pl.post_id = p.post_id) AS like_cnt,
                   p.created_at
            FROM posts p
            WHERE p.writer_id = ?
            ORDER BY p.post_id DESC
            LIMIT ?
        """;
        List<PostDTO> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PostDTO(
                            rs.getInt("post_id"),
                            rs.getString("writer_id"),
                            rs.getString("content"),
                            rs.getInt("like_cnt"),
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return list;
    }

    // ====== DTO ======
    public static class PostDTO implements FeedCardFactory.PostViewDTO {
        public final int postId;
        public final String writerId;
        public final String content;
        public final int likeCount;
        public final Timestamp createdAt;
        public PostDTO(int postId, String writerId, String content, int likeCount, Timestamp createdAt) {
            this.postId = postId; this.writerId = writerId; this.content = content; this.likeCount = likeCount; this.createdAt = createdAt;
        }
        @Override public int getPostId()     { return postId; }
        @Override public String getWriterId() { return writerId; }
        @Override public String getContent()  { return content; }
        @Override public int getLikeCount()   { return likeCount; }
        @Override public Timestamp getCreatedAt() { return createdAt; }
    }

    // ====== 작은 유틸 ======
    /** 예외 삼키고 null 아닌 리스트 반환 */
    private static List<String> safeList(SupplierWithException<List<String>> sup) {
        try {
            List<String> r = sup.get();
            return (r == null) ? new ArrayList<>() : r;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    @FunctionalInterface private interface SupplierWithException<T> { T get() throws Exception; }
}
