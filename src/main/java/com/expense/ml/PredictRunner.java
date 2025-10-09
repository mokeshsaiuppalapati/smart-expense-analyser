package com.expense.ml;

import com.expense.model.TransactionData;
import com.expense.service.ExpenseService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class PredictRunner {
    public static void main(String[] args) throws Exception {
        ExpenseService service = new ExpenseService();
        service.init();

        // 1. Get training data from the database
        List<TransactionData> trainingData = service.getTransactionDataForRegression();
        if (trainingData.isEmpty()) {
            System.out.println("No transaction data in the database to train the model.");
            return;
        }

        // 2. Train the model
        ExpensePredictor predictor = new ExpensePredictor();
        System.out.println("Training regression model from database...");
        predictor.train(trainingData);
        System.out.println("Training finished.");

        // 3. Load category map from the database
        Map<String, Integer> catToCodeMap = service.getCategoryCodeMap();
        if (catToCodeMap.isEmpty()) {
            System.out.println("No categories found in the database.");
            return;
        }

        // 4. Predict for tomorrow
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        System.out.println("\nPredicting for date: " + tomorrow);

        for (Map.Entry<String, Integer> entry : catToCodeMap.entrySet()) {
            String category = entry.getKey();
            int code = entry.getValue();

            // MODIFIED: Call the 'predict' method with the correct new arguments
            double pred = predictor.predict(
                    tomorrow.getDayOfWeek().getValue(),
                    tomorrow.getDayOfMonth(),
                    tomorrow.getMonthValue(),
                    code
            );

            if (pred > 0) {
                System.out.printf("- %s: predicted amount = %.2f%n", category, pred);
            }
        }

        System.out.println("\nDone.");
    }
}