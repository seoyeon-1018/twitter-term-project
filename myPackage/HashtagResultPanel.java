/*
 * 파일명: HashtagResultPanel.java
 * 역할: 특정 해시태그(#tag)에 매칭되는 게시글 목록을 조회/표시하는 결과 화면 패널
 *
 * 동작 개요
 * 1) 초기화
 *    - 생성자에서 태그 문자열( '#' 제거된 값 )을 받아 보관하고, 헤더/리스트 영역을 구성한 뒤 reload()로 즉시 로딩
 *
 * 2) UI 구성
 *    - 상단 헤더: [← Back] 버튼(메인으로 복귀), "Results for #tag" 타이틀, ESC 키로 뒤로가기 단축키
 *    - 본문 리스트: BoxLayout + JScrollPane. 각 행은 FeedCardFactory.createPostCard(...)를 사용해 공통 카드 UI 재사용
 *
 * 3) 데이터 로딩
 *    - posts, post_tag 조인으로 해당 태그의 게시글을 최신(post_id DESC) 순으로 조회
 *    - 좋아요 수는 서브쿼리( post_like COUNT )로 계산
 *    - 결과가 없으면 'No posts found for #tag' 문구 표시
 *
 * 4) 상호작용
 *    - 카드 내부(FeedCardFactory)에서 좋아요/댓글/댓글 좋아요 등 공통 액션이 동작
 *    - 카드 헤더의 작성자 라벨 클릭 시 해당 사용자의 개인 보드로 이동(FeedCardFactory에서 처리)
 *
 */

package myPackage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.sql.Timestamp; // ← 명시 import

public class HashtagResultPanel extends JPanel {

    private final TwitterApp app;
    private final String tag; // '#' 없이 저장
    private final JPanel listPanel = new JPanel();

    public HashtagResultPanel(TwitterApp app, String tag) {
        this.app = app;
        this.tag = tag;
        setLayout(new BorderLayout(0,10));

        add(buildHeader(), BorderLayout.NORTH);

        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(
                listPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        add(scroll, BorderLayout.CENTER);

        reload();
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(8,8,8,8));

        // ← Back (메인으로)
        JButton back = new JButton("← Back");
        back.addActionListener(e -> app.showPage(TwitterApp.PAGE_MAIN));
        header.add(back, BorderLayout.WEST);

        JLabel title = new JLabel("Results for  #" + tag, SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        header.add(title, BorderLayout.CENTER);

        // ESC 로 뒤로가기
        InputMap im = header.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = header.getActionMap();
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "GO_BACK");
        am.put("GO_BACK", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                app.showPage(TwitterApp.PAGE_MAIN);
            }
        });

        return header;
    }

    public void reload() {
        listPanel.removeAll();
        String sql = """
                SELECT p.post_id,
                       p.writer_id,
                       p.content,
                       (SELECT COUNT(*) FROM post_like pl WHERE pl.post_id = p.post_id) AS like_cnt,
                       p.created_at
                FROM posts p
                JOIN post_tag t ON p.post_id = t.post_id
                WHERE t.tag = ?
                ORDER BY p.post_id DESC
                """;
        try (Connection con = DBConn.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, tag);

            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;

                    // 이 패널 전용 DTO 생성 (공통 인터페이스 구현)
                    PostDTO p = new PostDTO(
                            rs.getInt("post_id"),
                            rs.getString("writer_id"),
                            rs.getString("content"),
                            rs.getInt("like_cnt"),
                            rs.getTimestamp("created_at")
                    );

                    // 공통 카드 UI 재사용
                    listPanel.add(FeedCardFactory.createPostCard(app, p));
                    listPanel.add(Box.createVerticalStrut(8));
                }
                if (!any) {
                    listPanel.add(new JLabel("No posts found for #" + tag));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            listPanel.add(new JLabel("검색 중 오류가 발생했습니다."));
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    /** 이 패널 전용 DTO: 공통 인터페이스 구현 */
    public static class PostDTO implements FeedCardFactory.PostViewDTO {
        public final int postId;
        public final String writerId;
        public final String content;
        public final int likeCount;
        public final Timestamp createdAt;

        public PostDTO(int postId, String writerId, String content, int likeCount, Timestamp createdAt) {
            this.postId = postId;
            this.writerId = writerId;
            this.content = content;
            this.likeCount = likeCount;
            this.createdAt = createdAt;
        }

        @Override public int getPostId() { return postId; }
        @Override public String getWriterId() { return writerId; }
        @Override public String getContent() { return content; }
        @Override public int getLikeCount() { return likeCount; }
        @Override public Timestamp getCreatedAt() { return createdAt; }
    }
}
