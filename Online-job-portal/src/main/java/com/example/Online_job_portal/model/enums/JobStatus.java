package com.example.Online_job_portal.model.enums;

/**
 * Job listing workflow and lifecycle status enumeration.
 * <p>
 * Demonstrates domain state segregation:
 * <ul>
 *   <li><b>Moderation States:</b> {@code PENDING}, {@code APPROVED}, {@code REJECTED}
 *       managed by platform administrators to ensure listing quality and policy compliance.</li>
 *   <li><b>Lifecycle States:</b> {@code OPEN}, {@code CLOSED}
 *       managed by hiring employers to represent candidate intake availability.</li>
 * </ul>
 */
public enum JobStatus {
    // Admin Moderation States
    PENDING("PENDING", "Pending Admin Moderation Review"),
    APPROVED("APPROVED", "Approved by Admin & Published Live"),
    REJECTED("REJECTED", "Rejected by Admin Moderation"),

    // Employer Lifecycle States
    OPEN("OPEN", "Open for Applications"),
    CLOSED("CLOSED", "Position Filled / Listing Closed");

    private final String code;
    private final String description;

    JobStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Indicates whether the job is available for public browsing and applications.
     */
    public boolean isActive() {
        return this == APPROVED || this == OPEN;
    }

    /**
     * Checks if this status belongs to admin moderation flow.
     */
    public boolean isModerationState() {
        return this == PENDING || this == APPROVED || this == REJECTED;
    }

    /**
     * Checks if this status belongs to employer vacancy lifecycle flow.
     */
    public boolean isLifecycleState() {
        return this == OPEN || this == CLOSED;
    }

    public static JobStatus fromString(String val) {
        if (val == null) return OPEN;
        for (JobStatus s : values()) {
            if (s.code.equalsIgnoreCase(val.trim())) {
                return s;
            }
        }
        return OPEN;
    }
}
