package com.expense.ui;

import com.expense.ml.ExpensePredictor;
import com.expense.ml.WekaPredictor;
import com.expense.ml.WekaTrainer;
import com.expense.model.Budget;
import com.expense.model.Transaction;
import com.expense.model.TransactionData;
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
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainController {

    @FXML private TableView<Transaction> table;
    @FXML private TableColumn<Transaction, String> colDate;
    @FXML private TableColumn<Transaction, Double> colAmount;
    @FXML private TableColumn<Transaction, String> colDesc;
    @FXML private TableColumn<Transaction, String> colCat;
    @FXML private PieChart pieChart;
    @FXML private BarChart<String, Number> barChart;
    @FXML private ComboBox<Integer> barYearCombo;
    @FXML private ComboBox<Integer> pieYearCombo;
    @FXML private ComboBox<Month> pieMonthCombo;
    @FXML private ProgressIndicator prog;
    @FXML private Button btnRetrain;
    @FXML private TextArea predictionResultsArea;
    @FXML private TabPane mainTabPane;
    @FXML private DashboardController dashboardController;

    private ExpenseService service;
    private WekaPredictor categorizer;
    private ExpensePredictor expensePredictor;

    private final ObservableList<Transaction> transactionList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        this.service = new ExpenseService();
        service.init();
        this.categorizer = new WekaPredictor();
        this.expensePredictor = new ExpensePredictor();

        dashboardController.setService(service);

        setupTable();
        refreshData();
    }

    private void setupTable() {
        table.setEditable(true);

        colDate.setCellValueFactory(cell -> cell.getValue().dateProperty());
        colDate.setCellFactory(TextFieldTableCell.forTableColumn());
        colDate.setOnEditCommit(event -> {
            Transaction transaction = event.getRowValue();
            try {
                LocalDate.parse(event.getNewValue());
                transaction.setDate(event.getNewValue());
                service.updateTransaction(transaction);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Date", "Please use YYYY-MM-DD format.");
                table.refresh();
            }
        });

        colAmount.setCellValueFactory(cell -> cell.getValue().amountProperty().asObject());
        colAmount.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colAmount.setOnEditCommit(event -> {
            if (event.getNewValue() == null || event.getNewValue() < 0) {
                showAlert(Alert.AlertType.ERROR, "Invalid Amount", "Amount cannot be empty or negative.");
                table.refresh();
                return;
            }
            Transaction transaction = event.getRowValue();
            transaction.setAmount(event.getNewValue());
            service.updateTransaction(transaction);
        });

        colDesc.setCellValueFactory(cell -> cell.getValue().descriptionProperty());
        colDesc.setCellFactory(TextFieldTableCell.forTableColumn());
        colDesc.setOnEditCommit(event -> {
            Transaction transaction = event.getRowValue();
            transaction.setDescription(event.getNewValue());
            service.updateTransaction(transaction);
        });

        colCat.setCellValueFactory(cell -> cell.getValue().categoryProperty());
        colCat.setCellFactory(TextFieldTableCell.forTableColumn());
        colCat.setOnEditCommit(event -> {
            Transaction transaction = event.getRowValue();
            String newCategory = event.getNewValue();

            if (newCategory != null && !newCategory.equalsIgnoreCase(transaction.getCategory())) {
                transaction.setCategory(newCategory);
                service.updateTransaction(transaction);

                service.logCorrection(transaction.getDescription(), newCategory);
                System.out.println("Feedback logged: '" + transaction.getDescription() + "' -> '" + newCategory + "'");
            }
        });

        table.setItems(transactionList);
    }

    public void refreshData() {
        transactionList.setAll(service.getAll());
        populateSelectors();

        Platform.runLater(() -> dashboardController.refreshData());
        onRefreshPie();
        onRefreshBar();
    }

    private void populateSelectors() {
        ObservableList<Integer> years = transactionList.stream()
                .map(t -> LocalDate.parse(t.getDate()).getYear())
                .distinct()
                .sorted()
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        Integer selectedBarYear = barYearCombo.getValue();
        barYearCombo.setItems(years);
        if (selectedBarYear != null && years.contains(selectedBarYear)) {
            barYearCombo.setValue(selectedBarYear);
        } else if (!years.isEmpty()) {
            barYearCombo.setValue(LocalDate.now().getYear());
        }

        Integer selectedPieYear = pieYearCombo.getValue();
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

    @FXML
    private void onExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Transactions to CSV");
        fileChooser.setInitialFileName("expenses_export.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        Stage stage = (Stage) mainTabPane.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            List<Transaction> allTransactions = service.getAll();
            DataExporter.exportTransactions(file.getAbsolutePath(), allTransactions);
            showAlert(Alert.AlertType.INFORMATION, "Export Successful", "All transactions have been exported to " + file.getName());
        }
    }

    @FXML
    private void onImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Transactions from CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        Stage stage = (Stage) mainTabPane.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

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
                showAlert(Alert.AlertType.ERROR, "Import Failed", "An error occurred during the import process. Please check the file format.");
                importTask.getException().printStackTrace();
            });

            new Thread(importTask).start();
        }
    }

    @FXML
    private void onAddExpense() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_expense.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add New Expense");
            stage.setScene(new Scene(loader.load()));
            AddExpenseController controller = loader.getController();
            controller.initData(service, this, categorizer);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open the Add Expense window.");
        }
    }

    @FXML
    private void onOpenBudgets() {
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
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open the Budget Manager window.");
        }
    }

    @FXML
    private void onDeleteTransaction() {
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
            service.deleteTransaction(selected.getId());
            refreshData();
        }
    }

    @FXML
    private void onRetrainModel() {
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
            showAlert(Alert.AlertType.INFORMATION, "Success", "Model has been retrained with your corrections!");
        });
        retrainTask.setOnFailed(e -> {
            prog.setVisible(false);
            btnRetrain.setDisable(false);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to retrain the model. See console for details.");
            retrainTask.getException().printStackTrace();
        });
        new Thread(retrainTask).start();
    }

    @FXML
    private void onRefreshPie() {
        Integer year = pieYearCombo.getValue();
        Month month = pieMonthCombo.getValue();
        if (year == null || month == null) {
            pieChart.getData().clear();
            return;
        }
        Map<String, Double> categoryTotals = service.getCategoryTotalsForMonth(year, month.getValue());
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        if (categoryTotals.isEmpty()) {
            pieChartData.add(new PieChart.Data("No Data for this month", 1));
        } else {
            categoryTotals.forEach((cat, total) -> pieChartData.add(new PieChart.Data(cat + String.format(" (%.2f)", total), total)));
        }
        pieChart.setData(pieChartData);
        pieChart.setTitle("Expenses for " + month.name() + " " + year);
    }

    @FXML
    private void onPieChartFilterChanged() {
        onRefreshPie();
    }

    @FXML
    private void onRefreshBar() {
        Integer selectedYear = barYearCombo.getValue();
        if (selectedYear == null) {
            barChart.getData().clear();
            return;
        }
        Map<String, Double> monthlyTotals = service.getMonthlyTotals();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Expenses for " + selectedYear);
        monthlyTotals.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(selectedYear.toString()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    XYChart.Data<String, Number> data = new XYChart.Data<>(entry.getKey(), entry.getValue());
                    series.getData().add(data);
                });

        barChart.getData().clear();
        barChart.getData().add(series);

        series.getData().forEach(data -> {
            Tooltip tooltip = new Tooltip(String.format("₹%.2f", data.getYValue().doubleValue()));
            Tooltip.install(data.getNode(), tooltip);
        });
    }

    @FXML
    private void onBarYearSelected() {
        onRefreshBar();
    }

    @FXML
    private void onPredictExpenses() {
        LocalDate nextMonth = LocalDate.now().plusMonths(1);

        predictionResultsArea.setText("Training model and running predictions...\nPlease wait...");
        prog.setVisible(true);

        Task<String> predictionTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                updateMessage("Training regression model...");
                List<TransactionData> trainingData = service.getTransactionDataForRegression();
                if (trainingData.size() < 10) {
                    return "Not enough transaction data to train the model.";
                }
                expensePredictor.train(trainingData);

                updateMessage("Fetching budgets and historical data...");
                Map<String, Integer> categoryCodeMap = service.getCategoryCodeMap();
                List<Budget> budgets = service.getAllBudgets();
                double lastMonthTotal = service.getLastMonthTotalSpending();

                StringBuilder results = new StringBuilder();
                results.append(String.format("Predictions & Suggestions for %s %d:\n\n", nextMonth.getMonth(), nextMonth.getYear()));

                double totalPredicted = 0;

                updateMessage("Running predictions...");
                Map<String, Double> predictedExpenses = new HashMap<>();
                for (Map.Entry<String, Integer> entry : categoryCodeMap.entrySet()) {
                    String category = entry.getKey();
                    int code = entry.getValue();
                    double totalCategoryPrediction = 0;

                    for (int day = 1; day <= nextMonth.lengthOfMonth(); day++) {
                        LocalDate predictionDate = LocalDate.of(nextMonth.getYear(), nextMonth.getMonth(), day);
                        totalCategoryPrediction += expensePredictor.predict(
                                predictionDate.getDayOfWeek().getValue(),
                                predictionDate.getDayOfMonth(),
                                predictionDate.getMonthValue(),
                                code
                        );
                    }
                    if (totalCategoryPrediction > 0) {
                        predictedExpenses.put(category, totalCategoryPrediction);
                        totalPredicted += totalCategoryPrediction;
                    }
                }

                updateMessage("Generating report...");
                results.append("--- Predicted Spending ---\n");
                predictedExpenses.forEach((category, amount) ->
                        results.append(String.format("- %s: ₹%.2f\n", category, amount))
                );
                results.append(String.format("\nTotal Predicted Expense: ₹%.2f\n", totalPredicted));

                if (lastMonthTotal > 0) {
                    double difference = totalPredicted - lastMonthTotal;
                    results.append(String.format("This is ₹%.2f %s than last month (₹%.2f).\n",
                            Math.abs(difference), difference > 0 ? "more" : "less", lastMonthTotal));
                }

                results.append("\n--- Budget Analysis & Suggestions ---\n");
                double potentialSavings = 0;
                boolean suggestionsMade = false;
                for (Budget budget : budgets) {
                    String category = budget.getCategory();
                    double limit = budget.getMonthlyLimit();

                    if (predictedExpenses.containsKey(category)) {
                        double prediction = predictedExpenses.get(category);
                        if (prediction > limit) {
                            double overspend = prediction - limit;
                            results.append(String.format("⚠️ Over Budget on %s! Predicted spending is ₹%.2f, which is ₹%.2f over your ₹%.2f limit.\n",
                                    category, prediction, overspend, limit));
                            potentialSavings += overspend;
                            suggestionsMade = true;
                        }
                    }
                }

                if (suggestionsMade) {
                    results.append(String.format("\n💡 Suggestion: Try to reduce spending in the flagged categories. By sticking to your budget, you could potentially save ₹%.2f next month!", potentialSavings));
                } else {
                    results.append("✅ Great job! Your predicted spending is within all your set budget limits.");
                }

                return results.toString();
            }
        };

        predictionTask.setOnSucceeded(e -> {
            predictionResultsArea.setText(predictionTask.getValue());
            prog.setVisible(false);
        });

        predictionTask.setOnFailed(e -> {
            predictionResultsArea.setText("An error occurred during prediction.\nSee console for details.");
            prog.setVisible(false);
            predictionTask.getException().printStackTrace();
        });

        new Thread(predictionTask).start();
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