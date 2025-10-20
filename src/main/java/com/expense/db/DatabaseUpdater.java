// File: src/main/java/com/expense/db/DatabaseUpdater.java

package com.expense.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseUpdater {

    /**
     * This is the single point of truth for ensuring the database schema is up-to-date.
     * It runs all necessary checks and creation/alteration statements. It is safe to run
     * every time the application starts.
     */
    public static void updateSchema() {
        System.out.println("Checking and updating database schema...");
        try {
            // First, ensure all tables exist. This is fast and safe.
            Database.createTables();

            // Now, perform checks to add missing columns to existing tables.
            checkAndAddColumn("transactions", "user_id", "INTEGER");
            checkAndAddColumn("budgets", "user_id", "INTEGER");
            checkAndAddColumn("recurring_transactions", "user_id", "INTEGER");
            checkAndAddColumn("savings_goals", "user_id", "INTEGER");

            System.out.println("Database schema is up-to-date.");
        } catch (Exception e) {
            System.err.println("FATAL: Could not update database schema!");
            e.printStackTrace();
        }
    }

    /**
     * Checks if a column exists in a table, and if not, adds it.
     *
     * @param tableName  The name of the table to check.
     * @param columnName The name of the column to add.
     * @param columnType The SQL type of the new column (e.g., "INTEGER", "TEXT").
     * @throws SQLException if a database error occurs.
     */
    private static void checkAndAddColumn(String tableName, String columnName, String columnType) throws SQLException {
        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement()) {

            // Use PRAGMA to get table info. This is a standard SQLite way to inspect a table.
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")");
            List<String> columns = new ArrayList<>();
            while (rs.next()) {
                columns.add(rs.getString("name"));
            }

            // If the column does NOT exist, add it.
            if (!columns.contains(columnName)) {
                System.out.println("Column '" + columnName + "' not found in table '" + tableName + "'. Adding it...");
                stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
                System.out.println("Column '" + columnName + "' added successfully.");
            }
        }
    }
}