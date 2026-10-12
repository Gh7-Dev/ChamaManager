package chamamanager.dao;

import chamamanager.model.Contribution;
import chamamanager.model.Contribution.Status;
import chamamanager.model.Member;
import chamamanager.exceptions.DataAccessException;
import chamamanager.util.DBConnection;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

/**
 * Database access for contributions, Treasurer approval (contributions and loans),
 * report export (CSV / PDF), the Treasurer class (extends Member), and the three
 * Swing screens (nested classes at the bottom).
 * Assumes util.DBConnection.getConnection() returns a MySQL Connection.
 * PDF export is built in (no external library needed).
 */
public class ContributionDAO implements CrudOperations<Contribution> {

    // ================= DATABASE LOGIC =================

    /** Saves a new contribution. It always starts as PENDING. */
    public boolean addContribution(Contribution c) throws SQLException {
        String sql = "INSERT INTO contributions "
                + "(member_id, amount, date_paid, period, status) "
                + "VALUES (?, ?, ?, ?, 'PENDING')";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getMemberId());
            ps.setBigDecimal(2, c.getAmount());
            ps.setDate(3, Date.valueOf(c.getContributionDate()));
            ps.setString(4, c.getPeriod());
            boolean ok = ps.executeUpdate() == 1;
            if (ok) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        c.setId(keys.getInt(1));
                    }
                }
            }
            return ok;
        }
    }

    // ================= CrudOperations contract =================

    @Override
    public void create(Contribution item) {
        try {
            addContribution(item);
        } catch (SQLException e) {
            throw wrap("Could not save contribution", e);
        }
    }

    @Override
    public Contribution getById(int id) {
        String sql = "SELECT * FROM contributions WHERE contribution_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw wrap("Could not load contribution " + id, e);
        }
    }

    @Override
    public List<Contribution> getAll() {
        try {
            return query("SELECT * FROM contributions ORDER BY date_paid DESC, contribution_id DESC", null, 0);
        } catch (SQLException e) {
            throw wrap("Could not load contributions", e);
        }
    }

    /** Edits the details of a contribution. Status is changed only by Treasurer approve/reject. */
    @Override
    public void update(Contribution item) {
        String sql = "UPDATE contributions SET member_id = ?, amount = ?, date_paid = ?, "
                + "period = ? WHERE contribution_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, item.getMemberId());
            ps.setBigDecimal(2, item.getAmount());
            ps.setDate(3, Date.valueOf(item.getContributionDate()));
            ps.setString(4, item.getPeriod());
            ps.setInt(5, item.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw wrap("Could not update contribution " + item.getId(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM contributions WHERE contribution_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw wrap("Could not delete contribution " + id, e);
        }
    }

    /** Single place that builds the exception. If DataAccessException's constructor differs, edit only this line. */
    private DataAccessException wrap(String message, SQLException cause) {
        return new DataAccessException(message, cause);
    }

    // ================= Queries =================

    public List<Contribution> getByStatus(Status status) throws SQLException {
        return query("SELECT * FROM contributions WHERE status = ? "
                + "ORDER BY date_paid DESC, contribution_id DESC", status.name(), 0);
    }

    public List<Contribution> getByMember(int memberId) throws SQLException {
        return query("SELECT * FROM contributions WHERE member_id = ? "
                + "ORDER BY date_paid DESC, contribution_id DESC", null, memberId);
    }

    /** Total of CONFIRMED (Treasurer-approved) contributions only. */
    public BigDecimal getApprovedTotal() throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM contributions WHERE status = 'CONFIRMED'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        }
    }

    // ----- Treasurer approval: contributions -----

    public boolean approveContribution(int id) throws SQLException {
        return setContributionStatus(id, Status.CONFIRMED);
    }

    public boolean rejectContribution(int id) throws SQLException {
        return setContributionStatus(id, Status.REJECTED);
    }

    /** Only PENDING rows can change, so a decision cannot be overwritten. */
    private boolean setContributionStatus(int id, Status status) throws SQLException {
        String sql = "UPDATE contributions SET status = ? WHERE contribution_id = ? AND status = 'PENDING'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    // ----- Treasurer approval: loans -----
    // Uses loans(loan_id, member_id, principal, status). Status values: PENDING/APPROVED/REJECTED.

    /** Each row: {loan_id, member_id, principal, status} */
    public List<Object[]> getPendingLoans() throws SQLException {
        String sql = "SELECT loan_id, member_id, principal, status FROM loans "
                + "WHERE status = 'PENDING' ORDER BY loan_id";
        List<Object[]> rows = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getInt("loan_id"), rs.getInt("member_id"),
                    rs.getBigDecimal("principal"), rs.getString("status")
                });
            }
        }
        return rows;
    }

    public boolean approveLoan(int loanId) throws SQLException {
        return setLoanStatus(loanId, "APPROVED");
    }

    public boolean rejectLoan(int loanId) throws SQLException {
        return setLoanStatus(loanId, "REJECTED");
    }

    private boolean setLoanStatus(int loanId, String status) throws SQLException {
        String sql = "UPDATE loans SET status = ? WHERE loan_id = ? AND status = 'PENDING'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, loanId);
            return ps.executeUpdate() == 1;
        }
    }

    // ----- Report export (CSV / PDF) -----

    public void exportCSV(List<Contribution> list, File file) throws IOException {
        try (PrintWriter out = new PrintWriter(file, "UTF-8")) {
            out.println("ID,MemberID,Amount,DatePaid,Period,Status");
            for (Contribution c : list) {
                String period = c.getPeriod() == null ? "" : c.getPeriod().replace("\"", "\"\"");
                out.printf("%d,%d,%s,%s,\"%s\",%s%n", c.getId(), c.getMemberId(),
                        c.getAmount(), c.getContributionDate(), period, c.getStatus());
            }
        }
    }

    /** Writes a simple multi-page A4 PDF report (built-in, no library needed). */
    public void exportPDF(List<Contribution> list, File file) throws IOException {
        String rule = new String(new char[72]).replace('\0', '-');
        BigDecimal approved = BigDecimal.ZERO;
        List<String> lines = new ArrayList<>();
        lines.add("CHAMA MANAGER - CONTRIBUTIONS REPORT");
        lines.add("");
        lines.add(String.format("%-5s %-8s %-12s %-12s %-20s %-10s",
                "ID", "Member", "Amount", "Date paid", "Period", "Status"));
        lines.add(rule);
        for (Contribution c : list) {
            lines.add(String.format("%-5d %-8d %-12s %-12s %-20s %-10s",
                    c.getId(), c.getMemberId(), c.getAmount().toPlainString(),
                    String.valueOf(c.getContributionDate()),
                    c.getPeriod() == null ? "" : c.getPeriod(),
                    c.getStatus().name()));
            if (c.getStatus() == Status.CONFIRMED) {
                approved = approved.add(c.getAmount());
            }
        }
        lines.add(rule);
        lines.add("Records: " + list.size() + "   Approved total: " + approved.toPlainString());
        writeSimplePdf(lines, file);
    }

    /** Minimal PDF writer: Courier 9pt, A4, 60 lines per page. */
    private void writeSimplePdf(List<String> lines, File file) throws IOException {
        final int perPage = 60;
        int pages = Math.max(1, (lines.size() + perPage - 1) / perPage);

        // Objects: 1 catalog, 2 page tree, 3 font, then for each page i: page = 4+2i, content = 5+2i
        List<String> objs = new ArrayList<>();
        objs.add("<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages; i++) {
            kids.append(4 + 2 * i).append(" 0 R ");
        }
        objs.add("<< /Type /Pages /Kids [" + kids + "] /Count " + pages + " >>");
        objs.add("<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>");

        for (int i = 0; i < pages; i++) {
            StringBuilder content = new StringBuilder("BT\n/F1 9 Tf\n12 TL\n40 800 Td\n");
            int from = i * perPage;
            int to = Math.min(lines.size(), from + perPage);
            for (int k = from; k < to; k++) {
                content.append('(').append(pdfEscape(lines.get(k))).append(") Tj\nT*\n");
            }
            content.append("ET");
            objs.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 3 0 R >> >> /Contents " + (5 + 2 * i) + " 0 R >>");
            objs.add("<< /Length " + content.length() + " >>\nstream\n" + content + "\nendstream");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
        int[] offsets = new int[objs.size()];
        for (int i = 0; i < objs.size(); i++) {
            offsets[i] = out.size();
            out.write(((i + 1) + " 0 obj\n" + objs.get(i) + "\nendobj\n")
                    .getBytes(StandardCharsets.ISO_8859_1));
        }
        int xref = out.size();
        StringBuilder x = new StringBuilder("xref\n0 " + (objs.size() + 1) + "\n");
        x.append("0000000000 65535 f \n");
        for (int off : offsets) {
            x.append(String.format("%010d 00000 n \n", off));
        }
        x.append("trailer\n<< /Size ").append(objs.size() + 1).append(" /Root 1 0 R >>\n")
         .append("startxref\n").append(xref).append("\n%%EOF\n");
        out.write(x.toString().getBytes(StandardCharsets.ISO_8859_1));

        try (FileOutputStream fos = new FileOutputStream(file)) {
            out.writeTo(fos);
        }
    }

    /** Escapes text for a PDF string and replaces characters the built-in font cannot show. */
    private static String pdfEscape(String s) {
        StringBuilder b = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch == '\\' || ch == '(' || ch == ')') {
                b.append('\\').append(ch);
            } else if (ch < 32 || ch > 126) {
                b.append('?');
            } else {
                b.append(ch);
            }
        }
        return b.toString();
    }

    // ----- Helpers -----

    private List<Contribution> query(String sql, String statusParam, int memberParam) throws SQLException {
        List<Contribution> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (statusParam != null) {
                ps.setString(1, statusParam);
            } else if (memberParam > 0) {
                ps.setInt(1, memberParam);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private Contribution mapRow(ResultSet rs) throws SQLException {
        Contribution c = new Contribution(
                rs.getInt("contribution_id"),
                rs.getInt("member_id"),
                rs.getBigDecimal("amount"),
                rs.getDate("date_paid").toLocalDate(),
                null,
                null,
                Status.valueOf(rs.getString("status").trim().toUpperCase()));
        c.setPeriod(rs.getString("period"));
        return c;
    }

    // Run this class to open the Treasurer dashboard
    public static void main(String[] args) {
        Contribution.showTreasurerDashboard();
    }

    // ================= TREASURER (extends Member) =================

    /**
     * The Treasurer is a Member with extra duties: approving or rejecting
     * contributions and loans, and reading the approved total.
     *
     */
    public static class Treasurer extends Member {

        private final ContributionDAO dao = new ContributionDAO();

        /** Your Member class has a no-argument constructor, so the Treasurer uses it. */
        public Treasurer() {
            super();
        }

        public boolean approveContribution(int id) throws SQLException {
            return dao.approveContribution(id);
        }

        public boolean rejectContribution(int id) throws SQLException {
            return dao.rejectContribution(id);
        }

        public boolean approveLoan(int loanId) throws SQLException {
            return dao.approveLoan(loanId);
        }

        public boolean rejectLoan(int loanId) throws SQLException {
            return dao.rejectLoan(loanId);
        }

        public BigDecimal getApprovedTotal() throws SQLException {
            return dao.getApprovedTotal();
        }
    }

    // ================= SCREEN 1: RECORD CONTRIBUTION =================

    public static class RecordContributionFrame extends JFrame {

        private final ContributionDAO dao = new ContributionDAO();
        private final JTextField memberIdField = new JTextField(15);
        private final JTextField amountField = new JTextField(15);
        private final JTextField dateField = new JTextField(LocalDate.now().toString(), 15);
        private final JTextField periodField = new JTextField(
                LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)), 15);

        public RecordContributionFrame() {
            super("Record Contribution");
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            JPanel form = new JPanel(new GridBagLayout());
            form.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(5, 5, 5, 5);
            g.anchor = GridBagConstraints.WEST;

            addRow(form, g, 0, "Member ID:", memberIdField);
            addRow(form, g, 1, "Amount:", amountField);
            addRow(form, g, 2, "Date paid (yyyy-MM-dd):", dateField);
            addRow(form, g, 3, "Period (e.g. October 2026):", periodField);

            JButton saveBtn = new JButton("Submit for Approval");
            JButton clearBtn = new JButton("Clear");
            saveBtn.addActionListener(e -> save());
            clearBtn.addActionListener(e -> clear());

            JPanel buttons = new JPanel();
            buttons.add(saveBtn);
            buttons.add(clearBtn);

            add(new JLabel("New contributions stay PENDING until the Treasurer approves them.",
                    SwingConstants.CENTER), BorderLayout.NORTH);
            add(form, BorderLayout.CENTER);
            add(buttons, BorderLayout.SOUTH);

            pack();
            setLocationRelativeTo(null);
        }

        private void addRow(JPanel p, GridBagConstraints g, int row, String label, JComponent field) {
            g.gridx = 0; g.gridy = row;
            p.add(new JLabel(label), g);
            g.gridx = 1;
            p.add(field, g);
        }

        private void save() {
            try {
                int memberId = Integer.parseInt(memberIdField.getText().trim());
                BigDecimal amount = new BigDecimal(amountField.getText().trim());
                LocalDate date = LocalDate.parse(dateField.getText().trim());

                String period = periodField.getText().trim();
                if (period.isEmpty() || period.length() > 20) {
                    error("Period is required (max 20 characters), e.g. October 2026.");
                    return;
                }
                Contribution c = new Contribution(memberId, amount, date, period);

                if (dao.addContribution(c)) {
                    JOptionPane.showMessageDialog(this, "Saved. Status: PENDING Treasurer approval.");
                    clear();
                }
            } catch (NumberFormatException ex) {
                error("Member ID and amount must be valid numbers.");
            } catch (java.time.format.DateTimeParseException ex) {
                error("Date must look like 2026-10-10.");
            } catch (IllegalArgumentException ex) {
                error(ex.getMessage());
            } catch (SQLException ex) {
                error("Database error: " + ex.getMessage());
            }
        }

        private void clear() {
            memberIdField.setText("");
            amountField.setText("");
            dateField.setText(LocalDate.now().toString());
        }

        private void error(String msg) {
            JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================= SCREEN 2: REPORTS =================

    public static class ReportsFrame extends JFrame {

        private final ContributionDAO dao = new ContributionDAO();
        private final DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Member", "Amount", "Date paid", "Period", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        private final JComboBox<String> filterBox =
                new JComboBox<>(new String[]{"ALL", "PENDING", "CONFIRMED", "REJECTED"});
        private final JLabel totalLabel = new JLabel(" ");
        private List<Contribution> current = new ArrayList<>();

        public ReportsFrame() {
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
                current = "ALL".equals(f) ? dao.getAll() : dao.getByStatus(Status.valueOf(f));
                model.setRowCount(0);
                for (Contribution c : current) {
                    model.addRow(new Object[]{c.getId(), c.getMemberId(), c.getAmount(),
                        c.getContributionDate(), c.getPeriod(), c.getStatus()});
                }
                totalLabel.setText("  Records: " + current.size()
                        + "   |   Approved total: " + dao.getApprovedTotal());
            } catch (SQLException ex) {
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
                if (csv) dao.exportCSV(current, fc.getSelectedFile());
                else dao.exportPDF(current, fc.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Exported to " + fc.getSelectedFile().getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
            }
        }
    }

    // ================= SCREEN 3: TREASURER DASHBOARD =================

    public static class TreasurerDashboardFrame extends JFrame {

        private final Treasurer treasurer;
        private final ContributionDAO dao = new ContributionDAO();
        private final DefaultTableModel contribModel = new DefaultTableModel(
                new String[]{"ID", "Member", "Amount", "Date paid", "Period", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        private final DefaultTableModel loanModel = new DefaultTableModel(
                new String[]{"Loan ID", "Member", "Principal", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        private final JTable contribTable = new JTable(contribModel);
        private final JTable loanTable = new JTable(loanModel);
        private final JLabel totalLabel = new JLabel(" ");

        /** Opens the dashboard with a default Treasurer. Replace with the logged-in Treasurer when you have login. */
        public TreasurerDashboardFrame() {
            this(new Treasurer());
        }

        public TreasurerDashboardFrame(Treasurer treasurer) {
            super("Treasurer Dashboard");
            this.treasurer = treasurer;
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setSize(760, 480);
            setLocationRelativeTo(null);

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Pending Contributions", buildPanel(contribTable, true));
            tabs.addTab("Pending Loans", buildPanel(loanTable, false));

            JButton recordBtn = new JButton("Record Contribution");
            JButton reportsBtn = new JButton("Reports");
            recordBtn.addActionListener(e -> new RecordContributionFrame().setVisible(true));
            reportsBtn.addActionListener(e -> new ReportsFrame().setVisible(true));

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            top.add(recordBtn);
            top.add(reportsBtn);

            add(top, BorderLayout.NORTH);
            add(tabs, BorderLayout.CENTER);
            add(totalLabel, BorderLayout.SOUTH);

            refresh();
        }

        private JPanel buildPanel(JTable table, boolean contributions) {
            JButton approve = new JButton("Approve");
            JButton reject = new JButton("Reject");
            approve.addActionListener(e -> decide(table, contributions, true));
            reject.addActionListener(e -> decide(table, contributions, false));

            JPanel buttons = new JPanel();
            buttons.add(approve);
            buttons.add(reject);

            JPanel p = new JPanel(new BorderLayout());
            p.add(new JScrollPane(table), BorderLayout.CENTER);
            p.add(buttons, BorderLayout.SOUTH);
            return p;
        }

        private void decide(JTable table, boolean contributions, boolean approve) {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a row first.");
                return;
            }
            int id = (int) table.getValueAt(row, 0);
            String verb = approve ? "approve" : "reject";
            if (JOptionPane.showConfirmDialog(this, "Do you want to " + verb + " #" + id + "?",
                    "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                boolean ok;
                if (contributions) {
                    ok = approve ? treasurer.approveContribution(id) : treasurer.rejectContribution(id);
                } else {
                    ok = approve ? treasurer.approveLoan(id) : treasurer.rejectLoan(id);
                }
                if (!ok) {
                    JOptionPane.showMessageDialog(this, "That record is no longer pending.");
                }
                refresh();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
            }
        }

        private void refresh() {
            try {
                contribModel.setRowCount(0);
                for (Contribution c : dao.getByStatus(Status.PENDING)) {
                    contribModel.addRow(new Object[]{c.getId(), c.getMemberId(), c.getAmount(),
                        c.getContributionDate(), c.getPeriod(), c.getStatus()});
                }
                loanModel.setRowCount(0);
                for (Object[] row : dao.getPendingLoans()) {
                    loanModel.addRow(row);
                }
                totalLabel.setText("  Approved contributions total: " + treasurer.getApprovedTotal());
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
            }
        }
    }
}