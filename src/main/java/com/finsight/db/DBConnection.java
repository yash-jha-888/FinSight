package com.finsight.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens the SQLite database file and creates the table the first time.
 * The whole app shares one connection.
 */
public class DBConnection {

    // the database is a single file in the user's home folder
    private static final String URL = "jdbc:sqlite:" + System.getProperty("user.home") + "/finsight.db";

    private static Connection connection;

    public static Connection getConnection() {
        try {
            if (connection == null) {
                connection = DriverManager.getConnection(URL);
                createTable();
            }
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException("Could not open database: " + e.getMessage());
        }
    }

    private static void createTable() throws SQLException {
        Statement st = connection.createStatement();

        st.execute("CREATE TABLE IF NOT EXISTS transactions ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "type TEXT NOT NULL, "              // 'Income' or 'Expense'
                + "amount REAL NOT NULL CHECK (amount > 0), "
                + "date TEXT NOT NULL, "              // like '2026-10-25'
                + "category TEXT NOT NULL, "
                + "description TEXT, "
                + "payment_method TEXT)");            // empty for income
        st.close();
    }
}
