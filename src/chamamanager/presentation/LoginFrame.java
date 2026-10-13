
package chamamanager.presentation;

import chamamanager.exceptions.InvalidLoginException;
import chamamanager.logic.AuthService;
import chamamanager.model.Member;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;

    private final JPanel mainPanel;
    private final JPanel loginPanel;

    private final AuthService authService = new AuthService();

    public LoginFrame() {

        setTitle("Chama Manager - Login");
        setSize(420, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainPanel = new JPanel(new CardLayout());
        mainPanel.setBackground(new Color(0, 255, 255));

        loginPanel = new JPanel();
        loginPanel.setLayout(null);
        loginPanel.setBackground(new Color(0, 255, 255));

        // Username
        JLabel lblUsername = new JLabel("Username:");
        lblUsername.setBounds(50, 40, 100, 25);
        loginPanel.add(lblUsername);

        txtUsername = new JTextField();
        txtUsername.setBounds(150, 40, 180, 25);
        loginPanel.add(txtUsername);

        // Password
        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setBounds(50, 80, 100, 25);
        loginPanel.add(lblPassword);

        txtPassword = new JPasswordField();
        txtPassword.setBounds(150, 80, 180, 25);
        loginPanel.add(txtPassword);

        // Login button
        btnLogin = new JButton("Login");
        btnLogin.setBounds(150, 130, 100, 30);
        loginPanel.add(btnLogin);

        btnLogin.addActionListener(e -> login());

        // Add the login screen
        mainPanel.add(loginPanel, "LOGIN");
        add(mainPanel);
    }

    private void login() {

        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter username and password.",
                    "Missing Information",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            Member member = authService.login(username, password);

            // Route using the role retrieved from the database
            if ("TREASURER".equalsIgnoreCase(member.getRole())) {
    JOptionPane.showMessageDialog(
            this,
            "Welcome, Treasurer!",
            "Login Successful",
            JOptionPane.INFORMATION_MESSAGE
    );

    ManageMembersPanel manageMembers = new ManageMembersPanel();

    JFrame manageFrame = new JFrame("Chama Manager - Manage Members");
    manageFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    manageFrame.setSize(800, 500);
    manageFrame.setLocationRelativeTo(null);
    manageFrame.add(manageMembers);
    manageFrame.setVisible(true);

    this.dispose();

} else {

            

                MemberDashboard dashboard =
                        new MemberDashboard(member.getFullName());

                dashboard.setVisible(true);
                this.dispose();
            }

        } catch (InvalidLoginException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE
            );

        } catch (chamamanager.exceptions.DataAccessException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "A database error occurred. Please try again.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
