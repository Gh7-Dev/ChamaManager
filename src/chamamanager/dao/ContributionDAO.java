package chamamanager.dao;

import chamamanager.exceptions.DataAccessException;
import chamamanager.model.Contribution;
import chamamanager.model.Contribution.Status;
import chamamanager.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Database access for contributions only.
 * Every method reports failures as DataAccessException (never SQLException).
 * Screens must not call this class directly: they go through ContributionService.
 * Loan approval belongs to LoanService.
 */
public class ContributionDAO implements CrudOperations<Contribution> {

    /** Saves a new contribution. It always starts as PENDING. */
    public boolean addContribution(Contribution c) {
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
        } catch (SQLException e) {
            throw wrap("Could not save contribution", e);
        }
    }

    // ================= CrudOperations contract =================

    @Override
    public void create(Contribution item) {
        addContribution(item);
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
        return query("SELECT * FROM contributions ORDER BY date_paid DESC, contribution_id DESC", null, 0);
    }

    /** Edits the details of a contribution. Status changes only through approve/reject. */
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

    public List<Contribution> getByStatus(Status status) {
        return query("SELECT * FROM contributions WHERE status = ? "
                + "ORDER BY date_paid DESC, contribution_id DESC", status.name(), 0);
    }

    public List<Contribution> getByMember(int memberId) {
        return query("SELECT * FROM contributions WHERE member_id = ? "
                + "ORDER BY date_paid DESC, contribution_id DESC", null, memberId);
    }

    /** True if the member already has a PENDING or CONFIRMED contribution for this period. */
    public boolean existsForMemberAndPeriod(int memberId, String period) {
        String sql = "SELECT COUNT(*) FROM contributions "
                + "WHERE member_id = ? AND period = ? AND status <> 'REJECTED'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.setString(2, period);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw wrap("Could not check for duplicate contribution", e);
        }
    }

    /** Total of CONFIRMED (Treasurer-approved) contributions only. */
    public BigDecimal getApprovedTotal() {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM contributions WHERE status = 'CONFIRMED'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        } catch (SQLException e) {
            throw wrap("Could not calculate approved total", e);
        }
    }

    /** Total of CONFIRMED contributions for one member (used for the loan limit). */
    public BigDecimal getConfirmedTotalByMember(int memberId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM contributions "
                + "WHERE member_id = ? AND status = 'CONFIRMED'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw wrap("Could not calculate confirmed total for member " + memberId, e);
        }
    }

    // ----- Approval (contributions only) -----

    public boolean approveContribution(int id) {
        return setContributionStatus(id, Status.CONFIRMED);
    }

    public boolean rejectContribution(int id) {
        return setContributionStatus(id, Status.REJECTED);
    }

    /** Only PENDING rows can change, so a decision cannot be overwritten. */
    private boolean setContributionStatus(int id, Status status) {
        String sql = "UPDATE contributions SET status = ? WHERE contribution_id = ? AND status = 'PENDING'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw wrap("Could not update status of contribution " + id, e);
        }
    }

    // ----- Helpers -----

    private List<Contribution> query(String sql, String statusParam, int memberParam) {
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
        } catch (SQLException e) {
            throw wrap("Could not load contributions", e);
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
}