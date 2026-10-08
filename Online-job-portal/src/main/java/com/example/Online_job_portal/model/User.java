package com.example.Online_job_portal.model;

public abstract class User {

    protected int id;
    protected String name;
    protected String email;
    protected String password;
    protected String salt = "";
    protected String phone;
    protected String status = "ACTIVE";
    protected String skills = "";
    protected String defaultResumePath = "";

    public User(int id, String name, String email, String password, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.status = "ACTIVE";
        this.skills = "";
        this.salt = "";
        this.defaultResumePath = "";
    }

    public User(int id, String name, String email, String password, String phone, String status, String skills) {
        this(id, name, email, password, "", phone, status, skills, "");
    }

    public User(int id, String name, String email, String password, String salt, String phone,
                String status, String skills, String defaultResumePath) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.salt = salt != null ? salt : "";
        this.phone = phone;
        this.status = status != null ? status : "ACTIVE";
        this.skills = skills != null ? skills : "";
        this.defaultResumePath = defaultResumePath != null ? defaultResumePath : "";
    }

    public abstract void showDashboard();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.status);
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getDefaultResumePath() {
        return defaultResumePath;
    }

    public void setDefaultResumePath(String defaultResumePath) {
        this.defaultResumePath = defaultResumePath;
    }
}
