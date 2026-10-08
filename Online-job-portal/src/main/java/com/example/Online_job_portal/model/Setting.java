package com.example.Online_job_portal.model;

import java.time.LocalDateTime;

public class Setting {

    private String key;
    private String value;
    private String description;
    private LocalDateTime updatedAt;

    public Setting() {
        this.updatedAt = LocalDateTime.now();
    }

    public Setting(String key, String value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public Setting(String key, String value, String description, LocalDateTime updatedAt) {
        this.key = key;
        this.value = value;
        this.description = description;
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
