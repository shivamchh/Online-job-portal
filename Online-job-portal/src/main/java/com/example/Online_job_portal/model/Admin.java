package com.example.Online_job_portal.model;

public class Admin extends User {

    public Admin(int id, String name, String email,
                 String password, String phone) {
        super(id, name, email, password, phone);
    }

    public Admin(int id, String name, String email,
                 String password, String phone,
                 String status, String skills) {
        super(id, name, email, password, phone, status, skills);
    }

    public Admin(int id, String name, String email,
                 String password, String salt, String phone,
                 String status, String skills, String defaultResumePath) {
        super(id, name, email, password, salt, phone, status, skills, defaultResumePath);
    }

    @Override
    public void showDashboard() {
        System.out.println("Admin Dashboard");
    }
}