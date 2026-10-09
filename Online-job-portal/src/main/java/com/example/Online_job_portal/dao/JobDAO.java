package com.example.Online_job_portal.dao;

import com.example.Online_job_portal.config.DBConnection;
import com.example.Online_job_portal.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class JobDAO implements DAO<Job> {

    private static final Logger log = LoggerFactory.getLogger(JobDAO.class);

    private final List<Job> jobs = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(10);
    private boolean useMemoryStore;

    public JobDAO() {
    }

    private static boolean isStrictDbMode() {
        return "true".equalsIgnoreCase(System.getenv("STRICT_DB_MODE"))
                || "true".equalsIgnoreCase(System.getProperty("STRICT_DB_MODE"));
    }

    private void handleSqlException(String operation, SQLException e) {
        log.error("Database SQL failure during [{}] in JobDAO: {}", operation, e.getMessage(), e);
        if (isStrictDbMode()) {
            throw new RuntimeException("Database operation [" + operation + "] failed in JobDAO", e);
        }
        useMemoryStore = true;
    }

    public synchronized void seedInitialJobs() {
        if (!jobs.isEmpty()) return;
        jobs.add(new Job(1, 2, "Senior Java Developer",
                "Develop scalable cloud services, enterprise applications, and secure REST APIs using Java 17 and Spring Boot.",
                "Java, Spring Boot, MySQL, REST API, Microservices", 85000.0, "Bangalore", "Full Time", "OPEN", LocalDate.now().minusDays(10)));

        jobs.add(new Job(2, 2, "Frontend React Engineer",
                "Build modern, high-performance responsive web applications using React and Bootstrap.",
                "JavaScript, React, Bootstrap, HTML5, CSS3", 65000.0, "Pune", "Full Time", "OPEN", LocalDate.now().minusDays(8)));

        jobs.add(new Job(3, 2, "Full Stack Java Specialist",
                "Lead end-to-end web engineering projects integrating Spring Boot backends with interactive web clients.",
                "Java, Spring Boot, React, MySQL, Git", 75000.0, "Delhi", "Remote", "OPEN", LocalDate.now().minusDays(5)));

        jobs.add(new Job(4, 2, "QA Automation Engineer",
                "Design and maintain automated testing suites using Selenium, JUnit, and Java.",
                "Java, Selenium, JUnit, TestNG, SQL", 55000.0, "Hyderabad", "Full Time", "OPEN", LocalDate.now().minusDays(3)));

        jobs.add(new Job(5, 2, "DevOps & Cloud Engineer",
                "Implement CI/CD deployment automation pipelines and monitor cloud infrastructure.",
                "AWS, Docker, Kubernetes, CI/CD, Linux", 90000.0, "Delhi", "Contract", "OPEN", LocalDate.now().minusDays(1)));
    }

    @Override
    public void save(Job job) {
        if (useMemoryStore || !DBConnection.isAvailable()) {
            saveInMemory(job);
            return;
        }

        String sql = """
                INSERT INTO jobs (employer_id, title, description, requirements, salary, location, job_type, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, job.getEmployerId());
            statement.setString(2, job.getTitle());
            statement.setString(3, job.getDescription());
            statement.setString(4, job.getRequirements());
            statement.setDouble(5, job.getSalary());
            statement.setString(6, job.getLocation());
            statement.setString(7, job.getJobType());
            statement.setString(8, job.getStatus() != null ? job.getStatus() : "OPEN");
            statement.setDate(9, Date.valueOf(job.getCreatedAt() != null ? job.getCreatedAt() : LocalDate.now()));
            statement.executeUpdate();

            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    job.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            handleSqlException("save", e);
            saveInMemory(job);
        }
    }

    private synchronized void saveInMemory(Job job) {
        if (job.getId() <= 0) {
            job.setId(idCounter.incrementAndGet());
        }
        if (job.getCreatedAt() == null) {
            job.setCreatedAt(LocalDate.now());
        }
        jobs.removeIf(j -> j.getId() == job.getId());
        jobs.add(0, job);
    }

    @Override
    public Optional<Job> findById(int id) {
        if (useMemoryStore || !DBConnection.isAvailable()) {
            return jobs.stream()
                    .filter(job -> job.getId() == id)
                    .findFirst();
        }

        String sql = "SELECT * FROM jobs WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapJob(resultSet));
                }
            }
        } catch (SQLException e) {
            handleSqlException("findById", e);
            return jobs.stream()
                    .filter(job -> job.getId() == id)
                    .findFirst();
        }

        return Optional.empty();
    }

    @Override
    public List<Job> findAll() {
        if (useMemoryStore || !DBConnection.isAvailable()) {
            return new ArrayList<>(jobs);
        }

        String sql = "SELECT * FROM jobs ORDER BY id DESC";
        List<Job> result = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                result.add(mapJob(resultSet));
            }

            return result;
        } catch (SQLException e) {
            useMemoryStore = true;
            return new ArrayList<>(jobs);
        }
    }

    public List<Job> findAllActive() {
        return findAll().stream()
                .filter(Job::isActive)
                .toList();
    }

    public List<Job> findByEmployerId(int employerId) {
        if (useMemoryStore || !DBConnection.isAvailable()) {
            return jobs.stream()
                    .filter(job -> job.getEmployerId() == employerId)
                    .toList();
        }

        String sql = "SELECT * FROM jobs WHERE employer_id = ? ORDER BY id DESC";
        List<Job> result = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, employerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(mapJob(resultSet));
                }
            }

            return result;
        } catch (SQLException e) {
            useMemoryStore = true;
            return jobs.stream()
                    .filter(job -> job.getEmployerId() == employerId)
                    .toList();
        }
    }

    public List<Job> searchJobs(String keyword, String location, String jobType, Double minSalary, int offset, int limit) {
        if (useMemoryStore) {
            return jobs.stream()
                    .filter(Job::isActive)
                    .filter(j -> keyword == null || keyword.isBlank() ||
                            (j.getTitle() != null && j.getTitle().toLowerCase().contains(keyword.toLowerCase())) ||
                            (j.getDescription() != null && j.getDescription().toLowerCase().contains(keyword.toLowerCase())) ||
                            (j.getRequirements() != null && j.getRequirements().toLowerCase().contains(keyword.toLowerCase())))
                    .filter(j -> location == null || location.isBlank() ||
                            (j.getLocation() != null && j.getLocation().toLowerCase().contains(location.toLowerCase())))
                    .filter(j -> jobType == null || jobType.isBlank() ||
                            (j.getJobType() != null && j.getJobType().equalsIgnoreCase(jobType)))
                    .filter(j -> minSalary == null || j.getSalary() >= minSalary)
                    .skip(Math.max(0, offset))
                    .limit(limit > 0 ? limit : 10)
                    .toList();
        }

        String sql = """
                SELECT * FROM jobs
                WHERE status IN ('OPEN', 'APPROVED')
                  AND (? IS NULL OR ? = '' OR LOWER(title) LIKE ? OR LOWER(description) LIKE ? OR LOWER(requirements) LIKE ?)
                  AND (? IS NULL OR ? = '' OR LOWER(location) LIKE ?)
                  AND (? IS NULL OR ? = '' OR LOWER(job_type) = LOWER(?))
                  AND (? IS NULL OR salary >= ?)
                ORDER BY id DESC
                LIMIT ? OFFSET ?
                """;

        List<Job> result = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            String kwLike = (keyword != null && !keyword.isBlank()) ? "%" + keyword.trim().toLowerCase() + "%" : "";
            statement.setString(1, keyword);
            statement.setString(2, keyword);
            statement.setString(3, kwLike);
            statement.setString(4, kwLike);
            statement.setString(5, kwLike);

            String locLike = (location != null && !location.isBlank()) ? "%" + location.trim().toLowerCase() + "%" : "";
            statement.setString(6, location);
            statement.setString(7, location);
            statement.setString(8, locLike);

            statement.setString(9, jobType);
            statement.setString(10, jobType);
            statement.setString(11, jobType);

            if (minSalary != null) {
                statement.setDouble(12, minSalary);
                statement.setDouble(13, minSalary);
            } else {
                statement.setNull(12, Types.DOUBLE);
                statement.setDouble(13, 0.0);
            }

            statement.setInt(14, limit > 0 ? limit : 10);
            statement.setInt(15, Math.max(0, offset));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(mapJob(resultSet));
                }
            }
            return result;
        } catch (SQLException e) {
            useMemoryStore = true;
            return searchJobs(keyword, location, jobType, minSalary, offset, limit);
        }
    }

    public int countSearchJobs(String keyword, String location, String jobType, Double minSalary) {
        if (useMemoryStore) {
            return (int) jobs.stream()
                    .filter(Job::isActive)
                    .filter(j -> keyword == null || keyword.isBlank() ||
                            (j.getTitle() != null && j.getTitle().toLowerCase().contains(keyword.toLowerCase())) ||
                            (j.getDescription() != null && j.getDescription().toLowerCase().contains(keyword.toLowerCase())) ||
                            (j.getRequirements() != null && j.getRequirements().toLowerCase().contains(keyword.toLowerCase())))
                    .filter(j -> location == null || location.isBlank() ||
                            (j.getLocation() != null && j.getLocation().toLowerCase().contains(location.toLowerCase())))
                    .filter(j -> jobType == null || jobType.isBlank() ||
                            (j.getJobType() != null && j.getJobType().equalsIgnoreCase(jobType)))
                    .filter(j -> minSalary == null || j.getSalary() >= minSalary)
                    .count();
        }

        String sql = """
                SELECT COUNT(*) FROM jobs
                WHERE status IN ('OPEN', 'APPROVED')
                  AND (? IS NULL OR ? = '' OR LOWER(title) LIKE ? OR LOWER(description) LIKE ? OR LOWER(requirements) LIKE ?)
                  AND (? IS NULL OR ? = '' OR LOWER(location) LIKE ?)
                  AND (? IS NULL OR ? = '' OR LOWER(job_type) = LOWER(?))
                  AND (? IS NULL OR salary >= ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            String kwLike = (keyword != null && !keyword.isBlank()) ? "%" + keyword.trim().toLowerCase() + "%" : "";
            statement.setString(1, keyword);
            statement.setString(2, keyword);
            statement.setString(3, kwLike);
            statement.setString(4, kwLike);
            statement.setString(5, kwLike);

            String locLike = (location != null && !location.isBlank()) ? "%" + location.trim().toLowerCase() + "%" : "";
            statement.setString(6, location);
            statement.setString(7, location);
            statement.setString(8, locLike);

            statement.setString(9, jobType);
            statement.setString(10, jobType);
            statement.setString(11, jobType);

            if (minSalary != null) {
                statement.setDouble(12, minSalary);
                statement.setDouble(13, minSalary);
            } else {
                statement.setNull(12, Types.DOUBLE);
                statement.setDouble(13, 0.0);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            useMemoryStore = true;
            return countSearchJobs(keyword, location, jobType, minSalary);
        }
        return 0;
    }

    public void setJobStatus(int id, String status) {
        if (useMemoryStore) {
            findById(id).ifPresent(j -> j.setStatus(status));
            return;
        }

        String sql = "UPDATE jobs SET status = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            useMemoryStore = true;
            findById(id).ifPresent(j -> j.setStatus(status));
        }
    }

    public void updateJobDetails(int id, String title, String description, String requirements, double salary, String location, String jobType) {
        findById(id).ifPresent(j -> {
            j.setTitle(title);
            j.setDescription(description);
            j.setRequirements(requirements);
            j.setSalary(salary);
            j.setLocation(location);
            j.setJobType(jobType);
        });

        if (useMemoryStore) {
            return;
        }

        String sql = "UPDATE jobs SET title = ?, description = ?, requirements = ?, salary = ?, location = ?, job_type = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.setString(2, description);
            statement.setString(3, requirements);
            statement.setDouble(4, salary);
            statement.setString(5, location);
            statement.setString(6, jobType);
            statement.setInt(7, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            useMemoryStore = true;
        }
    }

    public void toggleJobStatus(int id) {
        Optional<Job> opt = findById(id);
        if (opt.isPresent()) {
            Job j = opt.get();
            String newStatus = "OPEN".equalsIgnoreCase(j.getStatus()) ? "CLOSED" : "OPEN";
            setJobStatus(id, newStatus);
        }
    }

    @Override
    public void deleteById(int id) {
        // Soft delete: sets status to INACTIVE so past applications are preserved
        setJobStatus(id, "INACTIVE");
    }

    public void hardDeleteById(int id) {
        if (useMemoryStore) {
            jobs.removeIf(job -> job.getId() == id);
            return;
        }

        String sql = "DELETE FROM jobs WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            useMemoryStore = true;
            jobs.removeIf(job -> job.getId() == id);
        }
    }

    private Job mapJob(ResultSet resultSet) throws SQLException {
        Date createdDate = null;
        try {
            createdDate = resultSet.getDate("created_at");
        } catch (SQLException ignored) {
        }

        LocalDate createdAt = createdDate != null ? createdDate.toLocalDate() : LocalDate.now();

        return new Job(
                resultSet.getInt("id"),
                resultSet.getInt("employer_id"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("requirements"),
                resultSet.getDouble("salary"),
                resultSet.getString("location"),
                resultSet.getString("job_type"),
                resultSet.getString("status"),
                createdAt
        );
    }
}
