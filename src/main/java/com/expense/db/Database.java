// File: src/main/java/com/expense/db/Database.java

package com.expense.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:expenses.db";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void createTables() throws SQLException {
        String transactionsTableSql = "CREATE TABLE IF NOT EXISTS transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "timestamp INTEGER NOT NULL, " +
                "amount REAL NOT NULL, " +
                "description TEXT, " +
                "category TEXT NOT NULL)";

        String budgetsTableSql = "CREATE TABLE IF NOT EXISTS budgets (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "category TEXT NOT NULL UNIQUE, " +
                "monthly_limit REAL NOT NULL)";

        String recurringTransactionsTableSql = "CREATE TABLE IF NOT EXISTS recurring_transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "description TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "category TEXT NOT NULL, " +
                "frequency TEXT NOT NULL, " +
                "next_due_timestamp INTEGER NOT NULL)";

        // --- NEW TABLE FOR SAVINGS GOALS ---
        String savingsGoalsTableSql = "CREATE TABLE IF NOT EXISTS savings_goals (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "goal_name TEXT NOT NULL, " +
                "target_amount REAL NOT NULL, " +
                "current_amount REAL NOT NULL DEFAULT 0.0, " +
                "target_date_timestamp INTEGER)"; // Optional target date

        try (Connection conn = connect();
             Statement st = conn.createStatement()) {

            st.executeUpdate(transactionsTableSql);
            st.executeUpdate(budgetsTableSql);
            st.executeUpdate(recurringTransactionsTableSql);
            st.executeUpdate(savingsGoalsTableSql); // Create the new table
        }
    }
}