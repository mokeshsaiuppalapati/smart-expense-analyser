package com.expense.model;

import javafx.beans.property.*;

public class Transaction {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty date = new SimpleStringProperty();
    private final DoubleProperty amount = new SimpleDoubleProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty category = new SimpleStringProperty();

    // Full constructor with id
    public Transaction(int id, String date, double amount, String description, String category) {
        this.id.set(id);
        this.date.set(date);
        this.amount.set(amount);
        this.description.set(description);
        this.category.set(category);
    }

    // Constructor without id (for new rows not yet in DB)
    public Transaction(String date, double amount, String description, String category) {
        this(0, date, amount, description, category);
    }

    // Getters & Setters
    public int getId() { return id.get(); }
    public void setId(int v) { id.set(v); }
    public IntegerProperty idProperty() { return id; }

    public String getDate() { return date.get(); }
    public void setDate(String v) { date.set(v); }
    public StringProperty dateProperty() { return date; }

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
