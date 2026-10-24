package chamamanager.dao;

import chamamanager.exceptions.DataAccessException;
import chamamanager.model.Repayment;
import chamamanager.util.DBConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles persistence operations for {@link Repayment} via JDBC.
 *
 * @author gh7
 */
public class RepaymentDAO implements CrudOperations<Repayment> {

    @Override
    public void create(Repayment item) {
        String sql = "INSERT INTO repayments "
                + "(loan_id, amount_paid, date_paid) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getLoanId());
            ps.setDouble(2, item.getAmountPaid());
            ps.setDate(3, Date.valueOf(item.getDatePaid()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Could not create new payment", ex);
        }
    }

    @Override
    public Repayment getById(int id) {
        String sql = "SELECT * FROM repayments WHERE repayment_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not find repayment", ex);
        }
        return null;
    }

    @Override
    public List<Repayment> getAll() {
        List<Repayment> repayments = new ArrayList<>();
        String sql = "SELECT * FROM repayments ORDER BY repayment_id";
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                repayments.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not get all the repayments", ex);
        }
        return repayments;
    }

    @Override
    public void update(Repayment item) {
        String sql = "UPDATE repayments SET loan_id = ?, amount_paid = ?, "
                + "date_paid = ? WHERE repayment_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getLoanId());
            ps.setDouble(2, item.getAmountPaid());
            ps.setDate(3, Date.valueOf(item.getDatePaid()));
            ps.setInt(4, item.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Could not update the repayment", ex);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM repayments WHERE repayment_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Could not delete repayment", ex);
        }
    }

    // ---------------------------------------------------------------- lookups
    public List<Repayment> getByLoanId(int loanId) {
        List<Repayment> repayments = new ArrayList<>();
        String sql = "SELECT * FROM repayments WHERE loan_id = ? ORDER BY date_paid, repayment_id";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, loanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    repayments.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not load the loan's repayments", ex);
        }
        return repayments;
    }

    public BigDecimal sumByLoanId(int loanId) {
        String sql = "SELECT COALESCE(SUM(amount_paid), 0) FROM repayments WHERE loan_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, loanId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getBigDecimal(1) != null) {
                    return rs.getBigDecimal(1);
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not total the loan's repayments", ex);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Inserts the repayment and (optionally) updates the loan's status in ONE
     * transaction, so a repayment can never be saved without the matching
     * status change.
     *
     * @param item
     * @param newLoanStatus the status to set on the loan (e.g. PAID), or null
     * to leave it alone
     */
    public void createAndSetLoanStatus(Repayment item, String newLoanStatus) {
        String insertSql = "INSERT INTO repayments (loan_id, amount_paid, date_paid) VALUES (?, ?, ?)";
        String updateSql = "UPDATE loans SET status = ? WHERE loan_id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, item.getLoanId());
                    ps.setDouble(2, item.getAmountPaid());
                    ps.setDate(3, Date.valueOf(item.getDatePaid()));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            item.setId(keys.getInt(1));
                        }
                    }
                }
                if (newLoanStatus != null) {
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setString(1, newLoanStatus);
                        ps.setInt(2, item.getLoanId());
                        ps.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not record the repayment", ex);
        }
    }

    private Repayment mapRow(ResultSet rs) throws SQLException {
        Repayment repayment = new Repayment();
        repayment.setId(rs.getInt("repayment_id"));
        repayment.setLoanId(rs.getInt("loan_id"));
        repayment.setAmountPaid(rs.getDouble("amount_paid"));
        LocalDate datePaid = rs.getDate("date_paid").toLocalDate();
        repayment.setDatePaid(datePaid);
        return repayment;
    }
}
