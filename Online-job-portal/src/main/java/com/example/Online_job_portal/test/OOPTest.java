package com.example.Online_job_portal.test;

import com.example.Online_job_portal.model.Admin;
import com.example.Online_job_portal.model.Employer;
import com.example.Online_job_portal.model.JobSeeker;
import com.example.Online_job_portal.model.User;

public class OOPTest {

    public static void main(String[] args) {

        User user;

        user = new Admin(
                1,
                "Admin",
                "Billo@gmail.com",
                "Billo123",
                "9876543210"
        );

        user.showDashboard();

        user = new Employer(
                2,
                "ABC Company",
                "Shaunk@gmail.com",
                "Shaunak123",
                "9876543211"
        );

        user.showDashboard();

        user = new JobSeeker(
                3,
                "Shivam",
                "shivam@gmail.com",
                "shivam123",
                "9876543212"
        );

        user.showDashboard();
    }
}