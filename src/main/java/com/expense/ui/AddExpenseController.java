// File: src/main/java/com/expense/ui/AddExpenseController.java

package com.expense.ui;

import com.expense.ml.WekaPredictor;
import com.expense.service.ExpenseService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class AddExpenseController {

    @FXML private DatePicker datePicker;
    @FXML private TextField descField;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> categoryCombo;

    private ExpenseService service;
    private MainController parentController;
    private WekaPredictor categorizer;

    // Define confidence thresholds
    private static final double CONFIDENT_THRESHOLD = 0.90; // 90%
    private static final double UNCERTAIN_THRESHOLD = 0.60; // 60%

    public void initData(ExpenseService service, MainController parent, WekaPredictor predictor, List<String> categories) {
        this.service = service;
        this.parentController = parent;
        this.categorizer = predictor;

        categoryCombo.setItems(FXCollections.observableArrayList(categories));
        datePicker.setValue(LocalDate.now());

        // Add a listener to clear the confidence color when the user types
        categoryCombo.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            clearConfidenceStyles();
        });
    }

    @FXML
    private void onSave() {
        String desc = descField.getText().trim();
        String amtStr = amountField.getText().trim();
        String category = categoryCombo.getEditor().getText().trim();
        LocalDate date = datePicker.getValue();

        if (desc.isEmpty() || amtStr.isEmpty() || category.isEmpty() || date == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "All fields are required.");
            return;
        }

        try {
            double amount = Double.parseDouble(amtStr);
            if (amount <= 0) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Amount must be a positive number.");
                return;
            }

            if (service.isAnomalous(category, amount)) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Unusual Spending Alert");
                confirmAlert.setHeaderText(String.format("This expense of ₹%.2f seems unusually high for '%s'.", amount, category));
                confirmAlert.setContentText("Are you sure you want to add this transaction?");

                Optional<ButtonType> result = confirmAlert.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK) {
                    return;
                }
            }

            saveTransaction(date, amount, desc, category);

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Amount must be a valid number.");
        }
    }

    private void saveTransaction(LocalDate date, double amount, String desc, String category) {
        try {
            service.addTransaction(date, amount, desc, category);
            parentController.refreshData();
            closeWindow();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to save transaction: " + e.getMessage());
        }
    }

    @FXML
    private void onSuggest() {
        String desc = descField.getText().trim();
        if (desc.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Info", "Please enter a description first.");
            return;
        }
        if (categorizer != null && categorizer.isModelLoaded()) {
            WekaPredictor.Result result = categorizer.predict(desc);

            // --- THIS IS THE NEW LOGIC FOR THE CONFIDENCE METER ---
            clearConfidenceStyles();
            categoryCombo.setValue(result.category);

            if (result.confidence >= CONFIDENT_THRESHOLD) {
                categoryCombo.getStyleClass().add("combo-box-confident");
            } else if (result.confidence < UNCERTAIN_THRESHOLD) {
                categoryCombo.getStyleClass().add("combo-box-uncertain");
            }
            // --- END OF NEW LOGIC ---

        } else {
            showAlert(Alert.AlertType.WARNING, "Model Not Found", "The category suggestion model is not loaded.");
        }
    }

    private void clearConfidenceStyles() {
        categoryCombo.getStyleClass().removeAll("combo-box-confident", "combo-box-uncertain");
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) descField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}