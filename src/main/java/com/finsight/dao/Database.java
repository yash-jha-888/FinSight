package com.finsight.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns the SQLite connection and creates the schema on first use.
 *
 * <p>A desktop app has one user, so a single shared connection is enough and
 * keeps transactions simple.
 */
public class Database implements AutoCloseable {

    private final Connection connection;

    private Database(String url) {
        try {
            connection = DriverManager.getConnection(url);
            try (var st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            runScript("/com/finsight/db/schema.sql");
        } catch (SQLException e) {
            throw new DataAccessException("Could not open database " + url, e);
        }
    }

    /** Opens (and creates if needed) a database file. */
    public static Database open(Path file) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
        } catch (IOException e) {
            throw new DataAccessException("Could not create folder for " + file, e);
        }
        return new Database("jdbc:sqlite:" + file.toAbsolutePath());
    }

    /** A throwaway database that lives only in memory. Used by tests. */
    public static Database inMemory() {
        return new Database("jdbc:sqlite::memory:");
    }

    /** The default location: ~/.finsight/finsight.db */
    public static Path defaultFile() {
        return Path.of(System.getProperty("user.home"), ".finsight", "finsight.db");
    }

    public Connection connection() {
        return connection;
    }

    /**
     * Runs a block inside a database transaction, committing on success and
     * rolling back if it throws. A call nested inside another simply joins
     * the outer transaction.
     */
    public <R> R inTransaction(SqlFunction<R> work) {
        try {
            boolean previous = connection.getAutoCommit();
            if (!previous) {
                return work.apply(connection);
            }
            connection.setAutoCommit(false);
            try {
                R result = work.apply(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(previous);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database transaction failed: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    public interface SqlFunction<R> {
        R apply(Connection connection) throws SQLException;
    }

    private void runScript(String resource) throws SQLException {
        try (InputStream in = Database.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + resource);
            }
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            try (var st = connection.createStatement()) {
                for (String sql : splitStatements(script)) {
                    st.execute(sql);
                }
            }
        } catch (IOException e) {
            throw new DataAccessException("Could not read " + resource, e);
        }
    }

    /** Splits a script on semicolons, keeping trigger bodies (BEGIN ... END;) together. */
    static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inTrigger = false;
        for (String line : script.split("\\R")) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            current.append(line).append('\n');
            if (trimmed.equalsIgnoreCase("BEGIN")) {
                inTrigger = true;
            }
            boolean ends = inTrigger ? trimmed.equalsIgnoreCase("END;") : trimmed.endsWith(";");
            if (ends) {
                statements.add(current.toString().strip());
                current.setLength(0);
                inTrigger = false;
            }
        }
        if (!current.isEmpty()) {
            statements.add(current.toString().strip());
        }
        return statements;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new DataAccessException("Could not close database", e);
        }
    }
}
