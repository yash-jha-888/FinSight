package com.finsight.dao;

import com.finsight.model.Category;
import com.finsight.model.TransactionType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class CategoryRepository extends JdbcRepository<Category> {

    public CategoryRepository(Database db) {
        super(db);
    }

    @Override
    protected String selectSql() {
        return "SELECT c.category_id, c.name, c.type, c.color_hex FROM category c";
    }

    @Override
    protected String idColumn() {
        return "c.category_id";
    }

    @Override
    protected String tableName() {
        return "category";
    }

    @Override
    protected String orderBy() {
        return "c.type DESC, c.name COLLATE NOCASE";
    }

    @Override
    protected Category mapRow(ResultSet rs) throws SQLException {
        return new Category(rs.getLong("category_id"), rs.getString("name"),
                TransactionType.valueOf(rs.getString("type")), rs.getString("color_hex"));
    }

    public List<Category> findByType(TransactionType type) {
        return query(selectSql() + " WHERE c.type = ? ORDER BY c.name COLLATE NOCASE",
                ps -> ps.setString(1, type.name()));
    }

    @Override
    public Category save(Category category) {
        return db.inTransaction(conn -> {
            if (category.isNew()) {
                try (var ps = conn.prepareStatement(
                        "INSERT INTO category (name, type, color_hex) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, category.getName());
                    ps.setString(2, category.getType().name());
                    ps.setString(3, category.getColorHex());
                    ps.executeUpdate();
                    category.setId(generatedKey(ps));
                }
            } else {
                try (var ps = conn.prepareStatement(
                        "UPDATE category SET name = ?, type = ?, color_hex = ? WHERE category_id = ?")) {
                    ps.setString(1, category.getName());
                    ps.setString(2, category.getType().name());
                    ps.setString(3, category.getColorHex());
                    ps.setLong(4, category.getId());
                    ps.executeUpdate();
                }
            }
            return category;
        });
    }

    /** True if any transaction still uses this category, in which case it cannot be deleted. */
    public boolean isInUse(long categoryId) {
        try (var ps = db.connection().prepareStatement("SELECT EXISTS (SELECT 1 FROM txn WHERE category_id = ?)")) {
            ps.setLong(1, categoryId);
            try (var rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check category usage", e);
        }
    }
}
