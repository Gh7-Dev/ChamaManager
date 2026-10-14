package chamamanager.presentation;

import chamamanager.exceptions.DataAccessException;
import chamamanager.model.Contribution;
import chamamanager.logic.ContributionService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Screen 3: Treasurer dashboard. Talks to ContributionService only.
 * Loan approval is not here any more: it belongs to Person C's LoanService
 * (add a "Pending Loans" tab that calls LoanService once it is ready).
 */
public class TreasurerDashboard extends JFrame {

    private final ContributionService service = new ContributionService();
    private final DefaultTableModel contribModel = new DefaultTableModel(
            new String[]{"ID", "Member", "Amount", "Date paid", "Period", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable contribTable = new JTable(contribModel);
    private final JLabel totalLabel = new JLabel(" ");

    public TreasurerDashboard() {
        super("Treasurer Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(760, 480);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Pending Contributions", buildPanel());

        JButton recordBtn = new JButton("Record Contribution");
        JButton reportsBtn = new JButton("Reports");
        recordBtn.addActionListener(e -> new RecordContributionPanel().setVisible(true));
        reportsBtn.addActionListener(e -> new ReportsPanel().setVisible(true));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(recordBtn);
        top.add(reportsBtn);

        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(totalLabel, BorderLayout.SOUTH);

        refresh();
    }

    private JPanel buildPanel() {
        JButton approve = new JButton("Approve");
        JButton reject = new JButton("Reject");
        approve.addActionListener(e -> decide(true));
        reject.addActionListener(e -> decide(false));

        JPanel buttons = new JPanel();
        buttons.add(approve);
        buttons.add(reject);

        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(contribTable), BorderLayout.CENTER);
        p.add(buttons, BorderLayout.SOUTH);
        return p;
    }

    private void decide(boolean approve) {
        int row = contribTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row first.");
            return;
        }
        int id = (int) contribTable.getValueAt(row, 0);
        String verb = approve ? "approve" : "reject";
        if (JOptionPane.showConfirmDialog(this, "Do you want to " + verb + " #" + id + "?",
                "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            boolean ok = approve ? service.approveContribution(id) : service.rejectContribution(id);
            if (!ok) {
                JOptionPane.showMessageDialog(this, "That record is no longer pending.");
            }
            refresh();
        } catch (DataAccessException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    private void refresh() {
        try {
            contribModel.setRowCount(0);
            for (Contribution c : service.getPending()) {
                contribModel.addRow(new Object[]{c.getId(), c.getMemberId(), c.getAmount(),
                    c.getContributionDate(), c.getPeriod(), c.getStatus()});
            }
            totalLabel.setText("  Approved contributions total: " + service.getApprovedTotal());
        } catch (DataAccessException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    // Run this class to open the Treasurer dashboard
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TreasurerDashboard().setVisible(true));
    }
}