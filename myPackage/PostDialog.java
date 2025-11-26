/*
 * 파일명: PostDialog.java
 * 목적: 새 게시글을 작성·저장하는 모달 다이얼로그.
 *
 * 화면/동작 개요
 * - 텍스트 영역(TextArea)에 내용을 입력하고 [Post] 버튼으로 DB(posts) 테이블에 INSERT.
 * - [Cancel] 또는 X 버튼으로 닫기 가능. 닫힌 뒤 isPosted()로 작성 성공 여부 확인.
 * - 모달(true) 다이얼로그이므로 부모(Frame)를 블록하고, 작성/취소가 끝나면 닫힘.
 *
 * 주요 책임
 * - 매우 단순한 포스트 작성 UX 제공(내용 필수 검사).
 * - DB 연동: INSERT INTO posts(content, writer_id) VALUES(?, ?)
 * - 성공 시 posted 플래그를 true로 설정하여 호출측이 후처리(피드 리로드 등) 판단 가능.
 *
 * 협력/연동
 * - 외부에서 제공된 Connection(con) 과 현재 사용자 ID(userId)를 사용.
 * - 작성 후 호출측(예: MainPagePanel/PostWriter 등)에서 dialog.isPosted() 검사 → 피드 갱신.
 *
 * 제약/알림
 * - 해시태그 추출/저장(post_tag) 기능은 포함되어 있지 않음(필요 시 호출측 또는 별도 DAO에서 처리).
 * - 네트워크/DB 예외는 메시지 다이얼로그로 안내 후 다이얼로그는 유지(취소는 사용자가 직접).
 * - UI 스레드에서 실행되며, INSERT는 짧은 쿼리이므로 별도 스레드 처리 없이 즉시 수행.
 */

package myPackage;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class PostDialog extends JDialog {
    private boolean posted = false;   // 작성 성공 여부. 닫힌 뒤 isPosted()로 확인.

    public PostDialog(Frame owner, Connection con, String userId) {
        super(owner, "New Post", true); // true = modal(부모 프레임 블록)
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // 본문 입력 영역
        JTextArea ta = new JTextArea(6, 40);

        // 하단 버튼들
        JButton btnPost = new JButton("Post");
        JButton btnCancel = new JButton("Cancel");

        // [Post] 클릭 → 유효성 검사 → DB INSERT → 성공 시 posted=true → 닫기
        btnPost.addActionListener(e -> {
            String content = ta.getText().trim();
            if (content.isEmpty()) {
                JOptionPane.showMessageDialog(this, "내용을 입력하세요.");
                return;
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO posts(content, writer_id) VALUES(?, ?)")) {
                ps.setString(1, content);
                ps.setString(2, userId);
                ps.executeUpdate();
                posted = true;       // 성공 플래그
                dispose();           // 다이얼로그 종료
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "등록 실패", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // [Cancel] 클릭 → 다이얼로그 종료(작성 안 함)
        btnCancel.addActionListener(e -> dispose());

        // 버튼 영역 배치
        JPanel south = new JPanel();
        south.add(btnPost);
        south.add(btnCancel);

        // 레이아웃 구성: 중앙 입력 + 하단 버튼
        add(new JScrollPane(ta), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner); // 화면 중앙 정렬
    }

    /** 다이얼로그가 닫힌 뒤 작성 성공 여부를 반환(피드 갱신 판단에 사용) */
    public boolean isPosted() {
        return posted;
    }
}
