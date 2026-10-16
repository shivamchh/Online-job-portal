# Online Job Portal — System Architecture & Research Analysis

**Author:** Saunak Sharma  
**Project:** Online Job Portal  
**Primary Developer:** Shivam Chh  
**Co-Contributor & Research Analyst:** Saunak Sharma  

---

## 1. Executive Summary & Research Scope

The Online Job Portal is a Spring Boot web application designed to connect job seekers with employers through an efficient, scalable, and responsive interface. This document records the architectural analysis, algorithmic design, and research considerations behind the platform's core services, with special focus on:

1. **Intelligent Candidate-to-Job Matching Engine**
2. **Persistence Architecture & Multi-Mode High Availability**
3. **Session & Security Threat Modeling**
4. **Performance & Scalability Analysis under Concurrency**

---

## 2. Recommendation Algorithm Formulation

### 2.1 Problem Definition
Traditional keyword matching causes high false-positive rates due to varying skill nomenclatures (e.g., "Java", "Core Java", "J2EE"). Our recommendation module implements a normalized multi-factor scoring model.

### 2.2 Mathematical Model
Let candidate skills be represented as a set $S_c = \{s_{c1}, s_{c2}, \dots, s_{cm}\}$ and job requirement tokens as $S_j = \{s_{j1}, s_{j2}, \dots, s_{jn}\}$.

1. **Tokenization & Normalization:**
   $$\bar{S} = \{ \text{trim}(\text{lower}(s)) \mid s \in S, s \neq \emptyset \}$$

2. **Intersection Cardinality (Raw Match Score):**
   $$M(S_c, S_j) = |\bar{S}_c \cap \bar{S}_j|$$

3. **Match Percentage:**
   $$P(S_c, S_j) = \begin{cases} 100.0, & \text{if } |\bar{S}_j| = 0 \\ \frac{|\bar{S}_c \cap \bar{S}_j|}{|\bar{S}_j|} \times 100, & \text{otherwise} \end{cases}$$

4. **Skill Gap Vector:**
   $$G(S_c, S_j) = \bar{S}_j \setminus \bar{S}_c$$

5. **Composite Ranking Criterion:**
   Recommendations are ranked using a lexicographical comparator tuple:
   $$\text{Rank}(R) = \langle -M(S_c, S_j), -\text{Salary}(j), \text{PostedDate}(j) \rangle$$

---

## 3. Storage Layer Reliability Architecture

The portal features a dual-mode persistence architecture:
- **Primary:** MySQL relational storage via Hikari connection pooling.
- **Failover / Offline:** Resilient in-memory thread-safe `ConcurrentHashMap` structures ensuring 100% service uptime during database maintenance or localized connection loss.

```
+--------------------------------------------------------------+
|                    Application Service Layer                 |
+--------------------------------------------------------------+
                               |
                               v
               +-------------------------------+
               |     DBConnection Manager      |
               |  (Health probe: 2s timeout)   |
               +-------------------------------+
                      /                 \
        [Active Link] v                 v [Fallback Mode]
+--------------------------+       +--------------------------+
|  MySQL 8.0 Relational DB |       | In-Memory ConcurrentStore|
|  (ACID Compliant)        |       | (High Availability)      |
+--------------------------+       +--------------------------+
```

---

## 4. Security & Authentication Audit

1. **Password Hashing:** SHA-256 with unique salt generation (`PasswordUtil`).
2. **Access Control:** Role-Based Access Control (`ROLE_JOBSEEKER`, `ROLE_EMPLOYER`, `ROLE_ADMIN`) enforced via filter intercepts.
3. **Session Fixation Prevention:** Session invalidation on sign-out with clean attribute teardown.

---

## 5. Developer Preflight & Diagnostic Tools

To streamline local onboarding and multi-developer collaboration, automated diagnostic tooling (`scripts/verify-environment.bat`) was engineered to inspect:
- Java JDK 17+ availability
- Maven environment and wrapper health
- MySQL local service status (`MySQL80`)
- Port 8080 collision checks
