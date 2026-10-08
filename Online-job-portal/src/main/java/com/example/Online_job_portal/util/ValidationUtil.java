package com.example.Online_job_portal.util;

public final class ValidationUtil {

    public static final long MAX_RESUME_SIZE = 2 * 1024 * 1024; // 2MB

    private ValidationUtil() {
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean isValidPhone(String phone) {
        return isNotBlank(phone) && phone.replaceAll("[^0-9]", "").length() >= 7;
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 4;
    }

    public static boolean isValidSalary(Double salary) {
        return salary != null && salary >= 0;
    }

    public static boolean isValidRole(String role) {
        if (!isNotBlank(role)) return false;
        String r = role.trim().toUpperCase();
        return "ADMIN".equals(r) || "EMPLOYER".equals(r) || "JOB_SEEKER".equals(r);
    }

    public static boolean isValidResumeExtension(String filename) {
        if (!isNotBlank(filename)) return false;
        String lower = filename.trim().toLowerCase();
        return lower.endsWith(".pdf") || lower.endsWith(".docx");
    }

    public static boolean isValidResumeSize(long sizeInBytes) {
        return sizeInBytes > 0 && sizeInBytes <= MAX_RESUME_SIZE;
    }
}
