// File: src/main/java/com/expense/util/DataImporter.java

package com.expense.util;

import com.expense.ml.WekaPredictor;
import com.expense.service.ExpenseService;
import javafx.concurrent.Task;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class DataImporter {

    public static Task<String> createImportTask(File file, ExpenseService service, WekaPredictor categorizer) {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                int successCount = 0;
                int errorCount = 0;
                int lineCount = 1;

                // We assume CSV has 3 columns: Date,Description,Amount
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line = br.readLine(); // Skip header row

                    while ((line = br.readLine()) != null) {
                        lineCount++;
                        String[] values = line.split(",");
                        if (values.length < 3) {
                            System.err.println("Skipping malformed row " + lineCount + ": " + line);
                            errorCount++;
                            continue;
                        }

                        try {
                            // Trim values to handle whitespace
                            String dateStr = values[0].trim();
                            String description = values[1].trim();
                            double amount = Double.parseDouble(values[2].trim());

                            // Parse the date string into a LocalDate object
                            LocalDate date = LocalDate.parse(dateStr);

                            WekaPredictor.Result prediction = categorizer.predict(description);
                            String category = prediction.category;

                            // Call the new service method with the LocalDate object
                            service.addTransaction(date, amount, description, category);
                            successCount++;

                        } catch (NumberFormatException | DateTimeParseException e) {
                            System.err.println("Skipping row with bad data format " + lineCount + ": " + e.getMessage());
                            errorCount++;
                        }
                    }
                }
                return String.format("Import complete.\nSuccessfully imported: %d transactions.\nFailed to import: %d rows.", successCount, errorCount);
            }
        };
    }
}