import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JButton;
import java.awt.event.*;
import myPackage.LogIn2;
import java.awt.Panel;
import java.awt.Font;

public class twitter_GUI {

	private JFrame frame;
	private JTextField IdTxt;
	private JPasswordField PwdTxt;
	private JTextField ID;
	private JPasswordField Password;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					
					twitter_GUI window = new twitter_GUI();
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
	
	public twitter_GUI() {
		first_page();
	}
	 
	

	/**
	 * Initialize the contents of the frame.
	 */
	private void first_page() {
		frame = new JFrame();
		frame.setBounds(100, 100, 600, 400);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		
		
		JPanel first_page_panel = new JPanel();
		first_page_panel.setBounds(0, 0, 586, 363);
		frame.getContentPane().add(first_page_panel);
		first_page_panel.setLayout(null);
		
		JLabel Twitter_logo_1 = new JLabel("Twitter");
		Twitter_logo_1.setHorizontalAlignment(SwingConstants.CENTER);
		Twitter_logo_1.setBounds(214, 28, 106, 15);
		first_page_panel.add(Twitter_logo_1);
		
		JButton btnLogin_page = new JButton("Log in");
		btnLogin_page.setBounds(109, 145, 111, 50);
		first_page_panel.add(btnLogin_page);
		
		JButton btnSignUp_page = new JButton("Sign up");
		btnSignUp_page.setBounds(358, 145, 111, 50);
		first_page_panel.add(btnSignUp_page);
		
		
		first_page_panel.setVisible(true);
		
		btnLogin_page.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e)
			{
				first_page_panel.setVisible(false);
				Login(frame);
			}
		});
		
		btnSignUp_page.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e)
			{
				first_page_panel.setVisible(false);
				Singup(frame);
			}
		});
		
		
		
	}
	
	private void Login(JFrame frame)
	{
		
		JPanel Login_panel = new JPanel();
		Login_panel.setBounds(0, 0, 586, 363);
		frame.getContentPane().add(Login_panel);
		Login_panel.setLayout(null);
		
		JLabel ID = new JLabel("ID :");
		ID.setHorizontalAlignment(SwingConstants.CENTER);
		ID.setBounds(182, 126, 52, 15);
		Login_panel.add(ID);
		
		JLabel Pwd = new JLabel("Password :");
		Pwd.setHorizontalAlignment(SwingConstants.CENTER);
		Pwd.setBounds(127, 170, 125, 15);
		Login_panel.add(Pwd);
		
		JLabel Twitter_logo = new JLabel("Twitter");
		Twitter_logo.setHorizontalAlignment(SwingConstants.CENTER);
		Twitter_logo.setBounds(214, 28, 106, 15);
		Login_panel.add(Twitter_logo);
		
		JButton BtnLogin = new JButton("Log in");
		BtnLogin.setBounds(225, 234, 95, 23);
		Login_panel.add(BtnLogin);
		
		
		IdTxt = new JTextField();
		IdTxt.setBounds(228, 123, 106, 21);
		Login_panel.add(IdTxt);
		IdTxt.setColumns(20);
		
		PwdTxt = new JPasswordField();
		PwdTxt.setColumns(20);
		PwdTxt.setBounds(228, 167, 106, 21);
		Login_panel.add(PwdTxt);
		Login_panel.setVisible(true);
		
		BtnLogin.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e)
			{
				String ID = IdTxt.getText();
				String Password = PwdTxt.getText();
				
				try {
					if(LogIn2.login(ID,Password))
					{
						System.out.println("Login!!");
					}
				}
				catch(Exception e2)
				{
					e2.printStackTrace();
				}
				
			}
		});
	}
	
	private void Singup(JFrame frame)
	{
		JPanel SignUP_Panel = new JPanel();
		SignUP_Panel.setBounds(0, 0, 586, 363);
		frame.getContentPane().add(SignUP_Panel);
		SignUP_Panel.setLayout(null);
		
		JLabel ID_label = new JLabel("ID :");
		ID_label.setHorizontalAlignment(SwingConstants.CENTER);
		ID_label.setBounds(182, 126, 52, 15);
		SignUP_Panel.add(ID_label);
		
		JLabel Password_label = new JLabel("Password : ");
		Password_label.setHorizontalAlignment(SwingConstants.CENTER);
		Password_label.setBounds(127, 170, 125, 15);
		SignUP_Panel.add(Password_label);
		
		ID = new JTextField();
		ID.setBounds(228, 123, 106, 21);
		SignUP_Panel.add(ID);
		ID.setColumns(20);
		
		Password = new JPasswordField();
		Password.setBounds(228, 167, 106, 21);
		SignUP_Panel.add(Password);
		Password.setColumns(20);
		
		JLabel Singup_Logo = new JLabel("Sign up");
		Singup_Logo.setFont(new Font("굴림", Font.PLAIN, 18));
		Singup_Logo.setHorizontalAlignment(SwingConstants.CENTER);
		Singup_Logo.setBounds(214, 57, 130, 28);
		SignUP_Panel.add(Singup_Logo);
		
		JButton btnSingup = new JButton("Sing up");
		btnSingup.setBounds(228, 223, 95, 23);
		SignUP_Panel.add(btnSingup);
		
		btnSingup.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e)
			{
				
				
			}
		});
	}
}
