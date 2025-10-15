package com.infosecconference.desktop.db;

import com.infosecconference.desktop.config.AppConfiguration;
import com.infosecconference.desktop.util.SQLFunction;
import com.infosecconference.desktop.util.SQLRunnable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Thin JDBC helper that centralises access to the MySQL database.
 */
public final class DatabaseManager {
    private final AppConfiguration configuration;

    private DatabaseManager(AppConfiguration configuration) {
        this.configuration = configuration;
    }

    public static DatabaseManager from(AppConfiguration configuration) throws SQLException {
        DatabaseManager manager = new DatabaseManager(configuration);
        manager.ensureConnection();
        return manager;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                configuration.databaseUrl(),
                configuration.databaseUser(),
                configuration.databasePassword());
    }

    public void ensureConnection() throws SQLException {
        try (Connection connection = getConnection()) {
            // no-op, just validates connectivity
        }
    }

    public void executeInTransaction(SQLRunnable runnable) throws SQLException {
        executeInTransaction(conn -> {
            runnable.run(conn);
            return null;
        });
    }

    public <T> T executeInTransaction(SQLFunction<Connection, T> function) throws SQLException {
        try (Connection connection = getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = function.apply(connection);
                connection.commit();
                return result;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }
}
