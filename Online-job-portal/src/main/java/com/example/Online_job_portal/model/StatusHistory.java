package com.example.Online_job_portal.model;

import java.time.LocalDateTime;

public class StatusHistory {

    private int id;
    private int applicationId;
    private String status;
    private LocalDateTime changedAt;
    private String notes;

    public StatusHistory() {
    }

    public StatusHistory(int id, int applicationId, String status, LocalDateTime changedAt, String notes) {
        this.id = id;
        this.applicationId = applicationId;
        this.status = status;
        this.changedAt = changedAt != null ? changedAt : LocalDateTime.now();
        this.notes = notes != null ? notes : "";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
