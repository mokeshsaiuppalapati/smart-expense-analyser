package com.expense.ui;

import com.expense.ml.WekaPredictor;
import com.expense.service.ExpenseService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddExpenseController {

    @FXML private TextField descField;
    @FXML private TextField amountField;
    @FXML private TextField categoryField;

    private ExpenseService service;
    private MainController parentController;
    private WekaPredictor categorizer;

    // The parameter name 'predictor' was correct, but the assignment inside was wrong.
    public void initData(ExpenseService service, MainController parentController, WekaPredictor predictor) {
        this.service = service;
        this.parentController = parentController;
        // FIXED: Assign the 'categorizer' field from the 'predictor' parameter
        this.categorizer = predictor;
    }

    @FXML
    private void onSave() {
        String desc = descField.getText().trim();
        String amtStr = amountField.getText().trim();
        String category = categoryField.getText().trim();
        if (desc.isEmpty() || amtStr.isEmpty() || category.isEmpty()) {
            showAlert("Validation Error", "All fields are required.");
            return;
        }
        try {
            double amount = Double.parseDouble(amtStr);
            boolean success = service.addTransaction(amount, desc, category);
            if (success) {
                if (parentController != null) {
                    parentController.refreshData();
                }
                closeWindow();
            } else {
                showAlert("Error", "Failed to save transaction.");
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Amount must be a valid number.");
        }
    }

    @FXML
    private void onSuggest() {
        String desc = descField.getText().trim();
        if (desc.isEmpty()) {
            showAlert("Info", "Please enter a description first.");
            return;
        }
        if (categorizer != null && categorizer.isModelLoaded()) {
            WekaPredictor.Result result = categorizer.predict(desc);
            categoryField.setText(result.category);
        } else {
            showAlert("Model Not Found", "The category suggestion model is not loaded. Please train it first.");
        }
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) descField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}