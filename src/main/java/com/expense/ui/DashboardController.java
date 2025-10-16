// File: src/main/java/com/expense/ui/DashboardController.java

package com.expense.ui;

import com.expense.model.Budget;
import com.expense.model.Transaction;
import com.expense.service.ExpenseService;
import com.expense.util.CategoryIconManager;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private Label lblCurrentMonthSpending;
    @FXML private Label lblTopCategory;
    @FXML private TableView<Transaction> recentTransactionsTable;
    @FXML private TableColumn<Transaction, Void> colIcon;
    @FXML private TableColumn<Transaction, String> colDesc;
    @FXML private TableColumn<Transaction, Double> colAmount;
    @FXML private VBox budgetsContainer;

    private ExpenseService service;
    private final ObservableList<Transaction> recentTransactionsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupRecentTransactionsTable();
    }

    public void setService(ExpenseService service) {
        this.service = service;
    }

    private void setupRecentTransactionsTable() {
        recentTransactionsTable.setItems(recentTransactionsList);
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));

        // Custom cell for the amount to format it as currency
        colAmount.setCellFactory(tc -> new TableCell<Transaction, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(String.format("₹%.2f", amount));
                }
            }
        });

        // Custom cell for the icon
        colIcon.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Transaction transaction = getTableView().getItems().get(getIndex());
                    FontIcon icon = CategoryIconManager.getIcon(transaction.getCategory());
                    icon.getStyleClass().add("glyph-icon");
                    setGraphic(icon);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    public void refreshData() {
        if (service == null) return;

        try {
            // Refresh main widgets
            double currentMonthSpending = service.getTotalForMonth(YearMonth.now());
            animateNumberLabel(lblCurrentMonthSpending, currentMonthSpending);

            Map<String, Double> categoryTotals = service.getCategoryTotalsForMonth(YearMonth.now());
            String topCategory = categoryTotals.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse("N/A");
            lblTopCategory.setText(topCategory);

            // Refresh recent transactions table
            List<Transaction> recent = service.getRecentTransactions(5);
            recentTransactionsList.setAll(recent);

            // Refresh top budgets widget
            refreshBudgetsWidget(categoryTotals);

        } catch (Exception e) {
            lblCurrentMonthSpending.setText("Error");
            lblTopCategory.setText("Error");
            e.printStackTrace();
        }
    }

    private void refreshBudgetsWidget(Map<String, Double> currentMonthSpending) throws Exception {
        budgetsContainer.getChildren().clear();
        List<Budget> budgets = service.getAllBudgets();

        // Calculate progress for each budget
        List<BudgetProgress> budgetProgressList = budgets.stream().map(budget -> {
            double spent = currentMonthSpending.getOrDefault(budget.getCategory(), 0.0);
            return new BudgetProgress(budget.getCategory(), spent, budget.getMonthlyLimit());
        }).collect(Collectors.toList());

        // Sort by who is closest to their limit (highest percentage) and take the top 3
        budgetProgressList.sort(Comparator.comparingDouble(BudgetProgress::getPercent).reversed());
        List<BudgetProgress> topBudgets = budgetProgressList.stream().limit(3).collect(Collectors.toList());

        if (topBudgets.isEmpty()) {
            budgetsContainer.getChildren().add(new Label("No budgets set."));
            return;
        }

        for (BudgetProgress bp : topBudgets) {
            VBox budgetNode = new VBox(5);
            Label nameLabel = new Label(bp.getCategory());
            ProgressBar progressBar = new ProgressBar(bp.getPercent() > 1.0 ? 1.0 : bp.getPercent());
            progressBar.setMaxWidth(Double.MAX_VALUE);
            Label amountLabel = new Label(String.format("₹%.0f / ₹%.0f", bp.getSpent(), bp.getLimit()));

            // Style progress bar based on how close it is to the limit
            if (bp.getPercent() >= 1.0) {
                progressBar.setStyle("-fx-accent: #dc3545;"); // Red
            } else if (bp.getPercent() > 0.8) {
                progressBar.setStyle("-fx-accent: #fd7e14;"); // Orange
            } else {
                progressBar.setStyle("-fx-accent: -fx-accent-color;"); // Use theme accent color
            }

            budgetNode.getChildren().addAll(nameLabel, progressBar, amountLabel);
            budgetsContainer.getChildren().add(budgetNode);
        }
    }

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

    // Helper DTO for sorting budgets
    private static class BudgetProgress {
        private final String category;
        private final double spent;
        private final double limit;
        private final double percent;

        BudgetProgress(String category, double spent, double limit) {
            this.category = category;
            this.spent = spent;
            this.limit = limit;
            this.percent = (limit > 0) ? spent / limit : 0.0;
        }
        public String getCategory() { return category; }
        public double getSpent() { return spent; }
        public double getLimit() { return limit; }
        public double getPercent() { return percent; }
    }
}