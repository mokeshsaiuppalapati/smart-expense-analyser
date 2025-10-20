// File: src/main/java/com/expense/util/DataGenerator.java

package com.expense.util;

import com.expense.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Random;
import java.util.Scanner;

public class DataGenerator {

    private static final String[] CATEGORIES = {"Food", "Transport", "Groceries", "Bills", "Health", "Entertainment", "Shopping", "Personal Care", "Gifts"};
    private static final Random RAND = new Random();

    public static void main(String[] args) {
        // --- THIS IS THE NEW INTERACTIVE PART ---
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter the User ID to generate data for: ");
            int userId = scanner.nextInt();

            System.out.print("How many random transactions would you like to generate? (e.g., 500): ");
            int numberOfTransactions = scanner.nextInt();

            System.out.println("Generating " + numberOfTransactions + " sample data for user ID " + userId + "...");
            generateData(userId, numberOfTransactions);
            System.out.println("✅ Data generation complete.");

        } catch (Exception e) {
            System.err.println("❌ Failed to generate data. Please make sure you enter a valid number.");
            e.printStackTrace();
        }
    }

    /**
     * Generates random transaction data for a specific user.
     * @param userId The ID of the user to assign the data to.
     * @param numberOfTransactions The number of random transactions to create.
     * @throws SQLException if a database error occurs.
     */
    public static void generateData(int userId, int numberOfTransactions) throws SQLException {
        Database.createTables();

        // SQL now includes the user_id column
        String sql = "INSERT INTO transactions(user_id, timestamp, amount, description, category) VALUES(?,?,?,?,?)";

        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < numberOfTransactions; i++) {
                LocalDate date = generateRandomDate();
                String category = CATEGORIES[RAND.nextInt(CATEGORIES.length)];
                double amount = generateRandomAmount(category, date);
                String description = generateDescription(category);

                // We now set the userId as the first parameter
                ps.setInt(1, userId);
                ps.setLong(2, date.toEpochDay());
                ps.setDouble(3, amount);
                ps.setString(4, description);
                ps.setString(5, category);

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
            case "Personal Care" -> "Salon visit";
            case "Gifts" -> "Birthday present";
            default -> "Misc. purchase";
        };
    }
}