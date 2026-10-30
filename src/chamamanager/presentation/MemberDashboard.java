/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chamamanager.presentation;

import javax.swing.*;
import java.awt.*;

public class MemberDashboard extends JFrame {

    private JLabel lblWelcome;
    private JLabel lblContributions;
    private JLabel lblLoanStatus;
    private JLabel lblSavings;

    public MemberDashboard(String memberName) {

        setTitle("Chama Manager - Member Dashboard");
        setSize(500, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(null);

        // Welcome message
        lblWelcome = new JLabel("Welcome, " + memberName);
        lblWelcome.setBounds(50, 30, 400, 30);
        lblWelcome.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(lblWelcome);

        // Contributions
        lblContributions = new JLabel("Total Contributions: Not loaded");
        lblContributions.setBounds(50, 90, 350, 25);
        panel.add(lblContributions);

        // Loan status
        lblLoanStatus = new JLabel("Loan Status: Not loaded");
        lblLoanStatus.setBounds(50, 130, 350, 25);
        panel.add(lblLoanStatus);

        // Savings
        lblSavings = new JLabel("Savings: Not loaded");
        lblSavings.setBounds(50, 170, 350, 25);
        panel.add(lblSavings);

        add(panel);
    }
}
