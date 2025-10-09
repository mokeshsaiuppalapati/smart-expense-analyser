package com.expense.ui;

import com.expense.model.Budget;
import com.expense.model.Transaction;
import com.expense.service.ExpenseService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private Label lblCurrentMonthSpending;
    @FXML private Label lblTopCategory;
    @FXML private ProgressBar pbBudgetStatus;
    @FXML private Label lblBudgetStatus;
    @FXML private ListView<String> lvRecentTransactions;

    private ExpenseService service;

    public void setService(ExpenseService service) {
        this.service = service;
    }

    public void refreshData() {
        if (service == null) {
            return;
        }

        String currentMonthStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        double currentMonthSpending = service.getMonthlyTotals().getOrDefault(currentMonthStr, 0.0);
        lblCurrentMonthSpending.setText(String.format("₹%.2f", currentMonthSpending));

        Map<String, Double> categoryTotals = service.getCategoryTotalsForMonth(LocalDate.now().getYear(), LocalDate.now().getMonthValue());
        String topCategory = categoryTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        lblTopCategory.setText(topCategory);

        List<Budget> budgets = service.getAllBudgets();
        double totalBudget = budgets.stream().mapToDouble(Budget::getMonthlyLimit).sum();
        double totalSpent = 0;
        for (Budget budget : budgets) {
            totalSpent += service.getSpentAmountForCategoryThisMonth(budget.getCategory());
        }

        if (totalBudget > 0) {
            pbBudgetStatus.setProgress(totalSpent / totalBudget);
        } else {
            pbBudgetStatus.setProgress(0.0);
        }
        lblBudgetStatus.setText(String.format("₹%.2f / ₹%.2f", totalSpent, totalBudget));
        if (totalSpent > totalBudget) {
            pbBudgetStatus.setStyle("-fx-accent: red;");
        } else {
            pbBudgetStatus.setStyle("-fx-accent: #007bff;");
        }

        List<Transaction> recent = service.getRecentTransactions(5);

        List<String> recentAsString = recent.stream()
                .map(t -> String.format("%s: %s (₹%.2f)", t.getCategory(), t.getDescription(), t.getAmount()))
                .collect(Collectors.toList());

        lvRecentTransactions.setItems(FXCollections.observableArrayList(recentAsString));
    }
}