package chamamanager.presentation;

import chamamanager.exceptions.DataAccessException;
import chamamanager.logic.LoanService;
import chamamanager.model.Loan;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Screen 6: members apply for loans; the treasurer approves (= issues) or
 * rejects them. The treasurer clicks a row in the loans table, then uses the
 * Approve / Reject buttons. They are only enabled for a PENDING row.
 */
public class LoanPanel extends JPanel {

    private static final int COL_ID = 0;
    private static final int COL_MEMBER = 1;
    private static final int COL_STATUS = 6;

    private final LoanService loanService;
    private final int memberId;
    private final String role;
    private final boolean treasurer;

    private final JLabel savingsLabel = new JLabel();
    private final JTextField principalField = new JTextField(10);
    private final JTextField rateField = new JTextField("10", 5);
    private final JTextField dueField = new JTextField(LocalDate.now().plusMonths(3).toString(), 9);
    private final JCheckBox pendingOnlyBox = new JCheckBox("Show only PENDING");
    private final JLabel selectionLabel = new JLabel(" ");
    private final JButton approveButton = new JButton("Approve & issue");
    private final JButton rejectButton = new JButton("Reject");

    private final DefaultTableModel loansModel = readOnlyModel(
            "Loan ID", "Member ID", "Principal (KES)", "Rate", "Issued", "Due", "Status",
            "Total payable (KES)", "Outstanding (KES)");
    private final JTable loansTable = new JTable(loansModel);

    public LoanPanel(LoanService loanService, int memberId, String role) {
        this.loanService = loanService;
        this.memberId = memberId;
        this.role = role;
        this.treasurer = LoanService.ROLE_TREASURER.equals(role);

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        loansTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        loansTable.setRowSelectionAllowed(true);
        loansTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelection();
            }
        });
        approveButton.setEnabled(false);
        rejectButton.setEnabled(false);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(buildApplyPanel());
        if (treasurer) {
            top.add(buildDecisionPanel());
        }
        add(top, BorderLayout.NORTH);
        add(buildLoansPanel(), BorderLayout.CENTER);
        refresh();
    }

    // ------------------------------------------------------------ layout
    private JPanel buildApplyPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Apply for a loan (member #" + memberId + ")"));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> onApply());
        panel.add(savingsLabel);
        panel.add(new JLabel("Amount (KES):"));
        panel.add(principalField);
        panel.add(applyButton);
        return panel;
    }

    private JPanel buildDecisionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Approve or reject (treasurer)"));
        panel.setAlignmentX(LEFT_ALIGNMENT);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.setAlignmentX(LEFT_ALIGNMENT);
        approveButton.addActionListener(e -> onApprove());
        rejectButton.addActionListener(e -> onReject());
        pendingOnlyBox.addActionListener(e -> refresh());
        row1.add(new JLabel("Interest rate (%):"));
        row1.add(rateField);
        row1.add(new JLabel("Due date (yyyy-mm-dd):"));
        row1.add(dueField);
        row1.add(approveButton);
        row1.add(rejectButton);
        row1.add(pendingOnlyBox);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.setAlignmentX(LEFT_ALIGNMENT);
        row2.add(selectionLabel);

        panel.add(row1);
        panel.add(row2);
        return panel;
    }

    private JPanel buildLoansPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                treasurer ? "All loans (click a row to select it)" : "My loans"));
        panel.add(new JScrollPane(loansTable), BorderLayout.CENTER);
        return panel;
    }

    // ------------------------------------------------------------ actions
    private void onApply() {
        try {
            BigDecimal amount = new BigDecimal(principalField.getText().trim().replace(",", ""));
            Loan loan = loanService.applyForLoan(memberId, amount.doubleValue());
            principalField.setText("");
            info("Application submitted (loan #" + loan.getId() + "). Status: PENDING.");
            refresh();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void onApprove() {
        Integer loanId = selectedPendingId();
        if (loanId == null) {
            return;
        }
        try {
            BigDecimal ratePercent = new BigDecimal(rateField.getText().trim());
            double rate = ratePercent.divide(BigDecimal.valueOf(100)).doubleValue();
            LocalDate due = LocalDate.parse(dueField.getText().trim());
            loanService.approveLoan(role, loanId, rate, due);
            info("Loan #" + loanId + " approved and issued. Status: ACTIVE.");
            refresh();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void onReject() {
        Integer loanId = selectedPendingId();
        if (loanId == null) {
            return;
        }
        try {
            loanService.rejectLoan(role, loanId);
            info("Loan #" + loanId + " rejected.");
            refresh();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    /**
     * The id of the selected row if it is PENDING, otherwise tells the user why
     * not.
     */
    private Integer selectedPendingId() {
        int viewRow = loansTable.getSelectedRow();
        if (viewRow < 0) {
            info("Click a PENDING loan in the table first.");
            return null;
        }
        int row = loansTable.convertRowIndexToModel(viewRow);
        int id = (Integer) loansModel.getValueAt(row, COL_ID);
        String status = (String) loansModel.getValueAt(row, COL_STATUS);
        if (!LoanService.STATUS_PENDING.equals(status)) {
            info("Loan #" + id + " is " + status + ". Only PENDING loans can be approved or rejected.");
            return null;
        }
        return id;
    }

    /**
     * Enables/disables the buttons and describes the selected row.
     */
    private void updateSelection() {
        int viewRow = loansTable.getSelectedRow();
        boolean pending = false;
        String text;
        if (viewRow < 0) {
            text = treasurer ? "Click a PENDING loan in the table below to approve or reject it." : " ";
        } else {
            int row = loansTable.convertRowIndexToModel(viewRow);
            int id = (Integer) loansModel.getValueAt(row, COL_ID);
            int borrower = (Integer) loansModel.getValueAt(row, COL_MEMBER);
            String status = (String) loansModel.getValueAt(row, COL_STATUS);
            pending = LoanService.STATUS_PENDING.equals(status);
            if (pending) {
                text = String.format("Loan #%d selected: member #%d | confirmed savings KES %,.2f | borrow limit KES %,.2f",
                        id, borrower, loanService.getConfirmedSavings(borrower), loanService.getBorrowLimit(borrower));
            } else {
                text = "Loan #" + id + " is " + status + ": only PENDING loans can be approved or rejected.";
            }
        }
        selectionLabel.setText(text);
        approveButton.setEnabled(treasurer && pending);
        rejectButton.setEnabled(treasurer && pending);
    }

    // ------------------------------------------------------------ data
    /**
     * Reloads everything from the database. Also applies automatic DEFAULTED.
     */
    public final void refresh() {
        try {
            loanService.refreshLoanStatuses();
            savingsLabel.setText(String.format("Confirmed savings: KES %,.2f | Borrow limit: KES %,.2f   ",
                    loanService.getConfirmedSavings(memberId), loanService.getBorrowLimit(memberId)));

            loansModel.setRowCount(0);
            for (Loan loan : treasurer ? loanService.getAllLoans() : loanService.getLoansByMember(memberId)) {
                if (pendingOnlyBox.isSelected()
                        && !LoanService.STATUS_PENDING.equals(loan.getStatus())) {
                    continue;
                }
                loansModel.addRow(loanRow(loan));
            }
            updateSelection();
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    private Object[] loanRow(Loan loan) {
        boolean noTerms = LoanService.STATUS_PENDING.equals(loan.getStatus())
                || LoanService.STATUS_REJECTED.equals(loan.getStatus());
        return new Object[]{
            loan.getId(), loan.getMemberId(), money(loan.getPrincipal()),
            noTerms ? "-" : String.format("%.2f%%", loan.getInterestRate() * 100),
            loan.getDateIssued() == null ? "-" : loan.getDateIssued().toString(),
            loan.getDueDate() == null ? "-" : loan.getDueDate().toString(),
            loan.getStatus(),
            noTerms ? "-" : money(loanService.getTotalPayable(loan)),
            noTerms ? "-" : money(loanService.getOutstandingBalance(loan))};
    }

    // ------------------------------------------------------------ small helpers
    private static String money(Object value) {
        return String.format("%,.2f", value);
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void info(String message) {
        JOptionPane.showMessageDialog(this, message, "Loans", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(Exception ex) {
        String message;
        if (ex instanceof NumberFormatException) {
            message = "Please enter a valid number.";
        } else if (ex instanceof DateTimeParseException) {
            message = "Please enter the date as yyyy-mm-dd, e.g. 2026-12-31.";
        } else if (ex instanceof DataAccessException) {
            message = "Database error: " + ex.getMessage();
        } else {
            message = ex.getMessage();
        }
        JOptionPane.showMessageDialog(this, message, "Cannot complete action", JOptionPane.WARNING_MESSAGE);
    }
}
