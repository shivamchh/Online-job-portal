# JobSphere - Modern Enterprise Online Job Portal

[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Build Status](https://img.shields.io/badge/Tests-41%20Passed-success.svg)]()
[![License](https://img.shields.io/badge/License-MIT-blue.svg)]()

**JobSphere** is a high-performance recruitment and job search web platform built with **Java 17, Spring Boot 3, Java Servlets, JDBC, and Thymeleaf**. It follows enterprise design patterns, multi-layer security countermeasures, thread-safe asynchronous concurrency models, and an academic evaluation rubric covering Core Java, JDBC transactions, Java Servlets, and Object-Oriented Programming.

---

## 📋 Evaluation Rubric Alignment & Features Mapping

| Rubric Evaluation Category | Weightage | Architecture Implementation in JobSphere |
| :--- | :---: | :--- |
| **Problem Understanding & Solution Design** | **8 Marks** | Complete end-to-end recruitment lifecycle: employer job postings, deadline filters, applicant review, secure resume download, multi-step application status pipeline, direct messaging, live platform activity stream, and visual Chart.js analytics. |
| **Core Java & OOP Implementation** | **10 Marks** | • **Inheritance & Polymorphism**: Abstract base class `User` extended by `Admin`, `Employer`, and `JobSeeker`, each implementing specialized polymorphic behaviors like `showDashboard()`.<br>• **Strongly-Typed Enums**: Domain-specific enums `Role`, `JobStatus` (segregated moderation vs lifecycle), and `ApplicationStatus`.<br>• **Interfaces & Generics**: Generic repository interface `DAO<T>` and `JobOperations`.<br>• **Collections & Comparable**: `Job implements Comparable<Job>` for natural ordering by creation date; skill-matching engine using `Set<String>`, `Map`, and custom score `Comparator`.<br>• **Checked Exceptions**: Custom checked exceptions `ApplicationException`, `JobNotFoundException`, `UserNotFoundException` (all extend `Exception`).<br>• **Multithreading & Concurrency**: `ActivityLogger` using Producer-Consumer pattern with `LinkedBlockingQueue` and dedicated background worker thread; `ThreadManager` with `ExecutorService`, `ScheduledExecutorService`, and `Callable`/`Future` tasks. |
| **Database Integration (JDBC)** | **8 Marks** | • **ACID Transactions**: Atomic 3-step operations with `connection.setAutoCommit(false)`, `connection.commit()`, and `connection.rollback()` in `ApplicationDAO.saveWithTransaction()` / `executeTransaction()`.<br>• **Batch Inserts**: `addBatch()` and `executeBatch()` for seeding initial records in `DataInitializer`.<br>• **Performance Indexes**: High-performance indexes on `users(email)`, `jobs(status)`, `jobs(employer_id)`, and `applications(job_id)` in `schema.sql`.<br>• **PreparedStatement Security**: 100% parameterized queries guarding against SQL injection; automatic fallback memory mode for zero-downtime offline evaluation. |
| **Servlets & Web Integration** | **7 Marks** | • **Two-Layer Architecture**: Strict separation between GET presentation (`@Controller` classes) and state-mutating actions (POST handled by 13 dedicated `@WebServlet` classes under `/servlet/*`).<br>• **Ordered Filter Chain**: (0) `RequestLoggingFilter` -> (1) `SecurityHeadersFilter` -> (2) `AuthFilter` -> (3) `CsrfFilter` covering `/*` and `/servlet/*`.<br>• **Web Listeners**: `ActiveSessionListener` (`HttpSessionListener` tracking live users online in `listener/`), `AppContextListener` (`ServletContextListener` lifecycle cleanup in `listener/`).<br>• **Session Fixation Countermeasure**: `request.changeSessionId()` called immediately upon authenticated login.<br>• **Live Charts**: Real-time Chart.js visual analytics fed dynamically via `StatisticsServlet`. |

---

## 🏛️ Architecture: Two-Layer Design Rule

To guarantee clean separation of concerns and withstand rigorous evaluation:
1. **Presentation Layer (`com.example.Online_job_portal.controller`)**:
   - Spring MVC `@Controller` classes only respond to HTTP `GET` requests to render Thymeleaf views or stream read-only file downloads (e.g. CSV export).
   - Zero state-mutating operations exist in controllers.
2. **Action Layer (`com.example.Online_job_portal.servlet`)**:
   - Custom Java Servlets mapped under `/servlet/*` process all HTTP `POST` mutations (login, register, job approval/rejection, job creation, application submission, status transitions, withdrawals, profile updates).
   - All state-changing requests are protected by session authentication and cryptographic CSRF verification.

---

## 🏷️ Domain Model: JobStatus Segregation

The `JobStatus` enum cleanly separates two independent concerns:
* **Admin Moderation States**:
  * `PENDING`: Newly posted job awaiting administrative review.
  * `APPROVED`: Listing reviewed and approved by administrator (now live).
  * `REJECTED`: Listing rejected during administrative moderation.
* **Employer Lifecycle States**:
  * `OPEN`: Active listing accepting new candidate submissions.
  * `CLOSED`: Vacancy filled or intake suspended by employer.

---

## 👥 Demo Logins

For demo and grading evaluation, use the following pre-seeded test accounts:

| Role | Email Address | Password | Permissions & Dashboard |
| :--- | :--- | :--- | :--- |
| **Platform Administrator** | `admin@portal.com` | `admin123` | Moderate jobs, review users, live session monitoring, system activity audit |
| **Hiring Employer** | `employer@techcorp.com` | `employer123` | Post vacancies, review applicants, download resumes, update status, CSV export |
| **Job Seeker (Candidate)** | `seeker@example.com` | `seeker123` | Browse jobs, submit application, track visual timeline, withdraw application |

---

## 🚀 Quick Start Guide

### Option 1: One-Command Setup with Docker Compose (Recommended)
Launch MySQL 8 with pre-configured schemas and indexes:
```bash
docker-compose up -d
```
Run the application using the Maven wrapper:
```bash
./mvnw spring-boot:run
```

### Option 2: Local MySQL Setup
1. Ensure MySQL is running on `localhost:3306`.
2. Create the database schema:
   ```bash
   mysql -u root -p < src/main/resources/schema.sql
   ```
3. Update database credentials in `src/main/resources/application.properties` if needed.
4. Start the application:
   ```bash
   ./mvnw spring-boot:run
   ```

The application will be accessible at: **`http://localhost:8080`**

---

## 🛡️ Enterprise Security Implementation

* **Slow Salted Password Hashing**: Passwords stored using PBKDF2 (`PBKDF2WithHmacSHA256`) with 65,536 iterations and unique 16-byte cryptographic salts (`PasswordUtil.java`).
* **Session Fixation Prevention**: Immediate call to `request.changeSessionId()` on sign-in.
* **Brute-Force Rate Limiting**: Exponential cooldown and temporary 15-minute account lockout after 5 consecutive failed attempts (`LoginAttemptService.java`).
* **CSRF Token Verification**: Every state-changing form submission requires a validated cryptographically-random session token via `CsrfFilter`.
* **Security Headers**: Standard CSP, HSTS, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff` via `SecurityHeadersFilter`.
* **MIME & File Signature Sanitizer**: Uploaded files inspected for real magic bytes (`%PDF-`, `PK\x03\x04`), restricting arbitrary script execution.

---

## 🧪 Automated Testing

The project includes **41 automated tests** across 8 test classes verifying security, polymorphism, JDBC transactions, and core business rules:
```bash
./mvnw test
```
All 41 tests execute cleanly with `BUILD SUCCESS`:
1. `ApplicationDAOTest`: JDBC ACID transaction commit and rollback verification.
2. `JobDAOTest`: CRUD persistence operations.
3. `UserDAOTest`: User repository tests.
4. `UserHierarchyPolymorphismTest`: Inheritance and polymorphic dashboard dispatch.
5. `JobServiceTest`: Job filtering and business rules.
6. `JobPortalCoreFeaturesTest`: Application workflow, withdrawal, status timeline, and recommendations.
7. `SecurityAndRequestAuditTest`: RBAC authorization, CSRF protection, password hashing, and security headers.
8. `OnlineJobPortalApplicationTests`: Spring Boot application context load.

---

## 📂 Documentation & Architecture Assets
* **Entity-Relationship Diagram**: [docs/ER_DIAGRAM.md](docs/ER_DIAGRAM.md)
* **UML Class Diagram**: [docs/UML_CLASS_DIAGRAM.md](docs/UML_CLASS_DIAGRAM.md)
* **Postman / Bruno API Collection**: [docs/POSTMAN_COLLECTION.json](docs/POSTMAN_COLLECTION.json)
* **Application Screenshots**:
  * [Admin Dashboard & Live Counters](docs/screenshots/admin-dashboard.png)
  * [Admin Job Moderation & Approval](docs/screenshots/admin-job-moderation.png)
  * [Employer Dashboard & Vacancy Manager](docs/screenshots/employer-dashboard.png)
  * [Job Seeker Applications & Timeline](docs/screenshots/jobseeker-applications.png)
  * [Job Seeker Discovery Dashboard](docs/screenshots/jobseeker-dashboard.png)
  * [Secure Login Page](docs/screenshots/login-page.png)
  * [Job Inspection Modal](docs/screenshots/modal-inspection.png)
