package com.example.Online_job_portal.model;

public class Employer extends User {

    public Employer(int id, String name, String email,
                    String password, String phone) {
        super(id, name, email, password, phone);
    }

    public Employer(int id, String name, String email,
                    String password, String phone,
                    String status, String skills) {
        super(id, name, email, password, phone, status, skills);
    }

    public Employer(int id, String name, String email,
                    String password, String salt, String phone,
                    String status, String skills, String defaultResumePath) {
        super(id, name, email, password, salt, phone, status, skills, defaultResumePath);
    }

    @Override
    public void showDashboard() {
        System.out.println("Employer Dashboard");
    }
}