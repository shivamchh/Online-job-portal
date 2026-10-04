package com.example.Online_job_portal.model;

public class Employer extends User {

    public Employer(int id, String name, String email,
                    String password, String phone) {

        super(id, name, email, password, phone);
    }

    @Override
    public void showDashboard() {
        System.out.println("Employer Dashboard");
    }
}