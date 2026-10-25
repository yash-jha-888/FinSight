package com.finsight.dao;

import com.finsight.model.Budget;
import com.finsight.model.Category;
import com.finsight.model.TransactionType;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.List;

public class BudgetRepository extends JdbcRepository<Budget> {

    public BudgetRepository(Database db) {
        super(db);
    }

    /** A budget together with what has been spent against it, read from {@code v_budget_status}. */
    public record BudgetWithSpending(Budget budget, BigDecimal spent) {
    }

    @Override
    protected String selectSql() {
        return """
                SELECT b.budget_id, b.month, b.limit_cents,
                       c.category_id, c.name, c.type, c.color_hex
                FROM budget b
                JOIN category c ON c.category_id = b.category_id""";
    }

    @Override
    protected String idColumn() {
        return "b.budget_id";
    }

    @Override
    protected String tableName() {
        return "budget";
    }

    @Override
    protected String orderBy() {
        return "b.month DESC, c.name COLLATE NOCASE";
    }

    @Override
    protected Budget mapRow(ResultSet rs) throws SQLException {
        var category = new Category(rs.getLong("category_id"), rs.getString("name"),
                TransactionType.valueOf(rs.getString("type")), rs.getString("color_hex"));
        return new Budget(rs.getLong("budget_id"), category, YearMonth.parse(rs.getString("month")),
                Money.fromCents(rs.getLong("limit_cents")));
    }

    public List<Budget> findByMonth(YearMonth month) {
        return query(selectSql() + " WHERE b.month = ? ORDER BY c.name COLLATE NOCASE",
                ps -> ps.setString(1, month.toString()));
    }

    public List<BudgetWithSpending> findWithSpending(YearMonth month) {
        String sql = """
                SELECT s.budget_id, s.month, s.limit_cents, s.spent_cents,
                       c.category_id, c.name, c.type, c.color_hex
                FROM v_budget_status s
                JOIN category c ON c.category_id = s.category_id
                WHERE s.month = ?
                ORDER BY CAST(s.spent_cents AS REAL) / s.limit_cents DESC""";
        try (var ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, month.toString());
            try (var rs = ps.executeQuery()) {
                var result = new java.util.ArrayList<BudgetWithSpending>();
                while (rs.next()) {
                    result.add(new BudgetWithSpending(mapRow(rs), Money.fromCents(rs.getLong("spent_cents"))));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load budgets: " + e.getMessage(), e);
        }
    }

    @Override
    public Budget save(Budget budget) {
        return db.inTransaction(conn -> {
            if (budget.isNew()) {
                // One budget per category per month: setting it again replaces the limit.
                try (var ps = conn.prepareStatement("""
                        INSERT INTO budget (category_id, month, limit_cents) VALUES (?, ?, ?)
                        ON CONFLICT (category_id, month) DO UPDATE SET limit_cents = excluded.limit_cents""",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, budget.getCategory().getId());
                    ps.setString(2, budget.getMonth().toString());
                    ps.setLong(3, Money.toCents(budget.getLimit()));
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement(
                        "SELECT budget_id FROM budget WHERE category_id = ? AND month = ?")) {
                    ps.setLong(1, budget.getCategory().getId());
                    ps.setString(2, budget.getMonth().toString());
                    try (var rs = ps.executeQuery()) {
                        rs.next();
                        budget.setId(rs.getLong(1));
                    }
                }
            } else {
                try (var ps = conn.prepareStatement(
                        "UPDATE budget SET category_id = ?, month = ?, limit_cents = ? WHERE budget_id = ?")) {
                    ps.setLong(1, budget.getCategory().getId());
                    ps.setString(2, budget.getMonth().toString());
                    ps.setLong(3, Money.toCents(budget.getLimit()));
                    ps.setLong(4, budget.getId());
                    ps.executeUpdate();
                }
            }
            return budget;
        });
    }

    /** Copies every budget from one month into another, skipping categories that already have one. */
    public int copyMonth(YearMonth from, YearMonth to) {
        return db.inTransaction(conn -> {
            try (var ps = conn.prepareStatement("""
                    INSERT OR IGNORE INTO budget (category_id, month, limit_cents)
                    SELECT category_id, ?, limit_cents FROM budget WHERE month = ?""")) {
                ps.setString(1, to.toString());
                ps.setString(2, from.toString());
                return ps.executeUpdate();
            }
        });
    }
}
