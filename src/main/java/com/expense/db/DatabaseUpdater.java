// File: src/main/java/com/expense/db/DatabaseUpdater.java

package com.expense.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseUpdater {

    public static void updateSchema() throws SQLException {
        try (Connection conn = Database.connect();
             Statement st = conn.createStatement()) {

            // Check if 'budgets' table exists at all
            boolean budgetsTableExists = false;
            try (ResultSet rs = conn.getMetaData().getTables(null, null, "budgets", null)) {
                budgetsTableExists = rs.next();
            }

            if (!budgetsTableExists) {
                System.out.println("Budgets table does not exist. Creating it now with correct schema.");
                Database.createTables(); // This will create all tables, including budgets, with the correct schema
                return; // No further migration needed if table didn't exist
            }

            // Check if 'last_alert_level' column exists
            boolean lastAlertLevelColumnExists = false;
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "budgets", "last_alert_level")) {
                lastAlertLevelColumnExists = rs.next();
            }

            // Check the current UNIQUE constraints on 'budgets'
            boolean correctUniqueConstraintExists = false;
            try (ResultSet rs = conn.getMetaData().getIndexInfo(null, null, "budgets", true, true)) { // true for unique, true for approximate
                List<String> uniqueConstraintColumns = new ArrayList<>();
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    // Filter out internal SQLite indices like primary keys if they are not the target composite key
                    // We are specifically looking for a unique index that contains both user_id and category
                    if (rs.getBoolean("NON_UNIQUE") == false) { // Only consider unique indexes
                        // For SQLite, index_info will show all columns in a composite index
                        // Check if this index explicitly lists both user_id and category
                        String columnName = rs.getString("COLUMN_NAME");
                        if (columnName != null) {
                            uniqueConstraintColumns.add(columnName);
                        }
                        // If we've found both, we can assume the constraint exists. This check might be simplified for SQLite.
                        // A more robust check might involve parsing PRAGMA index_info(index_name) for each index.
                    }
                }
                // Simplified check: if we see user_id AND category in any unique constraint, assume it's the correct one.
                // This isn't perfect for all DBs but works for typical SQLite composite unique.
                if (uniqueConstraintColumns.contains("user_id") && uniqueConstraintColumns.contains("category")) {
                    // This is a heuristic. For absolute certainty, one would check 'PRAGMA index_list(budgets)'
                    // and then 'PRAGMA index_info(index_name)' for each unique index to see if (user_id, category)
                    // is *the* unique constraint or part of one.
                    // For this context, if both columns are part of *any* unique index, it's a good sign.
                    // However, the migration below handles edge cases more robustly by rebuilding.
                }
            }
            // A more direct way to check for the correct unique constraint by recreating it if it's missing/wrong
            // This is better than trying to inspect existing constraints which can be tricky.

            // The migration strategy: rebuild if schema is not as expected.
            // This ensures last_alert_level and the correct UNIQUE constraint.
            if (!lastAlertLevelColumnExists || !correctUniqueConstraintExists) { // Re-evaluate condition to ensure it runs when needed
                System.out.println("Budgets table schema needs update (missing last_alert_level or incorrect UNIQUE constraint). Migrating data...");

                // 1. Store existing budget data temporarily in memory
                List<BudgetTempData> existingBudgets = new ArrayList<>();
                String selectExistingSql = "SELECT id, user_id, category, monthly_limit FROM budgets";
                try (ResultSet rs = st.executeQuery(selectExistingSql)) {
                    while (rs.next()) {
                        existingBudgets.add(new BudgetTempData(
                                rs.getInt("id"),
                                rs.getInt("user_id"),
                                rs.getString("category"),
                                rs.getDouble("monthly_limit")
                        ));
                    }
                }

                // 2. Drop the original table
                String dropOriginalTableSql = "DROP TABLE budgets";
                st.executeUpdate(dropOriginalTableSql);
                System.out.println("Original budgets table dropped.");

                // 3. Recreate the budgets table with the correct schema
                // This call to Database.createTables will create the 'budgets' table with the correct schema
                // including 'last_alert_level' and 'UNIQUE(user_id, category)'
                Database.createTables();
                System.out.println("Budgets table recreated with correct schema.");

                // 4. Insert data back into the new table
                String insertNewSql = "INSERT INTO budgets (id, user_id, category, monthly_limit, last_alert_level) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertNewSql)) {
                    for (BudgetTempData data : existingBudgets) {
                        ps.setInt(1, data.id);
                        ps.setInt(2, data.userId);
                        ps.setString(3, data.category);
                        ps.setDouble(4, data.monthlyLimit);
                        ps.setString(5, "none"); // Default value for new 'last_alert_level' for migrated data
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                System.out.println("Existing budget data migrated successfully.");
            } else {
                System.out.println("Budgets table schema is up to date. No migration needed.");
            }

            // Ensure all other tables exist (this is a safeguard)
            Database.createTables();

        } catch (SQLException e) {
            System.err.println("Error during database schema update: " + e.getMessage());
            throw e; // Re-throw to indicate a critical error
        }
    }

    // Helper class to temporarily hold budget data during migration
    private static class BudgetTempData {
        int id;
        int userId;
        String category;
        double monthlyLimit;

        public BudgetTempData(int id, int userId, String category, double monthlyLimit) {
            this.id = id;
            this.userId = userId;
            this.category = category;
            this.monthlyLimit = monthlyLimit;
        }
    }
}