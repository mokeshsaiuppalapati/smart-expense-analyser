// File: src/main/java/com/expense/ui/SplitTransactionController.java

package com.expense.ui;

import com.expense.service.ExpenseService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplitTransactionController {

    // --- THE FIX: Added @FXML to all UI components ---
    @FXML private Label totalAmountLabel;
    @FXML private VBox splitBox;
    @FXML private Label remainingAmountLabel;
    @FXML private Button saveButton;

    private ExpenseService service;
    private double totalAmount;
    private LocalDate date;
    private String description;

    private boolean splitSuccessful = false;
    private final Map<String, TextField> categoryFields = new HashMap<>();

    public void initData(ExpenseService service, double totalAmount, LocalDate date, String description, List<String> suggestedCategories) {
        this.service = service;
        this.totalAmount = totalAmount;
        this.date = date;
        this.description = description;

        totalAmountLabel.setText(String.format("₹%.2f", totalAmount));
        updateRemainingAmount();

        for (String category : suggestedCategories) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            Label categoryLabel = new Label(category);
            categoryLabel.setPrefWidth(120);
            TextField amountField = new TextField();
            amountField.setPromptText("Enter amount");

            amountField.textProperty().addListener((obs, oldVal, newVal) -> updateRemainingAmount());

            row.getChildren().addAll(categoryLabel, amountField);
            splitBox.getChildren().add(row);
            categoryFields.put(category, amountField);
        }
    }

    private void updateRemainingAmount() {
        double allocatedAmount = 0;
        for (TextField field : categoryFields.values()) {
            try {
                if (!field.getText().trim().isEmpty()) {
                    allocatedAmount += Double.parseDouble(field.getText().trim());
                }
            } catch (NumberFormatException e) {
                // Ignore invalid numbers during typing
            }
        }
        double remaining = totalAmount - allocatedAmount;
        remainingAmountLabel.setText(String.format("₹%.2f", remaining));

        // This line will now work correctly because saveButton is no longer null
        saveButton.setDisable(Math.abs(remaining) > 0.001);
    }

    @FXML
    private void onSaveSplit() {
        List<SplitItem> splits = new ArrayList<>();
        double totalSplitAmount = 0;

        for (Map.Entry<String, TextField> entry : categoryFields.entrySet()) {
            String category = entry.getKey();
            String amountStr = entry.getValue().getText().trim();
            if (amountStr.isEmpty()) continue;

            try {
                double amount = Double.parseDouble(amountStr);
                if (amount > 0) {
                    splits.add(new SplitItem(category, amount));
                    totalSplitAmount += amount;
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers for all amounts.");
                return;
            }
        }

        if (Math.abs(totalSplitAmount - totalAmount) > 0.001) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "The split amounts do not add up to the total.");
            return;
        }

        try {
            for (int i = 0; i < splits.size(); i++) {
                SplitItem split = splits.get(i);
                String splitDescription = String.format("%s (Split %d/%d)", description, i + 1, splits.size());
                service.addTransaction(date, split.amount, splitDescription, split.category);
            }
            splitSuccessful = true;
            closeWindow();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Save Failed", "Could not save split transactions: " + e.getMessage());
        }
    }

    public boolean isSplitSuccessful() {
        return splitSuccessful;
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private static class SplitItem {
        final String category;
        final double amount;
        SplitItem(String category, double amount) { this.category = category; this.amount = amount; }
    }
}