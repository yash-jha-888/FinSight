package com.finsight.dao;

import com.finsight.model.Category;
import com.finsight.model.Expense;
import com.finsight.model.Income;
import com.finsight.model.PaymentMethod;
import com.finsight.model.Transaction;
import com.finsight.model.TransactionType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Stores income and expenses across the {@code txn} table and its two detail
 * tables. Reads use a single LEFT JOIN and build the right subclass per row.
 */
public class TransactionRepository extends JdbcRepository<Transaction> {

    public TransactionRepository(Database db) {
        super(db);
    }

    @Override
    protected String selectSql() {
        return """
                SELECT t.txn_id, t.type, t.amount_cents, t.txn_date, t.description,
                       c.category_id, c.name, c.type AS category_type, c.color_hex,
                       i.source, e.payment_method
                FROM txn t
                JOIN category c            ON c.category_id = t.category_id
                LEFT JOIN income_detail i  ON i.txn_id = t.txn_id
                LEFT JOIN expense_detail e ON e.txn_id = t.txn_id""";
    }

    @Override
    protected String idColumn() {
        return "t.txn_id";
    }

    @Override
    protected String tableName() {
        return "txn";
    }

    @Override
    protected String orderBy() {
        return "t.txn_date DESC, t.txn_id DESC";
    }

    @Override
    protected Transaction mapRow(ResultSet rs) throws SQLException {
        var category = new Category(rs.getLong("category_id"), rs.getString("name"),
                TransactionType.valueOf(rs.getString("category_type")), rs.getString("color_hex"));
        long id = rs.getLong("txn_id");
        var amount = Money.fromCents(rs.getLong("amount_cents"));
        var date = LocalDate.parse(rs.getString("txn_date"));
        String description = rs.getString("description");

        return switch (TransactionType.valueOf(rs.getString("type"))) {
            case INCOME -> new Income(id, amount, date, category, description, rs.getString("source"));
            case EXPENSE -> new Expense(id, amount, date, category, description,
                    PaymentMethod.valueOf(rs.getString("payment_method")));
        };
    }

    public List<Transaction> findByMonth(YearMonth month) {
        return findBetween(month.atDay(1), month.atEndOfMonth());
    }

    public List<Transaction> findBetween(LocalDate from, LocalDate to) {
        return query(selectSql() + " WHERE t.txn_date BETWEEN ? AND ? ORDER BY " + orderBy(), ps -> {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
        });
    }

    /** Text search over description and category name, newest first. */
    public List<Transaction> search(String text) {
        String like = "%" + text.strip() + "%";
        return query(selectSql() + " WHERE t.description LIKE ? OR c.name LIKE ? ORDER BY " + orderBy(), ps -> {
            ps.setString(1, like);
            ps.setString(2, like);
        });
    }

    @Override
    public Transaction save(Transaction tx) {
        return db.inTransaction(conn -> {
            if (tx.isNew()) {
                insert(conn, tx);
            } else {
                update(conn, tx);
            }
            return tx;
        });
    }

    private void insert(Connection conn, Transaction tx) throws SQLException {
        try (var ps = conn.prepareStatement(
                "INSERT INTO txn (type, amount_cents, txn_date, category_id, description) VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            bindBase(ps, tx);
            ps.executeUpdate();
            tx.setId(generatedKey(ps));
        }
        insertDetail(conn, tx);
    }

    private void update(Connection conn, Transaction tx) throws SQLException {
        try (var ps = conn.prepareStatement(
                "UPDATE txn SET type = ?, amount_cents = ?, txn_date = ?, category_id = ?, description = ? "
                        + "WHERE txn_id = ?")) {
            bindBase(ps, tx);
            ps.setLong(6, tx.getId());
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Transaction " + tx.getId() + " no longer exists");
            }
        }
        // The type may have changed, so replace whichever detail row exists.
        for (String table : List.of("income_detail", "expense_detail")) {
            try (var ps = conn.prepareStatement("DELETE FROM " + table + " WHERE txn_id = ?")) {
                ps.setLong(1, tx.getId());
                ps.executeUpdate();
            }
        }
        insertDetail(conn, tx);
    }

    private static void bindBase(java.sql.PreparedStatement ps, Transaction tx) throws SQLException {
        ps.setString(1, tx.getType().name());
        ps.setLong(2, Money.toCents(tx.getAmount()));
        ps.setString(3, tx.getDate().toString());
        ps.setLong(4, tx.getCategory().getId());
        ps.setString(5, tx.getDescription());
    }

    private static void insertDetail(Connection conn, Transaction tx) throws SQLException {
        switch (tx) {
            case Income income -> {
                try (var ps = conn.prepareStatement("INSERT INTO income_detail (txn_id, source) VALUES (?, ?)")) {
                    ps.setLong(1, income.getId());
                    ps.setString(2, income.getSource());
                    ps.executeUpdate();
                }
            }
            case Expense expense -> {
                try (var ps = conn.prepareStatement(
                        "INSERT INTO expense_detail (txn_id, payment_method) VALUES (?, ?)")) {
                    ps.setLong(1, expense.getId());
                    ps.setString(2, expense.getPaymentMethod().name());
                    ps.executeUpdate();
                }
            }
            default -> throw new IllegalArgumentException("Unknown transaction type " + tx.getClass());
        }
    }

    public boolean isEmpty() {
        try (var st = db.connection().createStatement(); var rs = st.executeQuery("SELECT COUNT(*) FROM txn")) {
            return rs.next() && rs.getLong(1) == 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not count transactions", e);
        }
    }
}
