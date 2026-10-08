package com.example.Online_job_portal.service;

import com.example.Online_job_portal.model.Job;
import com.example.Online_job_portal.model.JobRecommendation;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RecommendationService {

    private final JobService jobService;

    public RecommendationService(JobService jobService) {
        this.jobService = jobService;
    }

    /**
     * Legacy location-based recommendation maintained for backward compatibility.
     */
    public List<Job> recommendJobs(String location) {
        if (location == null || location.isBlank()) {
            return jobService.getAllActiveJobs();
        }
        return jobService.getAllJobs().stream()
                .filter(job -> job.isActive() && job.getLocation() != null && job.getLocation().equalsIgnoreCase(location))
                .toList();
    }

    /**
     * Skill-based recommendation logic:
     * 1. Tokenizes job seeker's skills into normalized keywords.
     * 2. Inspects each job's requirements, title, and description for matching keywords.
     * 3. Calculates match score based on count of matched skills.
     * 4. Sorts recommendations in descending order using a Comparator.
     */
    public List<JobRecommendation> recommendJobsBySkills(String seekerSkills) {
        List<Job> allJobs = jobService.getAllActiveJobs();

        if (seekerSkills == null || seekerSkills.isBlank()) {
            // If no skills specified, return all active jobs with score 0
            return allJobs.stream()
                    .map(job -> new JobRecommendation(job, 0, Collections.emptyList(), "Explore open position"))
                    .toList();
        }

        List<String> rawSkills = Arrays.stream(seekerSkills.split("[,;\\n]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        List<JobRecommendation> recommendations = new ArrayList<>();

        for (Job job : allJobs) {
            String combinedJobText = ((job.getTitle() != null ? job.getTitle() : "") + " "
                    + (job.getRequirements() != null ? job.getRequirements() : "") + " "
                    + (job.getDescription() != null ? job.getDescription() : "")).toLowerCase();

            List<String> matchedSkills = new ArrayList<>();
            for (String skill : rawSkills) {
                if (combinedJobText.contains(skill.toLowerCase())) {
                    matchedSkills.add(skill);
                }
            }

            int score = matchedSkills.size();
            String explanation = score > 0
                    ? score + " matching skills (" + String.join(", ", matchedSkills) + ")"
                    : "No specific skill match found";

            recommendations.add(new JobRecommendation(job, score, matchedSkills, explanation));
        }

        // Sort by match score descending, then by salary descending
        Comparator<JobRecommendation> scoreComparator = Comparator
                .comparingInt(JobRecommendation::getMatchScore).reversed()
                .thenComparing(Comparator.comparingDouble((JobRecommendation r) -> r.getJob().getSalary()).reversed());

        recommendations.sort(scoreComparator);
        return recommendations;
    }
}
