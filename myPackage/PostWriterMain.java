/*
 * 파일명: PostWriterMain.java
 * 목적: 트위터 스타일 글쓰기 창을 제공하고, 즉시 게시 또는 예약 게시를 지원하는 독립 실행형 스윙 프레임.
 *
 * 주요 기능
 * 1) 즉시 게시:
 *    - 상단 텍스트 영역의 내용을 posts(content, writer_id, created_at)로 INSERT
 *    - 본문에서 #해시태그를 정규식으로 추출하여 post_tag(post_id, tag)에 저장(중복 무시)
 *
 * 2) 예약 게시:
 *    - "예약 포스트" 체크 시 yyyy-MM-dd HH:mm 형식의 예약 시각 입력
 *    - reserved_post(writer_id, content, scheduled_time, is_posted=false)에 INSERT
 *    - 내부 타이머가 60초마다 reserved_post를 스캔하여
 *        scheduled_time <= NOW() && is_posted=false
 *      인 레코드를 실제 posts로 옮기고(post_id 회수 후 태그 저장), is_posted=true로 마킹
 *
 * 화면 구성
 * - NORTH: 안내 라벨
 * - CENTER: 멀티라인 글 작성 JTextArea(스크롤)
 * - SOUTH: 예약 체크박스 + 예약 시각 입력 + [Post] 버튼
 *
 * 정규식/태그 저장 규칙
 * - 정규식: "#([\\p{IsAlphabetic}\\p{IsDigit}_가-힣]+)"
 *   (영문자/숫자/밑줄/한글로 이루어진 연속 토큰을 태그로 인식)
 * - 소문자 정규화 및 길이 50자 컷
 * - INSERT IGNORE로 동일 (post_id, tag) 중복 방지
 *
 * DB 전제
 * - posts(post_id PK AI, content, writer_id FK user(user_id), created_at TIMESTAMP …)
 * - reserved_post(s_id PK AI, writer_id FK, content, scheduled_time DATETIME, is_posted BOOLEAN)
 * - post_tag(t_id PK AI, post_id FK posts(post_id), tag VARCHAR(50), UNIQUE(post_id, tag))
 *
 * 에러 처리
 * - 즉시/예약 저장 에러는 다이얼로그로 표시
 * - 예약 워커(processReservedPosts)는 주기 동작 특성상 예외를 콘솔 로그로만 출력
 *
 */

package myPackage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 트위터 스타일 글쓰기 화면
 * - 위쪽: 글 작성용 JTextArea
 * - 아래쪽: 즉시 Post 버튼
 * - 예약 체크 시: 예약 시간 입력 → reserved_post 테이블에 저장
 * - 예약 처리 타이머: 일정 주기마다 reserved_post를 확인해 posts로 실제 업로드(+해시태그 저장)
 */
public class PostWriterMain extends JFrame {

    private final Connection conn;
    private final String currentUserId;  // 로그인한 사용자 아이디

    private final JTextArea contentArea;
    private final JCheckBox reserveCheckBox;
    private final JTextField datetimeField; // "yyyy-MM-dd HH:mm" 형식
    private final JButton postButton;

    // 예약 처리용 포맷
    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 해시태그 추출 정규식: 영문/숫자/밑줄/한글
    private static final Pattern TAG = Pattern.compile("#([\\p{IsAlphabetic}\\p{IsDigit}_가-힣]+)");

    public PostWriterMain(Connection conn, String currentUserId) {
        this.conn = conn;
        this.currentUserId = currentUserId;

        setTitle("Write Post");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // ===== 상단: 안내 라벨 =====
        JLabel guideLabel = new JLabel("포스트 내용을 입력하세요:");
        guideLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(guideLabel, BorderLayout.NORTH);

        // ===== 중앙: 글 작성 영역 =====
        contentArea = new JTextArea();
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(contentArea);
        scrollPane.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );
        add(scrollPane, BorderLayout.CENTER);

        // ===== 하단: 예약 옵션 + 버튼 =====
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // 예약 옵션 패널 (왼쪽)
        JPanel reservePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        reserveCheckBox = new JCheckBox("예약 포스트");
        reservePanel.add(reserveCheckBox);

        // 예약 시간 입력 필드
        JLabel timeLabel = new JLabel("예약 시간 (yyyy-MM-dd HH:mm): ");
        reservePanel.add(timeLabel);

        datetimeField = new JTextField(16);
        datetimeField.setToolTipText("예: 2025-11-22 23:30");
        reservePanel.add(datetimeField);

        bottomPanel.add(reservePanel, BorderLayout.CENTER);

        // Post 버튼 (오른쪽)
        postButton = new JButton("Post");
        bottomPanel.add(postButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        // 버튼 액션
        postButton.addActionListener(this::onPostClicked);

        // 예약 포스트 자동 처리 타이머 시작 (1분마다 체크)
        startReservedPostWorker();

        setVisible(true);
    }

    // ===== Post 버튼 눌렀을 때 동작 =====
    private void onPostClicked(ActionEvent e) {
        String content = contentArea.getText().trim();

        if (content.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "내용을 입력하세요.",
                    "입력 오류",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!reserveCheckBox.isSelected()) {
            // 즉시 포스트
            if (insertPostNow(content)) {
                JOptionPane.showMessageDialog(this,
                        "포스트가 등록되었습니다.",
                        "성공",
                        JOptionPane.INFORMATION_MESSAGE);
                contentArea.setText("");
            }
        } else {
            // 예약 포스트
            String dtText = datetimeField.getText().trim();
            if (dtText.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "예약 시간을 입력하세요. (yyyy-MM-dd HH:mm)",
                        "입력 오류",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            LocalDateTime scheduledTime;
            try {
                scheduledTime = LocalDateTime.parse(dtText, formatter);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "예약 시간 형식이 올바르지 않습니다.\n예: 2025-11-22 23:30",
                        "형식 오류",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (insertReservedPost(content, scheduledTime)) {
                JOptionPane.showMessageDialog(this,
                        "예약 포스트가 등록되었습니다.",
                        "성공",
                        JOptionPane.INFORMATION_MESSAGE);
                contentArea.setText("");
                datetimeField.setText("");
                reserveCheckBox.setSelected(false);
            }
        }
    }

    // ===== 즉시 포스트: posts 테이블에 INSERT (+ 해시태그 저장) =====
    private boolean insertPostNow(String content) {
        String sql = "INSERT INTO posts (content, writer_id) VALUES (?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, content);
            ps.setString(2, currentUserId);
            ps.executeUpdate();

            int postId = -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) postId = keys.getInt(1);
            }
            if (postId > 0) {
                saveHashtags(conn, postId, content); // 본문 내 #태그 저장
            }
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "포스트 저장 중 오류: " + ex.getMessage(),
                    "DB 오류",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // ===== 예약 포스트: reserved_post 테이블에 INSERT =====
    private boolean insertReservedPost(String content, LocalDateTime scheduledTime) {
        String sql = "INSERT INTO reserved_post (writer_id, content, scheduled_time, is_posted) " +
                     "VALUES (?, ?, ?, FALSE)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, currentUserId);
            ps.setString(2, content);
            ps.setTimestamp(3, Timestamp.valueOf(scheduledTime));
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "예약 포스트 저장 중 오류: " + ex.getMessage(),
                    "DB 오류",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // ===== 예약 포스트 자동 처리: 일정 시간마다 reserved_post → posts 이동 (+ 해시태그 저장) =====
    private void startReservedPostWorker() {
        // 60초마다 한 번씩 예약글 처리 (EDT에서 안전하게 동작하는 Swing Timer 사용)
        int delay = 60 * 1000;
        new javax.swing.Timer(delay, e -> processReservedPosts()).start();
    }

    /**
     * reserved_post 테이블에서
     * - scheduled_time <= 현재 시각
     * - is_posted = FALSE
     * 인 것들을 찾아서
     * 1) posts 테이블에 INSERT
     * 2) reserved_post.is_posted = TRUE 로 업데이트
     * 3) 해당 content의 해시태그 post_tag에 저장
     */
    private void processReservedPosts() {
        String selectSql =
                "SELECT s_id, writer_id, content, scheduled_time " +
                "FROM reserved_post " +
                "WHERE is_posted = FALSE AND scheduled_time <= NOW()";

        String insertPostSql =
                "INSERT INTO posts (content, writer_id, created_at) " +
                "VALUES (?, ?, NOW())";

        String updateReservedSql =
                "UPDATE reserved_post SET is_posted = TRUE WHERE s_id = ?";

        try (PreparedStatement psSelect = conn.prepareStatement(selectSql);
             ResultSet rs = psSelect.executeQuery()) {

            while (rs.next()) {
                int sId = rs.getInt("s_id");
                String writerId = rs.getString("writer_id");
                String content  = rs.getString("content");

                int newPostId = -1;

                // 1) posts에 INSERT (생성된 post_id 회수)
                try (PreparedStatement psInsert =
                             conn.prepareStatement(insertPostSql, Statement.RETURN_GENERATED_KEYS)) {
                    psInsert.setString(1, content);
                    psInsert.setString(2, writerId);
                    psInsert.executeUpdate();
                    try (ResultSet keys = psInsert.getGeneratedKeys()) {
                        if (keys.next()) newPostId = keys.getInt(1);
                    }
                }

                // 1-1) 해시태그 저장
                if (newPostId > 0) {
                    saveHashtags(conn, newPostId, content);
                }

                // 2) reserved_post의 is_posted 갱신
                try (PreparedStatement psUpdate = conn.prepareStatement(updateReservedSql)) {
                    psUpdate.setInt(1, sId);
                    psUpdate.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            // 예약 워커는 조용히 로그만 출력
        }
    }

    /* ======================= 해시태그 저장 유틸 ======================= */

    private void saveHashtags(Connection con, int postId, String content) throws SQLException {
        if (content == null || content.isEmpty()) return;

        Matcher m = TAG.matcher(content);
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT IGNORE INTO post_tag(post_id, tag) VALUES(?, ?)")) {
            while (m.find()) {
                String tag = m.group(1);
                if (tag == null || tag.isBlank()) continue;

                String norm = tag.trim().toLowerCase(); // 정규화
                if (norm.length() > 50) norm = norm.substring(0, 50);

                ps.setInt(1, postId);
                ps.setString(2, norm);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ===== 실행용 main (단독 테스트용) =====
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Connection conn = DBConn.getConnection();
                String currentUserId = "kim";   // 테스트용. 실제 로그인 ID 사용.
                new PostWriterMain(conn, currentUserId);
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "DB 연결 실패: " + ex.getMessage(),
                        "DB 오류",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
