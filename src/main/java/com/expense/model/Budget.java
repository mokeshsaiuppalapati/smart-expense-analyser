// File: src/main/java/com/expense/model/Budget.java

package com.expense.model;

public class Budget {
    private int id;
    private String category;
    private double monthlyLimit;
    private String lastAlertedMonth; // The missing field

    public Budget() {}

    // The missing constructor that the repository needs
    public Budget(int id, String category, double monthlyLimit, String lastAlertedMonth) {
        this.id = id;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.lastAlertedMonth = lastAlertedMonth;
    }

    // Overloaded constructor for creating new budgets from the UI
    public Budget(String category, double monthlyLimit) {
        this(0, category, monthlyLimit, null);
    }

    // Constructor used by older code, kept for compatibility if needed
    public Budget(int id, String category, double monthlyLimit) {
        this(id, category, monthlyLimit, null);
    }


    // --- Getters and Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(double monthlyLimit) { this.monthlyLimit = monthlyLimit; }

    // The missing getter and setter
    public String getLastAlertedMonth() { return lastAlertedMonth; }
    public void setLastAlertedMonth(String lastAlertedMonth) { this.lastAlertedMonth = lastAlertedMonth; }
}