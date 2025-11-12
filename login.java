import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class login {
	
	 public static void main(String[] args)
		{
			SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
		}
	
	static class LoginFrame extends JFrame {
		private final JTextField idField = new JTextField(15); // 요구사항: 2개의 텍스트박스
		private final JPasswordField pwdField = new JPasswordField(15); // 비밀번호는 마스킹(텍스트필드 원하면 JTextField로 교체 가능)
		private final JButton loginBtn = new JButton("Login");
		private final JLabel statusLabel = new JLabel(" ");


		LoginFrame() {
		super("Twitter Login");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(360, 200);
		setLocationRelativeTo(null);
		setLayout(new GridBagLayout());


		GridBagConstraints gc = new GridBagConstraints();
		gc.insets = new Insets(6, 8, 6, 8);
		gc.fill = GridBagConstraints.HORIZONTAL;


		// Row 0: ID 라벨 + 텍스트박스
		gc.gridx = 0; gc.gridy = 0; gc.weightx = 0; add(new JLabel("ID"), gc);
		gc.gridx = 1; gc.gridy = 0; gc.weightx = 1; add(idField, gc);


		// Row 1: Password 라벨 + 텍스트박스
		gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; add(new JLabel("Password"), gc);
		gc.gridx = 1; gc.gridy = 1; gc.weightx = 1; add(pwdField, gc);


		// Row 2: Login 버튼
		gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 2; gc.weightx = 1;
		add(loginBtn, gc);


		// Row 3: 상태 라벨
		gc.gridx = 0; gc.gridy = 3; gc.gridwidth = 2; gc.weightx = 1;
		
		loginBtn.addActionListener(e -> doLogin());
		Action enterAction = new AbstractAction() {
		@Override public void actionPerformed(ActionEvent e) { doLogin(); }
		};
		idField.addActionListener(enterAction);
		pwdField.addActionListener(enterAction);
		}
		
		private void doLogin() {
			String userId = idField.getText().trim();
			String password = new String(pwdField.getPassword());


			if (userId.isEmpty() || password.isEmpty()) {
			setStatus("ID/Password를 모두 입력하세요.", Color.RED);
			return;
			}


			loginBtn.setEnabled(false);
			setStatus("로그인 중...", Color.DARK_GRAY);


			// UI 멈춤 방지를 위해 SwingWorker 사용(간단 예시)
			
		}
		
		private void setStatus(String msg, Color color) {
			statusLabel.setForeground(color);
			statusLabel.setText(msg);
			}
		
	
}
}
