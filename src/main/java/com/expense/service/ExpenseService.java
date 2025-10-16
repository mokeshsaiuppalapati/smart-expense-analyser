// File: src/main/java/com/expense/service/ExpenseService.java

package com.expense.service;

import com.expense.model.*;
import com.expense.repo.TransactionRepository;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class ExpenseService {
    private final TransactionRepository repo = new TransactionRepository();
    private Map<String, Double> spendingAveragesCache;
    private static final double ANOMALY_MULTIPLIER = 4.0;
    private static final double MINIMUM_ANOMALY_AMOUNT = 500.0;

    private final Preferences prefs = Preferences.userNodeForPackage(ExpenseService.class);
    private static final String BUDGET_ALERTS_DISABLED = "budgetAlertsDisabled";

    public void init() throws SQLException {
        repo.init();
        refreshSpendingAveragesCache();
    }

    // --- THIS IS THE UPGRADED METHOD FOR THE NEW BAR CHART ---
    public Map<String, Double> getMonthlyTotalsForYear(int year) throws SQLException {
        return repo.getMonthlyTotalsForYear(year);
    }

    public Budget checkBudgetBreach(String category, double newAmount) throws SQLException {
        if (prefs.getBoolean(BUDGET_ALERTS_DISABLED, false)) return null;
        Budget budget = repo.getBudgetByCategory(category);
        if (budget == null) return null;
        String currentMonthStr = YearMonth.now().toString();
        if (currentMonthStr.equals(budget.getLastAlertedMonth())) return null;
        double currentSpent = getSpentAmountForCategoryThisMonth(category);
        double projectedSpent = currentSpent + newAmount;
        if (projectedSpent > budget.getMonthlyLimit() && currentSpent <= budget.getMonthlyLimit()) {
            return budget;
        }
        return null;
    }
    public void markBudgetAsAlerted(Budget budget) throws SQLException {
        String currentMonthStr = YearMonth.now().toString();
        repo.updateBudgetAlertMonth(budget.getId(), currentMonthStr);
    }
    public void disableBudgetAlerts() {
        prefs.putBoolean(BUDGET_ALERTS_DISABLED, true);
    }

    public Optional<Persona> generatePersona() throws Exception {
        List<Transaction> transactions = repo.getAll();
        if (transactions.size() < 30) return Optional.empty();
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("amount"));
        attributes.add(new Attribute("dayOfWeek"));
        Instances dataset = new Instances("Transactions", attributes, transactions.size());
        for (Transaction t : transactions) {
            double[] values = new double[2];
            values[0] = t.getAmount();
            values[1] = t.getDate().getDayOfWeek().getValue();
            dataset.add(new DenseInstance(1.0, values));
        }
        Normalize normalizeFilter = new Normalize();
        normalizeFilter.setInputFormat(dataset);
        Instances normalizedDataset = Filter.useFilter(dataset, normalizeFilter);
        SimpleKMeans kMeans = new SimpleKMeans();
        kMeans.setNumClusters(3);
        kMeans.buildClusterer(normalizedDataset);
        Map<Integer, List<Transaction>> clusters = new HashMap<>();
        for (int i = 0; i < transactions.size(); i++) {
            int clusterId = kMeans.clusterInstance(normalizedDataset.get(i));
            clusters.computeIfAbsent(clusterId, k -> new ArrayList<>()).add(transactions.get(i));
        }
        List<Persona.ClusterDescription> clusterDescriptions = new ArrayList<>();
        for (Map.Entry<Integer, List<Transaction>> entry : clusters.entrySet()) {
            List<Transaction> clusterTransactions = entry.getValue();
            if (clusterTransactions.isEmpty()) continue;
            double avgAmount = clusterTransactions.stream().mapToDouble(Transaction::getAmount).average().orElse(0.0);
            double avgDay = clusterTransactions.stream().mapToDouble(t -> t.getDate().getDayOfWeek().getValue()).average().orElse(0.0);
            String timeFocus = (avgDay >= 5.5) ? "Weekend" : "Weekday";
            String clusterName = String.format("%s %s", (avgAmount > 1000 ? "High-Value" : "Low-Value"), timeFocus + " Spending");
            List<Map.Entry<String, Long>> topCategories = clusterTransactions.stream()
                    .map(Transaction::getCategory).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                    .entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(2).collect(Collectors.toList());
            clusterDescriptions.add(new Persona.ClusterDescription(clusterName, clusterTransactions.size(), avgAmount, timeFocus, topCategories));
        }
        return Optional.of(new Persona("Your Financial Persona", clusterDescriptions));
    }
    public int processRecurringTransactions() throws SQLException {
        long todayTimestamp = LocalDate.now().toEpochDay();
        List<RecurringTransaction> dueItems = repo.getDueRecurringTransactions(todayTimestamp);
        int itemsProcessed = 0;
        for (RecurringTransaction item : dueItems) {
            while (!item.getNextDueDate().isAfter(LocalDate.now())) {
                addTransaction(item.getNextDueDate(), item.getAmount(), item.getDescription(), item.getCategory());
                LocalDate newDueDate = item.getNextDueDate();
                if (item.getFrequency() == RecurringTransaction.Frequency.MONTHLY) newDueDate = newDueDate.plusMonths(1);
                else if (item.getFrequency() == RecurringTransaction.Frequency.YEARLY) newDueDate = newDueDate.plusYears(1);
                item.setNextDueDate(newDueDate);
                itemsProcessed++;
            }
            repo.updateRecurringTransactionDueDate(item.getId(), item.getNextDueDate().toEpochDay());
        }
        return itemsProcessed;
    }
    public List<RecurringTransaction> getAllRecurringTransactions() throws SQLException { return repo.getAllRecurringTransactions(); }
    public void addRecurringTransaction(RecurringTransaction rt) throws SQLException { repo.addRecurringTransaction(rt); }
    public void deleteRecurringTransaction(int id) throws SQLException { repo.deleteRecurringTransaction(id); }
    public void addSavingsGoal(SavingsGoal goal) throws SQLException { repo.addSavingsGoal(goal); }
    public List<SavingsGoal> getAllSavingsGoals() throws SQLException { return repo.getAllSavingsGoals(); }
    public void addContributionToGoal(int goalId, double amountToAdd) throws SQLException {
        SavingsGoal goal = repo.getSavingsGoalById(goalId);
        if (goal != null) {
            double newAmount = goal.getCurrentAmount() + amountToAdd;
            repo.updateSavingsGoalAmount(goalId, newAmount);
            String description = String.format("Contribution to goal: %s", goal.getGoalName());
            addTransaction(LocalDate.now(), amountToAdd, description, "Savings");
        }
    }
    public void deleteSavingsGoal(int goalId) throws SQLException { repo.deleteSavingsGoal(goalId); }
    public boolean isAnomalous(String category, double amount) {
        if (spendingAveragesCache == null || amount < MINIMUM_ANOMALY_AMOUNT) return false;
        double average = spendingAveragesCache.getOrDefault(category, 0.0);
        if (average == 0) return false;
        return amount > (average * ANOMALY_MULTIPLIER);
    }
    public void refreshSpendingAveragesCache() throws SQLException { this.spendingAveragesCache = repo.getCategoryAverageSpending(); }
    public void addTransaction(LocalDate date, double amount, String desc, String category) throws SQLException {
        repo.insert(new Transaction(date, amount, desc, category));
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
    public List<Transaction> getAll() throws SQLException { return repo.getAll(); }
    public List<Transaction> getRecentTransactions(int limit) throws SQLException { return repo.getRecentTransactions(limit); }
    public Map<String, Double> getCategoryTotalsForMonth(YearMonth yearMonth) throws SQLException { return repo.getCategoryTotalsForMonth(yearMonth); }
    public double getTotalForMonth(YearMonth yearMonth) throws SQLException { return repo.getTotalForMonth(yearMonth); }
    public List<String> getAllCategories() throws SQLException { return repo.getAllCategories(); }
    public List<Budget> getAllBudgets() throws SQLException { return repo.getAllBudgets(); }
    public void addBudget(Budget b) throws SQLException { repo.addBudget(b); }
    public void updateBudget(Budget b) throws SQLException { repo.updateBudget(b); }
    public void deleteBudget(int id) throws SQLException { repo.deleteBudget(id); }
    public double getSpentAmountForCategoryThisMonth(String category) throws SQLException { return repo.getSpentAmountForCategory(category, YearMonth.now()); }
    public List<Budget> getBudgetSuggestions() throws SQLException {
        Map<String, Double> averages = repo.getAverageMonthlySpendingPerCategory();
        return averages.entrySet().stream()
                .map(entry -> {
                    double suggestedLimit = entry.getValue() * 1.10;
                    suggestedLimit = Math.round(suggestedLimit / 50.0) * 50.0;
                    return new Budget(entry.getKey(), Math.max(50, suggestedLimit));
                })
                .collect(Collectors.toList());
    }
    public List<TransactionData> getTransactionDataForRegression() throws SQLException { return repo.getTransactionDataForRegression(); }
    public Map<String, Integer> getCategoryCodeMap() throws SQLException { return repo.getCategoryCodeMap(); }
    public double getLastMonthTotalSpending() throws SQLException { return repo.getTotalForMonth(YearMonth.now().minusMonths(1)); }
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