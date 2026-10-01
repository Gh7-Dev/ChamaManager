/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chamamanager.dao;

import chamamanager.exceptions.DataAccessException;
import chamamanager.model.Contribution;
import chamamanager.util.DBConnection;
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
 * Handles persistence operations for {@link Contribution} via JDBC.
 *
 * @author gh7
 */
public class ContributionDAO implements CrudOperations<Contribution> {

    @Override
    public void create(Contribution item) {
        String sql = "INSERT INTO contributions "
                + "(member_id, amount, date_paid, period, status) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getMemberId());
            ps.setDouble(2, item.getAmount());
            ps.setDate(3, Date.valueOf(item.getDatePaid()));
            ps.setString(4, item.getPeriod());
            ps.setString(5, item.getStatus());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Could not create a contribution", ex);
        }
    }

    @Override
    public Contribution getById(int id) {
        String sql = "SELECT * FROM contributions WHERE contribution_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Couuld not find contribution", ex);
        }
        return null;
    }

    @Override
    public List<Contribution> getAll() {
        List<Contribution> contributions = new ArrayList<>();
        String sql = "SELECT * FROM contributions";
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                contributions.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not find all the contributions", ex);

        }
        return contributions;
    }

    @Override
    public void update(Contribution item) {
        String sql = "UPDATE contributions SET member_id = ?, amount = ?, "
                + "date_paid = ?, period = ?, status = ? WHERE contribution_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getMemberId());
            ps.setDouble(2, item.getAmount());
            ps.setDate(3, Date.valueOf(item.getDatePaid()));
            ps.setString(4, item.getPeriod());
            ps.setString(5, item.getStatus());
            ps.setInt(6, item.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Could not update contribution", ex);

        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM contributions WHERE contribution_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Could not delete contribution", ex);
        }
    }

    private Contribution mapRow(ResultSet rs) throws SQLException {
        Contribution contribution = new Contribution();
        contribution.setId(rs.getInt("contribution_id"));
        contribution.setMemberId(rs.getInt("member_id"));
        contribution.setAmount(rs.getDouble("amount"));
        LocalDate datePaid = rs.getDate("date_paid").toLocalDate();
        contribution.setDatePaid(datePaid);
        contribution.setPeriod(rs.getString("period"));
        contribution.setStatus(rs.getString("status"));
        return contribution;
    }

    // Additional lookup methods pending — to be added once finalized.
}
