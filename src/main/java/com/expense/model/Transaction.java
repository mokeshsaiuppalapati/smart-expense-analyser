// File: src/main/java/com/expense/model/Transaction.java

package com.expense.model;

import javafx.beans.property.*;
import java.time.LocalDate;

public class Transaction {
    private final IntegerProperty id = new SimpleIntegerProperty();
    // CHANGED: Date is now a proper LocalDate object, perfect for JavaFX.
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
    private final DoubleProperty amount = new SimpleDoubleProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty category = new SimpleStringProperty();

    public Transaction(int id, LocalDate date, double amount, String description, String category) {
        this.id.set(id);
        this.date.set(date);
        this.amount.set(amount);
        this.description.set(description);
        this.category.set(category);
    }

    public Transaction(LocalDate date, double amount, String description, String category) {
        this(0, date, amount, description, category);
    }

    // --- Property Getters and Setters ---

    public int getId() { return id.get(); }
    public void setId(int v) { id.set(v); }
    public IntegerProperty idProperty() { return id; }

    public LocalDate getDate() { return date.get(); }
    public void setDate(LocalDate v) { date.set(v); }
    public ObjectProperty<LocalDate> dateProperty() { return date; }

    public double getAmount() { return amount.get(); }
    public void setAmount(double v) { amount.set(v); }
    public DoubleProperty amountProperty() { return amount; }

    public String getDescription() { return description.get(); }
    public void setDescription(String v) { description.set(v); }
    public StringProperty descriptionProperty() { return description; }

    public String getCategory() { return category.get(); }
    public void setCategory(String v) { category.set(v); }
    public StringProperty categoryProperty() { return category; }
}