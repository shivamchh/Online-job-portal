package com.example.Online_job_portal;

import com.example.Online_job_portal.model.Admin;
import com.example.Online_job_portal.model.Employer;
import com.example.Online_job_portal.model.JobSeeker;
import com.example.Online_job_portal.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Hierarchy and Polymorphism Verification Tests")
class UserHierarchyPolymorphismTest {

    @Test
    @DisplayName("Polymorphic invocation of showDashboard on abstract User subclasses")
    void testUserHierarchyAndPolymorphicDashboards() {
        User admin = new Admin(1, "Admin", "admin@example.com", "admin123", "9876543210");
        User employer = new Employer(2, "ABC Company", "employer@example.com", "pass123", "9876543211");
        User jobSeeker = new JobSeeker(3, "Alex Kumar", "alex@example.com", "pass123", "9876543212");

        // Verify abstract inheritance and concrete instances
        assertInstanceOf(User.class, admin);
        assertInstanceOf(User.class, employer);
        assertInstanceOf(User.class, jobSeeker);

        // Verify state initialization & getters
        assertEquals("Admin", admin.getName());
        assertEquals("ABC Company", employer.getName());
        assertEquals("Alex Kumar", jobSeeker.getName());

        assertTrue(admin.isActive());
        assertTrue(employer.isActive());
        assertTrue(jobSeeker.isActive());

        // Polymorphic invocation should execute without exceptions
        assertDoesNotThrow(admin::showDashboard);
        assertDoesNotThrow(employer::showDashboard);
        assertDoesNotThrow(jobSeeker::showDashboard);
    }

    @Test
    @DisplayName("Verify User status lifecycle, soft-deletion flag, and skills encapsulation")
    void testUserStatusAndSkills() {
        JobSeeker seeker = new JobSeeker(10, "Priya", "priya@example.com", "hash", "salt", "9876543213",
                "ACTIVE", "Java, Spring Boot", "resume.pdf");

        assertEquals("Java, Spring Boot", seeker.getSkills());
        assertEquals("resume.pdf", seeker.getDefaultResumePath());
        assertEquals("salt", seeker.getSalt());
        assertTrue(seeker.isActive());

        seeker.setStatus("INACTIVE");
        assertFalse(seeker.isActive());
    }
}
