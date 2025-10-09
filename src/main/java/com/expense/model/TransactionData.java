package com.expense.model;

public class TransactionData {
    // MODIFIED: Replaced 'day' with more specific features
    public final double dayOfWeek;
    public final double dayOfMonth;
    public final double month;
    public final double amount;
    public final double categoryCode;

    public TransactionData(double dayOfWeek, double dayOfMonth, double month, double amount, double categoryCode) {
        this.dayOfWeek = dayOfWeek;
        this.dayOfMonth = dayOfMonth;
        this.month = month;
        this.amount = amount;
        this.categoryCode = categoryCode;
    }
}