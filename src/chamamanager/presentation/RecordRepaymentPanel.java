package chamamanager.presentation;

import chamamanager.exceptions.DataAccessException;
import chamamanager.logic.LoanService;
import chamamanager.logic.LoanService.ArrearsEntry;
import chamamanager.model.Loan;
import chamamanager.model.Repayment;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Screen 7: the treasurer records repayments against repayable loans. "Days
 * overdue" comes from the arrears logic.
 */
public class RecordRepaymentPanel extends JPanel {

    private final LoanService loanService;
    private final String role;

    private final DefaultTableModel loansModel = readOnlyModel(
            "Loan ID", "Member ID", "Total payable (KES)", "Repaid (KES)", "Outstanding (KES)",
            "Due date", "Days overdue", "Status");
    private final JTable loansTable = new JTable(loansModel);
    private final DefaultTableModel historyModel = readOnlyModel("Payment ID", "Amount (KES)", "Date paid");
    private final JTable historyTable = new JTable(historyModel);
    private final JTextField amountField = new JTextField(10);
    private final JButton recordButton = new JButton("Record repayment");

    public RecordRepaymentPanel(LoanService loanService, String role) {
        this.loanService = loanService;
        this.role = role;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        loansTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        loansTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadHistory();
            }
        });

        JPanel tables = new JPanel(new GridLayout(2, 1, 8, 8));
        JPanel loansBox = new JPanel(new BorderLayout());
        loansBox.setBorder(BorderFactory.createTitledBorder("Loans accepting repayments"));
        loansBox.add(new JScrollPane(loansTable), BorderLayout.CENTER);
        JPanel historyBox = new JPanel(new BorderLayout());
        historyBox.setBorder(BorderFactory.createTitledBorder("Repayment history of the selected loan"));
        historyBox.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        tables.add(loansBox);
        tables.add(historyBox);
        add(tables, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Amount (KES):"));
        controls.add(amountField);
        controls.add(recordButton);
        if (!LoanService.ROLE_TREASURER.equals(role)) {
            amountField.setEnabled(false);
            recordButton.setEnabled(false);
            controls.add(new JLabel("Only the treasurer can record repayments."));
        }
        recordButton.addActionListener(e -> onRecord());
        add(controls, BorderLayout.SOUTH);

        refresh();
    }

    private void onRecord() {
        int row = loansTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a loan first.", "Repayments", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int loanId = (Integer) loansModel.getValueAt(row, 0);
        try {
            BigDecimal amount = new BigDecimal(amountField.getText().trim().replace(",", ""));
            loanService.recordRepayment(role, loanId, amount.doubleValue());
            BigDecimal left = loanService.getOutstandingBalance(loanId);
            String message = String.format("Recorded KES %,.2f against loan #%d. ", amount, loanId)
                    + (left.signum() == 0 ? "Loan fully repaid: status is now PAID."
                    : String.format("Outstanding: KES %,.2f.", left));
            amountField.setText("");
            JOptionPane.showMessageDialog(this, message, "Repayments", JOptionPane.INFORMATION_MESSAGE);
            refresh();
            selectLoan(loanId);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    /**
     * Reloads from the database. Also applies automatic DEFAULTED.
     */
    public final void refresh() {
        try {
            loanService.refreshLoanStatuses();
            Map<Integer, Long> daysOverdue = new HashMap<>();
            for (ArrearsEntry entry : loanService.getArrears()) {
                daysOverdue.put(entry.getLoan().getId(), entry.getDaysOverdue());
            }
            loansModel.setRowCount(0);
            for (Loan loan : loanService.getRepayableLoans()) {
                BigDecimal total = loanService.getTotalPayable(loan);
                BigDecimal repaid = loanService.getAmountRepaid(loan.getId());
                loansModel.addRow(new Object[]{
                    loan.getId(), loan.getMemberId(),
                    String.format("%,.2f", total), String.format("%,.2f", repaid),
                    String.format("%,.2f", loanService.getOutstandingBalance(loan)),
                    loan.getDueDate() == null ? "-" : loan.getDueDate().toString(),
                    daysOverdue.getOrDefault(loan.getId(), 0L),
                    loan.getStatus()});
            }
            historyModel.setRowCount(0);
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    private void loadHistory() {
        historyModel.setRowCount(0);
        int row = loansTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        int loanId = (Integer) loansModel.getValueAt(row, 0);
        try {
            for (Repayment r : loanService.getRepaymentsForLoan(loanId)) {
                historyModel.addRow(new Object[]{
                    r.getId(), String.format("%,.2f", r.getAmountPaid()), r.getDatePaid().toString()});
            }
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    private void selectLoan(int loanId) {
        for (int i = 0; i < loansModel.getRowCount(); i++) {
            if ((Integer) loansModel.getValueAt(i, 0) == loanId) {
                loansTable.setRowSelectionInterval(i, i);
                return;
            }
        }
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void showError(Exception ex) {
        String message;
        if (ex instanceof NumberFormatException) {
            message = "Please enter a valid amount, e.g. 2500.";
        } else if (ex instanceof DataAccessException) {
            message = "Database error: " + ex.getMessage();
        } else {
            message = ex.getMessage();
        }
        JOptionPane.showMessageDialog(this, message, "Cannot record repayment", JOptionPane.WARNING_MESSAGE);
    }
}
