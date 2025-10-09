package com.expense.util;

import com.expense.model.Transaction;
import com.expense.model.Budget;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class DataExporter {
    public static void exportTransactions(String path, List<Transaction> list) {
        try (FileWriter fw = new FileWriter(path)) {
            fw.write("Date,Amount,Description,Category\n");
            for (Transaction t : list) {
                fw.write(t.getDate() + "," + t.getAmount() + "," + t.getDescription() + "," + t.getCategory() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void exportBudgets(String path, List<Budget> list) {
        try (FileWriter fw = new FileWriter(path)) {
            fw.write("Category,Limit\n");
            for (Budget b : list) {
                fw.write(b.getCategory() + "," + b.getMonthlyLimit() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
