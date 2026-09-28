package chamamanager.presentation;
import chamamanager.util.PasswordHasher;
import chamamanager.dao.MemberDAO;
import chamamanager.model.Member;
import java.time.LocalDate;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import javax.swing.*;
import java.awt.*;

public class ManageMembersPanel extends JPanel {

    private JTextField txtName;
    private JTextField txtUsername;
    private JTextField txtPhone;
    private JPasswordField txtPassword;
    private JTextField txtSearch;

    private JButton btnSave;
    private JButton btnUpdate;
    private JButton btnDelete;
    private JButton btnSearch;

    private JTable memberTable;

    private MemberDAO memberDAO;

    public ManageMembersPanel() {

        memberDAO = new MemberDAO();

        setLayout(null);

        // Title
        JLabel lblTitle = new JLabel("Manage Members");
        lblTitle.setBounds(30, 20, 250, 30);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        add(lblTitle);

        // Name
        JLabel lblName = new JLabel("Full Name:");
        lblName.setBounds(30, 70, 100, 25);
        add(lblName);

        txtName = new JTextField();
        txtName.setBounds(130, 70, 200, 25);
        add(txtName);

        // Username
        JLabel lblUsername = new JLabel("Username:");
        lblUsername.setBounds(30, 110, 100, 25);
        add(lblUsername);

        txtUsername = new JTextField();
        txtUsername.setBounds(130, 110, 200, 25);
        add(txtUsername);

        // Phone
        JLabel lblPhone = new JLabel("Phone:");
        lblPhone.setBounds(30, 150, 100, 25);
        add(lblPhone);

        txtPhone = new JTextField();
        txtPhone.setBounds(130, 150, 200, 25);
        add(txtPhone);
        
        //Password
        JLabel lbnPassword = new JLabel("Password");
        lbnPassword.setBounds(30,190,100,25);
        add(lbnPassword);
        
        txtPassword = new JPasswordField();
        txtPassword.setBounds(130,190,200,25);
        add(txtPassword);

        // Buttons
        btnSave = new JButton("Save");
        btnSave.setBounds(30, 230, 90, 30);
        add(btnSave);
        btnSave.addActionListener(e -> saveMember());

        btnUpdate = new JButton("Update");
        btnUpdate.setBounds(130, 230, 90, 30);
        add(btnUpdate);

        btnDelete = new JButton("Delete");
        btnDelete.setBounds(230, 230, 90, 30);
        add(btnDelete);

        // Search
        JLabel lblSearch = new JLabel("Search:");
        lblSearch.setBounds(30, 280, 100, 25);
        add(lblSearch);

        txtSearch = new JTextField();
        txtSearch.setBounds(130, 280, 200, 25);
        add(txtSearch);

        btnSearch = new JButton("Search");
        btnSearch.setBounds(340, 280, 90, 25);
        add(btnSearch);
        btnSearch.addActionListener(e -> searchMembers());

        // Table
        memberTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(memberTable);
        scrollPane.setBounds(30, 320, 500, 180);
        add(scrollPane);
    }
    private void saveMember() {

    String name = txtName.getText();
    String username = txtUsername.getText();
    String phone = txtPhone.getText();
    String password = new String(txtPassword.getPassword());

    if (name.isEmpty() || username.isEmpty() || phone.isEmpty() || password.isEmpty()) {
        JOptionPane.showMessageDialog(
                this,
                "Please fill in all fields."
        );
        return;
    }

    Member member = new Member();

    member.setFullName(name);
    member.setUsername(username);
    member.setPhone(phone);
    member.setPasswordHash(PasswordHasher.hash(password));
    member.setDateJoined(LocalDate.now());

    memberDAO.create(member);

    JOptionPane.showMessageDialog(
            this,
            "Member saved successfully."
    );
}
    private void searchMembers(){
        String name = txtSearch.getText();
        List<Member> members = memberDAO.searchByName(name);
        String[]columns = {
            "ID",
            "Full Name",
            "Username",
            "Phone"
        };
     
    DefaultTableModel model = new DefaultTableModel(columns,0);
    for(Member member: members){
      Object[] row = {
          member.getId(),
          member.getFullName(),
          member.getUsername(),
          member.getPhone()
          
      };
      model.addRow(row);
    }
    memberTable.setModel(model);
             
    }
}