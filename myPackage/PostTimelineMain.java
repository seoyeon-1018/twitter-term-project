/*
 * 파일명: PostTimelineMain.java
 * 목적: DB의 posts(+user 조인) 데이터를 읽어 간단한 타임라인 UI로 표시하는 독립 실행형 스윙 프레임.
 *
 * 주요 기능
 * - 상단 바: "타임라인" 제목과 [새로고침] 버튼 제공.
 * - 본문: 스크롤 가능한 카드 목록; 각 카드에 작성자 ID, 좋아요 수, 본문 내용 표시.
 * - 데이터 로딩: posts와 user를 조인하여 최신 post_id 순으로 조회 후 화면에 렌더링.
 *
 * 동작 흐름
 * - main()에서 DBConn.getConnection()으로 커넥션 획득 → PostTimelineMain 생성 및 표시.
 * - 생성자에서 최초 1회 loadPosts() 호출 → 이후 [새로고침] 버튼으로 재호출.
 *
 * DB 가정/전제
 * - 테이블: posts(post_id, content, writer_id, num_of_likes, created_at…), user(user_id, …) 존재.
 * - FK: posts.writer_id → user.user_id (코드에서는 존재 가정; 실제 제약은 스키마에 따라 다를 수 있음)
 *
 * UI/설계 포인트
 * - 단순 미니 타임라인이므로 작성/좋아요 기능은 포함하지 않음(읽기 전용).
 *
 * 예외 처리
 * - DB 조회 실패 시 다이얼로그로 오류 메시지 노출.
 *
 */

package myPackage;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DB의 posts + user 테이블을 읽어서
 * 스크롤 가능한 타임라인으로 보여주는 간단한 예제
 */
public class PostTimelineMain extends JFrame {

    // DB 연결
    private final Connection conn;

    // 포스트 카드들이 쌓일 패널
    private final JPanel postsContainer;

    // ====== Post DTO ======
    private static class Post {
        int postId;
        String writerId;      // user.user_id
        String content;
        int numOfLikes;
    }

    // ====== 생성자 ======
    public PostTimelineMain(Connection conn) {
        this.conn = conn;

        setTitle("Mini Twitter Timeline");
        setSize(600, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 상단 바 (타이틀 + 새로고침 버튼)
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel titleLabel = new JLabel("타임라인");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));

        JButton refreshButton = new JButton("새로고침");
        topPanel.add(titleLabel);
        topPanel.add(refreshButton);

        add(topPanel, BorderLayout.NORTH);

        // 가운데: 스크롤 가능한 포스트 목록
        postsContainer = new JPanel();
        postsContainer.setLayout(new BoxLayout(postsContainer, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(postsContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        add(scrollPane, BorderLayout.CENTER);

        // 처음 한 번 로딩
        loadPosts();

        // 새로고침 버튼 클릭 시 다시 로딩
        refreshButton.addActionListener(e -> loadPosts());
    }

    // ====== DB에서 posts 읽어서 패널에 뿌리기 ======
    private void loadPosts() {
        postsContainer.removeAll(); // 기존 카드 제거

        List<Post> posts = fetchPostsFromDb();

        for (Post p : posts) {
            JPanel postPanel = createPostPanel(p);
            postsContainer.add(postPanel);
        }

        postsContainer.revalidate();
        postsContainer.repaint();
    }

    // posts + user 테이블에서 데이터 읽어오기
    private List<Post> fetchPostsFromDb() {
        List<Post> list = new ArrayList<>();

        // created_at은 조회/표시 대상에서 제외 (간단한 데모 목적)
        String sql =
                "SELECT p.post_id, p.content, p.writer_id, p.num_of_likes " +
                "FROM posts p " +
                "JOIN `user` u ON p.writer_id = u.user_id " +
                "ORDER BY p.post_id DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Post p = new Post();
                p.postId = rs.getInt("post_id");
                p.content = rs.getString("content");
                p.writerId = rs.getString("writer_id");
                p.numOfLikes = rs.getInt("num_of_likes");
                list.add(p);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "posts 불러오는 중 오류: " + e.getMessage(),
                    "DB 오류",
                    JOptionPane.ERROR_MESSAGE);
        }

        return list;
    }

    // ====== 포스트 하나를 카드 형태로 만드는 패널 ======
    private JPanel createPostPanel(Post post) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                        BorderFactory.createEmptyBorder(8, 8, 8, 8)
                )
        );

        // 상단: 작성자 + 좋아요 수 (시간 제거)
        String headerText = post.writerId + "  ·  ❤ " + post.numOfLikes;
        JLabel headerLabel = new JLabel(headerText);
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 12f));

        // 중앙: 내용 (여러 줄 지원)
        JTextArea contentArea = new JTextArea(post.content);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setEditable(false);
        contentArea.setOpaque(false);

        panel.add(headerLabel, BorderLayout.NORTH);
        panel.add(contentArea, BorderLayout.CENTER);

        return panel;
    }

    // ====== main: 바로 실행용 ======
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 네가 만든 DBConn 사용 (예: 커넥션 풀을 쓰는 경우 여기서 대체 가능)
                Connection conn = DBConn.getConnection();

                PostTimelineMain frame = new PostTimelineMain(conn);
                frame.setVisible(true);

            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "DB 연결 실패: " + e.getMessage(),
                        "DB 오류",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
