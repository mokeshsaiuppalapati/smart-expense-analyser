package com.expense.model;

public class BudgetAlert {
    private String category;
    private double spent;
    private double limit;
    private double remaining;
    private boolean exceeded;
    private double percent;

    // Full constructor
    public BudgetAlert(String category, double spent, double limit, double remaining, boolean exceeded, double percent) {
        this.category = category;
        this.spent = spent;
        this.limit = limit;
        this.remaining = remaining;
        this.exceeded = exceeded;
        this.percent = percent;
    }

    // Overloaded constructor (for compatibility with old calls)
    public BudgetAlert(String category, double spent, double limit, boolean exceeded) {
        this(category, spent, limit, limit - spent, exceeded, (spent / limit) * 100);
    }

    // Getters
    public String getCategory() { return category; }
    public double getSpent() { return spent; }
    public double getLimit() { return limit; }
    public double getRemaining() { return remaining; }
    public boolean isExceeded() { return exceeded; }
    public double getPercent() { return percent; }
}