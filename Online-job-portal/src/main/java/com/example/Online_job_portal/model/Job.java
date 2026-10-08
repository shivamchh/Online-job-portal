package com.example.Online_job_portal.model;

import java.time.LocalDate;

public class Job {

    private int id;
    private int employerId;
    private String title;
    private String description;
    private String requirements;
    private double salary;
    private String location;
    private String jobType;
    private String status;
    private LocalDate createdAt;

    public Job(int id, int employerId, String title,
               String description, String requirements,
               double salary, String location,
               String jobType, String status) {
        this(id, employerId, title, description, requirements, salary, location, jobType, status, LocalDate.now());
    }

    public Job(int id, int employerId, String title,
               String description, String requirements,
               double salary, String location,
               String jobType, String status, LocalDate createdAt) {
        this.id = id;
        this.employerId = employerId;
        this.title = title;
        this.description = description;
        this.requirements = requirements;
        this.salary = salary;
        this.location = location;
        this.jobType = jobType;
        this.status = status != null ? status : "OPEN";
        this.createdAt = createdAt != null ? createdAt : LocalDate.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEmployerId() {
        return employerId;
    }

    public void setEmployerId(int employerId) {
        this.employerId = employerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return "OPEN".equalsIgnoreCase(this.status);
    }
}