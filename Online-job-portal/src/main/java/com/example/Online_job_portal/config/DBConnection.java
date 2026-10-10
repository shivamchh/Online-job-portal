package com.example.Online_job_portal.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Enterprise JDBC Connection Manager backed by HikariCP Connection Pooling.
 * Reads environment variables (DB_URL, DB_USERNAME, DB_PASSWORD, APP_DB_FALLBACK)
 * and supports strict fail-fast production mode when in-memory fallback is disabled.
 */
public class DBConnection {

    private static final Logger log = LoggerFactory.getLogger(DBConnection.class);

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/online_job_portal?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8&connectTimeout=3000&socketTimeout=5000";

    private static volatile HikariDataSource pool = null;
    private static volatile Boolean reachableCache = null;

    private DBConnection() {
    }

    public static String getUrl() {
        return getConfigValue("DB_URL", "spring.datasource.url", DEFAULT_URL);
    }

    public static String getUsername() {
        return getConfigValue("DB_USERNAME", "spring.datasource.username", "root");
    }

    public static String getPassword() {
        return getConfigValue("DB_PASSWORD", "spring.datasource.password", "");
    }

    public static boolean isFallbackAllowed() {
        String val = getConfigValue("APP_DB_FALLBACK", "app.db.fallback", "true");
        return !"false".equalsIgnoreCase(val);
    }

    public static synchronized DataSource getDataSource() {
        if (pool == null || pool.isClosed()) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(getUrl());
            config.setUsername(getUsername());
            config.setPassword(getPassword());
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(3000);
            config.setValidationTimeout(2000);
            config.setIdleTimeout(60000);
            config.setMaxLifetime(1800000);
            config.setPoolName("JobSphereHikariPool");
            try {
                pool = new HikariDataSource(config);
            } catch (Exception e) {
                log.warn("HikariCP pool initialization notice: {}", e.getMessage());
            }
        }
        return pool;
    }

    public static Connection getConnection() throws SQLException {
        try {
            DataSource ds = getDataSource();
            if (ds != null) {
                Connection conn = ds.getConnection();
                reachableCache = true;
                return conn;
            }
            throw new SQLException("HikariCP DataSource is not available");
        } catch (SQLException e) {
            reachableCache = false;
            if (!isFallbackAllowed()) {
                log.error("CRITICAL: Database connection failed and in-memory fallback is disabled (app.db.fallback=false)!");
            }
            throw e;
        }
    }

    public static boolean isAvailable() {
        if (reachableCache != null) {
            return reachableCache;
        }
        try (Connection conn = getConnection()) {
            reachableCache = (conn != null && !conn.isClosed());
            return reachableCache;
        } catch (Exception e) {
            reachableCache = false;
            return false;
        }
    }

    public static void resetCache() {
        reachableCache = null;
        if (pool != null && !pool.isClosed()) {
            try {
                pool.close();
            } catch (Exception ignored) {}
            pool = null;
        }
    }

    private static String getConfigValue(String envName, String propName, String defaultValue) {
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String prop = System.getProperty(propName);
        if (prop != null && !prop.isBlank()) {
            return prop.trim();
        }
        String propUpper = System.getProperty(envName);
        if (propUpper != null && !propUpper.isBlank()) {
            return propUpper.trim();
        }
        return defaultValue;
    }
}
