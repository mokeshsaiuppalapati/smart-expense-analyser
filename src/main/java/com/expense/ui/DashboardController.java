// File: src/main/java/com/expense/ui/DashboardController.java

package com.expense.ui;

import com.expense.model.Persona;
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
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DashboardController {

    @FXML private Label lblCurrentMonthSpending;
    @FXML private Label lblTopCategory;
    @FXML private TableView<Transaction> recentTransactionsTable;
    @FXML private TableColumn<Transaction, Void> colIcon;
    @FXML private TableColumn<Transaction, String> colDesc;
    @FXML private TableColumn<Transaction, Double> colAmount;
    @FXML private VBox personaContainer;

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

        colAmount.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                setText(empty || amount == null ? null : String.format("₹%.2f", amount));
            }
        });

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
            double currentMonthSpending = service.getTotalForMonth(YearMonth.now());
            animateNumberLabel(lblCurrentMonthSpending, currentMonthSpending);

            Map<String, Double> categoryTotals = service.getCategoryTotalsForMonth(YearMonth.now());
            String topCategory = categoryTotals.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse("N/A");
            lblTopCategory.setText(topCategory);

            List<Transaction> recent = service.getRecentTransactions(5);
            recentTransactionsList.setAll(recent);

            generateAndDisplayPersona();

        } catch (Exception e) {
            lblCurrentMonthSpending.setText("Error");
            lblTopCategory.setText("Error");
            e.printStackTrace();
        }
    }

    private void generateAndDisplayPersona() {
        ProgressIndicator pi = new ProgressIndicator();
        // Keep the title, replace everything else with the loading indicator
        personaContainer.getChildren().setAll(
                personaContainer.getChildren().get(0),
                pi
        );

        Task<Optional<Persona>> personaTask = new Task<>() {
            @Override
            protected Optional<Persona> call() throws Exception {
                return service.generatePersona();
            }
        };

        personaTask.setOnSucceeded(e -> {
            Optional<Persona> personaOpt = personaTask.getValue();
            if (personaOpt.isPresent()) {
                displayPersona(personaOpt.get());
            } else {
                personaContainer.getChildren().setAll(
                        personaContainer.getChildren().get(0),
                        new Label("Not enough data to generate a persona.\n(Requires 30+ transactions)")
                );
            }
        });

        personaTask.setOnFailed(e -> {
            personaContainer.getChildren().setAll(
                    personaContainer.getChildren().get(0),
                    new Label("Error generating persona.")
            );
            e.getSource().getException().printStackTrace();
        });

        new Thread(personaTask).start();
    }

    private void displayPersona(Persona persona) {
        // Clear previous content but keep the title label
        personaContainer.getChildren().retainAll(personaContainer.getChildren().get(0));

        for (Persona.ClusterDescription desc : persona.getClusterDescriptions()) {
            VBox clusterBox = new VBox(5);
            Label clusterTitle = new Label(desc.getName());
            clusterTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-accent-color;");

            TextFlow details = new TextFlow(
                    new Text("Avg. Amount: "), new Text(String.format("₹%.2f\n", desc.getAverageAmount())),
                    new Text("Focus: "), new Text(desc.getTimeFocus() + "\n"),
                    new Text("Top Categories: "), new Text(desc.getTopCategories().stream().map(Map.Entry::getKey).collect(java.util.stream.Collectors.joining(", ")))
            );
            details.getChildren().forEach(node -> node.setStyle("-fx-fill: -fx-text-primary-color;"));

            clusterBox.getChildren().addAll(clusterTitle, details);
            personaContainer.getChildren().add(clusterBox);
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
}