// File: src/main/java/com/expense/model/RecurringTransaction.java

package com.expense.model;

import java.time.LocalDate;

public class RecurringTransaction {

    // Enum to define the frequency of the transaction
    public enum Frequency {
        MONTHLY,
        YEARLY
    }

    private int id;
    private String description;
    private double amount;
    private String category;
    private Frequency frequency;
    private LocalDate nextDueDate;

    // Full constructor
    public RecurringTransaction(int id, String description, double amount, String category, Frequency frequency, LocalDate nextDueDate) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.frequency = frequency;
        this.nextDueDate = nextDueDate;
    }

    // Constructor for creating new objects before saving to DB
    public RecurringTransaction(String description, double amount, String category, Frequency frequency, LocalDate nextDueDate) {
        this(0, description, amount, category, frequency, nextDueDate);
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Frequency getFrequency() { return frequency; }
    public void setFrequency(Frequency frequency) { this.frequency = frequency; }

    public LocalDate getNextDueDate() { return nextDueDate; }
    public void setNextDueDate(LocalDate nextDueDate) { this.nextDueDate = nextDueDate; }
}