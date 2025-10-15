// File: src/main/java/com/expense/service/ExpenseService.java

package com.expense.service;

import com.expense.model.Budget;
import com.expense.model.RecurringTransaction;
import com.expense.model.Transaction;
import com.expense.model.TransactionData;
import com.expense.repo.TransactionRepository;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExpenseService {
    private final TransactionRepository repo = new TransactionRepository();
    private Map<String, Double> spendingAveragesCache;

    private static final double ANOMALY_MULTIPLIER = 4.0;
    private static final double MINIMUM_ANOMALY_AMOUNT = 500.0;

    public void init() throws SQLException {
        repo.init();
        refreshSpendingAveragesCache();
    }

    // --- RECURRING TRANSACTION SERVICE METHODS (NEW) ---
    public int processRecurringTransactions() throws SQLException {
        long todayTimestamp = LocalDate.now().toEpochDay();
        List<RecurringTransaction> dueItems = repo.getDueRecurringTransactions(todayTimestamp);
        int itemsProcessed = 0;

        for (RecurringTransaction item : dueItems) {
            while (!item.getNextDueDate().isAfter(LocalDate.now())) {
                addTransaction(item.getNextDueDate(), item.getAmount(), item.getDescription(), item.getCategory());
                LocalDate newDueDate = item.getNextDueDate();
                if (item.getFrequency() == RecurringTransaction.Frequency.MONTHLY) {
                    newDueDate = newDueDate.plusMonths(1);
                } else if (item.getFrequency() == RecurringTransaction.Frequency.YEARLY) {
                    newDueDate = newDueDate.plusYears(1);
                }
                item.setNextDueDate(newDueDate);
                itemsProcessed++;
            }
            repo.updateRecurringTransactionDueDate(item.getId(), item.getNextDueDate().toEpochDay());
        }
        return itemsProcessed;
    }

    public List<RecurringTransaction> getAllRecurringTransactions() throws SQLException {
        return repo.getAllRecurringTransactions();
    }

    public void addRecurringTransaction(RecurringTransaction rt) throws SQLException {
        repo.addRecurringTransaction(rt);
    }

    public void deleteRecurringTransaction(int id) throws SQLException {
        repo.deleteRecurringTransaction(id);
    }

    // --- All other existing methods ---
    public boolean isAnomalous(String category, double amount) {
        if (spendingAveragesCache == null || amount < MINIMUM_ANOMALY_AMOUNT) {
            return false;
        }
        double average = spendingAveragesCache.getOrDefault(category, 0.0);
        if (average == 0) {
            return false;
        }
        return amount > (average * ANOMALY_MULTIPLIER);
    }

    public void refreshSpendingAveragesCache() throws SQLException {
        this.spendingAveragesCache = repo.getCategoryAverageSpending();
    }

    public void addTransaction(LocalDate date, double amount, String desc, String category) throws SQLException {
        Transaction t = new Transaction(date, amount, desc, category);
        repo.insert(t);
        refreshSpendingAveragesCache();
    }

    public void updateTransaction(Transaction t) throws SQLException {
        repo.updateTransaction(t);
        refreshSpendingAveragesCache();
    }

    public void deleteTransaction(int id) throws SQLException {
        repo.deleteTransaction(id);
        refreshSpendingAveragesCache();
    }

    public List<Transaction> getAll() throws SQLException {
        return repo.getAll();
    }

    public List<Transaction> getRecentTransactions(int limit) throws SQLException {
        return repo.getRecentTransactions(limit);
    }

    public Map<String, Double> getCategoryTotalsForMonth(YearMonth yearMonth) throws SQLException {
        return repo.getCategoryTotalsForMonth(yearMonth);
    }

    public Map<String, Double> getMonthlyTotals() throws SQLException {
        return repo.getMonthlyTotals();
    }

    public double getTotalForMonth(YearMonth yearMonth) throws SQLException {
        return repo.getTotalForMonth(yearMonth);
    }

    public List<String> getAllCategories() throws SQLException {
        return repo.getAllCategories();
    }

    public List<Budget> getAllBudgets() throws SQLException {
        return repo.getAllBudgets();
    }

    public void addBudget(Budget b) throws SQLException {
        repo.addBudget(b);
    }

    public void updateBudget(Budget b) throws SQLException {
        repo.updateBudget(b);
    }

    public void deleteBudget(int id) throws SQLException {
        repo.deleteBudget(id);
    }

    public double getSpentAmountForCategoryThisMonth(String category) throws SQLException {
        return repo.getSpentAmountForCategory(category, YearMonth.now());
    }

    public List<Budget> getBudgetSuggestions() throws SQLException {
        Map<String, Double> averages = repo.getAverageMonthlySpendingPerCategory();
        List<Budget> suggestions = new ArrayList<>();
        for (Map.Entry<String, Double> entry : averages.entrySet()) {
            double suggestedLimit = entry.getValue() * 1.10;
            suggestedLimit = Math.round(suggestedLimit / 50.0) * 50.0;
            suggestions.add(new Budget(entry.getKey(), Math.max(50, suggestedLimit)));
        }
        return suggestions;
    }

    public List<TransactionData> getTransactionDataForRegression() throws SQLException {
        return repo.getTransactionDataForRegression();
    }

    public Map<String, Integer> getCategoryCodeMap() throws SQLException {
        return repo.getCategoryCodeMap();
    }

    public double getLastMonthTotalSpending() throws SQLException {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        return repo.getTotalForMonth(lastMonth);
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