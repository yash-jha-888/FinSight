package com.finsight.service;

import com.finsight.alert.Alert;
import com.finsight.alert.AlertEngine;
import com.finsight.alert.BudgetStatus;
import com.finsight.dao.BudgetRepository;
import com.finsight.dao.DataAccessException;
import com.finsight.dao.Database;
import com.finsight.model.Category;
import com.finsight.model.TransactionType;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only reports for the dashboard. Totals are computed in SQL through the
 * views defined in {@code schema.sql}, so the database does the aggregation.
 */
public class FinanceService {

    private final Database db;
    private final BudgetRepository budgets;
    private final AlertEngine alertEngine;
    private final Clock clock;

    public FinanceService(Database db, BudgetRepository budgets, AlertEngine alertEngine, Clock clock) {
        this.db = db;
        this.budgets = budgets;
        this.alertEngine = alertEngine;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public MonthlySummary monthlySummary(YearMonth month) {
        String sql = "SELECT income_cents, expense_cents, txn_count FROM v_monthly_summary WHERE month = ?";
        try (var ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, month.toString());
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return MonthlySummary.empty(month);
                }
                return new MonthlySummary(month, cents(rs.getLong(1)), cents(rs.getLong(2)), rs.getInt(3));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load monthly summary", e);
        }
    }

    /** The last {@code count} months ending with {@code last}, oldest first, including empty months. */
    public List<MonthlySummary> monthlyTrend(YearMonth last, int count) {
        List<MonthlySummary> result = new ArrayList<>();
        for (int i = count - 1; i >= 0; i--) {
            result.add(monthlySummary(last.minusMonths(i)));
        }
        return result;
    }

    /** Totals per category for a month, largest first. */
    public List<CategoryTotal> categoryTotals(YearMonth month, TransactionType type) {
        String sql = """
                SELECT category_id, name, type, color_hex, total_cents, txn_count
                FROM v_category_totals
                WHERE month = ? AND type = ?
                ORDER BY total_cents DESC""";
        try (var ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, month.toString());
            ps.setString(2, type.name());
            try (var rs = ps.executeQuery()) {
                List<CategoryTotal> result = new ArrayList<>();
                while (rs.next()) {
                    var category = new Category(rs.getLong(1), rs.getString(2),
                            TransactionType.valueOf(rs.getString(3)), rs.getString(4));
                    result.add(new CategoryTotal(category, cents(rs.getLong(5)), rs.getInt(6)));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load category totals", e);
        }
    }

    /** Spending per day of a month, for the daily chart. Index 0 is the 1st. */
    public BigDecimal[] dailySpending(YearMonth month) {
        BigDecimal[] days = new BigDecimal[month.lengthOfMonth()];
        java.util.Arrays.fill(days, BigDecimal.ZERO.setScale(2));
        String sql = """
                SELECT CAST(substr(txn_date, 9, 2) AS INTEGER), SUM(amount_cents)
                FROM txn
                WHERE type = 'EXPENSE' AND substr(txn_date, 1, 7) = ?
                GROUP BY txn_date""";
        try (var ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, month.toString());
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    days[rs.getInt(1) - 1] = cents(rs.getLong(2));
                }
            }
            return days;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load daily spending", e);
        }
    }

    /** Spending per payment method for a month, largest first. */
    public java.util.Map<com.finsight.model.PaymentMethod, BigDecimal> spendingByPaymentMethod(YearMonth month) {
        String sql = """
                SELECT e.payment_method, SUM(t.amount_cents)
                FROM txn t
                JOIN expense_detail e ON e.txn_id = t.txn_id
                WHERE substr(t.txn_date, 1, 7) = ?
                GROUP BY e.payment_method
                ORDER BY SUM(t.amount_cents) DESC""";
        try (var ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, month.toString());
            try (var rs = ps.executeQuery()) {
                var result = new java.util.LinkedHashMap<com.finsight.model.PaymentMethod, BigDecimal>();
                while (rs.next()) {
                    result.put(com.finsight.model.PaymentMethod.valueOf(rs.getString(1)), cents(rs.getLong(2)));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load payment methods", e);
        }
    }

    /** Overall balance: all income minus all expenses ever recorded. */
    public BigDecimal balance() {
        try (var st = db.connection().createStatement();
             var rs = st.executeQuery("SELECT COALESCE(SUM(net_cents), 0) FROM v_monthly_summary")) {
            rs.next();
            return cents(rs.getLong(1));
        } catch (SQLException e) {
            throw new DataAccessException("Could not load balance", e);
        }
    }

    public List<BudgetStatus> budgetStatuses(YearMonth month) {
        LocalDate today = today();
        return budgets.findWithSpending(month).stream()
                .map(b -> new BudgetStatus(b.budget(), b.spent(), today))
                .toList();
    }

    public List<Alert> alerts(YearMonth month) {
        return alertEngine.evaluate(budgetStatuses(month));
    }

    private static BigDecimal cents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}
