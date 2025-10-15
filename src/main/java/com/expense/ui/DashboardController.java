// File: src/main/java/com/expense/ui/DashboardController.java

package com.expense.ui;

import com.expense.model.Budget;
import com.expense.model.Transaction;
import com.expense.service.ExpenseService;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public class DashboardController {

    @FXML private Label lblCurrentMonthSpending;
    @FXML private Label lblTopCategory;
    @FXML private ProgressBar pbBudgetStatus;
    @FXML private Label lblBudgetStatus;
    @FXML private ListView<String> lvRecentTransactions;

    private ExpenseService service;
    private final ObservableList<String> recentTransactionsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        lvRecentTransactions.setItems(recentTransactionsList);
    }

    public void setService(ExpenseService service) {
        this.service = service;
    }

    public void refreshData() {
        if (service == null) return;

        try {
            // 1. Get current month's spending
            double currentMonthSpending = service.getTotalForMonth(YearMonth.now());
            animateNumberLabel(lblCurrentMonthSpending, currentMonthSpending);

            // 2. Get top spending category
            Map<String, Double> categoryTotals = service.getCategoryTotalsForMonth(YearMonth.now());
            String topCategory = categoryTotals.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("N/A");
            lblTopCategory.setText(topCategory);

            // 3. Update overall budget status
            List<Budget> budgets = service.getAllBudgets();
            double totalBudget = budgets.stream().mapToDouble(Budget::getMonthlyLimit).sum();
            double totalSpent = categoryTotals.values().stream().mapToDouble(Double::doubleValue).sum();
            double progress = (totalBudget > 0) ? totalSpent / totalBudget : 0.0;
            animateProgressBar(pbBudgetStatus, progress);
            lblBudgetStatus.setText(String.format("₹%.2f / ₹%.2f", totalSpent, totalBudget));
            if (totalSpent > totalBudget) {
                pbBudgetStatus.setStyle("-fx-accent: red;");
            } else {
                pbBudgetStatus.setStyle("-fx-accent: #0d6efd;");
            }

            // 4. Update recent transactions list
            List<Transaction> recent = service.getRecentTransactions(5);
            recentTransactionsList.clear();
            recent.forEach(t ->
                    recentTransactionsList.add(String.format("%s: %s (₹%.2f)", t.getCategory(), t.getDescription(), t.getAmount()))
            );
        } catch (Exception e) {
            // If any database operation fails, show an error and clear the dashboard
            lblCurrentMonthSpending.setText("Error");
            lblTopCategory.setText("Error");
            recentTransactionsList.setAll("Could not load data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Animation Helper Methods (No Changes) ---
    private void animateNumberLabel(Label label, double endValue) {
        try {
            String currentText = label.getText().replaceAll("[^\\d.]", "");
            double startValue = currentText.isEmpty() ? 0.0 : Double.parseDouble(currentText);
            DoubleProperty value = new SimpleDoubleProperty(startValue);
            value.addListener((obs, oldVal, newVal) -> label.setText(String.format("₹%.2f", newVal.doubleValue())));
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(0.7), new KeyValue(value, endValue)));
            timeline.play();
        } catch (NumberFormatException e) {
            label.setText(String.format("₹%.2f", endValue));
        }
    }

    private void animateProgressBar(ProgressBar progressBar, double endValue) {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(0.7), new KeyValue(progressBar.progressProperty(), endValue)));
        timeline.play();
    }
}