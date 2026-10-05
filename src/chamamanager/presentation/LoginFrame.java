/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chamamanager.presentation;
import chamamanager.exceptions.InvalidLoginException;
import chamamanager.logic.AuthService;
import javax.swing.*;
import java.awt.*;

/**
 *
 * @author gh7
 */
public class LoginFrame extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    
    private AuthService authService = new AuthService();
    public LoginFrame(){
        setTitle("Chama Manager-login");
        setSize(400,250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        // Main panel
        JPanel panel = new JPanel();
        panel.setLayout(null);
        // User Panel
        JLabel username = new JLabel("Username");
        username.setBounds(50,40,100,25);
        panel.add(username);
        JTextField txtusername=new JTextField();
        txtusername.setBounds(150,40,180,25);
        panel.add(txtusername);
        
        JLabel lblpassword = new JLabel("Password");
        lblpassword.setBounds(50,80,100,25);
        panel.add(lblpassword);
        
        JPasswordField txtpassword= new JPasswordField();
        txtpassword.setBounds(150,80,180,25);
        panel.add(txtpassword);
        
        btnLogin = new JButton("Login");
        btnLogin.setBounds(150,130,100,30);
        panel.add(btnLogin);
        
        // Login button action
        btnLogin.addActionListener(e -> login());

        add(panel);
    }

    private void login() {

        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter username and password."
            );
            return;
        }

        try {

            boolean success = authService.login(username, password);

            if (success) {

                JOptionPane.showMessageDialog(
                        this,
                        "Login successful!"
                );

                // Dashboard routing will be added here.
                
            }

        } catch (InvalidLoginException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    
    
}
        
        



