package com.finsight.db;

import com.finsight.model.Expense;
import com.finsight.model.Income;
import com.finsight.model.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Saves and loads transactions from the "transactions" table. */
public class TransactionDao {

    private Connection conn = DBConnection.getConnection();

    public void add(Transaction t) {
        String sql = "INSERT INTO transactions (type, amount, date, category, description, payment_method) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getType());
            ps.setDouble(2, t.getAmount());
            ps.setString(3, t.getDate().toString());
            ps.setString(4, t.getCategory());
            ps.setString(5, t.getDescription());
            ps.setString(6, t.getPaymentMethod());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not add transaction: " + e.getMessage());
        }
    }

    // loads every transaction, newest first
    public List<Transaction> getAll() {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY date DESC, id DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                double amount = rs.getDouble("amount");
                LocalDate date = LocalDate.parse(rs.getString("date"));
                String category = rs.getString("category");
                String description = rs.getString("description");

                // the type column says which kind of object to create
                if (rs.getString("type").equals("Income")) {
                    list.add(new Income(id, amount, date, category, description));
                } else {
                    list.add(new Expense(id, amount, date, category, description, rs.getString("payment_method")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not load transactions: " + e.getMessage());
        }
        return list;
    }

    public void delete(int id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete transaction: " + e.getMessage());
        }
    }
}
