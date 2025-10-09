package com.expense.service;

import com.expense.model.Budget;
import com.expense.model.Transaction;
import com.expense.model.TransactionData;
import com.expense.repo.TransactionRepository;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExpenseService {
    private final TransactionRepository repo = new TransactionRepository();

    public void init() {
        repo.init();
    }

    public boolean addTransaction(double amount, String desc, String category) {
        try {
            Transaction t = new Transaction(LocalDate.now().toString(), amount, desc, category);
            repo.insert(t);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Transaction> getAll() {
        return repo.getAll();
    }

    public void updateTransaction(Transaction t) {
        repo.updateTransaction(t);
    }

    public void deleteTransaction(int id) {
        repo.deleteTransaction(id);
    }

    public Map<String, Double> getCategoryTotals() {
        return repo.getCategoryTotals();
    }

    public Map<String, Double> getCategoryTotalsForMonth(int year, int month) {
        String yearMonth = String.format("%d-%02d", year, month);
        return repo.getCategoryTotalsForMonth(yearMonth);
    }

    public Map<String, Double> getMonthlyTotals() {
        return repo.getMonthlyTotals();
    }

    public double getSpentAmountForCategoryThisMonth(String category) {
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return repo.getSpentAmountForCategory(category, currentMonth);
    }

    public List<Budget> getAllBudgets() {
        return repo.getAllBudgets();
    }

    public void addBudget(Budget b) {
        repo.addBudget(b);
    }

    public void updateBudget(Budget b) {
        repo.updateBudget(b);
    }

    public void deleteBudget(int id) {
        repo.deleteBudget(id);
    }

    public List<TransactionData> getTransactionDataForRegression() {
        return repo.getTransactionDataForRegression();
    }

    public Map<String, Integer> getCategoryCodeMap() {
        return repo.getCategoryCodeMap();
    }

    public double getLastMonthTotalSpending() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        String yearMonth = lastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return repo.getTotalForMonth(yearMonth);
    }

    public List<Budget> getBudgetSuggestions() {
        Map<String, Double> averages = repo.getAverageMonthlySpendingPerCategory();
        List<Budget> suggestions = new ArrayList<>();

        for (Map.Entry<String, Double> entry : averages.entrySet()) {
            double suggestedLimit = entry.getValue() * 1.10; // Add 10% buffer
            suggestedLimit = Math.round(suggestedLimit / 50.0) * 50.0;
            if (suggestedLimit == 0) suggestedLimit = 50;
            suggestions.add(new Budget(entry.getKey(), suggestedLimit));
        }
        return suggestions;
    }

    public List<Transaction> getRecentTransactions(int limit) {
        return repo.getRecentTransactions(limit);
    }

    public void logCorrection(String description, String correctCategory) {
        try (FileWriter fw = new FileWriter("data/corrections.csv", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println("\"" + description.replace("\"", "\"\"") + "\",\"" + correctCategory.replace("\"", "\"\"") + "\"");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}