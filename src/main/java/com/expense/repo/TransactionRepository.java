// File: src/main/java/com/expense/repo/TransactionRepository.java

package com.expense.repo;

import com.expense.db.Database;
import com.expense.model.Budget;
import com.expense.model.RecurringTransaction;
import com.expense.model.SavingsGoal;
import com.expense.model.Transaction;
import com.expense.model.TransactionData;

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

    // --- SAVINGS GOAL METHODS ---

    public void addSavingsGoal(SavingsGoal goal) throws SQLException {
        String sql = "INSERT INTO savings_goals(goal_name, target_amount, current_amount, target_date_timestamp) VALUES(?,?,?,?)";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, goal.getGoalName());
            ps.setDouble(2, goal.getTargetAmount());
            ps.setDouble(3, goal.getCurrentAmount());
            if (goal.getTargetDate() != null) {
                ps.setLong(4, goal.getTargetDate().toEpochDay());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.executeUpdate();
        }
    }

    public List<SavingsGoal> getAllSavingsGoals() throws SQLException {
        List<SavingsGoal> goals = new ArrayList<>();
        String sql = "SELECT * FROM savings_goals ORDER BY id";
        try (Connection conn = Database.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                long dateTimestamp = rs.getLong("target_date_timestamp");
                LocalDate targetDate = rs.wasNull() ? null : LocalDate.ofEpochDay(dateTimestamp);
                goals.add(new SavingsGoal(
                        rs.getInt("id"),
                        rs.getString("goal_name"),
                        rs.getDouble("target_amount"),
                        rs.getDouble("current_amount"),
                        targetDate
                ));
            }
        }
        return goals;
    }

    public void updateSavingsGoalAmount(int goalId, double newAmount) throws SQLException {
        String sql = "UPDATE savings_goals SET current_amount = ? WHERE id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newAmount);
            ps.setInt(2, goalId);
            ps.executeUpdate();
        }
    }

    public void deleteSavingsGoal(int goalId) throws SQLException {
        String sql = "DELETE FROM savings_goals WHERE id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            ps.executeUpdate();
        }
    }

    public SavingsGoal getSavingsGoalById(int goalId) throws SQLException {
        String sql = "SELECT * FROM savings_goals WHERE id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long dateTimestamp = rs.getLong("target_date_timestamp");
                    LocalDate targetDate = rs.wasNull() ? null : LocalDate.ofEpochDay(dateTimestamp);
                    return new SavingsGoal(
                            rs.getInt("id"),
                            rs.getString("goal_name"),
                            rs.getDouble("target_amount"),
                            rs.getDouble("current_amount"),
                            targetDate
                    );
                }
            }
        }
        return null;
    }

    // --- RECURRING TRANSACTION METHODS ---

    public List<RecurringTransaction> getAllRecurringTransactions() throws SQLException {
        List<RecurringTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM recurring_transactions ORDER BY next_due_timestamp ASC";
        try (Connection conn = Database.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new RecurringTransaction(
                        rs.getInt("id"),
                        rs.getString("description"),
                        rs.getDouble("amount"),
                        rs.getString("category"),
                        RecurringTransaction.Frequency.valueOf(rs.getString("frequency")),
                        LocalDate.ofEpochDay(rs.getLong("next_due_timestamp"))
                ));
            }
        }
        return list;
    }

    public void addRecurringTransaction(RecurringTransaction rt) throws SQLException {
        String sql = "INSERT INTO recurring_transactions(description, amount, category, frequency, next_due_timestamp) VALUES(?,?,?,?,?)";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rt.getDescription());
            ps.setDouble(2, rt.getAmount());
            ps.setString(3, rt.getCategory());
            ps.setString(4, rt.getFrequency().name());
            ps.setLong(5, rt.getNextDueDate().toEpochDay());
            ps.executeUpdate();
        }
    }

    public void deleteRecurringTransaction(int id) throws SQLException {
        String sql = "DELETE FROM recurring_transactions WHERE id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<RecurringTransaction> getDueRecurringTransactions(long currentTimestamp) throws SQLException {
        List<RecurringTransaction> dueItems = new ArrayList<>();
        String sql = "SELECT * FROM recurring_transactions WHERE next_due_timestamp <= ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentTimestamp);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dueItems.add(new RecurringTransaction(
                            rs.getInt("id"),
                            rs.getString("description"),
                            rs.getDouble("amount"),
                            rs.getString("category"),
                            RecurringTransaction.Frequency.valueOf(rs.getString("frequency")),
                            LocalDate.ofEpochDay(rs.getLong("next_due_timestamp"))
                    ));
                }
            }
        }
        return dueItems;
    }

    public void updateRecurringTransactionDueDate(int id, long newDueDateTimestamp) throws SQLException {
        String sql = "UPDATE recurring_transactions SET next_due_timestamp = ? WHERE id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, newDueDateTimestamp);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // --- ALL OTHER METHODS ---

    public Map<String, Double> getCategoryAverageSpending() throws SQLException {
        Map<String, Double> averages = new HashMap<>();
        String sql = "SELECT category, AVG(amount) as avg_spend FROM transactions GROUP BY category";
        try (Connection conn = Database.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                averages.put(rs.getString("category"), rs.getDouble("avg_spend"));
            }
        }
        return averages;
    }

    public void insert(Transaction t) throws SQLException {
        String sql = "INSERT INTO transactions(timestamp, amount, description, category) VALUES(?,?,?,?)";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, t.getDate().toEpochDay());
            ps.setDouble(2, t.getAmount());
            ps.setString(3, t.getDescription());
            ps.setString(4, t.getCategory());
            ps.executeUpdate();
        }
    }

    public List<Transaction> getAll() throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY timestamp DESC, id DESC";
        try (Connection conn = Database.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Transaction(
                        rs.getInt("id"),
                        LocalDate.ofEpochDay(rs.getLong("timestamp")),
                        rs.getDouble("amount"),
                        rs.getString("description"),
                        rs.getString("category")
                ));
            }
        }
        return list;
    }

    public List<Transaction> getRecentTransactions(int limit) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY timestamp DESC, id DESC LIMIT ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Transaction(
                            rs.getInt("id"),
                            LocalDate.ofEpochDay(rs.getLong("timestamp")),
                            rs.getDouble("amount"),
                            rs.getString("description"),
                            rs.getString("category")
                    ));
                }
            }
        }
        return list;
    }

    public void updateTransaction(Transaction t) throws SQLException {
        String sql = "UPDATE transactions SET timestamp=?, amount=?, description=?, category=? WHERE id=?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, t.getDate().toEpochDay());
            ps.setDouble(2, t.getAmount());
            ps.setString(3, t.getDescription());
            ps.setString(4, t.getCategory());
            ps.setInt(5, t.getId());
            ps.executeUpdate();
        }
    }

    public void deleteTransaction(int id) throws SQLException {
        String sql = "DELETE FROM transactions WHERE id=?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<String> getAllCategories() throws SQLException {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM transactions ORDER BY category";
        try (Connection conn = Database.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        }
        return categories;
    }

    public Map<String, Double> getCategoryTotalsForMonth(YearMonth yearMonth) throws SQLException {
        Map<String, Double> map = new HashMap<>();
        long startOfMonth = yearMonth.atDay(1).toEpochDay();
        long endOfMonth = yearMonth.atEndOfMonth().toEpochDay();
        String sql = "SELECT category, SUM(amount) as total FROM transactions WHERE timestamp BETWEEN ? AND ? GROUP BY category";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, startOfMonth);
            ps.setLong(2, endOfMonth);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("category"), rs.getDouble("total"));
                }
            }
        }
        return map;
    }

    public Map<String, Double> getMonthlyTotals() throws SQLException {
        Map<String, Double> map = new HashMap<>();
        String sql = "SELECT strftime('%Y-%m', timestamp, 'unixepoch') as month, SUM(amount) as total " +
                "FROM transactions GROUP BY month";
        try (Connection conn = Database.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("month"), rs.getDouble("total"));
            }
        }
        return map;
    }

    public double getTotalForMonth(YearMonth yearMonth) throws SQLException {
        long startOfMonth = yearMonth.atDay(1).toEpochDay();
        long endOfMonth = yearMonth.atEndOfMonth().toEpochDay();
        String sql = "SELECT SUM(amount) FROM transactions WHERE timestamp BETWEEN ? AND ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, startOfMonth);
            ps.setLong(2, endOfMonth);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    public double getSpentAmountForCategory(String category, YearMonth yearMonth) throws SQLException {
        long startOfMonth = yearMonth.atDay(1).toEpochDay();
        long endOfMonth = yearMonth.atEndOfMonth().toEpochDay();
        String sql = "SELECT SUM(amount) as total FROM transactions WHERE category = ? AND (timestamp BETWEEN ? AND ?)";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            ps.setLong(2, startOfMonth);
            ps.setLong(3, endOfMonth);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        }
        return 0.0;
    }

    public Map<String, Double> getAverageMonthlySpendingPerCategory() throws SQLException {
        Map<String, Double> averages = new HashMap<>();
        long sixMonthsAgo = LocalDate.now().minusMonths(6).toEpochDay();
        String sql = "SELECT category, AVG(monthly_total) as avg_spend FROM (" +
                "  SELECT category, strftime('%Y-%m', timestamp, 'unixepoch') as month, SUM(amount) as monthly_total " +
                "  FROM transactions WHERE timestamp >= ? " +
                "  GROUP BY category, month" +
                ") GROUP BY category";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sixMonthsAgo);
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    averages.put(rs.getString("category"), rs.getDouble("avg_spend"));
                }
            }
        }
        return averages;
    }

    public List<Budget> getAllBudgets() throws SQLException {
        List<Budget> list = new ArrayList<>();
        try (Connection conn = Database.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM budgets")) {
            while (rs.next()) {
                list.add(new Budget(rs.getInt("id"), rs.getString("category"), rs.getDouble("monthly_limit")));
            }
        }
        return list;
    }

    public void addBudget(Budget b) throws SQLException {
        String sql = "INSERT OR IGNORE INTO budgets(category, monthly_limit) VALUES(?,?)";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getCategory());
            ps.setDouble(2, b.getMonthlyLimit());
            ps.executeUpdate();
        }
    }

    public void updateBudget(Budget b) throws SQLException {
        String sql = "UPDATE budgets SET monthly_limit = ? WHERE id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, b.getMonthlyLimit());
            ps.setInt(2, b.getId());
            ps.executeUpdate();
        }
    }

    public void deleteBudget(int id) throws SQLException {
        String sql = "DELETE FROM budgets WHERE id=?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<TransactionData> getTransactionDataForRegression() throws SQLException {
        List<TransactionData> data = new ArrayList<>();
        String sql = "WITH CategoryMap AS (" +
                "  SELECT DISTINCT category, ROW_NUMBER() OVER () - 1 as categoryCode FROM transactions" +
                ") " +
                "SELECT t.timestamp, t.amount, cm.categoryCode " +
                "FROM transactions t JOIN CategoryMap cm ON t.category = cm.category " +
                "ORDER BY t.timestamp;";
        try (Connection conn = Database.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                LocalDate transactionDate = LocalDate.ofEpochDay(rs.getLong("timestamp"));
                data.add(new TransactionData(
                        transactionDate.getDayOfWeek().getValue(),
                        transactionDate.getDayOfMonth(),
                        transactionDate.getMonthValue(),
                        rs.getDouble("amount"),
                        rs.getInt("categoryCode")
                ));
            }
        }
        return data;
    }

    public Map<String, Integer> getCategoryCodeMap() throws SQLException {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT DISTINCT category, ROW_NUMBER() OVER () - 1 as categoryCode FROM transactions;";
        try (Connection conn = Database.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("category"), rs.getInt("categoryCode"));
            }
        }
        return map;
    }
}