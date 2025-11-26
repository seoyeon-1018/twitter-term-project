/*
 * 파일명: FirstPagePanel.java
 * 목적: 앱 실행 직후 표시되는 첫 진입 화면(Welcome/랜딩 페이지) 패널.
 *
 * 개요
 * - 중앙에 간단한 로고 텍스트와 두 개의 메인 버튼(Log in / Sign up)을 고정 좌표 배치로 제공합니다.
 * - 사용자는 여기서 로그인 화면 또는 회원가입 화면으로 이동합니다.
 *
 * 주요 책임
 *  1) UI 레이아웃: 프레임을 1200x800 기준으로 가정하고 컴포넌트의 절대 위치를 지정합니다.
 *  2) 네비게이션: 버튼 클릭 시 TwitterApp의 카드 레이아웃을 전환하여
 *     - Log in → PAGE_LOGIN
 *     - Sign up → PAGE_SIGNUP
 *     으로 이동합니다.
 *
 * 협력 객체
 * - TwitterApp: showPage(String) API로 카드 전환을 수행합니다.
 *
 */

package myPackage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class FirstPagePanel extends JPanel {

    private final TwitterApp app;

    public FirstPagePanel(TwitterApp app) {
        this.app = app;
        setLayout(null); // 절대 좌표 배치(고정 크기 레이아웃 가정)

        // 고정 프레임 사이즈 기준 좌표(프레임 1200 x 800)
        final int PANEL_W = 1200;
        final int PANEL_H = 800;
        final int CX = PANEL_W / 2; // 화면 가로 중앙 x

        // ===== 로고 =====
        JLabel twitterLogo = new JLabel("Twitter", SwingConstants.CENTER);
        twitterLogo.setFont(new Font("SansSerif", Font.BOLD, 48));
        int logoW = 360, logoH = 100, logoY = 180; // 로고 폭/높이/상단 y 위치
        twitterLogo.setBounds(CX - logoW / 2, logoY, logoW, logoH);
        add(twitterLogo);

        // ===== 버튼들 =====
        int btnW = 180, btnH = 48, btnY = logoY + logoH + 40; // 로고 아래 버튼 y
        int gap = 24;  // 좌우 버튼 사이 간격

        JButton btnLogin = new JButton("Log in");
        // 중앙 기준으로 왼쪽 버튼 배치
        btnLogin.setBounds(CX - gap/2 - btnW, btnY, btnW, btnH);
        add(btnLogin);

        JButton btnSignUp = new JButton("Sign up");
        // 중앙 기준으로 오른쪽 버튼 배치
        btnSignUp.setBounds(CX + gap/2, btnY, btnW, btnH);
        add(btnSignUp);

        // 버튼 이벤트: TwitterApp 카드 전환
        btnLogin.addActionListener(this::onLoginClicked);
        btnSignUp.addActionListener(this::onSignupClicked);
    }

    // 로그인 화면으로 이동
    private void onLoginClicked(ActionEvent e) {
        app.showPage(TwitterApp.PAGE_LOGIN);
    }

    // 회원가입 화면으로 이동
    private void onSignupClicked(ActionEvent e) {
        app.showPage(TwitterApp.PAGE_SIGNUP);
    }
}
