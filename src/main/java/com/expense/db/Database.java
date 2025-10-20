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
        try (Connection conn = connect();
             Statement st = conn.createStatement()) {

            String usersTableSql = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT NOT NULL UNIQUE, " +
                    "password_hash TEXT NOT NULL)";
            st.executeUpdate(usersTableSql);

            String transactionsTableSql = "CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "timestamp INTEGER NOT NULL, " +
                    "amount REAL NOT NULL, " +
                    "description TEXT, " +
                    "category TEXT NOT NULL, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id))";
            st.executeUpdate(transactionsTableSql);

            String budgetsTableSql = "CREATE TABLE IF NOT EXISTS budgets (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "category TEXT NOT NULL, " +
                    "monthly_limit REAL NOT NULL, " +
                    "last_alert_level TEXT, " + // e.g., "90_2025-10"
                    "UNIQUE(user_id, category), " +
                    "FOREIGN KEY (user_id) REFERENCES users(id))";
            st.executeUpdate(budgetsTableSql);

            String recurringTransactionsTableSql = "CREATE TABLE IF NOT EXISTS recurring_transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "description TEXT NOT NULL, " +
                    "amount REAL NOT NULL, " +
                    "category TEXT NOT NULL, " +
                    "frequency TEXT NOT NULL, " +
                    "next_due_timestamp INTEGER NOT NULL, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id))";
            st.executeUpdate(recurringTransactionsTableSql);

            String savingsGoalsTableSql = "CREATE TABLE IF NOT EXISTS savings_goals (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "goal_name TEXT NOT NULL, " +
                    "target_amount REAL NOT NULL, " +
                    "current_amount REAL NOT NULL DEFAULT 0.0, " +
                    "target_date_timestamp INTEGER, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id))";
            st.executeUpdate(savingsGoalsTableSql);
        }
    }
}