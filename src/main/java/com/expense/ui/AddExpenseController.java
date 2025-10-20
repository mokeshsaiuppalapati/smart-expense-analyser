// File: src/main/java/com/expense/ui/AddExpenseController.java

package com.expense.ui;

import com.expense.model.Budget;
import com.expense.ml.WekaPredictor;
import com.expense.service.ExpenseService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class AddExpenseController {

    @FXML private DatePicker datePicker;
    @FXML private TextField descField;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private StackPane confidenceIconPane;

    private ExpenseService service;
    private MainController parentController;
    private WekaPredictor categorizer;

    private static final double CONFIDENT_THRESHOLD = 0.90;
    private static final double UNCERTAIN_THRESHOLD = 0.60;

    public void initData(ExpenseService service, MainController parent, WekaPredictor predictor, List<String> categories) {
        this.service = service;
        this.parentController = parent;
        this.categorizer = predictor;
        categoryCombo.setItems(FXCollections.observableArrayList(categories));
        datePicker.setValue(LocalDate.now());
        categoryCombo.getEditor().textProperty().addListener((obs, oldVal, newVal) -> clearConfidenceIcon());
    }

    @FXML
    private void onSave() {
        String desc = descField.getText().trim();
        String amtStr = amountField.getText().trim();
        String category = categoryCombo.getEditor().getText().trim();
        LocalDate date = datePicker.getValue();

        if (desc.isEmpty() || amtStr.isEmpty() || category.isEmpty() || date == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "All fields are required.");
            return;
        }

        try {
            double amount = Double.parseDouble(amtStr);
            if (amount <= 0) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Amount must be a positive number.");
                return;
            }

            Optional<ExpenseService.BudgetAlertInfo> alertInfoOpt = service.checkBudgetThresholds(category, amount);
            if (alertInfoOpt.isPresent()) {
                ExpenseService.BudgetAlertInfo alertInfo = alertInfoOpt.get();
                boolean userProceeds = showBudgetThresholdAlert(alertInfo);

                if (userProceeds && alertInfo.level < 101) {
                    service.markBudgetAsAlerted(alertInfo.budget, alertInfo.level);
                } else if (!userProceeds) {
                    return;
                }
            }

            if (service.isAnomalous(category, amount)) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Unusual Spending Alert");
                confirmAlert.setHeaderText(String.format("This expense of ₹%.2f seems unusually high for '%s'.", amount, category));
                confirmAlert.setContentText("Are you sure you want to add this transaction?");

                Optional<ButtonType> result = confirmAlert.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK) {
                    return;
                }
            }

            saveTransaction(date, amount, desc, category);

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Amount must be a valid number.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Save Error", "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean showBudgetThresholdAlert(ExpenseService.BudgetAlertInfo alertInfo) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Budget Alert");
        String header;

        if (alertInfo.level == 101) {
            header = "You are continuing to spend in an over-budget category: '" + alertInfo.budget.getCategory() + "'.";
        } else if (alertInfo.level == 100) {
            header = "You've just exceeded your budget for '" + alertInfo.budget.getCategory() + "'!";
        } else {
            header = "You've spent over " + alertInfo.level + "% of your budget for '" + alertInfo.budget.getCategory() + "'.";
        }

        alert.setHeaderText(header);
        alert.setContentText("Your limit is " + String.format("₹%.2f", alertInfo.budget.getMonthlyLimit()) + ".\n\nDo you still want to add this expense?");

        if (alertInfo.level >= 100) {
            CheckBox checkBox = new CheckBox("Do not show these budget alerts again");
            alert.getDialogPane().setExpandableContent(checkBox);
            alert.getDialogPane().setExpanded(true);

            alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
            Optional<ButtonType> result = alert.showAndWait();

            if (checkBox.isSelected()) {
                service.disableBudgetAlertsGlobally();
            }
            return result.isPresent() && result.get() == ButtonType.OK;
        } else {
            alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.OK;
        }
    }

    private void saveTransaction(LocalDate date, double amount, String desc, String category) {
        try {
            service.addTransaction(date, amount, desc, category);
            parentController.refreshData();
            closeWindow();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to save transaction: " + e.getMessage());
        }
    }

    @FXML
    private void onSmartSplit() {
        String description = descField.getText().trim();
        String amountStr = amountField.getText().trim();
        LocalDate date = datePicker.getValue();
        if (description.isEmpty() || amountStr.isEmpty() || date == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Description, Amount, and Date are required to use Smart Split.");
            return;
        }
        try {
            double totalAmount = Double.parseDouble(amountStr);
            if (totalAmount <= 0) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Amount must be a positive number.");
                return;
            }
            List<String> suggestions = categorizer.predictTopCategories(description);
            if (suggestions.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "No Suggestions", "The AI couldn't find any clear categories in the description. Please add a category manually.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/split_transaction.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Smart Split Transaction");
            stage.setScene(new Scene(loader.load()));
            SplitTransactionController controller = loader.getController();
            controller.initData(service, totalAmount, date, description, suggestions);
            stage.showAndWait();
            if (controller.isSplitSuccessful()) {
                parentController.refreshData();
                closeWindow();
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter a valid number for the amount.");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "UI Error", "Could not open the Smart Split window.");
            e.printStackTrace();
        }
    }

    @FXML
    private void onSuggest() {
        String desc = descField.getText().trim();
        if (desc.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Info", "Please enter a description first.");
            return;
        }
        if (categorizer != null && categorizer.isModelLoaded()) {
            WekaPredictor.Result result = categorizer.predict(desc);
            clearConfidenceIcon();
            categoryCombo.setValue(result.category);
            if (result.confidence >= CONFIDENT_THRESHOLD) {
                FontIcon icon = new FontIcon(MaterialDesignC.CHECK_CIRCLE);
                icon.setIconColor(Color.web("#198754"));
                icon.setIconSize(18);
                confidenceIconPane.getChildren().add(icon);
            } else if (result.confidence < UNCERTAIN_THRESHOLD) {
                FontIcon icon = new FontIcon(MaterialDesignH.HELP_CIRCLE);
                icon.setIconColor(Color.web("#ffc107"));
                icon.setIconSize(18);
                confidenceIconPane.getChildren().add(icon);
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Model Not Found", "The category suggestion model is not loaded.");
        }
    }

    private void clearConfidenceIcon() { confidenceIconPane.getChildren().clear(); }
    @FXML private void onCancel() { closeWindow(); }
    private void closeWindow() {
        Stage stage = (Stage) descField.getScene().getWindow();
        stage.close();
    }
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}