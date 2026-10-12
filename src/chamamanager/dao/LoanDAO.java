package chamamanager.dao;

import chamamanager.exceptions.DataAccessException;
import chamamanager.model.Loan;
import chamamanager.util.DBConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles persistence operations for {@link Loan} via JDBC.
 *
 * @author gh7
 */
public class LoanDAO implements CrudOperations<Loan> {

    @Override
    public void create(Loan item) {
        String sql = "INSERT INTO loans "
                + "(member_id, principal, interest_rate, date_issued, due_date, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.getMemberId());
            ps.setDouble(2, item.getPrincipal());
            bindRate(ps, 3, item);
            //ps.setDouble(3, item.getInterestRate());
            // PENDING loans have no dates yet — bind NULL instead of NPE.
            ps.setDate(4, toSqlDate(item.getDateIssued()));
            ps.setDate(5, toSqlDate(item.getDueDate()));
            ps.setString(6, item.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    item.setId(keys.getInt(1));
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Create a new loan failed", ex);

        }
    }

    @Override
    public Loan getById(int id) {
        String sql = "SELECT * FROM loans WHERE loan_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not find the loan", ex);
        }
        return null;
    }

    @Override
    public List<Loan> getAll() {
        return queryLoans("SELECT * FROM loans ORDER BY loan_id", "Could not find all the loans");
    }

    /*List<Loan> loans = new ArrayList<>();
        String sql = "SELECT * FROM loans";
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                loans.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not find all the loans", ex);
        }
        return loans;*/
    @Override
    public void update(Loan item) {
        String sql = "UPDATE loans SET member_id = ?, principal = ?, "
                + "interest_rate = ?, date_issued = ?, due_date = ?, status = ? "
                + "WHERE loan_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getMemberId());
            ps.setDouble(2, item.getPrincipal());
            bindRate(ps, 3, item);
            // PENDING loans have no dates yet — bind NULL instead of NPE. Same rule as create()/mapRow().
            ps.setDate(4, toSqlDate(item.getDateIssued()));
            ps.setDate(5, toSqlDate(item.getDueDate()));
            ps.setString(6, item.getStatus());
            ps.setInt(7, item.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Could not update the loan", ex);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM loans WHERE loan_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Could not delete the loan", ex);
        }
    }
//+++++Lookups++++++

    public List<Loan> getByMemberId(int memberId) {
        return queryLoans("SELECT * FROM loans WHERE member_id =? ORDER BY loan_id",
                "Could not load the member's loans", memberId);
    }

    public List<Loan> getByStatus(String status) {
        return queryLoans("SELECT * FROM loans WHERE member_id =? ORDER BY loan_id",
                "Could not load the member's loans", status);
    }

    public List<Loan> getByMemberAndStatus(int memberId, String status) {
        return queryLoans("SELECT * FROM loans WHERE member_id =? AND status = ? ORDER BY loan_id",
                "Could not load the member's loans", memberId, status);
    }

    /*
    *Turns a PENDING loan into an ACTIVE one in a single guarded UPDATE.
    *@return true if exactly one PENDING  loan was updated
     */
    public boolean issuePending(int loanId, double rate, LocalDate issued, LocalDate due) {
        String sql = "UPDATE loans SET interest_rate = ?, date_issued = ?, due_date = ?, status = 'ACTIVE' "
                + "WHERE loan_id = ? AND status = 'PENDING'";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, rate);
            ps.setDate(2, Date.valueOf(issued));
            ps.setDate(3, Date.valueOf(due));
            ps.setInt(4, loanId);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new DataAccessException("Could not issue the loan", ex);
        }
    }

    /**
     * Changes a loan's status only if it currently has the expected status.
     *
     * @param loanId
     * @param expectedCurrent
     * @param newStatus
     * @return true if the loan was updated
     */
    public boolean changeStatus(int loanId, String expectedCurrent, String newStatus) {
        String sql = "UPDATE loans SET status = ? WHERE loan_id = ? AND status = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, loanId);
            ps.setString(3, expectedCurrent);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new DataAccessException("Could not change the loan status", ex);
        }
    }

    // TEMP: replace with Person B's CONFIRMED-only total once it is on master.
    public BigDecimal getConfirmedSavings(int memberId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM contributions "
                + "WHERE member_id = ? AND status = 'CONFIRMED'";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getBigDecimal(1) != null) {
                    return rs.getBigDecimal(1);
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not total the member's savings", ex);
        }
        return BigDecimal.ZERO;
    }
    // --- helpers-------

    private List<Loan> queryLoans(String sql, String errorMessage, Object... params) {
        List<Loan> loans = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    loans.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException(errorMessage, ex);
        }
        return loans;
    }

    private static Date toSqlDate(LocalDate d) {
        return d == null ? null : Date.valueOf(d);
    }

    /**
     * PENDING and REJECTED loans have no interest rate yet: store NULL, not 0.
     */
    private static void bindRate(PreparedStatement ps, int index, Loan loan) throws SQLException {
        boolean noRate = "PENDING".equals(loan.getStatus()) || "REJECTED".equals(loan.getStatus());
        if (noRate) {
            ps.setNull(index, Types.DECIMAL);
        } else {
            ps.setDouble(index, loan.getInterestRate());
        }
    }

    private Loan mapRow(ResultSet rs) throws SQLException {
        Loan loan = new Loan();
        loan.setId(rs.getInt("loan_id"));
        loan.setMemberId(rs.getInt("member_id"));
        loan.setPrincipal(rs.getDouble("principal"));
        loan.setInterestRate(rs.getDouble("interest_rate"));
        java.sql.Date dIssued = rs.getDate("date_issued");
        loan.setDateIssued(dIssued == null ? null : dIssued.toLocalDate());
        java.sql.Date dDue = rs.getDate("due_date");
        loan.setDueDate(dDue == null ? null : dDue.toLocalDate());
        loan.setStatus(rs.getString("status"));
        return loan;
    }

}
