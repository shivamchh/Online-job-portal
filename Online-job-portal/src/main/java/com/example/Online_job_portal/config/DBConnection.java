package com.example.Online_job_portal.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/online_job_portal?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=2000&socketTimeout=3000";

    private static final String URL = getConfigValue("DB_URL", DEFAULT_URL);
    private static final String USERNAME = getConfigValue("DB_USERNAME", "root");
    private static final String PASSWORD = getConfigValue("DB_PASSWORD", "");

    private static volatile Boolean reachableCache = null;

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        // Enforce a fast login timeout (2s) so requests do not hang if MySQL is stopped or unreachable
        try {
            DriverManager.setLoginTimeout(2);
            Connection conn = DriverManager.getConnection(
                    URL,
                    USERNAME,
                    PASSWORD
            );
            reachableCache = true;
            return conn;
        } catch (SQLException e) {
            reachableCache = false;
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
    }

    private static String getConfigValue(String name, String defaultValue) {
        String prop = System.getProperty(name);
        if (prop != null && !prop.isBlank()) {
            return prop.trim();
        }
        String env = System.getenv(name);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return defaultValue;
    }
}
