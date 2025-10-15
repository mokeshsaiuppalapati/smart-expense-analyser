// File: src/main/java/com/expense/app/Main.java

package com.expense.app;

import com.expense.model.Transaction;
import com.expense.service.ExpenseService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        ExpenseService service = new ExpenseService();
        try {
            service.init();
        } catch (Exception e) {
            System.out.println("FATAL: Could not initialize database. Exiting.");
            e.printStackTrace();
            return;
        }

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n====== Smart Expense Analyzer (CLI) ======");
            System.out.println("1) Add transaction");
            System.out.println("2) List transactions");
            System.out.println("3) Exit");

            System.out.print("Choose: ");
            String line = sc.nextLine();
            if (line.isBlank()) continue;
            int ch;
            try {
                ch = Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid number.");
                continue;
            }

            try {
                switch (ch) {
                    case 1:
                        System.out.print("Enter description: ");
                        String desc = sc.nextLine().trim();
                        System.out.print("Enter amount: ");
                        double amt = Double.parseDouble(sc.nextLine().trim());
                        System.out.print("Enter category: ");
                        String cat = sc.nextLine().trim();
                        // Call the updated service method
                        service.addTransaction(LocalDate.now(), amt, desc, cat);
                        System.out.println("Saved.");
                        break;
                    case 2:
                        List<Transaction> list = service.getAll();
                        if (list.isEmpty()) System.out.println("No transactions yet.");
                        else list.forEach(t -> System.out.println(t.getDate() + " | " + t.getAmount() + " | " + t.getDescription() + " | " + t.getCategory()));
                        break;
                    case 3:
                        System.out.println("Bye.");
                        sc.close();
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}