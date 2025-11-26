package myPackage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * SignupPanel
 * -----------------------------------------------------------------------------
 * 목적
 *  - 신규 사용자가 계정을 생성할 수 있는 회원가입 화면을 제공한다.
 *
 * 주요 구성
 *  - 중앙 정렬 폼(UI 고정 크기 1200x800 기준):
 *      • ID 입력(TextField)
 *      • Password 입력(PasswordField)
 *      • "Sign up" 제출 버튼
 *
 * 동작 흐름
 *  1) 사용자가 ID/Password를 입력하고 "Sign up" 클릭
 *  2) 입력값 기본 검증(공백, 최대 길이)
 *  3) MemberJoin.signUp(userId, password) 호출로 DB 회원가입 시도
 *  4) 성공 시 안내 후 로그인 화면으로 전환(TwitterApp.PAGE_LOGIN)
 *     실패 시 사유 안내(중복 ID 등)
 *
 * 연동 지점
 *  - TwitterApp: 화면 전환(app.showPage)
 *  - MemberJoin: 실제 회원가입 로직(INSERT 및 중복 처리)
 *
 * UI/UX 메모
 *  - Enter 키로 제출 가능하도록 루트페인 기본 버튼을 "Sign up"으로 지정
 *  - 경고/오류/성공 안내는 JOptionPane 사용
 */
public class SignupPanel extends JPanel {

    private final TwitterApp app;
    private JTextField idField;
    private JPasswordField passwordField;

    public SignupPanel(TwitterApp app) {
        this.app = app;
        setLayout(null); // 고정 좌표 배치(프레임 1200x800 기준)

        // 고정 프레임 크기 기준(1200 x 800)
        final int PANEL_W = 1200;
        final int PANEL_H = 800;
        final int CX = PANEL_W / 2;

        // ===== 타이틀 =====
        JLabel signupLogo = new JLabel("Sign up", SwingConstants.CENTER);
        signupLogo.setFont(new Font("SansSerif", Font.BOLD, 44));
        int logoW = 360, logoH = 100, logoY = 160;
        signupLogo.setBounds(CX - logoW/2, logoY, logoW, logoH);
        add(signupLogo);

        // ===== 라벨/입력 =====
        int fieldW = 320, fieldH = 38, gapY = 16;
        int labelW = 120, labelH = 24;
        int formY = logoY + logoH + 20;

        JLabel idLabel = new JLabel("ID :", SwingConstants.RIGHT);
        idLabel.setBounds(CX - fieldW/2 - labelW - 10, formY, labelW, labelH);
        add(idLabel);

        idField = new JTextField();
        idField.setBounds(CX - fieldW/2, formY - 6, fieldW, fieldH);
        add(idField);

        JLabel pwdLabel = new JLabel("Password :", SwingConstants.RIGHT);
        pwdLabel.setBounds(CX - fieldW/2 - labelW - 10, formY + fieldH + gapY, labelW, labelH);
        add(pwdLabel);

        passwordField = new JPasswordField();
        passwordField.setBounds(CX - fieldW/2, formY + fieldH + gapY - 6, fieldW, fieldH);
        add(passwordField);

        // ===== 회원가입 버튼 =====
        JButton btnSignup = new JButton("Sign up");
        int btnW = 180, btnH = 42;
        int btnY = formY + fieldH*2 + gapY + 24;
        btnSignup.setBounds(CX - btnW/2, btnY, btnW, btnH);
        add(btnSignup);

        // 이벤트: 회원가입 시도
        btnSignup.addActionListener(this::onSignupClicked);

        // Enter로 제출 (기본 버튼 지정)
        SwingUtilities.invokeLater(() -> {
            JRootPane root = SwingUtilities.getRootPane(this);
            if (root != null) root.setDefaultButton(btnSignup);
        });
    }

    /** "Sign up" 클릭 처리: 값 검증 → MemberJoin.signUp → 성공 시 로그인 화면 이동 */
    private void onSignupClicked(ActionEvent e) {
        String userId = idField.getText().trim();
        String password = new String(passwordField.getPassword());

        // 간단 검증
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
            boolean ok = MemberJoin.signUp(userId, password);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Sign up success", "Success", JOptionPane.INFORMATION_MESSAGE);
                // 입력 초기화 후 로그인 화면으로
                idField.setText("");
                passwordField.setText("");
                app.showPage(TwitterApp.PAGE_LOGIN);
            } else {
                JOptionPane.showMessageDialog(this, "Sign up failed (이미 사용 중인 ID일 수 있습니다).", "Sign up Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "회원가입 처리 중 오류가 발생했습니다.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
