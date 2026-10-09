package com.example.Online_job_portal.util;

public final class ValidationUtil {

    public static final long MAX_RESUME_SIZE = 2 * 1024 * 1024; // 2MB

    private ValidationUtil() {
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
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

    public static boolean isValidResumeMimeType(String contentType) {
        if (!isNotBlank(contentType)) return false;
        String ct = contentType.trim().toLowerCase();
        return ct.equals("application/pdf")
                || ct.equals("application/x-pdf")
                || ct.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || ct.equals("application/msword")
                || ct.equals("application/octet-stream");
    }

    public static boolean isValidFileSignature(byte[] headerBytes, String filename) {
        if (headerBytes == null || headerBytes.length < 4 || !isNotBlank(filename)) {
            return false;
        }
        String lower = filename.trim().toLowerCase();

        // PDF signature: %PDF- (0x25, 0x50, 0x44, 0x46)
        if (lower.endsWith(".pdf")) {
            return headerBytes[0] == 0x25
                    && headerBytes[1] == 0x50
                    && headerBytes[2] == 0x44
                    && headerBytes[3] == 0x46;
        }

        // DOCX signature: PK\x03\x04 (0x50, 0x4B, 0x03, 0x04)
        if (lower.endsWith(".docx")) {
            return headerBytes[0] == 0x50
                    && headerBytes[1] == 0x4B
                    && headerBytes[2] == 0x03
                    && headerBytes[3] == 0x04;
        }

        return false;
    }

    public static boolean isValidResumeSize(long sizeInBytes) {
        return sizeInBytes > 0 && sizeInBytes <= MAX_RESUME_SIZE;
    }
}
