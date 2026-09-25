/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chamamanager.dao;

import chamamanager.model.Repayment;
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
 * Handles persistence operations for {@link Repayment} via JDBC.
 *
 * @author gh7
 */
public class RepaymentDAO implements CrudOperations<Repayment> {

    @Override
    public void create(Repayment item) {
        String sql = "INSERT INTO repayments "
                + "(loan_id, amount_paid, date_paid) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getLoanId());
            ps.setDouble(2, item.getAmountPaid());
            ps.setDate(3, Date.valueOf(item.getDatePaid()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Repayment getById(int id) {
        String sql = "SELECT * FROM repayments WHERE repayment_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Repayment> getAll() {
        List<Repayment> repayments = new ArrayList<>();
        String sql = "SELECT * FROM repayments";
        try (Connection conn = DBConnection.getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                repayments.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return repayments;
    }

    @Override
    public void update(Repayment item) {
        String sql = "UPDATE repayments SET loan_id = ?, amount_paid = ?, "
                + "date_paid = ? WHERE repayment_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getLoanId());
            ps.setDouble(2, item.getAmountPaid());
            ps.setDate(3, Date.valueOf(item.getDatePaid()));
            ps.setInt(4, item.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM repayments WHERE repayment_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
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

    // Additional lookup methods pending — to be added once finalized.
}
