/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chamamanager.dao;

import chamamanager.exceptions.DataAccessException;
import chamamanager.model.Member;
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
 * Handles persistence operations for {@link Member} via JDBC.
 *
 * @author gh7
 */
public class MemberDAO implements CrudOperations<Member> {

    @Override
    public void create(Member item) {
        String sql = "INSERT INTO members "
                + "(full_name, username, password_hash, phone, date_joined) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getFullName());
            ps.setString(2, item.getUsername());
            ps.setString(3, item.getPasswordHash());
            ps.setString(4, item.getPhone());
            ps.setDate(5, Date.valueOf(item.getDateJoined()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Could not add new member", ex);
        }
    }

    @Override
    public Member getById(int id) {
        String sql = "SELECT * FROM members WHERE member_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not find new member", ex);
        }
        return null;
    }

    @Override
    public List<Member> getAll() {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT * FROM members";
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                members.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Could not retieve the members", ex);
        }
        return members;
    }

    @Override
    public void update(Member item) {
        String sql = "UPDATE members SET full_name = ?, username = ?, "
                + "password_hash = ?, phone = ?, date_joined = ? WHERE member_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getFullName());
            ps.setString(2, item.getUsername());
            ps.setString(3, item.getPasswordHash());
            ps.setString(4, item.getPhone());
            ps.setDate(5, Date.valueOf(item.getDateJoined()));
            ps.setInt(6, item.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Could not update the member details", ex);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM members WHERE member_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Could not delete a member", ex);
        }
    }

    private Member mapRow(ResultSet rs) throws SQLException {
        Member member = new Member();
        member.setId(rs.getInt("member_id"));
        member.setFullName(rs.getString("full_name"));
        member.setUsername(rs.getString("username"));
        member.setPasswordHash(rs.getString("password_hash"));
        member.setPhone(rs.getString("phone"));
        LocalDate dateJoined = rs.getDate("date_joined").toLocalDate();
        member.setDateJoined(dateJoined);
        return member;
    }

    // Additional lookup methods pending — to be added once finalized.
}
