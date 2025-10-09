package com.expense.util;

import com.expense.ml.WekaPredictor;
import com.expense.service.ExpenseService;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class DataImporter {

    public static Task<String> createImportTask(File file, ExpenseService service, WekaPredictor categorizer) {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                int successCount = 0;
                int errorCount = 0;

                // We assume the CSV has 3 columns: Date,Description,Amount
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    br.readLine(); // Skip header row

                    while ((line = br.readLine()) != null) {
                        String[] values = line.split(",");
                        if (values.length < 3) {
                            errorCount++;
                            continue;
                        }

                        try {
                            String date = values[0].trim();
                            String description = values[1].trim();
                            double amount = Double.parseDouble(values[2].trim());

                            // Use the AI to suggest a category!
                            WekaPredictor.Result prediction = categorizer.predict(description);
                            String category = prediction.category;

                            service.addTransaction(amount, description, category);
                            successCount++;

                        } catch (NumberFormatException e) {
                            // This happens if the amount column is not a valid number
                            errorCount++;
                        }
                    }
                }

                return String.format("Import complete.\nSuccessfully imported: %d transactions.\nFailed to import: %d rows.", successCount, errorCount);
            }
        };
    }
}