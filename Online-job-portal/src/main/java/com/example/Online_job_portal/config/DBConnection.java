
package com.example.Online_job_portal.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL =
            getEnvOrDefault("DB_URL", "jdbc:mysql://localhost:3306/online_job_portal");
    private static final String USERNAME = getEnvOrDefault("DB_USERNAME", "root");
    private static final String PASSWORD = getEnvOrDefault("DB_PASSWORD", "");

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {

        return DriverManager.getConnection(
                URL,
                USERNAME,
                PASSWORD
        );
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
