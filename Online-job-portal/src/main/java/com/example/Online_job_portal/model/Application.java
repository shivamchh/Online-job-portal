package com.example.Online_job_portal.model;

import java.time.LocalDate;

public class Application {

    private int id;
    private int jobId;
    private int jobSeekerId;
    private String resumePath;
    private String status;
    private LocalDate appliedDate;
    private LocalDate statusUpdatedAt;
    private String notes;

    public Application(int id, int jobId, int jobSeekerId,
                       String resumePath, String status, LocalDate appliedDate) {
        this(id, jobId, jobSeekerId, resumePath, status, appliedDate, appliedDate, "");
    }

    public Application(int id, int jobId, int jobSeekerId,
                       String resumePath, String status, LocalDate appliedDate,
                       LocalDate statusUpdatedAt, String notes) {
        this.id = id;
        this.jobId = jobId;
        this.jobSeekerId = jobSeekerId;
        this.resumePath = resumePath;
        this.status = status != null ? status : "SUBMITTED";
        this.appliedDate = appliedDate != null ? appliedDate : LocalDate.now();
        this.statusUpdatedAt = statusUpdatedAt != null ? statusUpdatedAt : this.appliedDate;
        this.notes = notes != null ? notes : "";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public int getJobSeekerId() {
        return jobSeekerId;
    }

    public void setJobSeekerId(int jobSeekerId) {
        this.jobSeekerId = jobSeekerId;
    }

    public String getResumePath() {
        return resumePath;
    }

    public void setResumePath(String resumePath) {
        this.resumePath = resumePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(LocalDate appliedDate) {
        this.appliedDate = appliedDate;
    }

    public LocalDate getStatusUpdatedAt() {
        return statusUpdatedAt;
    }

    public void setStatusUpdatedAt(LocalDate statusUpdatedAt) {
        this.statusUpdatedAt = statusUpdatedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
