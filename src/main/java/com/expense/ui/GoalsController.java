// File: src/main/java/com/expense/ui/GoalsController.java

package com.expense.ui;

import com.expense.model.SavingsGoal;
import com.expense.service.ExpenseService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class GoalsController {

    @FXML private VBox goalsContainer;
    @FXML private TextField goalNameField;
    @FXML private TextField targetAmountField;
    @FXML private DatePicker targetDateField;

    private ExpenseService service;

    public void setService(ExpenseService service) {
        this.service = service;
    }

    public void refreshGoals() {
        if (service == null) return;

        goalsContainer.getChildren().clear();
        try {
            List<SavingsGoal> goals = service.getAllSavingsGoals();
            if (goals.isEmpty()) {
                Label placeholder = new Label("You haven't set any savings goals yet. Create one below!");
                goalsContainer.getChildren().add(placeholder);
            } else {
                for (SavingsGoal goal : goals) {
                    goalsContainer.getChildren().add(createGoalNode(goal));
                }
            }
        } catch (SQLException e) {
            goalsContainer.getChildren().add(new Label("Error loading goals: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private VBox createGoalNode(SavingsGoal goal) {
        VBox container = new VBox(10);
        // --- THIS IS THE FIX ---
        // The hardcoded style is removed and replaced with a style class.
        container.getStyleClass().add("goal-widget");

        // Goal Name and Delete Button
        HBox topRow = new HBox();
        Label nameLabel = new Label(goal.getGoalName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(e -> handleDeleteGoal(goal));
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        topRow.getChildren().addAll(nameLabel, spacer, deleteButton);

        // Progress Bar
        double progress = goal.getTargetAmount() > 0 ? goal.getCurrentAmount() / goal.getTargetAmount() : 0;
        ProgressBar progressBar = new ProgressBar(progress);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        // Progress Text
        HBox progressTextRow = new HBox();
        Label progressLabel = new Label(String.format("₹%.2f / ₹%.2f (%.0f%%)", goal.getCurrentAmount(), goal.getTargetAmount(), progress * 100));
        Label remainingLabel = new Label(String.format("₹%.2f left to save", Math.max(0, goal.getTargetAmount() - goal.getCurrentAmount())));
        remainingLabel.setAlignment(Pos.CENTER_RIGHT);
        HBox textSpacer = new HBox();
        HBox.setHgrow(textSpacer, javafx.scene.layout.Priority.ALWAYS);
        progressTextRow.getChildren().addAll(progressLabel, textSpacer, remainingLabel);

        // Action Row (Add Funds)
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        TextField addAmountField = new TextField();
        addAmountField.setPromptText("Amount to add");
        Button addButton = new Button("Add Funds");
        addButton.setOnAction(e -> handleAddFunds(goal, addAmountField.getText()));

        // Smart Suggestion
        Label suggestionLabel = new Label();
        if (goal.getTargetDate() != null && goal.getTargetDate().isAfter(LocalDate.now())) {
            long monthsRemaining = ChronoUnit.MONTHS.between(LocalDate.now(), goal.getTargetDate());
            if (monthsRemaining >= 1) {
                double amountNeeded = goal.getTargetAmount() - goal.getCurrentAmount();
                if (amountNeeded > 0) {
                    double suggestion = amountNeeded / monthsRemaining;
                    suggestionLabel.setText(String.format("💡 Suggestion: Save ₹%.2f/month.", suggestion));
                    suggestionLabel.setStyle("-fx-text-fill: -fx-accent-color;");
                }
            }
        }

        actionRow.getChildren().addAll(addAmountField, addButton, suggestionLabel);

        container.getChildren().addAll(topRow, progressBar, progressTextRow, actionRow);
        return container;
    }

    // --- All other methods are unchanged ---

    @FXML private void onAddGoal() {
        String name = goalNameField.getText().trim();
        String amountStr = targetAmountField.getText().trim();
        LocalDate targetDate = targetDateField.getValue();
        if (name.isEmpty() || amountStr.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Goal Name and Target Amount are required.");
            return;
        }
        try {
            double targetAmount = Double.parseDouble(amountStr);
            if (targetAmount <= 0) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Target Amount must be a positive number.");
                return;
            }
            SavingsGoal newGoal = new SavingsGoal(name, targetAmount, targetDate);
            service.addSavingsGoal(newGoal);
            refreshGoals();
            clearForm();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid number for the Target Amount.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Save Failed", "Could not save the new goal: " + e.getMessage());
        }
    }
    private void handleAddFunds(SavingsGoal goal, String amountStr) {
        if (amountStr.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter an amount to add.");
            return;
        }
        try {
            double amountToAdd = Double.parseDouble(amountStr);
            if (amountToAdd <= 0) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Amount must be positive.");
                return;
            }
            service.addContributionToGoal(goal.getId(), amountToAdd);
            refreshGoals();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid number.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Update Failed", "Could not add funds to goal: " + e.getMessage());
        }
    }
    private void handleDeleteGoal(SavingsGoal goal) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Goal: '" + goal.getGoalName() + "'?");
        confirm.setContentText("Are you sure? This action cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                service.deleteSavingsGoal(goal.getId());
                refreshGoals();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Delete Failed", "Could not delete goal: " + e.getMessage());
            }
        }
    }
    private void clearForm() {
        goalNameField.clear();
        targetAmountField.clear();
        targetDateField.setValue(null);
    }
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}