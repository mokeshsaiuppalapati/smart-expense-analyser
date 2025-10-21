// File: src/main/java/com/expense/model/Budget.java

package com.expense.model;

public class Budget {
    private int id;
    private int userId;
    private String category;
    private double monthlyLimit;
    private String lastAlertLevel;

    // Default constructor is good practice for JavaFX and other frameworks.
    public Budget() {}

    /**
     * The single, primary constructor used for loading a complete budget object from the database.
     */
    public Budget(int id, int userId, String category, double monthlyLimit, String lastAlertLevel) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.lastAlertLevel = lastAlertLevel;
    }

    /**
     * A convenience constructor for creating a brand new budget from the UI or from a suggestion.
     * It calls the main constructor with default values for ID and user ID.
     */
    public Budget(String category, double monthlyLimit) {
        this(0, 0, category, monthlyLimit, null);
    }

    // --- Getters and Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(double monthlyLimit) { this.monthlyLimit = monthlyLimit; }

    public String getLastAlertLevel() { return lastAlertLevel; }
    public void setLastAlertLevel(String lastAlertLevel) { this.lastAlertLevel = lastAlertLevel; }
}