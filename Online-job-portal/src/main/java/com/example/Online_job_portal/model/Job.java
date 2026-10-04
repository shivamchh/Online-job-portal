package com.example.Online_job_portal.model;

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

    public Job(int id, int employerId, String title,
               String description, String requirements,
               double salary, String location,
               String jobType, String status) {

        this.id = id;
        this.employerId = employerId;
        this.title = title;
        this.description = description;
        this.requirements = requirements;
        this.salary = salary;
        this.location = location;
        this.jobType = jobType;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getEmployerId() {
        return employerId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getRequirements() {
        return requirements;
    }

    public double getSalary() {
        return salary;
    }

    public String getLocation() {
        return location;
    }

    public String getJobType() {
        return jobType;
    }

    public String getStatus() {
        return status;
    }
}