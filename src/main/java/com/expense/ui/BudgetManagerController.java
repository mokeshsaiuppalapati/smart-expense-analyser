// File: src/main/java/com/expense/ui/BudgetManagerController.java

package com.expense.ui;

import com.expense.model.Budget;
import com.expense.service.ExpenseService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.converter.DoubleStringConverter;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class BudgetManagerController {

    @FXML private TableView<Budget> budgetTable;
    @FXML private TableColumn<Budget, String> categoryCol;
    @FXML private TableColumn<Budget, Double> limitCol;
    @FXML private TableColumn<Budget, Double> spentCol;
    @FXML private TableColumn<Budget, Double> remainingCol;
    @FXML private TableColumn<Budget, Void> progressCol;
    @FXML private TableColumn<Budget, Void> actionsCol;
    @FXML private TextField categoryField;
    @FXML private TextField limitField;
    @FXML private Label lblTotalBudget;
    @FXML private Label lblTotalSpent;
    @FXML private Label lblTotalRemaining;

    private ExpenseService service;
    private final ObservableList<Budget> budgetList = FXCollections.observableArrayList();
    private final Map<String, Double> spendingCache = new HashMap<>();

    public void initData(ExpenseService service) {
        this.service = service;
        loadBudgets();
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        budgetTable.setItems(budgetList);
    }

    private void setupTableColumns() {
        categoryCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCategory()));
        limitCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleDoubleProperty(cell.getValue().getMonthlyLimit()).asObject());

        limitCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        limitCol.setOnEditCommit(event -> {
            Budget budget = event.getRowValue();
            budget.setMonthlyLimit(event.getNewValue());
            if (budget.getId() != 0) {
                try {
                    service.updateBudget(budget);
                    loadBudgets();
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Update Failed", "Could not update budget: " + e.getMessage());
                    loadBudgets();
                }
            }
        });

        spentCol.setCellValueFactory(cell -> {
            double spent = spendingCache.getOrDefault(cell.getValue().getCategory(), 0.0);
            return new javafx.beans.property.SimpleDoubleProperty(spent).asObject();
        });

        remainingCol.setCellValueFactory(cell -> {
            double spent = spendingCache.getOrDefault(cell.getValue().getCategory(), 0.0);
            double remaining = cell.getValue().getMonthlyLimit() - spent;
            return new javafx.beans.property.SimpleDoubleProperty(remaining).asObject();
        });

        progressCol.setCellFactory(param -> new TableCell<>() {
            private final ProgressBar progressBar = new ProgressBar();
            private final Label percentLabel = new Label();
            private final HBox container = new HBox(5, progressBar, percentLabel);
            { container.setAlignment(Pos.CENTER_LEFT); }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Budget budget = getTableView().getItems().get(getIndex());
                    double spent = spendingCache.getOrDefault(budget.getCategory(), 0.0);
                    double limit = budget.getMonthlyLimit();
                    double progress = (limit > 0) ? spent / limit : 0.0;
                    progressBar.setProgress(progress);
                    percentLabel.setText(String.format("%.0f%%", progress * 100));
                    if (progress > 1.0) {
                        progressBar.setStyle("-fx-accent: #CF6679;");
                    } else if (progress > 0.85) {
                        progressBar.setStyle("-fx-accent: #FBB13C;");
                    } else {
                        progressBar.setStyle("-fx-accent: #03DAC6;");
                    }
                    setGraphic(container);
                }
            }
        });

        Callback<TableColumn<Budget, Void>, TableCell<Budget, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btn = new Button("Delete");
            {
                btn.getStyleClass().add("delete-button");
                btn.setOnAction(event -> {
                    Budget budget = getTableView().getItems().get(getIndex());
                    onDeleteBudget(budget);
                });
            }
            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                setAlignment(Pos.CENTER);
            }
        };
        actionsCol.setCellFactory(cellFactory);
    }

    private void loadBudgets() {
        try {
            spendingCache.clear();
            List<Budget> budgets = service.getAllBudgets();
            Map<String, Double> currentMonthSpending = service.getCategoryTotalsForMonth(YearMonth.now());
            spendingCache.putAll(currentMonthSpending);
            budgetList.setAll(budgets);
            updateSummary();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load budget data: " + e.getMessage());
        }
    }

    private void updateSummary() {
        double totalBudget = budgetList.stream().mapToDouble(Budget::getMonthlyLimit).sum();
        double totalSpent = spendingCache.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalRemaining = totalBudget - totalSpent;

        lblTotalBudget.setText(String.format("₹%.2f", totalBudget));
        lblTotalSpent.setText(String.format("₹%.2f", totalSpent));
        lblTotalRemaining.setText(String.format("₹%.2f", totalRemaining));
        lblTotalRemaining.setTextFill(totalRemaining < 0 ? Color.RED : Color.GREEN);
    }

    @FXML
    public void onSuggestBudgets() {
        try {
            List<Budget> suggestions = service.getBudgetSuggestions();
            List<String> existingCategories = budgetList.stream().map(Budget::getCategory).collect(Collectors.toList());
            suggestions.removeIf(suggestion -> existingCategories.contains(suggestion.getCategory()));

            if (suggestions.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "No New Suggestions", "All your spending categories already have a budget.");
            } else {
                budgetList.addAll(suggestions);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not generate budget suggestions: " + e.getMessage());
        }
    }

    // --- THIS METHOD IS UPGRADED ---
    @FXML
    public void onAddBudget() {
        Budget selected = budgetTable.getSelectionModel().getSelectedItem();

        // If a new suggestion is selected in the table, save that one directly.
        if (selected != null && selected.getId() == 0) {
            try {
                service.addBudget(selected);
                loadBudgets();
                clearForm();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Save Failed", "Could not save the selected budget: " + e.getMessage());
            }
            return;
        }

        // Otherwise, fall back to using the text fields as before.
        String cat = categoryField.getText().trim();
        String limitText = limitField.getText().trim();

        if (cat.isEmpty() || limitText.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Category and limit are required.");
            return;
        }

        try {
            double limit = Double.parseDouble(limitText);
            if (limit <= 0) throw new NumberFormatException();

            service.addBudget(new Budget(cat, limit));
            loadBudgets();
            clearForm();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter a valid, positive number for the limit.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not save the budget: " + e.getMessage());
        }
    }

    // --- THIS METHOD IS UPGRADED ---
    private void onDeleteBudget(Budget budgetToDelete) {
        // If it's a new suggestion (not in DB), just remove it from the list without confirmation.
        if (budgetToDelete.getId() == 0) {
            budgetList.remove(budgetToDelete);
            return;
        }

        // If it's an existing budget, show confirmation before deleting from the database.
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Budget for '" + budgetToDelete.getCategory() + "'?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                service.deleteBudget(budgetToDelete.getId());
                loadBudgets();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Could not delete budget: " + e.getMessage());
            }
        }
    }

    private void clearForm() {
        categoryField.clear();
        limitField.clear();
        budgetTable.getSelectionModel().clearSelection();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}