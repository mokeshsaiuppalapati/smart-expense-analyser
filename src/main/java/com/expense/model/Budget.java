// File: src/main/java/com/expense/model/Budget.java

package com.expense.model;

public class Budget {
    private int id;
    private String category;
    private double monthlyLimit;

    public Budget() {}

    // Constructor for creating new budgets from the UI
    public Budget(String category, double monthlyLimit) {
        this.id = 0;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
    }

    // Constructor for loading budgets from the database
    public Budget(int id, String category, double monthlyLimit) {
        this.id = id;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
    }

    // --- Getters and Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(double monthlyLimit) { this.monthlyLimit = monthlyLimit; }
}