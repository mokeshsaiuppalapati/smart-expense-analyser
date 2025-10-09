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

import java.util.List;
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

    public void initData(ExpenseService service) {
        this.service = service;
        loadBudgets();
    }

    @FXML
    public void initialize() {
        setupTableColumns();
    }

    private void setupTableColumns() {
        categoryCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCategory()));

        // Make the Limit column editable
        limitCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        limitCol.setOnEditCommit(event -> {
            Budget budget = event.getRowValue();
            budget.setMonthlyLimit(event.getNewValue());
            // If the budget is already saved in the DB, update it immediately.
            if (budget.getId() != 0) {
                service.updateBudget(budget);
                loadBudgets(); // Reload to refresh summary and calculations
            }
        });
        limitCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleDoubleProperty(cell.getValue().getMonthlyLimit()).asObject());

        spentCol.setCellValueFactory(cell -> {
            double spent = service.getSpentAmountForCategoryThisMonth(cell.getValue().getCategory());
            return new javafx.beans.property.SimpleDoubleProperty(spent).asObject();
        });

        remainingCol.setCellValueFactory(cell -> {
            double spent = service.getSpentAmountForCategoryThisMonth(cell.getValue().getCategory());
            double remaining = cell.getValue().getMonthlyLimit() - spent;
            return new javafx.beans.property.SimpleDoubleProperty(remaining).asObject();
        });

        progressCol.setCellFactory(param -> new TableCell<>() {
            private final ProgressBar progressBar = new ProgressBar();
            private final Label percentLabel = new Label();
            private final HBox container = new HBox(5, progressBar, percentLabel);

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Budget budget = getTableView().getItems().get(getIndex());
                    double spent = service.getSpentAmountForCategoryThisMonth(budget.getCategory());
                    double limit = budget.getMonthlyLimit();
                    double progress = (limit > 0) ? spent / limit : 0.0;

                    progressBar.setProgress(progress);
                    percentLabel.setText(String.format("%.0f%%", progress * 100));

                    if (progress > 1.0) {
                        progressBar.setStyle("-fx-accent: red;");
                    } else {
                        progressBar.setStyle("-fx-accent: #007bff;");
                    }
                    setGraphic(container);
                }
            }
        });

        Callback<TableColumn<Budget, Void>, TableCell<Budget, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btn = new Button("Delete");
            {
                btn.setOnAction(event -> {
                    Budget budget = getTableView().getItems().get(getIndex());
                    onDeleteBudget(budget);
                });
            }
            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                    setAlignment(Pos.CENTER);
                }
            }
        };
        actionsCol.setCellFactory(cellFactory);
    }

    private void loadBudgets() {
        budgetList.setAll(service.getAllBudgets());
        budgetTable.setItems(budgetList);
        updateSummary();
    }

    private void updateSummary() {
        double totalBudget = 0;
        double totalSpent = 0;

        for (Budget budget : budgetList) {
            totalBudget += budget.getMonthlyLimit();
            totalSpent += service.getSpentAmountForCategoryThisMonth(budget.getCategory());
        }

        double totalRemaining = totalBudget - totalSpent;

        lblTotalBudget.setText(String.format("₹%.2f", totalBudget));
        lblTotalSpent.setText(String.format("₹%.2f", totalSpent));
        lblTotalRemaining.setText(String.format("₹%.2f", totalRemaining));

        if (totalRemaining < 0) {
            lblTotalRemaining.setTextFill(Color.RED);
        } else {
            lblTotalRemaining.setTextFill(Color.GREEN);
        }
    }

    @FXML
    public void onSuggestBudgets() {
        List<Budget> suggestions = service.getBudgetSuggestions();
        // Get categories that already have a budget
        List<String> existingCategories = budgetList.stream().map(Budget::getCategory).collect(Collectors.toList());
        // Filter out suggestions for categories that already exist
        suggestions.removeIf(suggestion -> existingCategories.contains(suggestion.getCategory()));

        if (suggestions.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No New Suggestions", "Budget suggestions are already up to date with your spending categories.");
        } else {
            budgetList.addAll(suggestions);
        }
    }

    @FXML
    public void onAddBudget() {
        Budget selected = budgetTable.getSelectionModel().getSelectedItem();

        if (selected != null) {
            // If a row is selected, save that one (it might be a new suggestion)
            service.addBudget(selected);
        } else {
            // Otherwise, use the text fields as before
            String cat = categoryField.getText().trim();
            String limitText = limitField.getText().trim();
            if (cat.isEmpty() || limitText.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Error", "Category and limit are required.");
                return;
            }
            double limit;
            try {
                limit = Double.parseDouble(limitText);
                if (limit <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Please enter a valid, positive limit.");
                return;
            }
            service.addBudget(new Budget(cat, limit));
        }

        loadBudgets();
        clearForm();
    }

    private void onDeleteBudget(Budget budgetToDelete) {
        if (budgetToDelete.getId() == 0) {
            // If it's a suggestion (not in DB), just remove it from the list
            budgetList.remove(budgetToDelete);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Budget for '" + budgetToDelete.getCategory() + "'?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            service.deleteBudget(budgetToDelete.getId());
            loadBudgets();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void clearForm() {
        categoryField.clear();
        limitField.clear();
    }
}