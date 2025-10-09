package com.expense.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:expenses.db";

    public static Connection connect() throws Exception {
        // This line establishes the connection to the database file
        return DriverManager.getConnection(DB_URL);
    }

    public static void createTables() {
        // This SQL command creates the tables if they don't already exist
        String transactionsTableSql = "CREATE TABLE IF NOT EXISTS transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "date TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "description TEXT, " +
                "category TEXT)";

        String budgetsTableSql = "CREATE TABLE IF NOT EXISTS budgets (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "category TEXT NOT NULL UNIQUE, " + // Made category unique
                "monthly_limit REAL NOT NULL)";

        try (Connection conn = connect();
             Statement st = conn.createStatement()) {

            st.executeUpdate(transactionsTableSql);
            st.executeUpdate(budgetsTableSql);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}