package com.example.Online_job_portal.model;

import java.time.LocalDate;
import java.util.Objects;
import com.example.Online_job_portal.model.enums.JobStatus;

/**
 * Job entity representing a employment opportunity.
 * Implements Comparable<Job> for natural ordering by publication date.
 */
public class Job implements Comparable<Job> {

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
    private LocalDate deadline;

    public Job(int id, int employerId, String title,
               String description, String requirements,
               double salary, String location,
               String jobType, String status) {
        this(id, employerId, title, description, requirements, salary, location, jobType, status, LocalDate.now(), LocalDate.now().plusDays(30));
    }

    public Job(int id, int employerId, String title,
               String description, String requirements,
               double salary, String location,
               String jobType, String status, LocalDate createdAt) {
        this(id, employerId, title, description, requirements, salary, location, jobType, status, createdAt, createdAt != null ? createdAt.plusDays(30) : LocalDate.now().plusDays(30));
    }

    public Job(int id, int employerId, String title,
               String description, String requirements,
               double salary, String location,
               String jobType, String status, LocalDate createdAt, LocalDate deadline) {
        this.id = id;
        this.employerId = employerId;
        this.title = title;
        this.description = description;
        this.requirements = requirements;
        this.salary = salary;
        this.location = location;
        this.jobType = jobType;
        this.status = status != null ? status : JobStatus.OPEN.getCode();
        this.createdAt = createdAt != null ? createdAt : LocalDate.now();
        this.deadline = deadline != null ? deadline : this.createdAt.plusDays(30);
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
        return ("OPEN".equalsIgnoreCase(this.status) || "APPROVED".equalsIgnoreCase(this.status)) && !isExpired();
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public boolean isExpired() {
        return deadline != null && deadline.isBefore(LocalDate.now());
    }

    public JobStatus getJobStatusEnum() {
        return JobStatus.fromString(this.status);
    }

    public void setJobStatusEnum(JobStatus jobStatus) {
        this.status = jobStatus != null ? jobStatus.getCode() : JobStatus.OPEN.getCode();
    }

    @Override
    public int compareTo(Job other) {
        if (other == null) return 1;
        if (this.createdAt != null && other.createdAt != null) {
            int cmp = other.createdAt.compareTo(this.createdAt); // newest first
            if (cmp != 0) return cmp;
        }
        return Integer.compare(other.id, this.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Job job = (Job) o;
        return id == job.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}