package com.expense.app;

import com.expense.service.ExpenseService;
import com.expense.model.Transaction;
import com.expense.ml.AutoCategorizer;
import com.expense.util.ChartHelper;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        ExpenseService service = new ExpenseService();
        service.init();

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n====== Smart Expense Analyzer (CLI) ======");
            System.out.println("1) Add transaction");
            System.out.println("2) List transactions");
            System.out.println("3) Show category totals");
            System.out.println("4) Show monthly totals");
            System.out.println("5) Show charts");
            System.out.println("6) Exit");

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

            switch (ch) {
                case 1:
                    System.out.print("Enter description: ");
                    String desc = sc.nextLine().trim();
                    if (desc.isBlank()) {
                        System.out.println("Description required.");
                        break;
                    }

                    System.out.print("Enter amount: ");
                    String amtLine = sc.nextLine().trim();
                    double amt;
                    try { amt = Double.parseDouble(amtLine); }
                    catch (NumberFormatException e) { System.out.println("Invalid amount."); break; }

                    String suggested = AutoCategorizer.predict(desc);
                    System.out.println("Suggested category: " + suggested);
                    System.out.print("Press Enter to accept or type new category: ");
                    String catInput = sc.nextLine().trim();
                    String cat = catInput.isBlank() ? suggested : catInput;

                    service.addTransaction(amt, desc, cat);
                    System.out.println("Saved.");
                    break;

                case 2:
                    List<Transaction> list = service.getAll();
                    if (list.isEmpty()) System.out.println("No transactions yet.");
                    else {
                        System.out.println("\nDate | Amount | Description | Category");
                        for (Transaction t : list) {
                            System.out.println(t.getDate() + " | " +
                                    t.getAmount() + " | " +
                                    t.getDescription() + " | " +
                                    t.getCategory());
                        }
                    }
                    break;

                case 3:
                    Map<String, Double> catTotals = service.getCategoryTotals();
                    if (catTotals.isEmpty()) System.out.println("No data.");
                    else catTotals.forEach((k, v) -> System.out.println(k + " : " + v));
                    break;

                case 4:
                    Map<String, Double> months = service.getMonthlyTotals();
                    if (months.isEmpty()) System.out.println("No data.");
                    else months.forEach((k, v) -> System.out.println(k + " : " + v));
                    break;

                case 5:
                    ChartHelper.showPieChart("Category Totals", service.getCategoryTotals());
                    ChartHelper.showBarChart("Monthly Totals", service.getMonthlyTotals());
                    break;

                case 6:
                    System.out.println("Bye.");
                    sc.close();
                    System.exit(0);
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }
}
