// File: src/main/java/com/expense/util/DataExporter.java

package com.expense.util;

import com.expense.model.Budget;
import com.expense.model.Transaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class DataExporter {

    /**
     * Exports a list of transactions to a CSV file, correctly handling commas and quotes.
     * @param path The file path to save to.
     * @param list The list of transactions to export.
     */
    public static void exportTransactions(String path, List<Transaction> list) throws IOException {
        String[] HEADERS = { "Date", "Amount", "Description", "Category" };
        // Use try-with-resources for automatic closing of the writer and printer
        try (FileWriter out = new FileWriter(path);
             CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.builder().setHeader(HEADERS).build())) {

            for (Transaction t : list) {
                // This will correctly handle commas in the description by quoting the field
                printer.printRecord(t.getDate(), t.getAmount(), t.getDescription(), t.getCategory());
            }
        }
    }

    /**
     * Exports a list of budgets to a CSV file.
     * @param path The file path to save to.
     * @param list The list of budgets to export.
     */
    public static void exportBudgets(String path, List<Budget> list) throws IOException {
        String[] HEADERS = { "Category", "Limit" };
        try (FileWriter out = new FileWriter(path);
             CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.builder().setHeader(HEADERS).build())) {
            for (Budget b : list) {
                printer.printRecord(b.getCategory(), b.getMonthlyLimit());
            }
        }
    }
}