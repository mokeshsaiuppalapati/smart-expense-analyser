package com.expense.repo;

import com.expense.db.Database;
import com.expense.model.Budget;
import com.expense.model.Transaction;
import com.expense.model.TransactionData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionRepository {

    public void init() {
        Database.createTables();
    }

    public void insert(Transaction t) throws Exception {
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO transactions(date, amount, description, category) VALUES(?,?,?,?)")) {
            ps.setString(1, t.getDate());
            ps.setDouble(2, t.getAmount());
            ps.setString(3, t.getDescription());
            ps.setString(4, t.getCategory());
            ps.executeUpdate();
        }
    }

    public List<Transaction> getAll() {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY date DESC, id DESC";
        try (Connection conn = Database.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Transaction(
                        rs.getInt("id"),
                        rs.getString("date"),
                        rs.getDouble("amount"),
                        rs.getString("description"),
                        rs.getString("category")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public Map<String, Double> getCategoryTotals() {
        Map<String, Double> map = new HashMap<>();
        try (Connection conn = Database.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT category, SUM(amount) as total FROM transactions GROUP BY category")) {
            while (rs.next()) {
                map.put(rs.getString("category"), rs.getDouble("total"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public Map<String, Double> getCategoryTotalsForMonth(String yearMonth) {
        Map<String, Double> map = new HashMap<>();
        String sql = "SELECT category, SUM(amount) as total FROM transactions WHERE strftime('%Y-%m', date) = ? GROUP BY category";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, yearMonth);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("category"), rs.getDouble("total"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public Map<String, Double> getMonthlyTotals() {
        Map<String, Double> map = new HashMap<>();
        try (Connection conn = Database.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT substr(date,1,7) as month, SUM(amount) as total FROM transactions GROUP BY month")) {
            while (rs.next()) {
                map.put(rs.getString("month"), rs.getDouble("total"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public double getSpentAmountForCategory(String category, String monthYear) {
        String sql = "SELECT SUM(amount) as total FROM transactions WHERE category = ? AND substr(date, 1, 7) = ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            ps.setString(2, monthYear);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public void updateTransaction(Transaction t) {
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE transactions SET date=?, amount=?, description=?, category=? WHERE id=?")) {
            ps.setString(1, t.getDate());
            ps.setDouble(2, t.getAmount());
            ps.setString(3, t.getDescription());
            ps.setString(4, t.getCategory());
            ps.setInt(5, t.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteTransaction(int id) {
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Budget> getAllBudgets() {
        List<Budget> list = new ArrayList<>();
        try (Connection conn = Database.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM budgets")) {
            while (rs.next()) {
                list.add(new Budget(rs.getInt("id"), rs.getString("category"), rs.getDouble("monthly_limit")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void addBudget(Budget b) {
        String sql = "INSERT OR IGNORE INTO budgets(category, monthly_limit) VALUES(?,?)";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getCategory());
            ps.setDouble(2, b.getMonthlyLimit());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateBudget(Budget b) {
        String sql = "UPDATE budgets SET monthly_limit = ? WHERE id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, b.getMonthlyLimit());
            ps.setInt(2, b.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteBudget(int id) {
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM budgets WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<TransactionData> getTransactionDataForRegression() {
        List<TransactionData> data = new ArrayList<>();
        String sql = "WITH CategoryMap AS ( " +
                "  SELECT DISTINCT category, ROW_NUMBER() OVER () - 1 as categoryCode " +
                "  FROM transactions " +
                ") " +
                "SELECT t.date, t.amount, cm.categoryCode " +
                "FROM transactions t " +
                "JOIN CategoryMap cm ON t.category = cm.category " +
                "ORDER BY t.date;";

        try (Connection conn = Database.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                LocalDate transactionDate = LocalDate.parse(rs.getString("date"));
                data.add(new TransactionData(
                        transactionDate.getDayOfWeek().getValue(),
                        transactionDate.getDayOfMonth(),
                        transactionDate.getMonthValue(),
                        rs.getDouble("amount"),
                        rs.getInt("categoryCode")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public Map<String, Integer> getCategoryCodeMap() {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT DISTINCT category, ROW_NUMBER() OVER () - 1 as categoryCode FROM transactions;";
        try (Connection conn = Database.connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("category"), rs.getInt("categoryCode"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public double getTotalForMonth(String yearMonth) {
        String sql = "SELECT SUM(amount) FROM transactions WHERE strftime('%Y-%m', date) = ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, yearMonth);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public List<Transaction> getRecentTransactions(int limit) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY date DESC, id DESC LIMIT ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Transaction(
                        rs.getInt("id"),
                        rs.getString("date"),
                        rs.getDouble("amount"),
                        rs.getString("description"),
                        rs.getString("category")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public Map<String, Double> getAverageMonthlySpendingPerCategory() {
        Map<String, Double> averages = new HashMap<>();
        String sixMonthsAgo = LocalDate.now().minusMonths(6).toString();

        String sql = "SELECT category, AVG(monthly_total) as avg_spend " +
                "FROM ( " +
                "  SELECT category, strftime('%Y-%m', date) as month, SUM(amount) as monthly_total " +
                "  FROM transactions " +
                "  WHERE date >= ? " +
                "  GROUP BY category, month " +
                ") " +
                "GROUP BY category";

        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sixMonthsAgo);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                averages.put(rs.getString("category"), rs.getDouble("avg_spend"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return averages;
    }
}