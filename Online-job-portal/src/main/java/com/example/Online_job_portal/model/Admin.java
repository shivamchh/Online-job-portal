package com.example.Online_job_portal.model;

public class Admin extends User {

    public Admin(int id, String name, String email,
                 String password, String phone) {

        super(id, name, email, password, phone);
    }

    @Override
    public void showDashboard() {
        System.out.println("Admin Dashboard");
    }
}