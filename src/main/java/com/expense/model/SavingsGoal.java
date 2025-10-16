// File: src/main/java/com/expense/model/SavingsGoal.java

package com.expense.model;

import javafx.beans.property.*;

import java.time.LocalDate;

public class SavingsGoal {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty goalName = new SimpleStringProperty();
    private final DoubleProperty targetAmount = new SimpleDoubleProperty();
    private final DoubleProperty currentAmount = new SimpleDoubleProperty();
    private final ObjectProperty<LocalDate> targetDate = new SimpleObjectProperty<>();

    // Full constructor
    public SavingsGoal(int id, String goalName, double targetAmount, double currentAmount, LocalDate targetDate) {
        this.id.set(id);
        this.goalName.set(goalName);
        this.targetAmount.set(targetAmount);
        this.currentAmount.set(currentAmount);
        this.targetDate.set(targetDate);
    }

    // Constructor for creating new goals
    public SavingsGoal(String goalName, double targetAmount, LocalDate targetDate) {
        this(0, goalName, targetAmount, 0.0, targetDate);
    }

    // --- JavaFX Property Getters ---
    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getGoalName() { return goalName.get(); }
    public StringProperty goalNameProperty() { return goalName; }

    public double getTargetAmount() { return targetAmount.get(); }
    public DoubleProperty targetAmountProperty() { return targetAmount; }

    public double getCurrentAmount() { return currentAmount.get(); }
    public DoubleProperty currentAmountProperty() { return currentAmount; }

    public LocalDate getTargetDate() { return targetDate.get(); }
    public ObjectProperty<LocalDate> targetDateProperty() { return targetDate; }

    // --- Setters ---
    public void setId(int id) { this.id.set(id); }
    public void setGoalName(String goalName) { this.goalName.set(goalName); }
    public void setTargetAmount(double targetAmount) { this.targetAmount.set(targetAmount); }
    public void setCurrentAmount(double currentAmount) { this.currentAmount.set(currentAmount); }
    public void setTargetDate(LocalDate targetDate) { this.targetDate.set(targetDate); }
}