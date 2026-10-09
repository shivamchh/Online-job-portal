package com.example.Online_job_portal.model.enums;

/**
 * Application workflow status enumeration.
 */
public enum ApplicationStatus {
    SUBMITTED("SUBMITTED", "Submitted & Awaiting Review"),
    REVIEWED("REVIEWED", "Under Employer Review"),
    PENDING("PENDING", "Pending Decision"),
    SHORTLISTED("SHORTLISTED", "Shortlisted for Interview"),
    APPROVED("APPROVED", "Offer Selected / Approved"),
    REJECTED("REJECTED", "Application Rejected"),
    WITHDRAWN("WITHDRAWN", "Withdrawn by Candidate");

    private final String code;
    private final String description;

    ApplicationStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ApplicationStatus fromString(String val) {
        if (val == null) return SUBMITTED;
        for (ApplicationStatus s : values()) {
            if (s.code.equalsIgnoreCase(val.trim())) {
                return s;
            }
        }
        return SUBMITTED;
    }
}
