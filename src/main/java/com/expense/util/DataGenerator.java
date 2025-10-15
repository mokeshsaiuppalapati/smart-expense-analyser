// File: src/main/java/com/expense/util/DataGenerator.java

package com.expense.util;

import com.expense.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Random;

public class DataGenerator {

    private static final String[] CATEGORIES = {"Food", "Transport", "Groceries", "Bills", "Health", "Entertainment", "Shopping"};
    private static final Random RAND = new Random();

    public static void main(String[] args) {
        try {
            System.out.println("Generating sample data...");
            generateData(500); // Generate 500 random transactions
            System.out.println("Data generation complete.");
        } catch (Exception e) {
            System.err.println("Failed to generate data.");
            e.printStackTrace();
        }
    }

    public static void generateData(int numberOfTransactions) throws SQLException {
        Database.createTables(); // Ensure tables exist

        // FIXED: SQL now correctly references the 'timestamp' column
        String sql = "INSERT INTO transactions(timestamp, amount, description, category) VALUES(?,?,?,?)";

        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < numberOfTransactions; i++) {
                LocalDate date = generateRandomDate();
                String category = CATEGORIES[RAND.nextInt(CATEGORIES.length)];
                double amount = generateRandomAmount(category, date);
                String description = generateDescription(category);

                // FIXED: Now correctly setting the date as a long (epoch day)
                ps.setLong(1, date.toEpochDay());
                ps.setDouble(2, amount);
                ps.setString(3, description);
                ps.setString(4, category);

                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    private static LocalDate generateRandomDate() {
        long minDay = LocalDate.now().minusYears(2).toEpochDay();
        long maxDay = LocalDate.now().toEpochDay();
        long randomDay = minDay + RAND.nextLong(maxDay - minDay);
        return LocalDate.ofEpochDay(randomDay);
    }

    private static double generateRandomAmount(String category, LocalDate date) {
        double baseAmount = switch (category) {
            case "Groceries" -> 50 + RAND.nextDouble() * 200;
            case "Food" -> 10 + RAND.nextDouble() * 50;
            case "Transport" -> 5 + RAND.nextDouble() * 30;
            case "Bills" -> 100 + RAND.nextDouble() * 400;
            case "Health" -> 20 + RAND.nextDouble() * 150;
            default -> 15 + RAND.nextDouble() * 100;
        };

        if ((category.equals("Food") || category.equals("Entertainment")) && (date.getDayOfWeek().getValue() >= 6)) {
            baseAmount *= 1.5;
        }
        if (category.equals("Bills") && date.getDayOfMonth() < 5) {
            baseAmount *= 2.0;
        }

        return Math.round(baseAmount * 100.0) / 100.0;
    }

    private static String generateDescription(String category) {
        return switch (category) {
            case "Groceries" -> "Supermarket run";
            case "Food" -> "Lunch at cafe";
            case "Transport" -> "Bus fare";
            case "Bills" -> "Internet bill";
            case "Health" -> "Pharmacy purchase";
            case "Entertainment" -> "Movie ticket";
            case "Shopping" -> "New clothes";
            default -> "Misc. purchase";
        };
    }
}