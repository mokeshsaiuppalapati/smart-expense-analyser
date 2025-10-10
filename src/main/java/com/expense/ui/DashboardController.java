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
import javafx.collections.ObservableList; // ADDED: This import was missing
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        if (service == null) {
            return;
        }

        // 1. Get current month's spending (Animated)
        String currentMonthStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        double currentMonthSpending = service.getMonthlyTotals().getOrDefault(currentMonthStr, 0.0);
        animateNumberLabel(lblCurrentMonthSpending, currentMonthSpending);

        // 2. Get top spending category
        Map<String, Double> categoryTotals = service.getCategoryTotalsForMonth(LocalDate.now().getYear(), LocalDate.now().getMonthValue());
        String topCategory = categoryTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        lblTopCategory.setText(topCategory);

        // 3. Update overall budget status (Animated)
        List<Budget> budgets = service.getAllBudgets();
        double totalBudget = budgets.stream().mapToDouble(Budget::getMonthlyLimit).sum();
        double totalSpent = 0;
        for (Budget budget : budgets) {
            totalSpent += service.getSpentAmountForCategoryThisMonth(budget.getCategory());
        }

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
    }

    private void animateNumberLabel(Label label, double endValue) {
        try {
            String currentText = label.getText().replaceAll("[^\\d.]", "");
            double startValue = Double.parseDouble(currentText);

            DoubleProperty value = new SimpleDoubleProperty(startValue);
            value.addListener((obs, oldVal, newVal) -> {
                label.setText(String.format("₹%.2f", newVal.doubleValue()));
            });

            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.seconds(1), new KeyValue(value, endValue))
            );
            timeline.play();
        } catch (NumberFormatException e) {
            label.setText(String.format("₹%.2f", endValue));
        }
    }

    private void animateProgressBar(ProgressBar progressBar, double endValue) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), new KeyValue(progressBar.progressProperty(), endValue))
        );
        timeline.play();
    }
}