/*
 * 파일명: ChangePasswordPanel.java
 * 목적: 로그인한 사용자가 비밀번호를 안전하게 변경할 수 있는 전용 UI 패널.
 *
 * 화면 구성(고정 좌표 배치; SignupPanel 과 동일 스타일):
 *  - 상단 큰 제목 "Change Password"
 *  - 중앙 입력 폼: [현재 비밀번호] [새 비밀번호] [새 비밀번호 확인]
 *  - 하단 버튼: [Back] [Update]  (입력란 바로 아래에 좌우 배치)
 *
 * 핵심 동작 흐름:
 *  - TwitterApp.goChangePasswordFor(userId) 가 이 패널을 열고 setTargetUser(userId)로 대상 주입
 *  - 사용자가 입력 후 [Update] 클릭 → 입력 검증(빈값/일치/길이)
 *  - DB 트랜잭션:
 *      1) (확인) user(user_id, pwd) 일치 여부 SELECT
 *      2) (변경) UPDATE user SET pwd=? WHERE user_id=? AND pwd=?  ← WHERE 에 현재 비번 포함(경쟁상황 방지)
 *  - 성공 시 알림 후 로그인 화면으로 이동
 *
 * 의존:
 *  - DBConn.getConnection() : 커넥션 획득
 *  - TwitterApp.PAGE_LOGIN  : 변경 완료 후 돌아갈 카드 이름
 *  - TwitterApp.goChangePasswordFor(String) 로 진입
 *
 * 주의/보안:
 *  - 현재 구현은 평문 비밀번호 비교/저장(DB 스키마가 pwd VARCHAR(20)) → 수업/연습용 가정.
 *    실제 서비스에서는 반드시 해시(예: BCrypt) 사용과 비밀번호 정책(길이/복잡도/재사용 등) 필요.
 *  - UPDATE 문에 현재 비밀번호를 함께 넣어 원자성 보장(동시 변경 충돌 방지).
 *  - try-with-resources 로 자원 누수 방지.
 */

package myPackage;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class ChangePasswordPanel extends JPanel {

    private final TwitterApp app;
    private String targetUserId;

    private JPasswordField curPwField;
    private JPasswordField newPwField;
    private JPasswordField confirmPwField;

    public ChangePasswordPanel(TwitterApp app) {
        this.app = app;
        setLayout(null); // SignupPanel과 동일: 고정 좌표 배치

        // 고정 프레임 크기 기준(1200 x 800)
        final int PANEL_W = 1200;
        final int PANEL_H = 800;
        final int CX = PANEL_W / 2;

        // ===== 타이틀 =====
        JLabel title = new JLabel("Change Password", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 44));
        int titleW = 540, titleH = 100, titleY = 140;  // SignupPanel과 비슷한 높이
        title.setBounds(CX - titleW/2, titleY, titleW, titleH);
        add(title);

        // ===== 라벨/입력 =====
        int fieldW = 320, fieldH = 38, gapY = 16;
        int labelW = 220, labelH = 24;   // 라벨 길이를 조금 넉넉히
        int formY  = titleY + titleH + 20;

        JLabel curPwLbl = new JLabel("Current password :", SwingConstants.RIGHT);
        curPwLbl.setBounds(CX - fieldW/2 - labelW - 10, formY, labelW, labelH);
        add(curPwLbl);

        curPwField = new JPasswordField();
        curPwField.setBounds(CX - fieldW/2, formY - 6, fieldW, fieldH);
        add(curPwField);

        JLabel newPwLbl = new JLabel("New password :", SwingConstants.RIGHT);
        newPwLbl.setBounds(CX - fieldW/2 - labelW - 10, formY + fieldH + gapY, labelW, labelH);
        add(newPwLbl);

        newPwField = new JPasswordField();
        newPwField.setBounds(CX - fieldW/2, formY + fieldH + gapY - 6, fieldW, fieldH);
        add(newPwField);

        JLabel confirmPwLbl = new JLabel("Confirm new password :", SwingConstants.RIGHT);
        confirmPwLbl.setBounds(CX - fieldW/2 - labelW - 10, formY + (fieldH + gapY)*2, labelW, labelH);
        add(confirmPwLbl);

        confirmPwField = new JPasswordField();
        confirmPwField.setBounds(CX - fieldW/2, formY + (fieldH + gapY)*2 - 6, fieldW, fieldH);
        add(confirmPwField);

        // ===== 버튼 (입력란 바로 아래, 가운데 정렬) =====
        int btnW = 180, btnH = 42;
        int btnY  = formY + (fieldH + gapY)*3 + 24;

        JButton backBtn = new JButton("Back");
        backBtn.setBounds(CX - btnW - 10, btnY, btnW, btnH);
        add(backBtn);

        JButton saveBtn = new JButton("Update");
        saveBtn.setBounds(CX + 10, btnY, btnW, btnH);
        add(saveBtn);

        // 이벤트
        backBtn.addActionListener(e -> app.showPage(TwitterApp.PAGE_LOGIN));
        saveBtn.addActionListener(e -> doChange());

        // Enter로 제출 (기본 버튼 지정)
        SwingUtilities.invokeLater(() -> {
            JRootPane root = SwingUtilities.getRootPane(this);
            if (root != null) root.setDefaultButton(saveBtn);
        });
    }

    /** 
     * 화면 전환 전, TwitterApp 이 대상 사용자 ID를 주입하는 메서드.
     * 패널 재사용 시 잔여 입력값 초기화를 함께 수행.
     */
    public void setTargetUser(String userId) {
        this.targetUserId = userId;
        // 전환 시 필드 클리어
        if (curPwField != null)     curPwField.setText("");
        if (newPwField != null)     newPwField.setText("");
        if (confirmPwField != null) confirmPwField.setText("");
    }

    /**
     * [Update] 버튼 핸들러:
     * - 입력 검증(빈칸/일치/길이) → DB 확인/변경
     * - UPDATE 시 WHERE 에 현재 비밀번호를 포함하여 경쟁상황 방지
     */
    private void doChange() {
        if (targetUserId == null || targetUserId.isBlank()) {
            JOptionPane.showMessageDialog(this, "대상 사용자가 없습니다. 로그인 화면에서 다시 시도하세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String cur = new String(curPwField.getPassword());
        String nw  = new String(newPwField.getPassword());
        String cf  = new String(confirmPwField.getPassword());

        // --- 기본 입력 검증 ---
        if (cur.isEmpty() || nw.isEmpty() || cf.isEmpty()) {
            JOptionPane.showMessageDialog(this, "모든 칸을 입력하세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!nw.equals(cf)) {
            JOptionPane.showMessageDialog(this, "새 비밀번호와 확인이 일치하지 않습니다.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (nw.length() < 4) {
            JOptionPane.showMessageDialog(this, "비밀번호는 4자 이상으로 해주세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // --- DB 처리 (try-with-resources 로 자원 정리 보장) ---
        try (Connection con = DBConn.getConnection()) {
            // 1) 현재 비번 확인
            String checkSql = "SELECT 1 FROM user WHERE user_id = ? AND pwd = ? LIMIT 1";
            try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                ps.setString(1, targetUserId);
                ps.setString(2, cur);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(this, "현재 비밀번호가 올바르지 않습니다.", "실패", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            // 2) 비번 변경(원자적 보장: WHERE에 현재 비번 포함 → 중간에 다른 곳에서 바꿔도 안전)
            String updateSql = "UPDATE user SET pwd = ? WHERE user_id = ? AND pwd = ?";
            try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                ps.setString(1, nw);
                ps.setString(2, targetUserId);
                ps.setString(3, cur);
                int updated = ps.executeUpdate();
                if (updated == 0) {
                    // 이 경우: 확인 시점과 변경 시점 사이에 pwd 가 바뀐 상황 등
                    JOptionPane.showMessageDialog(this, "변경 중 충돌이 발생했습니다. 다시 시도하세요.", "실패", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            JOptionPane.showMessageDialog(this, "비밀번호가 변경되었습니다.", "완료", JOptionPane.INFORMATION_MESSAGE);
            app.showPage(TwitterApp.PAGE_LOGIN);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "변경 중 오류가 발생했습니다.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
