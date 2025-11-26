/*
 * 파일명: LoginPanel.java
 * 목적: 로그인 화면 UI와 동작을 담당하는 패널
 *
 * 구성/역할
 * - 중앙 정렬된 로그인 폼(ID, Password)과 두 개의 버튼
 *   1) Log in: 자격 증명 검증(LogIn2.login) 후 성공 시 메인 페이지로 전환
 *   2) Change password: 입력된 ID(없으면 현재 로그인 사용자)를 대상으로 비밀번호 변경 화면으로 이동
 *
 * 주요 동작 흐름
 * - onLoginClicked:
 *     · 입력값 기본 검증(공백/길이)
 *     · LogIn2.login(userId, password) 호출
 *     · 성공 → TwitterApp에 현재 사용자 설정(app.setCurrentUserId) → 메인 페이지로 전환
 *     · 실패/예외 → 모달 다이얼로그로 오류 알림
 * - onChangePwClicked:
 *     · 우선 입력칸의 ID 사용, 없으면 app.getCurrentUserId() 사용
 *     · 대상 ID가 없으면 경고
 *     · app.goChangePasswordFor(target) 호출로 비밀번호 변경 패널로 전환
 *
 * UI 배치
 * - 고정 크기 프레임(1200x800)을 기준으로 수동 좌표 배치(null layout)
 * - 타이틀, ID/Password 필드, 버튼들을 수평 중앙 정렬
 *
 * 상호작용/편의
 * - Enter 키로 로그인(submit) 되도록 root pane에 기본 버튼 설정
 * - 사용자 피드백을 위해 JOptionPane 메시지 사용
 *
 * 전제/의존
 * - TwitterApp: 페이지 전환 및 현재 사용자 상태 관리(PAGE_LOGIN, PAGE_MAIN 등)
 * - LogIn2.login(String, String): 로그인 검증 DAO/서비스
 */

package myPackage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginPanel extends JPanel {

    private final TwitterApp app;
    private JTextField idTxt;
    private JPasswordField pwdTxt;

    public LoginPanel(TwitterApp app) {
        this.app = app;
        setLayout(null); // 수동 좌표 배치

        // 고정 프레임 크기 기준(1200 x 800)
        final int PANEL_W = 1200;
        final int PANEL_H = 800;
        final int CX = PANEL_W / 2;

        // ===== 타이틀 =====
        JLabel logo = new JLabel("Log in", SwingConstants.CENTER);
        logo.setFont(new Font("SansSerif", Font.BOLD, 44));
        int logoW = 360, logoH = 100, logoY = 160;
        logo.setBounds(CX - logoW/2, logoY, logoW, logoH);
        add(logo);

        // ===== 라벨/입력 =====
        int fieldW = 320, fieldH = 38, gapY = 16;
        int labelW = 120, labelH = 24;
        int formY = logoY + logoH + 20;

        JLabel idLabel = new JLabel("ID :", SwingConstants.RIGHT);
        idLabel.setBounds(CX - fieldW/2 - labelW - 10, formY, labelW, labelH);
        add(idLabel);

        idTxt = new JTextField();
        idTxt.setBounds(CX - fieldW/2, formY - 6, fieldW, fieldH);
        add(idTxt);

        JLabel pwdLabel = new JLabel("Password :", SwingConstants.RIGHT);
        pwdLabel.setBounds(CX - fieldW/2 - labelW - 10, formY + fieldH + gapY, labelW, labelH);
        add(pwdLabel);

        pwdTxt = new JPasswordField();
        pwdTxt.setBounds(CX - fieldW/2, formY + fieldH + gapY - 6, fieldW, fieldH);
        add(pwdTxt);

        // ===== 버튼들 (왼쪽: Change password, 오른쪽: Log in) =====
        int btnW = 180, btnH = 42;
        int btnY = formY + fieldH*2 + gapY + 24;

        JButton btnChangePw = new JButton("Change password");
        JButton btnLogin    = new JButton("Log in");

        // 버튼 간격/위치(수평 중앙 정렬)
        int gapBtn = 16;
        int totalW = btnW*2 + gapBtn;

        btnChangePw.setBounds(CX - totalW/2, btnY, btnW, btnH);
        btnLogin.setBounds(CX - totalW/2 + btnW + gapBtn, btnY, btnW, btnH);

        add(btnChangePw);
        add(btnLogin);

        // ===== 이벤트 =====
        btnLogin.addActionListener(this::onLoginClicked);
        btnChangePw.addActionListener(this::onChangePwClicked);

        // Enter로 로그인 (기본 버튼 지정)
        SwingUtilities.invokeLater(() -> {
            JRootPane root = SwingUtilities.getRootPane(this);
            if (root != null) root.setDefaultButton(btnLogin);
        });
    }

    private void onLoginClicked(ActionEvent e) {
        String userId = idTxt.getText().trim();
        String password = new String(pwdTxt.getPassword());

        // 기본 입력 검증
        if (userId.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "ID와 Password를 입력하세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (userId.length() > 20) {
            JOptionPane.showMessageDialog(this, "ID는 최대 20자까지 가능합니다.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (password.length() > 20) {
            JOptionPane.showMessageDialog(this, "Password는 최대 20자까지 가능합니다.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // 자격 검증
            if (LogIn2.login(userId, password)) {
                app.setCurrentUserId(userId);           // 현재 사용자 상태 저장
                app.showPage(TwitterApp.PAGE_MAIN);     // 메인으로 전환
            } else {
                JOptionPane.showMessageDialog(this, "Log in failed", "Login Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "로그인 처리 중 오류가 발생했습니다.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onChangePwClicked(ActionEvent e) {
        // 입력된 ID가 있으면 우선 사용, 없으면 현재 로그인 사용자 사용
        String typedId = idTxt.getText().trim();
        String target  = !typedId.isEmpty() ? typedId : app.getCurrentUserId();

        if (target == null || target.isEmpty()) {
            JOptionPane.showMessageDialog(this, "변경할 사용자 ID가 없습니다. ID를 입력하거나 로그인 후 이용하세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        app.goChangePasswordFor(target); // 비밀번호 변경 화면으로 이동
    }
}
