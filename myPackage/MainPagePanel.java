/*
 * 파일명: MainPagePanel.java
 * 목적: 로그인 이후 메인 화면의 레이아웃/상호작용을 담당하는 컨테이너 패널
 *
 * 화면 구성
 * - 상단: 검색 입력창 + [Search] + [My Board]
 * - 좌측: 팔로우 추천 패널(FollowRecommendPanel) — 스크롤 영역
 * - 중앙: 전체 피드(FeedPanel) — 스크롤 영역
 * - 좌하단: [Post] 버튼 — 글쓰기 창(PostWriterMain) 오픈
 *
 * 주요 동작
 * - Search 버튼:
 *     · 입력이 "#태그" 형태면 해시태그 결과 패널로 전환(app.openHashtagResult(tag))
 *     · 그 외 문자열이면 사용자 ID로 간주 → user 테이블에 존재 여부를 DB에서 확인
 *       존재하면 개인 보드 열기(app.openPersonalBoard), 없으면 에러 다이얼로그 표시
 * - My Board 버튼: 로그인된 사용자의 개인 보드로 이동(app.openMyBoard)
 * - Post 버튼: 글쓰기 창 오픈 → 창이 닫히면 중앙 피드/추천 목록을 즉시 갱신
 *
 * 레이아웃
 * - 고정 좌표 배치(null layout). 패널 크기 변경 시 layoutAll()에서 위치/크기를 재계산
 *
 * 연동/의존
 * - TwitterApp: 페이지 전환, DB 커넥션 공유, 현재 사용자 ID 취득
 * - DB 스키마: user 테이블(user_id 존재 여부 확인)
 * - FollowRecommendPanel, FeedPanel, PostWriterMain과의 UI 협력
 */

package myPackage;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MainPagePanel extends JPanel {

    private final TwitterApp app;

    // 상단 검색/버튼
    private JTextField searchField;
    private JButton searchBtn;
    private JButton myBoardBtn;

    // 좌측 추천 패널
    private FollowRecommendPanel recommendPanel;

    // 중앙 피드
    private FeedPanel feedPanel;

    // 좌하단 Post
    private JButton postButton;

    public MainPagePanel(TwitterApp app) {
        this.app = app;
        setLayout(null); // 고정 좌표 배치 사용

        // ===== 상단 검색 영역 =====
        searchField = new JTextField();
        add(searchField);

        searchBtn = new JButton("Search");
        add(searchBtn);

        myBoardBtn = new JButton("My Board");
        add(myBoardBtn);

        // ===== 좌측 추천 패널 =====
        recommendPanel = new FollowRecommendPanel(app);
        add(recommendPanel);

        // ===== 중앙 피드 =====
        feedPanel = new FeedPanel(app);
        add(feedPanel);

        // ===== 좌하단 Post 버튼 =====
        postButton = new JButton("Post");
        add(postButton);

        // ----- 리스너 -----
        searchBtn.addActionListener(e -> {
            String q = searchField.getText().trim();
            if (q.isEmpty()) {
                JOptionPane.showMessageDialog(this, "검색어를 입력하세요.", "알림", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 1) 해시태그 검색 분기: "#tag" 형태
            if (q.startsWith("#")) {
                String tag = q.substring(1).trim();
                if (tag.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "해시태그를 입력하세요. 예) #java", "알림", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                // 해시태그 결과 패널로 전환
                app.openHashtagResult(tag);
                return;
            }

            // 2) 사용자 검색: 개인 보드 열기 전 DB에서 존재 여부 확인
            try {
                Connection con = app.getConnection();
                if (con == null) {
                    JOptionPane.showMessageDialog(this, "DB 연결이 필요합니다.", "오류", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT 1 FROM user WHERE user_id = ? LIMIT 1")) {
                    ps.setString(1, q);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            app.openPersonalBoard(q); // 존재 → 보드 열기
                        } else {
                            // 존재하지 않음 → 사용자 없음 안내
                            JOptionPane.showMessageDialog(
                                    this,
                                    "해당 ID의 사용자가 없습니다: " + q,
                                    "사용자 없음",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "검색 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 내 보드로 이동
        myBoardBtn.addActionListener(e -> app.openMyBoard());

        // 글쓰기 버튼: 창 닫히면 피드/추천 목록 갱신
        postButton.addActionListener(e -> onPostButtonClicked());

        // 화면 표시/리사이즈 훅: 배치 재계산 및 추천 목록 갱신
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) {
                layoutAll();
                recommendPanel.reload(); // 표시될 때 추천 갱신
            }
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                layoutAll(); // 크기 변경 시 재배치
            }
        });
    }

    /** 글쓰기 창 오픈 → 닫힐 때 피드/추천 자동 갱신 */
    private void onPostButtonClicked() {
        String userId = app.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "로그인이 필요합니다.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // PostWriterMain은 DB 커넥션과 작성자 ID를 인자로 받는다
        PostWriterMain win = new PostWriterMain(app.getConnection(), userId);
        win.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        win.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                // 창이 닫히면 최신 글이 보이도록 피드 리로드
                feedPanel.reload();
                // 팔로우 변동 가능성이 있으면 추천도 갱신
                recommendPanel.reload();
            }
        });
        win.setVisible(true);
    }

    /** 고정 크기(1200x800) 기준 레이아웃 계산 및 동적 리사이즈 대응 */
    private void layoutAll() {
        int W = getWidth();
        int H = getHeight();

        int margin = 12;

        // 상단 검색 바 영역 치수
        int topH = 56;
        int searchFieldW = 320;
        int btnW = 120;
        int fieldH = 32;

        int x = margin;
        int y = margin;

        // 검색 필드
        searchField.setBounds(x, y + (topH - fieldH)/2, searchFieldW, fieldH);
        x += searchFieldW + 8;

        // Search 버튼
        searchBtn.setBounds(x, y + (topH - fieldH)/2, btnW, fieldH);
        x += btnW + 8;

        // My Board 버튼
        myBoardBtn.setBounds(x, y + (topH - fieldH)/2, btnW, fieldH);

        // 좌측 추천 패널
        int leftW = 260;                 // 추천 영역 너비
        int leftX = margin;
        int leftY = margin + topH + 4;
        int leftH = H - leftY - (margin + 48); // 하단 버튼/여백 고려
        recommendPanel.setBounds(leftX, leftY, leftW, Math.max(120, leftH));

        // 중앙 피드(추천 오른쪽 전체)
        int feedX = leftX + leftW + 12;
        int feedY = leftY;
        int feedW = W - feedX - margin;
        int feedH = leftH;
        feedPanel.setBounds(feedX, feedY, Math.max(320, feedW), Math.max(120, feedH));
        feedPanel.revalidate();
        feedPanel.repaint();

        // 좌하단 Post 버튼
        int btnH = 40;
        int btnW2 = 120;
        postButton.setBounds(margin, H - btnH - margin, btnW2, btnH);
    }
}
