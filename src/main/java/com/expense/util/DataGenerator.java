package com.expense.util;

import com.expense.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.Month;
import java.util.Random;

public class DataGenerator {

    private static final String[] CATEGORIES = {"Food", "Transport", "Groceries", "Bills", "Health", "Entertainment", "Shopping"};
    private static final Random RAND = new Random();

    public static void main(String[] args) throws Exception {
        System.out.println("Generating sample data...");
        generateData(2000); // Generate 500 random transactions
        System.out.println("Data generation complete.");
    }

    public static void generateData(int numberOfTransactions) throws Exception {
        Database.createTables(); // Ensure tables exist

        String sql = "INSERT INTO transactions(date, amount, description, category) VALUES(?,?,?,?)";

        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < numberOfTransactions; i++) {
                LocalDate date = generateRandomDate();
                String category = CATEGORIES[RAND.nextInt(CATEGORIES.length)];
                double amount = generateRandomAmount(category, date);
                String description = generateDescription(category);

                ps.setString(1, date.toString());
                ps.setDouble(2, amount);
                ps.setString(3, description);
                ps.setString(4, category);

                ps.addBatch(); // Add to a batch for efficient insertion
            }

            ps.executeBatch(); // Execute all insertions at once
        }
    }

    private static LocalDate generateRandomDate() {
        // Generate a date within the last 2 years
        long minDay = LocalDate.now().minusYears(2).toEpochDay();
        long maxDay = LocalDate.now().toEpochDay();
        long randomDay = minDay + RAND.nextLong(maxDay - minDay);
        return LocalDate.ofEpochDay(randomDay);
    }

    private static double generateRandomAmount(String category, LocalDate date) {
        double baseAmount = switch (category) {
            case "Groceries" -> 50 + RAND.nextDouble() * 200; // 50-250
            case "Food" -> 10 + RAND.nextDouble() * 50;      // 10-60
            case "Transport" -> 5 + RAND.nextDouble() * 30;       // 5-35
            case "Bills" -> 100 + RAND.nextDouble() * 400;    // 100-500
            case "Health" -> 20 + RAND.nextDouble() * 150;    // 20-170
            default -> 15 + RAND.nextDouble() * 100;    // 15-115
        };

        // Make weekends slightly more expensive for Food & Entertainment
        if ((category.equals("Food") || category.equals("Entertainment")) &&
                (date.getDayOfWeek().getValue() >= 6)) { // Saturday or Sunday
            baseAmount *= 1.5;
        }

        // Make bills higher at the start of the month
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