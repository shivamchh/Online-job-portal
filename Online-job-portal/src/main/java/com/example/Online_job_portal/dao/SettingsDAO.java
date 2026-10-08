package com.example.Online_job_portal.dao;

import com.example.Online_job_portal.config.DBConnection;
import com.example.Online_job_portal.model.Setting;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SettingsDAO {

    private final Map<String, Setting> memoryStore = new ConcurrentHashMap<>();
    private boolean useMemoryStore = false;

    public SettingsDAO() {
        // Initialize default configuration values
        memoryStore.put("portal_name", new Setting("portal_name", "Online Job Portal", "Display title of the portal"));
        memoryStore.put("max_resume_size_mb", new Setting("max_resume_size_mb", "2", "Maximum allowable resume size in Megabytes"));
        memoryStore.put("registration_open", new Setting("registration_open", "true", "Whether new candidate registration is enabled"));
        memoryStore.put("maintenance_mode", new Setting("maintenance_mode", "false", "Portal maintenance lock"));
        memoryStore.put("contact_email", new Setting("contact_email", "support@jobportal.com", "System administrator contact email"));
    }

    public String getValue(String key, String defaultValue) {
        if (useMemoryStore) {
            Setting s = memoryStore.get(key);
            return s != null ? s.getValue() : defaultValue;
        }

        String sql = "SELECT key_value FROM settings WHERE key_name = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, key);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("key_value");
                }
            }
        } catch (SQLException e) {
            useMemoryStore = true;
            Setting s = memoryStore.get(key);
            return s != null ? s.getValue() : defaultValue;
        }

        Setting s = memoryStore.get(key);
        return s != null ? s.getValue() : defaultValue;
    }

    public synchronized void setValue(String key, String value) {
        if (memoryStore.containsKey(key)) {
            memoryStore.get(key).setValue(value);
        } else {
            memoryStore.put(key, new Setting(key, value, ""));
        }

        if (useMemoryStore) {
            return;
        }

        String sql = "INSERT INTO settings (key_name, key_value, description) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE key_value = VALUES(key_value), updated_at = CURRENT_TIMESTAMP";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, key);
            statement.setString(2, value);
            statement.setString(3, memoryStore.containsKey(key) ? memoryStore.get(key).getDescription() : "");
            statement.executeUpdate();
        } catch (SQLException e) {
            useMemoryStore = true;
        }
    }

    public Map<String, String> getAll() {
        Map<String, String> map = new LinkedHashMap<>();

        if (useMemoryStore) {
            memoryStore.forEach((k, v) -> map.put(k, v.getValue()));
            return map;
        }

        String sql = "SELECT key_name, key_value FROM settings";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                map.put(rs.getString("key_name"), rs.getString("key_value"));
            }

            if (map.isEmpty()) {
                memoryStore.forEach((k, v) -> map.put(k, v.getValue()));
            }
            return map;
        } catch (SQLException e) {
            useMemoryStore = true;
            memoryStore.forEach((k, v) -> map.put(k, v.getValue()));
            return map;
        }
    }

    public List<Setting> findAllSettings() {
        if (useMemoryStore) {
            return new ArrayList<>(memoryStore.values());
        }

        String sql = "SELECT key_name, key_value, description, updated_at FROM settings";
        List<Setting> list = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                Setting s = new Setting(
                        rs.getString("key_name"),
                        rs.getString("key_value"),
                        rs.getString("description")
                );
                Timestamp ts = rs.getTimestamp("updated_at");
                if (ts != null) {
                    s.setUpdatedAt(ts.toLocalDateTime());
                }
                list.add(s);
            }

            if (list.isEmpty()) {
                return new ArrayList<>(memoryStore.values());
            }
            return list;
        } catch (SQLException e) {
            useMemoryStore = true;
            return new ArrayList<>(memoryStore.values());
        }
    }
}
