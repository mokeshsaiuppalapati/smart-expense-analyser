// File: src/main/java/com/expense/db/DatabaseUpdater.java

package com.expense.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseUpdater {

    public static void updateSchema() {
        System.out.println("Checking and updating database schema...");
        try {
            // First, ensure all tables exist.
            Database.createTables();

            // Now, perform checks to add missing columns to existing tables.
            checkAndAddColumn("transactions", "user_id", "INTEGER");
            checkAndAddColumn("budgets", "user_id", "INTEGER");
            checkAndAddColumn("recurring_transactions", "user_id", "INTEGER");
            checkAndAddColumn("savings_goals", "user_id", "INTEGER");

            // --- NEW CHECK FOR THE BUDGET ALERT FEATURE ---
            checkAndAddColumn("budgets", "last_alert_level", "TEXT");

            System.out.println("Database schema is up-to-date.");
        } catch (Exception e) {
            System.err.println("FATAL: Could not update database schema!");
            e.printStackTrace();
        }
    }

    private static void checkAndAddColumn(String tableName, String columnName, String columnType) throws SQLException {
        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")");
            List<String> columns = new ArrayList<>();
            while (rs.next()) {
                columns.add(rs.getString("name"));
            }

            if (!columns.contains(columnName)) {
                System.out.println("Column '" + columnName + "' not found in table '" + tableName + "'. Adding it...");
                stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
                System.out.println("Column '" + columnName + "' added successfully.");
            }
        }
    }
}