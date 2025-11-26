// TwitterApp.java
package myPackage;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * TwitterApp
 * -----------------------------------------------------------------------------
 * 역할
 *  - 애플리케이션의 메인 프레임.
 *  - CardLayout 기반으로 각 화면(첫 화면/로그인/회원가입/메인/비밀번호 변경/동적 보드/해시태그)을 전환한다.
 *
 * 화면 구성
 *  - 정적 카드:
 *      • PAGE_FIRST   : FirstPagePanel
 *      • PAGE_LOGIN   : LoginPanel
 *      • PAGE_SIGNUP  : SignupPanel
 *      • PAGE_MAIN    : MainPagePanel
 *      • PAGE_CHPASS  : ChangePasswordPanel
 *  - 동적 카드(캐시):
 *      • 개인 보드(PersonalBoardPanel) : 사용자별 "board:{userId}" 키로 추가/재사용
 *      • 해시태그 결과(HashtagResultPanel) : "hashtag:{tag}" 키로 추가/재사용
 *
 * 핵심 상태
 *  - conn           : DB 연결 객체(앱 시작 시 1회 획득)
 *  - currentUserId  : 현재 로그인한 사용자 ID
 *
 * 핵심 동작
 *  - showPage(name)                     : 지정된 카드로 전환
 *  - openPersonalBoard(userId)          : 사용자 존재 확인 후 개인 보드 카드를 동적 생성/표시
 *  - openHashtag(raw/#tag)              : 태그 정규화 후 결과 패널 동적 생성/표시
 *  - goChangePasswordFor(userId)        : 비밀번호 변경 대상 지정 후 패널 전환
 *  - refreshPersonalBoardHeader(userId) : 특정 보드 헤더(레벨/팔로워/팔로잉) 즉시 갱신
 *  
 */
public class TwitterApp extends JFrame {

    private CardLayout cardLayout;
    private JPanel cardPanel;

    private Connection conn;
    private String currentUserId;

    public static final String PAGE_FIRST   = "first";
    public static final String PAGE_LOGIN   = "login";
    public static final String PAGE_SIGNUP  = "signup";
    public static final String PAGE_MAIN    = "main";
    public static final String PAGE_CHPASS  = "change_password";

    // 동적 카드 캐시: 동일 사용자/태그로 재방문 시 패널 재사용
    private final Map<String, PersonalBoardPanel> personalBoards = new HashMap<>();
    private final Map<String, HashtagResultPanel> hashtagPanels  = new HashMap<>();

    // 비밀번호 변경 패널(정적 카드)
    private ChangePasswordPanel changePwPage;

    public TwitterApp() {
        setTitle("Twitter Clone");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        try {
            conn = DBConn.getConnection(); // 앱 시작 시 DB 커넥션 한번 획득
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "DB 연결 실패: " + e.getMessage(), "DB 오류", JOptionPane.ERROR_MESSAGE);
        }

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);

        // 정적 카드 구성
        FirstPagePanel firstPage  = new FirstPagePanel(this);
        LoginPanel     loginPage  = new LoginPanel(this);
        SignupPanel    signupPage = new SignupPanel(this);
        MainPagePanel  mainPage   = new MainPagePanel(this);
        changePwPage              = new ChangePasswordPanel(this);

        cardPanel.add(firstPage,  PAGE_FIRST);
        cardPanel.add(loginPage,  PAGE_LOGIN);
        cardPanel.add(signupPage, PAGE_SIGNUP);
        cardPanel.add(mainPage,   PAGE_MAIN);
        cardPanel.add(changePwPage, PAGE_CHPASS);

        add(cardPanel);
        showPage(PAGE_FIRST); // 초기 진입 화면
        new javax.swing.Timer(60_000, e -> ReservedPostWorker.runOnce()).start();

    }

    /** 카드 전환 헬퍼 */
    public void showPage(String pageName) {
        cardLayout.show(cardPanel, pageName);
    }

    public void setCurrentUserId(String userId) { this.currentUserId = userId; }
    public String getCurrentUserId() { return currentUserId; }
    public Connection getConnection() { return conn; }

    /* ===================== 사용자 존재 확인 ===================== */

    /** user 테이블에 해당 아이디가 존재하는지 간단 확인 */
    private boolean userExists(String userId) {
        if (userId == null || userId.isBlank()) return false;
        String sql = "SELECT 1 FROM user WHERE user_id=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            // 오류는 UI에 노출하지 않고 존재하지 않는 것으로 처리
            return false;
        }
    }

    /* ===================== 개인보드 열기 ===================== */

    /**
     * 사용자 존재 검증 후 해당 사용자의 PersonalBoardPanel을 동적으로 생성/표시.
     * 이미 생성되어 있으면 캐시에서 재사용.
     */
    public void openPersonalBoard(String userId) {
        if (userId == null || userId.isBlank()) {
            JOptionPane.showMessageDialog(this, "유효하지 않은 사용자입니다.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // 존재 검증
        if (!userExists(userId)) {
            JOptionPane.showMessageDialog(this, "해당 ID의 사용자가 없습니다: " + userId, "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        final String cardName = "board:" + userId;

        PersonalBoardPanel panel = personalBoards.get(userId);
        if (panel == null) {
            panel = new PersonalBoardPanel(this, userId);
            personalBoards.put(userId, panel);
            cardPanel.add(panel, cardName);
        }
        cardLayout.show(cardPanel, cardName);
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    /** 현재 로그인한 본인 보드로 바로 이동 */
    public void openMyBoard() {
        if (currentUserId == null || currentUserId.isBlank()) {
            JOptionPane.showMessageDialog(this, "로그인이 필요합니다.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        openPersonalBoard(currentUserId);
    }

    /* ===================== 비밀번호 변경 진입 ===================== */

    /** 대상 사용자 지정 후 비밀번호 변경 화면으로 전환 */
    public void goChangePasswordFor(String userId) {
        if (userId == null || userId.isBlank()) {
            JOptionPane.showMessageDialog(this, "대상 사용자가 없습니다.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        changePwPage.setTargetUser(userId);
        showPage(PAGE_CHPASS);
    }

    /* ===================== 해시태그 결과 열기 ===================== */

    /** 구 버전 별칭(호환) */
    public void openHashtagResult(String raw) { openHashtag(raw); }

    /**
     * 해시태그 결과 패널 동적 생성/표시.
     * raw가 "#tag" 형식이면 접두어 제거 후 소문자로 정규화하여 캐시 키로 사용.
     */
    public void openHashtag(String raw) {
        if (raw == null) return;
        String tag = raw.startsWith("#") ? raw.substring(1) : raw;
        tag = tag.trim();
        if (tag.isEmpty()) {
            JOptionPane.showMessageDialog(this, "해시태그를 입력하세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        tag = tag.toLowerCase(); // 카드 키는 소문자 통일
        final String cardName = "hashtag:" + tag;

        HashtagResultPanel p = hashtagPanels.get(tag);
        if (p == null) {
            p = new HashtagResultPanel(this, tag);
            hashtagPanels.put(tag, p);
            cardPanel.add(p, cardName);
        } else {
            p.reload(); // 이미 존재하면 최신 데이터로 갱신
        }
        cardLayout.show(cardPanel, cardName);
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    /* ===================== 보드 헤더(팔로워/팔로잉/레벨) 즉시 갱신 ===================== */

    /**
     * 특정 PersonalBoardPanel 상단 헤더를 즉시 새로고침한다.
     * (팔로워/팔로잉/레벨/토글 상태 등)
     */
    public void refreshPersonalBoardHeader(String userId) {
        if (userId == null || userId.isBlank()) return;
        PersonalBoardPanel p = personalBoards.get(userId);
        if (p != null) {
            SwingUtilities.invokeLater(p::refreshSocialHeader);
        }
    }

    /** 애플리케이션 엔트리 포인트 */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            TwitterApp app = new TwitterApp();
            app.setVisible(true);
        });
    }
}
