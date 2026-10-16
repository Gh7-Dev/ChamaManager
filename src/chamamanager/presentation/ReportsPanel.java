package chamamanager.presentation;

import chamamanager.exceptions.DataAccessException;
import chamamanager.model.Contribution;
import chamamanager.model.Contribution.Status;
import chamamanager.logic.ContributionService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Screen 2: contribution reports with CSV / PDF export. Talks to ContributionService only. */
public class ReportsPanel extends JFrame {

    private final ContributionService service = new ContributionService();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Member", "Amount", "Date paid", "Period", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JComboBox<String> filterBox =
            new JComboBox<>(new String[]{"ALL", "PENDING", "CONFIRMED", "REJECTED"});
    private final JLabel totalLabel = new JLabel(" ");
    private List<Contribution> current = new ArrayList<>();

    public ReportsPanel() {
        super("Contribution Reports");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(720, 450);
        setLocationRelativeTo(null);

        JButton csvBtn = new JButton("Export CSV");
        JButton pdfBtn = new JButton("Export PDF");
        JButton refreshBtn = new JButton("Refresh");

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Status:"));
        top.add(filterBox);
        top.add(refreshBtn);
        top.add(csvBtn);
        top.add(pdfBtn);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        add(totalLabel, BorderLayout.SOUTH);

        filterBox.addActionListener(e -> load());
        refreshBtn.addActionListener(e -> load());
        csvBtn.addActionListener(e -> export(true));
        pdfBtn.addActionListener(e -> export(false));

        load();
    }

    private void load() {
        try {
            String f = (String) filterBox.getSelectedItem();
            current = "ALL".equals(f) ? service.getAll() : service.getByStatus(Status.valueOf(f));
            model.setRowCount(0);
            for (Contribution c : current) {
                model.addRow(new Object[]{c.getId(), c.getMemberId(), c.getAmount(),
                    c.getContributionDate(), c.getPeriod(), c.getStatus()});
            }
            totalLabel.setText("  Records: " + current.size()
                    + "   |   Approved total: " + service.getApprovedTotal());
        } catch (DataAccessException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    private void export(boolean csv) {
        if (current.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nothing to export.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(csv ? "contributions.csv" : "contributions.pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            if (csv) service.exportCSV(current, fc.getSelectedFile());
            else service.exportPDF(current, fc.getSelectedFile());
            JOptionPane.showMessageDialog(this, "Exported to " + fc.getSelectedFile().getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
        }
    }
}