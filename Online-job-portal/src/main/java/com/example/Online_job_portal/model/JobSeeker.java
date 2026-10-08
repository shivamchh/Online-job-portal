package com.example.Online_job_portal.model;

public class JobSeeker extends User {

    public JobSeeker(int id, String name, String email,
                     String password, String phone) {
        super(id, name, email, password, phone);
    }

    public JobSeeker(int id, String name, String email,
                     String password, String phone,
                     String status, String skills) {
        super(id, name, email, password, phone, status, skills);
    }

    public JobSeeker(int id, String name, String email,
                     String password, String salt, String phone,
                     String status, String skills, String defaultResumePath) {
        super(id, name, email, password, salt, phone, status, skills, defaultResumePath);
    }

    @Override
    public void showDashboard() {
        System.out.println("Job Seeker Dashboard");
    }
}