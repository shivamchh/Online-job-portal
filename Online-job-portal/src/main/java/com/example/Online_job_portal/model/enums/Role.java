package com.example.Online_job_portal.model.enums;

/**
 * Role enumeration defining system user access tiers.
 */
public enum Role {
    ADMIN("ADMIN", "Platform Administrator"),
    EMPLOYER("EMPLOYER", "Hiring Employer"),
    JOB_SEEKER("JOB_SEEKER", "Job Seeker Candidate");

    private final String code;
    private final String displayName;

    Role(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Role fromString(String val) {
        if (val == null) return JOB_SEEKER;
        for (Role r : values()) {
            if (r.code.equalsIgnoreCase(val.trim())) {
                return r;
            }
        }
        return JOB_SEEKER;
    }
}
