package com.finsight.dao;

import com.finsight.model.Identifiable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared JDBC plumbing for the repositories.
 *
 * <p><b>OOP: inheritance and abstraction.</b> Finding, listing and deleting
 * work the same for every table, so they live here once. Each subclass only
 * says which query selects its rows and how to turn a row into an object.
 */
public abstract class JdbcRepository<T extends Identifiable> implements Repository<T> {

    protected final Database db;

    protected JdbcRepository(Database db) {
        this.db = db;
    }

    /** SELECT that returns every column {@link #mapRow} needs, without a WHERE or ORDER BY. */
    protected abstract String selectSql();

    /** The table alias and primary key column used to filter {@link #selectSql()}, e.g. {@code t.txn_id}. */
    protected abstract String idColumn();

    /** The table rows are deleted from. */
    protected abstract String tableName();

    protected abstract String orderBy();

    protected abstract T mapRow(ResultSet rs) throws SQLException;

    @Override
    public Optional<T> findById(long id) {
        List<T> found = query(selectSql() + " WHERE " + idColumn() + " = ?", ps -> ps.setLong(1, id));
        return found.stream().findFirst();
    }

    @Override
    public List<T> findAll() {
        return query(selectSql() + " ORDER BY " + orderBy(), ps -> { });
    }

    @Override
    public boolean delete(long id) {
        String pk = idColumn().substring(idColumn().indexOf('.') + 1);
        try (var ps = db.connection().prepareStatement("DELETE FROM " + tableName() + " WHERE " + pk + " = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete from " + tableName() + ": " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    protected interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    protected List<T> query(String sql, Binder binder) {
        try (var ps = db.connection().prepareStatement(sql)) {
            binder.bind(ps);
            try (var rs = ps.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Query failed: " + e.getMessage(), e);
        }
    }

    protected static long generatedKey(PreparedStatement ps) throws SQLException {
        try (var keys = ps.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("No id was generated");
            }
            return keys.getLong(1);
        }
    }
}
