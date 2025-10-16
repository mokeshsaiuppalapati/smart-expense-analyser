// File: src/main/java/com/expense/ui/MainController.java

package com.expense.ui;

import com.expense.model.Budget;
import com.expense.model.Transaction;
import com.expense.model.TransactionData;
import com.expense.ml.ExpensePredictor;
import com.expense.ml.WekaPredictor;
import com.expense.ml.WekaTrainer;
import com.expense.service.ExpenseService;
import com.expense.util.DataExporter;
import com.expense.util.DataImporter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.LocalDateStringConverter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainController {

    @FXML private TableView<Transaction> table;
    @FXML private TableColumn<Transaction, LocalDate> colDate;
    @FXML private TableColumn<Transaction, Double> colAmount;
    @FXML private TableColumn<Transaction, String> colDesc;
    @FXML private TableColumn<Transaction, String> colCat;
    @FXML private PieChart pieChart;
    @FXML private BarChart<String, Number> barChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private ComboBox<Integer> barYearCombo;
    @FXML private ComboBox<Integer> pieYearCombo;
    @FXML private ComboBox<Month> pieMonthCombo;
    @FXML private ProgressIndicator prog;
    @FXML private Button btnRetrain;
    @FXML private TextArea predictionResultsArea;
    @FXML private TabPane mainTabPane;
    @FXML private DashboardController dashboardController;
    @FXML private GoalsController goalsController;

    private ExpenseService service;
    private WekaPredictor categorizer;
    private ExpensePredictor expensePredictor;
    private final ObservableList<Transaction> transactionList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        this.service = new ExpenseService();
        this.categorizer = new WekaPredictor();
        this.expensePredictor = new ExpensePredictor();

        // Configure the x-axis with all month names from the start
        List<String> monthNames = Arrays.stream(Month.values())
                .map(m -> m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                .collect(Collectors.toList());
        xAxis.setCategories(FXCollections.observableArrayList(monthNames));

        try {
            service.init();
            int itemsProcessed = service.processRecurringTransactions();
            if (itemsProcessed > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Transactions Auto-Added",
                        itemsProcessed + " recurring transaction(s) were automatically added to your log.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Fatal Error", "Could not initialize application: " + e.getMessage());
            e.printStackTrace();
            Platform.exit();
        }

        dashboardController.setService(service);
        goalsController.setService(service);

        setupTable();
        refreshData();
    }

    // ==========================================================================
    // --- THIS IS THE DEFINITIVE FIX FOR THE BAR CHART ---
    // ==========================================================================
    private void onRefreshBar() {
        Integer selectedYear = barYearCombo.getValue();
        if (selectedYear == null) {
            barChart.getData().clear();
            return;
        }

        try {
            int previousYear = selectedYear - 1;

            // 1. Fetch data for both years
            Map<String, Double> currentYearTotals = service.getMonthlyTotalsForYear(selectedYear);
            Map<String, Double> previousYearTotals = service.getMonthlyTotalsForYear(previousYear);

            // 2. Create a data series for each year
            XYChart.Series<String, Number> currentYearSeries = new XYChart.Series<>();
            currentYearSeries.setName(String.valueOf(selectedYear));

            XYChart.Series<String, Number> previousYearSeries = new XYChart.Series<>();
            previousYearSeries.setName(String.valueOf(previousYear));

            // 3. Find the overall maximum amount for scaling the Y-axis
            double maxAmount = 0.0;
            maxAmount = Math.max(currentYearTotals.values().stream().mapToDouble(d -> d).max().orElse(0.0), maxAmount);
            maxAmount = Math.max(previousYearTotals.values().stream().mapToDouble(d -> d).max().orElse(0.0), maxAmount);
            yAxis.setAutoRanging(false);
            yAxis.setUpperBound(maxAmount == 0 ? 100 : maxAmount * 1.1);

            // 4. Populate both series with data for all 12 months
            for (Month month : Month.values()) {
                String shortMonthName = month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

                // Current Year Data
                String currentYearKey = String.format("%d-%02d", selectedYear, month.getValue());
                currentYearSeries.getData().add(new XYChart.Data<>(shortMonthName, currentYearTotals.getOrDefault(currentYearKey, 0.0)));

                // Previous Year Data
                String previousYearKey = String.format("%d-%02d", previousYear, month.getValue());
                previousYearSeries.getData().add(new XYChart.Data<>(shortMonthName, previousYearTotals.getOrDefault(previousYearKey, 0.0)));
            }

            // 5. Display the data
            barChart.setAnimated(false);
            barChart.getData().setAll(currentYearSeries, previousYearSeries);
            barChart.setTitle("Monthly Comparison: " + previousYear + " vs " + selectedYear);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Chart Error", "Could not load data for Bar Chart: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- All other methods remain correct ---

    private void setupTable() {
        table.setEditable(true);
        colDate.setCellValueFactory(cell -> cell.getValue().dateProperty());
        colDate.setCellFactory(TextFieldTableCell.forTableColumn(new LocalDateStringConverter()));
        colDate.setOnEditCommit(event -> {
            Transaction transaction = event.getRowValue();
            transaction.setDate(event.getNewValue());
            updateTransactionAndRefresh(transaction);
        });
        colAmount.setCellValueFactory(cell -> cell.getValue().amountProperty().asObject());
        colAmount.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colAmount.setOnEditCommit(event -> {
            Transaction transaction = event.getRowValue();
            transaction.setAmount(event.getNewValue());
            updateTransactionAndRefresh(transaction);
        });
        colDesc.setCellValueFactory(cell -> cell.getValue().descriptionProperty());
        colDesc.setCellFactory(TextFieldTableCell.forTableColumn());
        colDesc.setOnEditCommit(event -> {
            Transaction transaction = event.getRowValue();
            transaction.setDescription(event.getNewValue());
            updateTransactionAndRefresh(transaction);
        });
        colCat.setCellValueFactory(cell -> cell.getValue().categoryProperty());
        colCat.setCellFactory(TextFieldTableCell.forTableColumn());
        colCat.setOnEditCommit(event -> {
            Transaction transaction = event.getRowValue();
            String oldCategory = transaction.getCategory();
            String newCategory = event.getNewValue();
            if (newCategory != null && !newCategory.equalsIgnoreCase(oldCategory)) {
                transaction.setCategory(newCategory);
                updateTransactionAndRefresh(transaction);
                service.logCorrection(transaction.getDescription(), newCategory);
                onRetrainModel();
                System.out.println("Correction logged and auto-retraining started for: '" + transaction.getDescription() + "' -> '" + newCategory + "'");
            }
        });
        table.setItems(transactionList);
    }
    public void refreshData() {
        try {
            transactionList.setAll(service.getAll());
            populateSelectors();
            Platform.runLater(() -> {
                dashboardController.refreshData();
                goalsController.refreshGoals();
            });
            onRefreshPie();
            onRefreshBar();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load data: " + e.getMessage());
        }
    }
    private void updateTransactionAndRefresh(Transaction t) {
        try {
            service.updateTransaction(t);
            refreshData();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Update Failed", "Could not update transaction: " + e.getMessage());
            refreshData();
        }
    }
    private void populateSelectors() {
        Integer selectedBarYear = barYearCombo.getValue();
        Integer selectedPieYear = pieYearCombo.getValue();
        ObservableList<Integer> years = transactionList.stream()
                .map(t -> t.getDate().getYear())
                .distinct().sorted()
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        barYearCombo.setItems(years);
        if (selectedBarYear != null && years.contains(selectedBarYear)) {
            barYearCombo.setValue(selectedBarYear);
        } else if (!years.isEmpty()) {
            barYearCombo.setValue(LocalDate.now().getYear());
        }
        pieYearCombo.setItems(years);
        pieMonthCombo.setItems(FXCollections.observableArrayList(Month.values()));
        if (selectedPieYear != null && years.contains(selectedPieYear)) {
            pieYearCombo.setValue(selectedPieYear);
        } else if (!years.isEmpty()) {
            pieYearCombo.setValue(LocalDate.now().getYear());
        }
        if (pieMonthCombo.getValue() == null) {
            pieMonthCombo.setValue(LocalDate.now().getMonth());
        }
    }
    @FXML private void onAddExpense() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_expense.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add New Expense");
            stage.setScene(new Scene(loader.load()));
            AddExpenseController controller = loader.getController();
            List<String> allCategories = service.getAllCategories();
            controller.initData(service, this, categorizer, allCategories);
            stage.showAndWait();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open Add Expense window: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML private void onOpenBudgets() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/budget_manager.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Budget Manager");
            stage.setScene(new Scene(loader.load()));
            BudgetManagerController controller = loader.getController();
            controller.initData(service);
            stage.showAndWait();
            refreshData();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open the Budget Manager window.");
            e.printStackTrace();
        }
    }
    @FXML private void onOpenRecurring() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/recurring_manager.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Subscriptions & Recurring Bills");
            stage.setScene(new Scene(loader.load()));
            RecurringManagerController controller = loader.getController();
            List<String> allCategories = service.getAllCategories();
            controller.initData(service, allCategories);
            stage.showAndWait();
            refreshData();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open Recurring Manager: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML private void onDeleteTransaction() {
        Transaction selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a transaction to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete transaction: '" + selected.getDescription() + "'?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                service.deleteTransaction(selected.getId());
                refreshData();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Deletion Failed", "Could not delete transaction: " + e.getMessage());
            }
        }
    }
    @FXML private void onPieChartFilterChanged() { onRefreshPie(); }
    @FXML private void onBarYearSelected() { onRefreshBar(); }
    private void onRefreshPie() {
        Integer year = pieYearCombo.getValue();
        Month month = pieMonthCombo.getValue();
        if (year == null || month == null) return;
        try {
            Map<String, Double> categoryTotals = service.getCategoryTotalsForMonth(YearMonth.of(year, month));
            ObservableList<PieChart.Data> pieChartData = categoryTotals.entrySet().stream()
                    .map(entry -> new PieChart.Data(entry.getKey() + String.format(" (₹%.2f)", entry.getValue()), entry.getValue()))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            if(pieChartData.isEmpty()) {
                pieChartData.add(new PieChart.Data("No Data for this month", 1));
            }
            pieChart.setData(pieChartData);
            pieChart.setTitle("Expenses for " + month.name() + " " + year);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Chart Error", "Could not load data for Pie Chart: " + e.getMessage());
        }
    }
    @FXML private void onExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Transactions to CSV");
        fileChooser.setInitialFileName("expenses_export_" + LocalDate.now() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(mainTabPane.getScene().getWindow());
        if (file != null) {
            try {
                List<Transaction> allTransactions = service.getAll();
                DataExporter.exportTransactions(file.getAbsolutePath(), allTransactions);
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", "Exported " + allTransactions.size() + " transactions.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Export Failed", "Could not export data: " + e.getMessage());
            }
        }
    }
    @FXML private void onImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Transactions from CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(mainTabPane.getScene().getWindow());
        if (file != null) {
            prog.setVisible(true);
            Task<String> importTask = DataImporter.createImportTask(file, service, categorizer);
            importTask.setOnSucceeded(e -> {
                prog.setVisible(false);
                showAlert(Alert.AlertType.INFORMATION, "Import Complete", importTask.getValue());
                refreshData();
            });
            importTask.setOnFailed(e -> {
                prog.setVisible(false);
                showAlert(Alert.AlertType.ERROR, "Import Failed", "An error occurred: " + importTask.getException().getMessage());
            });
            new Thread(importTask).start();
        }
    }
    @FXML private void onRetrainModel() {
        prog.setVisible(true);
        btnRetrain.setDisable(true);
        Task<Void> retrainTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                new WekaTrainer().trainAndSave(
                        "data/transactions_labeled.csv",
                        "data/corrections.csv",
                        "model/classifier.model",
                        "model/filter.model",
                        "model/header.instance"
                );
                return null;
            }
        };
        retrainTask.setOnSucceeded(e -> {
            prog.setVisible(false);
            btnRetrain.setDisable(false);
            categorizer.reloadModel();
            System.out.println("Auto-retraining successful.");
        });
        retrainTask.setOnFailed(e -> {
            prog.setVisible(false);
            btnRetrain.setDisable(false);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to retrain the model: " + retrainTask.getException().getMessage());
        });
        new Thread(retrainTask).start();
    }
    @FXML private void onPredictExpenses() {
        predictionResultsArea.setText("Generating forecast... This may take a moment.");
        prog.setVisible(true);
        Task<String> predictionTask = createPredictionTask();
        predictionTask.setOnSucceeded(e -> {
            predictionResultsArea.setText(predictionTask.getValue());
            prog.setVisible(false);
        });
        predictionTask.setOnFailed(e -> {
            predictionResultsArea.setText("An error occurred during prediction:\n" + predictionTask.getException().getMessage());
            prog.setVisible(false);
        });
        new Thread(predictionTask).start();
    }
    private Task<String> createPredictionTask() {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                updateMessage("Fetching training data...");
                List<TransactionData> trainingData = service.getTransactionDataForRegression();
                if (trainingData.size() < 10) {
                    return "Not enough transaction data to train the prediction model (minimum 10 required).";
                }
                updateMessage("Training regression model...");
                expensePredictor.train(trainingData);
                updateMessage("Analyzing historical data...");
                Map<String, Integer> categoryCodeMap = service.getCategoryCodeMap();
                List<Budget> budgets = service.getAllBudgets();
                double lastMonthTotal = service.getLastMonthTotalSpending();
                StringBuilder results = new StringBuilder();
                LocalDate nextMonthDate = LocalDate.now().plusMonths(1);
                results.append(String.format("AI Forecast & Budget Insights for %s %d:\n\n", nextMonthDate.getMonth(), nextMonthDate.getYear()));
                double totalPredicted = 0;
                Map<String, Double> predictedExpenses = new HashMap<>();
                int categoryCount = categoryCodeMap.size();
                int currentCategory = 0;
                for (Map.Entry<String, Integer> entry : categoryCodeMap.entrySet()) {
                    currentCategory++;
                    updateMessage(String.format("Predicting for category %d of %d: %s", currentCategory, categoryCount, entry.getKey()));
                    double totalCategoryPrediction = 0;
                    for (int day = 1; day <= nextMonthDate.lengthOfMonth(); day++) {
                        LocalDate predictionDate = LocalDate.of(nextMonthDate.getYear(), nextMonthDate.getMonth(), day);
                        totalCategoryPrediction += expensePredictor.predict(
                                predictionDate.getDayOfWeek().getValue(),
                                predictionDate.getDayOfMonth(),
                                predictionDate.getMonthValue(),
                                entry.getValue()
                        );
                    }
                    if (totalCategoryPrediction > 0) {
                        predictedExpenses.put(entry.getKey(), totalCategoryPrediction);
                        totalPredicted += totalCategoryPrediction;
                    }
                }
                updateMessage("Generating report...");
                results.append("--- Predicted Spending ---\n");
                predictedExpenses.entrySet().stream()
                        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                        .forEach(entry -> results.append(String.format("- %s: ₹%.2f\n", entry.getKey(), entry.getValue())));
                results.append(String.format("\nTotal Predicted Expense: ₹%.2f\n", totalPredicted));
                if (lastMonthTotal > 0) {
                    double difference = totalPredicted - lastMonthTotal;
                    results.append(String.format("This is ₹%.2f %s than last month (₹%.2f).\n", Math.abs(difference), difference > 0 ? "more" : "less", lastMonthTotal));
                }
                results.append("\n--- Budget Analysis & Suggestions ---\n");
                double potentialOverspend = 0;
                boolean suggestionsMade = false;
                for (Budget budget : budgets) {
                    double prediction = predictedExpenses.getOrDefault(budget.getCategory(), 0.0);
                    if (prediction > budget.getMonthlyLimit()) {
                        double overspend = prediction - budget.getMonthlyLimit();
                        results.append(String.format("⚠️ WARNING: You are on track to overspend on %s by ₹%.2f.\n", budget.getCategory(), overspend));
                        potentialOverspend += overspend;
                        suggestionsMade = true;
                    }
                }
                if (suggestionsMade) {
                    results.append(String.format("\n💡 Suggestion: Focus on reducing spending in the flagged categories to save at least ₹%.2f!", potentialOverspend));
                } else {
                    results.append("✅ Great job! Your predicted spending is within all your set budget limits.");
                }
                return results.toString();
            }
        };
    }
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }
}