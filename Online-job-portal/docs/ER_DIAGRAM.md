# Entity Relationship (ER) Diagram

Below is the complete database schema for the **JobSphere - Online Job Portal** system, detailing all primary keys, foreign keys, relationships, and data attributes.

```mermaid
erDiagram
    USERS ||--o{ JOBS : "posts (employer_id)"
    USERS ||--o{ APPLICATIONS : "submits (job_seeker_id)"
    USERS ||--o{ NOTIFICATIONS : "receives (user_id)"
    USERS ||--o{ MESSAGES : "sends (sender_id)"
    USERS ||--o{ MESSAGES : "receives (receiver_id)"
    JOBS ||--o{ APPLICATIONS : "receives (job_id)"
    APPLICATIONS ||--o{ STATUS_HISTORY : "tracks (application_id)"
    USERS ||--o{ ACTIVITY_LOGS : "logs (user_id)"

    USERS {
        int id PK "Auto Increment"
        string name "User Full Name"
        string email UK "Unique Login Email"
        string password "Salted PBKDF2 Hash"
        string salt "Cryptographic Salt"
        string phone "Phone Number"
        string role "ADMIN / EMPLOYER / JOB_SEEKER"
        string status "ACTIVE / PENDING / DEACTIVATED"
        string skills "CSV of Seeker Skills"
        string default_resume_path "Saved Resume Filename"
    }

    JOBS {
        int id PK "Auto Increment"
        int employer_id FK "References users(id)"
        string title "Job Position Title"
        text description "Detailed Job Description"
        text requirements "Key Skills & Qualifications"
        double salary "Annual Salary USD"
        string location "City / Remote"
        string job_type "Full Time / Part Time / Contract"
        string status "OPEN / APPROVED / PENDING / REJECTED / CLOSED"
        date created_at "Posting Date"
        date deadline "Expiry Deadline"
    }

    APPLICATIONS {
        int id PK "Auto Increment"
        int job_id FK "References jobs(id)"
        int job_seeker_id FK "References users(id)"
        string resume_path "Uploaded Resume Document"
        string status "SUBMITTED / SHORTLISTED / APPROVED / REJECTED / WITHDRAWN"
        date applied_date "Submission Date"
        date status_updated_at "Last Status Transition"
        string notes "Reviewer Notes & Feedback"
    }

    STATUS_HISTORY {
        int id PK "Auto Increment"
        int application_id FK "References applications(id)"
        string status "Audited Status"
        timestamp changed_at "Timestamp of Change"
        string notes "Audit Note"
    }

    NOTIFICATIONS {
        int id PK "Auto Increment"
        int user_id FK "References users(id)"
        string title "Notification Title"
        text message "Body Content"
        boolean is_read "Read Status Flag"
        timestamp created_at "Delivery Timestamp"
    }

    MESSAGES {
        int id PK "Auto Increment"
        int sender_id FK "References users(id)"
        int receiver_id FK "References users(id)"
        text content "Message Body"
        datetime sent_at "Sent Timestamp"
    }

    ACTIVITY_LOGS {
        int id PK "Auto Increment"
        int user_id "User ID"
        string user_name "User Display Name"
        string action "Action Code (e.g. LOGIN, JOB_APPROVE)"
        text details "Audit Details"
        timestamp timestamp "Event Timestamp"
    }

    SETTINGS {
        string key_name PK "Setting Name Key"
        string key_value "Configuration Value"
        string description "Setting Documentation"
        timestamp updated_at "Last Modified Timestamp"
    }
```

### Table Indexes for Query Optimization
- `idx_users_email` on `users(email)`
- `idx_jobs_status` on `jobs(status)`
- `idx_jobs_employer` on `jobs(employer_id)`
- `idx_applications_job` on `applications(job_id)`
- `idx_applications_seeker` on `applications(job_seeker_id)`
- `idx_notifications_user` on `notifications(user_id)`
