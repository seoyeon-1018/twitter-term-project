/*
 * 파일명: FeedCardFactory.java
 * 목적: 피드(게시글) 카드 UI를 생성하는 공용 팩토리.
 *
 * 이 파일은 다양한 화면(메인 피드, 개인 보드, 해시태그 검색 결과 등)에서
 * 동일한 "게시글 카드" UI를 재사용할 수 있도록 구성된 유틸성 클래스입니다.
 *
 * 핵심 구성:
 *  1) PostViewDTO 인터페이스
 *     - 화면/쿼리마다 DTO 구조가 조금씩 달라도, 아래 5개 필드만 가지면
 *       어디서 온 DTO든 카드 생성에 재사용할 수 있도록 하는 최소 공통 인터페이스입니다.
 *       (postId, writerId, content, likeCount, createdAt)
 *
 *  2) createPostCard(TwitterApp, PostViewDTO)
 *     - 위 인터페이스를 만족하는 DTO를 받아서 Swing 컴포넌트로 카드(게시글 한 장)를 구성합니다.
 *     - 카드 상단 헤더(작성자 • 작성시각), 본문, 하단 액션(좋아요/댓글 토글)로 이루어집니다.
 *     - 작성자 라벨을 클릭하면 해당 사용자의 보드를 엽니다(app.openPersonalBoard).
 *
 *  3) 좋아요 처리
 *     - UI 즉시 반영(낙관적 업데이트) 후, 백그라운드에서 PostLike.likePost(...) 호출.
 *     - 실패하면 롤백(숫자/색상 복원) 및 안내 메시지 출력.
 *     - 성공 시 app.refreshPersonalBoardHeader(post.getWriterId())를 호출하여
 *       작성자 보드 상단의 숫자/레벨 등을 즉시 갱신할 수 있게 훅을 제공합니다.
 *       (레벨/경험치 시스템과의 연동을 고려한 호출 지점)
 *
 *  4) 댓글 영역(CommentArea 내부 클래스)
 *     - 해당 게시글의 댓글 목록을 조회/표시하고, 댓글 작성 및 댓글 좋아요를 처리합니다.
 *     - 댓글 작성/댓글 좋아요 성공 시에도 app.refreshPersonalBoardHeader(...)를 통해
 *       작성자 헤더 갱신 훅을 제공합니다(레벨/경험치 반영 시 즉시 UI 업데이트).
 *
 */

package myPackage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.sql.Timestamp;   // ← 명시 import
import java.util.ArrayList;
import java.util.List;

public class FeedCardFactory {

    /* ========= 공통 뷰 인터페이스 ========= */
    // 다양한 출처의 DTO(개인보드/해시태그 결과 등)를 동일한 카드로 그리기 위한 최소 공통 인터페이스
    public static interface PostViewDTO {
        int getPostId();
        String getWriterId();
        String getContent();
        int getLikeCount();
        Timestamp getCreatedAt();
    }

    /* ========= 단일 엔트리: 인터페이스만 받음 ========= */
    // 위 인터페이스만 만족하면 어디서 온 DTO든 동일한 카드 UI를 생성 가능
    public static JPanel createPostCard(TwitterApp app, PostViewDTO post) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210,210,210)),
                new EmptyBorder(8,8,8,8)
        ));

        // 상단 헤더: 작성자 • 작성시각  (작성자 클릭 시 해당 보드 열기)
        JLabel header = new JLabel(post.getWriterId() + "  •  " + post.getCreatedAt());
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.setToolTipText("Open @" + post.getWriterId() + "'s board");
        header.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                app.openPersonalBoard(post.getWriterId());
            }
        });
        card.add(header, BorderLayout.NORTH);

        // 본문 텍스트(읽기 전용)
        JTextArea body = new JTextArea(post.getContent());
        body.setEditable(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setBackground(card.getBackground());
        card.add(body, BorderLayout.CENTER);

        // 하단: 좋아요 / 댓글 토글
        JLabel likeLabel = new JLabel("♥ " + post.getLikeCount());
        JButton likeBtn   = new JButton("Like");
        JButton cmtToggle = new JButton("Comments");

        // 현재 유저가 이미 좋아요한 글인지 여부(색으로 표시)
        boolean initiallyLiked = isPostLikedByUser(post.getPostId(), app.getCurrentUserId());
        if (initiallyLiked) likeLabel.setForeground(Color.RED);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(likeLabel);
        actions.add(likeBtn);
        actions.add(cmtToggle);

        // 댓글 영역(초기에는 접어둠)
        CommentArea cArea = new CommentArea(app, post.getPostId(), post.getWriterId());
        JPanel cWrap = new JPanel(new BorderLayout());
        cWrap.add(cArea, BorderLayout.CENTER);
        cWrap.setVisible(false);
        cWrap.setBorder(BorderFactory.createMatteBorder(8, 0, 0, 0, new Color(245,245,245)));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(cWrap,  BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        // 좋아요 버튼 핸들러: 낙관적 UI 업데이트 → 백엔드 처리 → 실패 시 롤백
        likeBtn.addActionListener(e -> {
            String uid = app.getCurrentUserId();
            if (uid == null || uid.isBlank()) {
                JOptionPane.showMessageDialog(card, "로그인이 필요합니다.", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }
            likeBtn.setEnabled(false);
            int prev = parseLike(likeLabel.getText());
            likeLabel.setText("♥ " + (prev+1));
            likeLabel.setForeground(Color.RED);

            new Thread(() -> {
                boolean ok=false; Exception err=null;
                try { ok = PostLike.likePost(post.getPostId(), uid); } catch (Exception ex) { err=ex; }
                final boolean okF = ok; final Exception errF = err;
                SwingUtilities.invokeLater(() -> {
                    if(!okF){
                        // 실패 → 롤백
                        likeLabel.setText("♥ " + prev);
                        likeLabel.setForeground(UIManager.getColor("Label.foreground"));
                        likeBtn.setEnabled(true);
                        if(errF!=null){ errF.printStackTrace(); JOptionPane.showMessageDialog(card,"좋아요 실패","Error",JOptionPane.ERROR_MESSAGE); }
                        else JOptionPane.showMessageDialog(card,"이미 좋아요한 글입니다.","알림",JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        likeBtn.setEnabled(true);
                        // 성공 → 작성자 보드 상단(팔로워/레벨 등) 갱신 훅
                        //  (레벨/EXP 시스템을 적용 중이면 이 호출로 즉시 UI에 반영 가능)
                        app.refreshPersonalBoardHeader(post.getWriterId());
                    }
                });
            }).start();
        });

        // 댓글 영역 토글(펼치기/접기)
        cmtToggle.addActionListener(e -> {
            cWrap.setVisible(!cWrap.isVisible());
            card.revalidate(); card.repaint();
        });

        return card;
    }

    // "♥ 12" → 12 변환용 유틸
    private static int parseLike(String s) {
        try { return Integer.parseInt(s.replace("♥","").trim()); }
        catch (Exception e) { return 0; }
    }

    // 현재 사용자가 특정 글에 좋아요를 눌렀는지 여부 조회
    private static boolean isPostLikedByUser(int postId, String userId) {
        if (userId == null || userId.isBlank()) return false;
        String sql = "SELECT 1 FROM post_like WHERE post_id=? AND liker_id=? LIMIT 1";
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { return false; }
    }

    /* ========= 댓글 영역 ========= */
    // 단일 게시글의 댓글 목록/작성/댓글 좋아요를 담당하는 내부 패널
    private static class CommentArea extends JPanel {
        private final TwitterApp app;
        private final int postId;
        private final JPanel list = new JPanel();
        private final JTextArea inputArea = new JTextArea(3, 20);
        private final JButton addBtn = new JButton("Comment");
        private final String postWriterId; // 댓글 등록/좋아요 성공 시 보드 헤더 갱신을 위한 대상 작성자

        CommentArea(TwitterApp app, int postId, String postWriterId) {
            this.app = app;
            this.postId = postId;
            this.postWriterId = postWriterId;

            setLayout(new BorderLayout());

            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            JScrollPane sp = new JScrollPane(list,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            add(sp, BorderLayout.CENTER);

            inputArea.setLineWrap(true); inputArea.setWrapStyleWord(true);
            JPanel input = new JPanel(new BorderLayout(6,6));
            input.add(new JScrollPane(inputArea), BorderLayout.CENTER);
            input.add(addBtn, BorderLayout.EAST);
            add(input, BorderLayout.SOUTH);

            addBtn.addActionListener(e -> submit());

            // Enter = 제출 / Shift+Enter = 줄바꿈
            InputMap im = inputArea.getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap am = inputArea.getActionMap();
            im.put(KeyStroke.getKeyStroke("ENTER"), "submit");
            im.put(KeyStroke.getKeyStroke("shift ENTER"), "newline");
            am.put("submit", new AbstractAction(){ @Override public void actionPerformed(java.awt.event.ActionEvent e){ submit(); }});
            am.put("newline", new AbstractAction(){ @Override public void actionPerformed(java.awt.event.ActionEvent e){ inputArea.insert("\n", inputArea.getCaretPosition()); }});

            loadComments();
        }

        // 댓글 등록 처리(낙관적 UI는 사용하지 않고 등록 후 재조회)
        private void submit() {
            String text = inputArea.getText().trim();
            if(text.isEmpty()) return;
            String uid = app.getCurrentUserId();
            if (uid == null || uid.isBlank()) {
                JOptionPane.showMessageDialog(this, "로그인이 필요합니다.", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }
            addBtn.setEnabled(false);
            new Thread(() -> {
                Exception err=null;
                try { Comment.write(postId, uid, text); }
                catch (Exception ex){ err=ex; }
                final Exception ef = err;
                SwingUtilities.invokeLater(() -> {
                    addBtn.setEnabled(true);
                    if(ef!=null){
                        ef.printStackTrace();
                        JOptionPane.showMessageDialog(this, "댓글 등록 실패", "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        inputArea.setText("");
                        loadComments();
                        // 댓글을 받은 '게시글 작성자' 보드 상단 갱신(레벨/EXP 반영용 훅)
                        app.refreshPersonalBoardHeader(postWriterId);
                    }
                });
            }).start();
        }

        // 댓글 목록 재조회
        private void loadComments() {
            list.removeAll();
            try (Connection con = DBConn.getConnection()) {
                for (CommentRow r : selectComments(con, postId)) {
                    list.add(buildRow(r));
                    list.add(new JSeparator());
                }
            } catch (Exception e) {
                list.add(new JLabel("댓글을 불러오지 못했습니다."));
            }
            list.revalidate(); list.repaint();
        }

        // 단일 댓글 행 UI + 좋아요 처리
        private JPanel buildRow(CommentRow c) {
            JPanel row = new JPanel(new BorderLayout());
            row.setBorder(new EmptyBorder(4,4,4,4));
            JLabel left = new JLabel("<html><b>" + c.writerId + "</b> : " +
                    escape(c.content) + "<br/><span style='color:gray'>" + c.createdAt + "</span></html>");
            row.add(left, BorderLayout.CENTER);

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            JLabel like = new JLabel("♥ " + c.likes);
            JButton likeBtn = new JButton("Like");
            if (isCommentLikedByUser(c.commentId, app.getCurrentUserId()))
                like.setForeground(Color.RED);
            right.add(like); right.add(likeBtn);
            row.add(right, BorderLayout.EAST);

            // 댓글 좋아요 처리(낙관적 업데이트 → 실패 시 롤백)
            likeBtn.addActionListener(e -> {
                String uid = app.getCurrentUserId();
                if (uid == null || uid.isBlank()) {
                    JOptionPane.showMessageDialog(this, "로그인이 필요합니다.", "알림", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                likeBtn.setEnabled(false);
                int prev = c.likes; like.setText("♥ " + (prev+1)); like.setForeground(Color.RED);

                new Thread(() -> {
                    boolean ok=false; Exception err=null;
                    try { ok = CommentLike.likeComment(c.commentId, uid); } catch (Exception ex){ err=ex; }
                    final boolean okF=ok; final Exception errF=err;
                    SwingUtilities.invokeLater(() -> {
                        if(!okF){
                            like.setText("♥ " + prev);
                            like.setForeground(UIManager.getColor("Label.foreground"));
                            likeBtn.setEnabled(true);
                            if(errF!=null){
                                errF.printStackTrace();
                                JOptionPane.showMessageDialog(this, "댓글 좋아요 실패", "Error", JOptionPane.ERROR_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(this, "이미 좋아요한 댓글입니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                            }
                        } else {
                            likeBtn.setEnabled(true);
                            // 댓글 작성자 보드 상단 갱신(레벨/EXP 반영 시 즉시 보이도록)
                            app.refreshPersonalBoardHeader(c.writerId);
                        }
                    });
                }).start();
            });

            return row;
        }

        /* ---- DB helpers ---- */
        // 댓글 목록 조회(좋아요 수 포함)
        private static List<CommentRow> selectComments(Connection con, int postId) throws SQLException {
            String sql = """
                SELECT c.comment_id,
                       c.writer_id,
                       c.content,
                       (SELECT COUNT(*) FROM comment_like cl WHERE cl.comment_id=c.comment_id) AS like_cnt,
                       c.created_at
                FROM comment c
                WHERE c.post_id=?
                ORDER BY c.comment_id ASC
            """;
            List<CommentRow> list = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, postId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new CommentRow(
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

        // 현재 사용자가 특정 댓글을 좋아요했는지 여부
        private static boolean isCommentLikedByUser(int cId, String userId) {
            if (userId == null || userId.isBlank()) return false;
            String sql = "SELECT 1 FROM comment_like WHERE comment_id=? AND liker_id=? LIMIT 1";
            try (Connection con = DBConn.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, cId); ps.setString(2, userId);
                try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
            } catch (SQLException e) { return false; }
        }

        // HTML 이스케이프(간단)
        private static String escape(String s){
            return s==null? "" : s.replace("&","&amp;")
                                  .replace("<","&lt;")
                                  .replace(">","&gt;");
        }

        // 댓글 1개 데이터 보관용 로컬 DTO
        private static class CommentRow {
            final int commentId; final String writerId; final String content; final int likes; final Timestamp createdAt;
            CommentRow(int id, String w, String c, int l, Timestamp t){
                commentId=id; writerId=w; content=c; likes=l; createdAt=t;
            }
        }
    }
}
