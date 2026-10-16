package chamamanager.logic;

import chamamanager.dao.ContributionDAO;
import chamamanager.exceptions.DuplicateContributionException;
import chamamanager.model.Contribution;
import chamamanager.model.Contribution.Status;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Business rules for contributions. The screens call this class, never the DAO.
 * Database failures surface as the unchecked DataAccessException from the DAO.
 */
public class ContributionService {

    private final ContributionDAO dao = new ContributionDAO();

    // ----- Recording -----

    /**
     * Validates and saves a new contribution (status PENDING).
     * @throws IllegalArgumentException if the period is not YYYY-MM
     * @throws DuplicateContributionException if the member already has a
     *         PENDING or CONFIRMED contribution for that period
     */
    public boolean record(Contribution c) throws DuplicateContributionException {
        c.setPeriod(validatePeriod(c.getPeriod()));
        if (dao.existsForMemberAndPeriod(c.getMemberId(), c.getPeriod())) {
            throw new DuplicateContributionException("Member " + c.getMemberId() + " already has a contribution for " + c.getPeriod() + ".");
        }
        return dao.addContribution(c);
    }

    /** Edits details of a contribution; the period format is validated first. */
    public void update(Contribution c) {
        c.setPeriod(validatePeriod(c.getPeriod()));
        dao.update(c);
    }

    /** Accepts only YYYY-MM (e.g. 2026-10) and returns it in that exact form. */
    public static String validatePeriod(String period) {
        if (period == null || !period.trim().matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("Period must be in YYYY-MM format, e.g. 2026-10.");
        }
        try {
            return YearMonth.parse(period.trim()).toString();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Period must be a real month in YYYY-MM format, e.g. 2026-10.");
        }
    }

    // ----- Queries -----

    public List<Contribution> getAll() { return dao.getAll(); }

    public List<Contribution> getByStatus(Status status) { return dao.getByStatus(status); }

    public List<Contribution> getPending() { return dao.getByStatus(Status.PENDING); }

    public List<Contribution> getByMember(int memberId) { return dao.getByMember(memberId); }

    public BigDecimal getApprovedTotal() { return dao.getApprovedTotal(); }

    /** For the loan limit (LoanService). */
    public BigDecimal getConfirmedTotalByMember(int memberId) {
        return dao.getConfirmedTotalByMember(memberId);
    }

    // ----- Treasurer decisions -----

    public boolean approveContribution(int id) { return dao.approveContribution(id); }

    public boolean rejectContribution(int id) { return dao.rejectContribution(id); }

    // ----- Report export (CSV / PDF) -----

    public void exportCSV(List<Contribution> list, File file) throws IOException {
        try (PrintWriter out = new PrintWriter(file, "UTF-8")) {
            out.println("ID,MemberID,Amount,DatePaid,Period,Status");
            for (Contribution c : list) {
                String period = c.getPeriod() == null ? "" : c.getPeriod().replace("\"", "\"\"");
                out.printf("%d,%d,%s,%s,\"%s\",%s%n", c.getId(), c.getMemberId(),
                        c.getAmount().toPlainString(), c.getContributionDate(), period, c.getStatus());
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
}