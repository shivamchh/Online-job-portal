package com.example.Online_job_portal.model;

public abstract class User {

    protected int id;
    protected  String name;
    protected  String email;
    protected  String password;
    protected  String phone;

    public User(int id, String name, String email, String password, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
    }


    public  abstract  void showDashboard();


    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getPhone() {
        return phone;
    }


}
