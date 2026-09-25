/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chamamanager.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Provides the application's database-connection functionality.
 *
 * @author gh7
 */
public final class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/chama_manager";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private DBConnection() {
        // Prevent instantiation; this is a utility entry point.
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
