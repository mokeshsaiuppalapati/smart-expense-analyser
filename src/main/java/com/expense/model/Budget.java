// File: src/main/java/com/expense/model/Budget.java

package com.expense.model;

public class Budget {
    private int id;
    private String category;
    private double monthlyLimit;
    private String lastAlertLevel;

    public Budget() {}

    public Budget(int id, String category, double monthlyLimit, String lastAlertLevel) {
        this.id = id;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.lastAlertLevel = lastAlertLevel;
    }

    public Budget(String category, double monthlyLimit) {
        this(0, category, monthlyLimit, null);
    }

    public Budget(int id, String category, double monthlyLimit) {
        this(id, category, monthlyLimit, null);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(double monthlyLimit) { this.monthlyLimit = monthlyLimit; }
    public String getLastAlertLevel() { return lastAlertLevel; }
    public void setLastAlertLevel(String lastAlertLevel) { this.lastAlertLevel = lastAlertLevel; }
}