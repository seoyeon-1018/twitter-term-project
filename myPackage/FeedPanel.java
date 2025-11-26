/*
 * 파일명: FeedPanel.java
 * 목적: 메인 타임라인(피드) 화면을 담당하는 Swing 패널.
 *
 * 개요
 * - 로그인한 사용자가 보는 "최근 게시글 목록"을 스크롤로 표시합니다.
 * - 각 게시글 카드는 작성자/본문/좋아요/댓글 토글 UI로 구성됩니다.
 * - 댓글 영역은 접힌 상태로 시작하며, 버튼으로 펼쳐서 조회·작성·좋아요를 수행할 수 있습니다.
 *
 * 주요 흐름
 *  1) 생성자에서 스크롤 가능한 리스트 패널(listPanel)을 세팅하고 reload()로 최근 글을 초기 로드합니다.
 *  2) reload():
 *     - DB에서 최근 N개(기본 10개) 게시글을 조회(selectRecentPosts).
 *     - 각 행을 createPostCard(...)로 카드 컴포넌트로 만들어 listPanel에 추가합니다.
 *  3) 게시글 좋아요:
 *     - 낙관적 업데이트(즉시 ♥ 수와 색상을 반영) → 백그라운드 DB 처리 → 실패 시 롤백.
 *  4) 댓글(CommentArea 내부 클래스):
 *     - 특정 postId의 댓글 목록을 조회(selectComments)하고, 댓글 작성/댓글 좋아요를 처리합니다.
 *     - 댓글 좋아요 역시 낙관적 업데이트 → 실패 시 롤백 정책을 따릅니다.
 *
 * DB 의존
 * - posts, post_like, comment, comment_like 테이블을 조회/사용합니다.
 * - 현재 사용자가 해당 글/댓글에 이미 좋아요를 눌렀는지는 isPostLikedByUser / isCommentLikedByUser 로 조회합니다.
 *
 * 스레드/UX
 * - DB 갱신(좋아요/댓글 작성 등)은 새 스레드에서 실행하고, UI 갱신은 EDT(SwingUtilities.invokeLater)로 수행합니다.
 * - 낙관적 업데이트로 사용자 체감 성능을 높이고, 실패 시 이전 상태로 복원합니다.
 *
 * 변경 가이드(확장 포인트)
 * - 타임라인 범위(팔로우한 사용자 글만, 특정 조건 필터 등)를 바꾸려면 selectRecentPosts 쿼리를 수정하면 됩니다.
 * - 카드 UI를 통합해 재사용하려면 FeedCardFactory를 사용하도록 교체하는 것도 가능합니다(현재는 로컬 구현).
 * - 경험치/레벨 UI를 피드에서도 갱신하려면 좋아요/댓글 처리 성공 시 상단 헤더 갱신 훅(app.refreshPersonalBoardHeader 등)을 호출하세요.
 *
 */

package myPackage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FeedPanel extends JPanel {

    private final TwitterApp app;
    private String currentUserId() { return app.getCurrentUserId(); }

    // 메인 피드의 게시글 카드들을 세로로 쌓아놓는 컨테이너
    private final JPanel listPanel = new JPanel();

    public FeedPanel(TwitterApp app) {
        this.app = app;
        setLayout(new BorderLayout());

        // 스크롤 가능한 리스트 영역 구성
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(
                listPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        add(scroll, BorderLayout.CENTER);

        // 초기 로드
        reload();
    }

    /** 최근 10개 글 다시 그리기 */
    public final void reload() {
        listPanel.removeAll();
        try (Connection con = DBConn.getConnection()) {
            List<PostDTO> posts = selectRecentPosts(con, 10);
            for (PostDTO p : posts) {
                listPanel.add(createPostCard(p));      // 게시글 카드 1장 추가
                listPanel.add(Box.createVerticalStrut(8)); // 카드 간 간격
            }
        } catch (Exception e) {
            e.printStackTrace();
            listPanel.add(new JLabel("피드를 불러오지 못했습니다."));
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    /** DB: 최근 게시글 조회 (post_like COUNT 포함) */
    private List<PostDTO> selectRecentPosts(Connection con, int limit) throws SQLException {
        String sql = """
            SELECT p.post_id,
                   p.writer_id,
                   p.content,
                   (SELECT COUNT(*) FROM post_like pl WHERE pl.post_id = p.post_id) AS like_cnt,
                   p.created_at
            FROM posts p
            ORDER BY p.post_id DESC
            LIMIT ?
        """;
        List<PostDTO> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
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

    /** DB: 특정 글의 댓글 목록 (comment_like COUNT 포함) */
    private List<CommentDTO> selectComments(Connection con, int postId) throws SQLException {
        String sql = """
            SELECT c.comment_id,
                   c.writer_id,
                   c.content,
                   (SELECT COUNT(*) FROM comment_like cl WHERE cl.comment_id = c.comment_id) AS like_cnt,
                   c.created_at
            FROM comment c
            WHERE c.post_id = ?
            ORDER BY c.comment_id ASC
        """;
        List<CommentDTO> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CommentDTO(
                            rs.getInt("comment_id"),
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

    /** 현재 유저가 포스트를 좋아요했는지(중복 방지용) */
    private boolean isPostLikedByUser(int postId, String userId) {
        if (userId == null || userId.isBlank()) return false;
        String sql = "SELECT 1 FROM post_like WHERE post_id=? AND liker_id=? LIMIT 1";
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 현재 유저가 댓글을 좋아요했는지(중복 방지용) */
    private boolean isCommentLikedByUser(int commentId, String userId) {
        if (userId == null || userId.isBlank()) return false;
        String sql = "SELECT 1 FROM comment_like WHERE comment_id=? AND liker_id=? LIMIT 1";
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, commentId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 포스트 카드: 한 개의 게시글을 화면에 표현하는 UI 컴포넌트 생성 */
    private JPanel createPostCard(PostDTO post) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210,210,210)),
                new EmptyBorder(8,8,8,8)
        ));

        // 상단 헤더: 작성자 • 작성시각
        JLabel header = new JLabel(post.writerId + "  •  " + post.createdAt);
        card.add(header, BorderLayout.NORTH);

        // 본문(읽기 전용, 줄바꿈)
        JTextArea body = new JTextArea(post.content);
        body.setEditable(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setBackground(card.getBackground());
        card.add(body, BorderLayout.CENTER);

        // 하단 액션(좋아요/댓글 토글)
        JLabel likeLabel = new JLabel("♥ " + post.likes);
        JButton likeBtn = new JButton("Like");
        JButton cmtToggle = new JButton("Comments");

        // 이미 좋아요한 글이면 빨간색으로 표시
        boolean initiallyLiked = isPostLikedByUser(post.postId, currentUserId());
        if (initiallyLiked) likeLabel.setForeground(Color.RED);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(likeLabel);
        actions.add(likeBtn);
        actions.add(cmtToggle);

        // 댓글 영역(기본 접힘)
        CommentArea cArea = new CommentArea(post.postId);
        JPanel cWrap = new JPanel(new BorderLayout());
        cWrap.add(cArea, BorderLayout.CENTER);
        cWrap.setVisible(false);
        cWrap.setBorder(BorderFactory.createMatteBorder(8, 0, 0, 0, new Color(245,245,245)));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(cWrap,  BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        // 포스트 좋아요(낙관적 업데이트 → 실패 시 롤백)
        likeBtn.addActionListener(e -> {
            String uid = currentUserId();
            if (uid == null || uid.isBlank()) {
                JOptionPane.showMessageDialog(this, "로그인이 필요합니다.", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }
            final int prevLikes = post.likes;
            final String prevText = likeLabel.getText();
            final Color prevColor = likeLabel.getForeground();
            likeBtn.setEnabled(false);

            // 낙관적 증가
            post.likes = prevLikes + 1;
            likeLabel.setText("♥ " + post.likes);
            likeLabel.setForeground(Color.RED);

            new Thread(() -> {
                boolean ok = false; Exception err = null;
                try { ok = PostLike.likePost(post.postId, uid); } catch (Exception ex) { err = ex; }
                final boolean okFinal = ok; final Exception errFinal = err;

                SwingUtilities.invokeLater(() -> {
                    if (!okFinal) {
                        // 실패 → 원상 복귀
                        post.likes = prevLikes;
                        likeLabel.setText(prevText);
                        likeLabel.setForeground(prevColor);
                        likeBtn.setEnabled(true);
                        if (errFinal != null) {
                            errFinal.printStackTrace();
                            JOptionPane.showMessageDialog(this, "좋아요 처리 중 오류", "Error", JOptionPane.ERROR_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this, "이미 좋아요를 누른 게시글입니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        // 성공 → 빨간색 유지
                        likeLabel.setForeground(Color.RED);
                        likeBtn.setEnabled(true);
                    }
                });
            }).start();
        });

        // 댓글 영역 토글
        cmtToggle.addActionListener(e -> {
            boolean vis = !cWrap.isVisible();
            cWrap.setVisible(vis);
            card.revalidate();
            card.repaint();
        });

        return card;
    }

    /** ======================= 댓글 영역 ======================= */
    private class CommentArea extends JPanel {
        private final int postId;
        private final JPanel list = new JPanel();

        private final JTextArea inputArea = new JTextArea(3, 20);
        private final JButton addBtn = new JButton("Comment");

        CommentArea(int postId) {
            this.postId = postId;
            setLayout(new BorderLayout());
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

            // 댓글 리스트 스크롤
            JScrollPane sp = new JScrollPane(
                    list,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            );
            add(sp, BorderLayout.CENTER);

            // 입력창(멀티라인)
            inputArea.setLineWrap(true);
            inputArea.setWrapStyleWord(true);
            JScrollPane inScroll = new JScrollPane(inputArea);

            JPanel input = new JPanel(new BorderLayout(6,6));
            input.add(inScroll, BorderLayout.CENTER);
            input.add(addBtn,   BorderLayout.EAST);
            add(input, BorderLayout.SOUTH);

            loadComments();

            // 버튼 클릭 → 제출
            addBtn.addActionListener(e -> submitComment());

            // Enter=제출 / Shift+Enter=줄바꿈
            InputMap im = inputArea.getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap am = inputArea.getActionMap();
            im.put(KeyStroke.getKeyStroke("ENTER"), "submit");
            im.put(KeyStroke.getKeyStroke("shift ENTER"), "insert-newline");
            am.put("submit", new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { submitComment(); }
            });
            am.put("insert-newline", new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                    inputArea.insert("\n", inputArea.getCaretPosition());
                }
            });

            // 기본 버튼 지정(엔터 제출 UX)
            SwingUtilities.invokeLater(() -> {
                JRootPane root = SwingUtilities.getRootPane(this);
                if (root != null) root.setDefaultButton(addBtn);
            });
        }

        /** 댓글 제출 공용 로직 */
        private void submitComment() {
            String text = inputArea.getText().trim();
            if (text.isEmpty()) return;

            String uid = currentUserId();
            if (uid == null || uid.isBlank()) {
                JOptionPane.showMessageDialog(this, "로그인이 필요합니다.", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }

            addBtn.setEnabled(false);
            final String toSave = text;

            new Thread(() -> {
                Exception err = null;
                try { Comment.write(postId, uid, toSave); } catch (Exception ex) { err = ex; }
                final Exception errFinal = err;
                SwingUtilities.invokeLater(() -> {
                    addBtn.setEnabled(true);
                    if (errFinal != null) {
                        errFinal.printStackTrace();
                        JOptionPane.showMessageDialog(this, "댓글 등록 실패", "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        inputArea.setText("");
                        loadComments(); // 등록 후 새로고침
                    }
                });
            }).start();
        }

        /** 댓글 목록 + 댓글 좋아요 버튼/상태(낙관적 업데이트 방식) */
        private void loadComments() {
            list.removeAll();
            try (Connection con = DBConn.getConnection()) {
                for (CommentDTO c : selectComments(con, postId)) {
                    JPanel row = new JPanel(new BorderLayout());
                    row.setBorder(new EmptyBorder(4,4,4,4));

                    JLabel left = new JLabel("<html><b>" + c.writerId + "</b> : " +
                            escapeHtml(c.content) +
                            "<br/><span style='color:gray'>" + c.createdAt + "</span></html>");
                    row.add(left, BorderLayout.CENTER);

                    JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
                    JLabel like = new JLabel("♥ " + c.likes);
                    JButton likeBtn = new JButton("Like");

                    // 이미 좋아요한 댓글이면 표시/차단
                    boolean initiallyLiked = isCommentLikedByUser(c.commentId, currentUserId());
                    if (initiallyLiked) {
                        like.setForeground(Color.RED);
                        likeBtn.setText("Liked");
                        likeBtn.setEnabled(false); // 정책: 이미 눌렀으면 비활성화
                    }

                    right.add(like);
                    right.add(likeBtn);
                    row.add(right, BorderLayout.EAST);

                    // 댓글 좋아요(낙관적 업데이트 → 실패 시 롤백)
                    likeBtn.addActionListener(e -> {
                        String uid = currentUserId();
                        if (uid == null || uid.isBlank()) {
                            JOptionPane.showMessageDialog(this, "로그인이 필요합니다.", "알림", JOptionPane.WARNING_MESSAGE);
                            return;
                        }

                        final int prevLikes = c.likes;
                        final String prevText = like.getText();
                        final Color prevColor = like.getForeground();
                        likeBtn.setEnabled(false);

                        // 낙관적 증가
                        c.likes = prevLikes + 1;
                        like.setText("♥ " + c.likes);
                        like.setForeground(Color.RED);

                        new Thread(() -> {
                            boolean ok = false; Exception err = null;
                            try { ok = CommentLike.likeComment(c.commentId, uid); } catch (Exception ex) { err = ex; }
                            final boolean okFinal = ok; final Exception errFinal = err;

                            SwingUtilities.invokeLater(() -> {
                                if (!okFinal) {
                                    // 실패 → 복원
                                    c.likes = prevLikes;
                                    like.setText(prevText);
                                    like.setForeground(prevColor);
                                    likeBtn.setEnabled(true);
                                    if (errFinal != null) {
                                        errFinal.printStackTrace();
                                        JOptionPane.showMessageDialog(this, "댓글 좋아요 실패", "Error", JOptionPane.ERROR_MESSAGE);
                                    } else {
                                        JOptionPane.showMessageDialog(this, "이미 좋아요를 누른 댓글입니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                                    }
                                } else {
                                    like.setForeground(Color.RED);
                                    likeBtn.setText("Liked");
                                    likeBtn.setEnabled(false); // 성공 후 비활성화 유지
                                }
                            });
                        }).start();
                    });

                    list.add(row);
                    list.add(new JSeparator());
                }
            } catch (Exception e) {
                e.printStackTrace();
                list.add(new JLabel("댓글을 불러오지 못했습니다."));
            }
            list.revalidate();
            list.repaint();
        }
    }

    // ---------- 유틸/DTO ----------

    // 간단한 HTML 이스케이프(댓글 표시용)
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    // 게시글 1개를 메모리에 담아두는 DTO(좋아요 수는 가변)
    private static class PostDTO {
        final int postId;
        final String writerId;
        final String content;
        int likes; // COUNT로 채움(가변)
        final Timestamp createdAt;
        PostDTO(int postId, String writerId, String content, int likes, Timestamp createdAt) {
            this.postId = postId; this.writerId = writerId;
            this.content = content; this.likes = likes; this.createdAt = createdAt;
        }
    }

    // 댓글 1개를 메모리에 담아두는 DTO(좋아요 수는 가변)
    private static class CommentDTO {
        final int commentId;
        final String writerId;
        final String content;
        int likes; // COUNT로 채움(가변)
        final Timestamp createdAt;
        CommentDTO(int commentId, String writerId, String content, int likes, Timestamp createdAt) {
            this.commentId = commentId; this.writerId = writerId;
            this.content = content; this.likes = likes; this.createdAt = createdAt;
        }
    }
}
