/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chamamanager.dao;

import chamamanager.exceptions.DataAccessException;
import chamamanager.model.Loan;
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
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getMemberId());
            ps.setDouble(2, item.getPrincipal());
            ps.setDouble(3, item.getInterestRate());
            // PENDING loans have no dates yet — bind NULL instead of NPE.
            ps.setDate(4, item.getDateIssued() == null ? null : Date.valueOf(item.getDateIssued()));
            ps.setDate(5, item.getDueDate() == null ? null : Date.valueOf(item.getDueDate()));
            ps.setString(6, item.getStatus());
            ps.executeUpdate();
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
        List<Loan> loans = new ArrayList<>();
        String sql = "SELECT * FROM loans";
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                loans.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not find all the loans", ex);
        }
        return loans;
    }

    @Override
    public void update(Loan item) {
        String sql = "UPDATE loans SET member_id = ?, principal = ?, "
                + "interest_rate = ?, date_issued = ?, due_date = ?, status = ? "
                + "WHERE loan_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getMemberId());
            ps.setDouble(2, item.getPrincipal());
            ps.setDouble(3, item.getInterestRate());
            // PENDING loans have no dates yet — bind NULL instead of NPE. Same rule as create()/mapRow().
            ps.setDate(4, item.getDateIssued() == null ? null : Date.valueOf(item.getDateIssued()));
            ps.setDate(5, item.getDueDate() == null ? null : Date.valueOf(item.getDueDate()));
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

    // Additional lookup methods pending — to be added once finalized.
}
