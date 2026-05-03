package io.audita.infrastructure.tenant;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

/**
 * Switches the PostgreSQL schema for each Hibernate connection based on the resolved tenant.
 * Uses SET search_path so every query targets the correct tenant schema automatically.
 */
@Component
public class AuditaMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private static final Pattern TENANT_SCHEMA_PATTERN = Pattern.compile("^[a-z0-9-]{1,100}$");

    private final DataSource dataSource;

    public AuditaMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        if (tenantIdentifier == null || !TENANT_SCHEMA_PATTERN.matcher(tenantIdentifier).matches()) {
            throw new SQLException("Invalid tenant identifier.");
        }

        Connection connection = getAnyConnection();
        // Set search_path to tenant schema first, then public (for shared sequences etc.)
        String quotedSchema = quoteIdentifier(tenantIdentifier);
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + quotedSchema + ", public");
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        // Reset to public before returning to pool — prevents schema bleed between requests
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO public");
        }
        connection.close();
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public boolean supportsAggressiveRelease() {
        // False: we manage the connection lifecycle explicitly
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        throw new UnsupportedOperationException("Unwrapping is not supported");
    }
}
