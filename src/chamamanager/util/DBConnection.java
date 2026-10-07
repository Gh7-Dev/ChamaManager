/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package chamamanager.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Provides the application's database-connection functionality.
 *
 * Settings are read from an optional local file named db.properties in the
 * project root (never committed to Git). If the file is missing, the defaults
 * below are used.
 *
 * @author gh7
 */
public final class DBConnection {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/chama_manager";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";
    private static final String CONFIG_FILE = "db.properties";

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            p.load(in);
        } catch (IOException e) {
            // No local db.properties: fall back to the defaults.
        }
        URL = p.getProperty("db.url", DEFAULT_URL);
        USER = p.getProperty("db.user", DEFAULT_USER);
        PASSWORD = p.getProperty("db.password", DEFAULT_PASSWORD);
    }

    private DBConnection() {
        // Prevent instantiation; this is a utility entry point.
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
