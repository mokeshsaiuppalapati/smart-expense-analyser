package com.expense.model;

public class Budget {
    private int id;
    private String category;
    private double monthlyLimit;

    public Budget() {}

    public Budget(int id, String category, double monthlyLimit) {
        this.id = id;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
    }

    public Budget(String category, double monthlyLimit) {
        this(0, category, monthlyLimit);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(double monthlyLimit) { this.monthlyLimit = monthlyLimit; }
}

