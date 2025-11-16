import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JButton;
import java.awt.event.*;

public class Login {

	private JFrame frame;
	private JTextField IdTxt;
	private JPasswordField PwdTxt;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Login window = new Login();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Login() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 600, 400);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		JPanel panel = new JPanel();
		panel.setBounds(0, 0, 586, 363);
		frame.getContentPane().add(panel);
		panel.setLayout(null);
		
		JLabel ID = new JLabel("ID :");
		ID.setHorizontalAlignment(SwingConstants.CENTER);
		ID.setBounds(182, 126, 52, 15);
		panel.add(ID);
		
		JLabel Pwd = new JLabel("Password :");
		Pwd.setHorizontalAlignment(SwingConstants.CENTER);
		Pwd.setBounds(127, 170, 125, 15);
		panel.add(Pwd);
		
		IdTxt = new JTextField();
		IdTxt.setBounds(228, 123, 106, 21);
		panel.add(IdTxt);
		IdTxt.setColumns(20);
		
		PwdTxt = new JPasswordField();
		PwdTxt.setColumns(20);
		PwdTxt.setBounds(228, 167, 106, 21);
		panel.add(PwdTxt);
		
		JLabel Twitter_logo = new JLabel("Twitter_Design");
		Twitter_logo.setHorizontalAlignment(SwingConstants.CENTER);
		Twitter_logo.setBounds(214, 28, 106, 15);
		panel.add(Twitter_logo);
		
		JButton BtnLogin = new JButton("Log in");
		BtnLogin.setBounds(225, 234, 95, 23);
		panel.add(BtnLogin);
		
		BtnLogin.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e)
			{
				String ID = IdTxt.getText();
				String Password = PwdTxt.getText();
			}
		});
	}
}
