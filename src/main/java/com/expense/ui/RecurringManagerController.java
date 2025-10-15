// File: src/main/java/com/expense/ui/RecurringManagerController.java

package com.expense.ui;

import com.expense.model.RecurringTransaction;
import com.expense.service.ExpenseService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class RecurringManagerController {

    @FXML private TableView<RecurringTransaction> recurringTable;
    @FXML private TableColumn<RecurringTransaction, String> colDescription;
    @FXML private TableColumn<RecurringTransaction, Double> colAmount;
    @FXML private TableColumn<RecurringTransaction, String> colCategory;
    @FXML private TableColumn<RecurringTransaction, RecurringTransaction.Frequency> colFrequency;
    @FXML private TableColumn<RecurringTransaction, LocalDate> colNextDate;
    @FXML private TableColumn<RecurringTransaction, Void> colActions;
    @FXML private TextField descField;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<RecurringTransaction.Frequency> frequencyCombo;
    @FXML private DatePicker datePicker;

    private ExpenseService service;
    private final ObservableList<RecurringTransaction> recurringList = FXCollections.observableArrayList();

    /**
     * Initializes the controller with data from the main application.
     * @param service The main expense service.
     * @param categories A list of existing categories to populate the dropdown.
     */
    public void initData(ExpenseService service, List<String> categories) {
        this.service = service;
        categoryCombo.setItems(FXCollections.observableArrayList(categories));
        loadRecurringTransactions();
    }

    @FXML
    public void initialize() {
        // Populate the frequency dropdown with our enum values
        frequencyCombo.setItems(FXCollections.observableArrayList(RecurringTransaction.Frequency.values()));
        setupTableColumns();
        recurringTable.setItems(recurringList);
    }

    private void setupTableColumns() {
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colFrequency.setCellValueFactory(new PropertyValueFactory<>("frequency"));
        colNextDate.setCellValueFactory(new PropertyValueFactory<>("nextDueDate"));

        // Create a custom cell factory for the "Actions" column to add a delete button
        Callback<TableColumn<RecurringTransaction, Void>, TableCell<RecurringTransaction, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btn = new Button("Delete");
            {
                btn.setOnAction(event -> {
                    RecurringTransaction transaction = getTableView().getItems().get(getIndex());
                    handleDelete(transaction);
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
        colActions.setCellFactory(cellFactory);
    }

    /**
     * Loads all recurring transactions from the database and refreshes the table.
     */
    private void loadRecurringTransactions() {
        try {
            List<RecurringTransaction> items = service.getAllRecurringTransactions();
            recurringList.setAll(items);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Load Failed", "Could not load recurring transactions: " + e.getMessage());
        }
    }

    @FXML
    private void onAdd() {
        // 1. Get and Validate User Input
        String description = descField.getText().trim();
        String amountStr = amountField.getText().trim();
        String category = categoryCombo.getEditor().getText().trim();
        RecurringTransaction.Frequency frequency = frequencyCombo.getValue();
        LocalDate nextDueDate = datePicker.getValue();

        if (description.isEmpty() || amountStr.isEmpty() || category.isEmpty() || frequency == null || nextDueDate == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "All fields are required.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Amount must be a positive number.");
                return;
            }

            // 2. Create a new object and save it via the service
            RecurringTransaction newTransaction = new RecurringTransaction(description, amount, category, frequency, nextDueDate);
            service.addRecurringTransaction(newTransaction);

            // 3. Refresh the UI
            loadRecurringTransactions();
            clearForm();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter a valid number for the amount.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Save Failed", "Could not save the recurring transaction: " + e.getMessage());
        }
    }

    private void handleDelete(RecurringTransaction transaction) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete '" + transaction.getDescription() + "'?");
        confirm.setContentText("This will stop the automatic addition of this expense. Are you sure?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                service.deleteRecurringTransaction(transaction.getId());
                loadRecurringTransactions(); // Refresh the table
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Delete Failed", "Could not delete the recurring transaction: " + e.getMessage());
            }
        }
    }

    private void clearForm() {
        descField.clear();
        amountField.clear();
        categoryCombo.getEditor().clear();
        frequencyCombo.getSelectionModel().clearSelection();
        datePicker.setValue(null);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}