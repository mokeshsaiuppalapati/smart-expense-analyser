// File: src/main/java/com/expense/repo/TransactionRepository.java

package com.expense.repo;

import com.expense.db.Database;
import com.expense.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionRepository {

    public void init() throws SQLException {
        Database.createTables();
    }

    // --- USER METHODS ---
    public User findUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"));
                }
            }
        }
        return null;
    }

    public void createUser(String username, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users(username, password_hash) VALUES(?,?)";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.executeUpdate();
        }
    }

    // --- SAVINGS GOAL METHODS ---
    public void addSavingsGoal(int userId, SavingsGoal goal) throws SQLException {
        String sql = "INSERT INTO savings_goals(user_id, goal_name, target_amount, current_amount, target_date_timestamp) VALUES(?,?,?,?,?)";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, goal.getGoalName()); ps.setDouble(3, goal.getTargetAmount()); ps.setDouble(4, goal.getCurrentAmount());
            if (goal.getTargetDate() != null) ps.setLong(5, goal.getTargetDate().toEpochDay()); else ps.setNull(5, Types.INTEGER);
            ps.executeUpdate();
        }
    }
    public List<SavingsGoal> getAllSavingsGoals(int userId) throws SQLException {
        List<SavingsGoal> goals = new ArrayList<>();
        String sql = "SELECT * FROM savings_goals WHERE user_id = ? ORDER BY id";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long dateTimestamp = rs.getLong("target_date_timestamp");
                    LocalDate targetDate = rs.wasNull() ? null : LocalDate.ofEpochDay(dateTimestamp);
                    goals.add(new SavingsGoal(rs.getInt("id"), rs.getString("goal_name"), rs.getDouble("target_amount"), rs.getDouble("current_amount"), targetDate));
                }
            }
        }
        return goals;
    }
    public void updateSavingsGoalAmount(int userId, int goalId, double newAmount) throws SQLException {
        String sql = "UPDATE savings_goals SET current_amount = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newAmount); ps.setInt(2, goalId); ps.setInt(3, userId); ps.executeUpdate();
        }
    }
    public void deleteSavingsGoal(int userId, int goalId) throws SQLException {
        String sql = "DELETE FROM savings_goals WHERE id = ? AND user_id = ?";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, goalId); ps.setInt(2, userId); ps.executeUpdate();
        }
    }
    public SavingsGoal getSavingsGoalById(int userId, int goalId) throws SQLException {
        String sql = "SELECT * FROM savings_goals WHERE id = ? AND user_id = ?";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, goalId); ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long dateTimestamp = rs.getLong("target_date_timestamp");
                    LocalDate targetDate = rs.wasNull() ? null : LocalDate.ofEpochDay(dateTimestamp);
                    return new SavingsGoal(rs.getInt("id"), rs.getString("goal_name"), rs.getDouble("target_amount"), rs.getDouble("current_amount"), targetDate);
                }
            }
        }
        return null;
    }

    // --- RECURRING TRANSACTION METHODS ---
    public List<RecurringTransaction> getAllRecurringTransactions(int userId) throws SQLException {
        List<RecurringTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM recurring_transactions WHERE user_id = ? ORDER BY next_due_timestamp ASC";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new RecurringTransaction(rs.getInt("id"), rs.getString("description"), rs.getDouble("amount"), rs.getString("category"), RecurringTransaction.Frequency.valueOf(rs.getString("frequency")), LocalDate.ofEpochDay(rs.getLong("next_due_timestamp"))));
            }
        }
        return list;
    }
    public void addRecurringTransaction(int userId, RecurringTransaction rt) throws SQLException {
        String sql = "INSERT INTO recurring_transactions(user_id, description, amount, category, frequency, next_due_timestamp) VALUES(?,?,?,?,?,?)";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, rt.getDescription()); ps.setDouble(3, rt.getAmount()); ps.setString(4, rt.getCategory()); ps.setString(5, rt.getFrequency().name()); ps.setLong(6, rt.getNextDueDate().toEpochDay());
            ps.executeUpdate();
        }
    }
    public void deleteRecurringTransaction(int userId, int id) throws SQLException {
        String sql = "DELETE FROM recurring_transactions WHERE id = ? AND user_id = ?";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id); ps.setInt(2, userId); ps.executeUpdate();
        }
    }
    public List<RecurringTransaction> getDueRecurringTransactions(int userId, long currentTimestamp) throws SQLException {
        List<RecurringTransaction> dueItems = new ArrayList<>();
        String sql = "SELECT * FROM recurring_transactions WHERE user_id = ? AND next_due_timestamp <= ?";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setLong(2, currentTimestamp);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) dueItems.add(new RecurringTransaction(rs.getInt("id"), rs.getString("description"), rs.getDouble("amount"), rs.getString("category"), RecurringTransaction.Frequency.valueOf(rs.getString("frequency")), LocalDate.ofEpochDay(rs.getLong("next_due_timestamp"))));
            }
        }
        return dueItems;
    }
    public void updateRecurringTransactionDueDate(int userId, int id, long newDueDateTimestamp) throws SQLException {
        String sql = "UPDATE recurring_transactions SET next_due_timestamp = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, newDueDateTimestamp); ps.setInt(2, id); ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    // --- TRANSACTION METHODS ---
    public void insert(int userId, Transaction t) throws SQLException {
        String sql = "INSERT INTO transactions(user_id, timestamp, amount, description, category) VALUES(?,?,?,?,?)";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setLong(2, t.getDate().toEpochDay()); ps.setDouble(3, t.getAmount()); ps.setString(4, t.getDescription()); ps.setString(5, t.getCategory());
            ps.executeUpdate();
        }
    }
    public List<Transaction> getAll(int userId) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY timestamp DESC, id DESC";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Transaction(rs.getInt("id"), LocalDate.ofEpochDay(rs.getLong("timestamp")), rs.getDouble("amount"), rs.getString("description"), rs.getString("category")));
            }
        }
        return list;
    }
    public List<Transaction> getRecentTransactions(int userId, int limit) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY timestamp DESC, id DESC LIMIT ?";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Transaction(rs.getInt("id"), LocalDate.ofEpochDay(rs.getLong("timestamp")), rs.getDouble("amount"), rs.getString("description"), rs.getString("category")));
            }
        }
        return list;
    }
    public void updateTransaction(int userId, Transaction t) throws SQLException {
        String sql = "UPDATE transactions SET timestamp=?, amount=?, description=?, category=? WHERE id=? AND user_id = ?";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, t.getDate().toEpochDay()); ps.setDouble(2, t.getAmount()); ps.setString(3, t.getDescription()); ps.setString(4, t.getCategory()); ps.setInt(5, t.getId()); ps.setInt(6, userId);
            ps.executeUpdate();
        }
    }
    public void deleteTransaction(int userId, int id) throws SQLException {
        String sql = "DELETE FROM transactions WHERE id=? AND user_id = ?";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id); ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }
    public List<String> getAllCategories(int userId) throws SQLException {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM transactions WHERE user_id = ? ORDER BY category";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) categories.add(rs.getString("category"));
            }
        }
        return categories;
    }
    public Map<String, Double> getCategoryTotalsForMonth(int userId, YearMonth yearMonth) throws SQLException {
        Map<String, Double> map = new HashMap<>();
        long startOfMonth = yearMonth.atDay(1).toEpochDay(); long endOfMonth = yearMonth.atEndOfMonth().toEpochDay();
        String sql = "SELECT category, SUM(amount) as total FROM transactions WHERE user_id = ? AND timestamp BETWEEN ? AND ? GROUP BY category";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setLong(2, startOfMonth); ps.setLong(3, endOfMonth);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getString("category"), rs.getDouble("total"));
            }
        }
        return map;
    }
    public Map<String, Double> getMonthlyTotalsForYear(int userId, int year) throws SQLException {
        Map<String, Double> map = new HashMap<>();
        String sql = "SELECT strftime('%Y-%m', timestamp, 'unixepoch') as month, SUM(amount) as total FROM transactions WHERE user_id = ? AND strftime('%Y', timestamp, 'unixepoch') = ? GROUP BY month";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, String.valueOf(year));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getString("month"), rs.getDouble("total"));
            }
        }
        return map;
    }
    public double getTotalForMonth(int userId, YearMonth yearMonth) throws SQLException {
        long startOfMonth = yearMonth.atDay(1).toEpochDay(); long endOfMonth = yearMonth.atEndOfMonth().toEpochDay();
        String sql = "SELECT SUM(amount) FROM transactions WHERE user_id = ? AND timestamp BETWEEN ? AND ?";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setLong(2, startOfMonth); ps.setLong(3, endOfMonth);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0.0;
    }
    public double getSpentAmountForCategory(int userId, String category, YearMonth yearMonth) throws SQLException {
        long startOfMonth = yearMonth.atDay(1).toEpochDay(); long endOfMonth = yearMonth.atEndOfMonth().toEpochDay();
        String sql = "SELECT SUM(amount) as total FROM transactions WHERE user_id = ? AND category = ? AND (timestamp BETWEEN ? AND ?)";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, category); ps.setLong(3, startOfMonth); ps.setLong(4, endOfMonth);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        }
        return 0.0;
    }
    public Map<String, Double> getCategoryAverageSpending(int userId) throws SQLException {
        Map<String, Double> averages = new HashMap<>();
        String sql = "SELECT category, AVG(amount) as avg_spend FROM transactions WHERE user_id = ? GROUP BY category";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) averages.put(rs.getString("category"), rs.getDouble("avg_spend"));
            }
        }
        return averages;
    }
    public Map<String, Double> getAverageMonthlySpendingPerCategory(int userId) throws SQLException {
        Map<String, Double> averages = new HashMap<>();
        long sixMonthsAgo = LocalDate.now().minusMonths(6).toEpochDay();
        String sql = "SELECT category, AVG(monthly_total) as avg_spend FROM ( SELECT category, strftime('%Y-%m', timestamp, 'unixepoch') as month, SUM(amount) as monthly_total FROM transactions WHERE user_id = ? AND timestamp >= ? GROUP BY category, month) GROUP BY category";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setLong(2, sixMonthsAgo);
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()) averages.put(rs.getString("category"), rs.getDouble("avg_spend"));
            }
        }
        return averages;
    }

    // --- BUDGET METHODS ---
    public List<Budget> getAllBudgets(int userId) throws SQLException {
        List<Budget> list = new ArrayList<>();
        String sql = "SELECT * FROM budgets WHERE user_id = ?";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Budget(
                            rs.getInt("id"),
                            rs.getString("category"),
                            rs.getDouble("monthly_limit"),
                            rs.getString("last_alert_level")
                    ));
                }
            }
        }
        return list;
    }
    public Budget getBudgetByCategory(int userId, String category) throws SQLException {
        String sql = "SELECT * FROM budgets WHERE user_id = ? AND category = ?";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, category);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Budget(
                            rs.getInt("id"),
                            rs.getString("category"),
                            rs.getDouble("monthly_limit"),
                            rs.getString("last_alert_level")
                    );
                }
            }
        }
        return null;
    }
    public void addBudget(int userId, Budget b) throws SQLException {
        String sql = "INSERT INTO budgets(user_id, category, monthly_limit) VALUES(?,?,?) ON CONFLICT(user_id, category) DO UPDATE SET monthly_limit = excluded.monthly_limit";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setString(2, b.getCategory()); ps.setDouble(3, b.getMonthlyLimit());
            ps.executeUpdate();
        }
    }
    public void updateBudget(int userId, Budget b) throws SQLException {
        String sql = "UPDATE budgets SET monthly_limit = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, b.getMonthlyLimit()); ps.setInt(2, b.getId()); ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }
    public void deleteBudget(int userId, int id) throws SQLException {
        String sql = "DELETE FROM budgets WHERE id=? AND user_id = ?";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id); ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }
    public void updateBudgetAlertLevel(int userId, int budgetId, String alertLevel) throws SQLException {
        String sql = "UPDATE budgets SET last_alert_level = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, alertLevel);
            ps.setInt(2, budgetId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    // --- ML Data Methods ---
    public List<TransactionData> getTransactionDataForRegression(int userId) throws SQLException {
        List<TransactionData> data = new ArrayList<>();
        String sql = "WITH CategoryMap AS ( SELECT DISTINCT category, ROW_NUMBER() OVER () - 1 as categoryCode FROM transactions WHERE user_id = ?) SELECT t.timestamp, t.amount, cm.categoryCode FROM transactions t JOIN CategoryMap cm ON t.category = cm.category WHERE t.user_id = ? ORDER BY t.timestamp;";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate transactionDate = LocalDate.ofEpochDay(rs.getLong("timestamp"));
                    data.add(new TransactionData(transactionDate.getDayOfWeek().getValue(), transactionDate.getDayOfMonth(), transactionDate.getMonthValue(), rs.getDouble("amount"), rs.getInt("categoryCode")));
                }
            }
        }
        return data;
    }
    public Map<String, Integer> getCategoryCodeMap(int userId) throws SQLException {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT DISTINCT category, ROW_NUMBER() OVER () - 1 as categoryCode FROM transactions WHERE user_id = ?;";
        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getString("category"), rs.getInt("categoryCode"));
            }
        }
        return map;
    }
}