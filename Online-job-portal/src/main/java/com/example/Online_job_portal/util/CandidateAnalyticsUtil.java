package com.example.Online_job_portal.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility for analyzing candidate skill sets, calculating match percentages,
 * and identifying skill gap matrices for targeted job recommendations.
 *
 * @author Saunak Sharma
 */
public class CandidateAnalyticsUtil {

    private CandidateAnalyticsUtil() {
        // Utility class
    }

    /**
     * Splits comma- or space-separated skill strings into a clean, normalized lowercase set.
     */
    public static Set<String> tokenizeSkills(String skillsCsv) {
        if (skillsCsv == null || skillsCsv.trim().isEmpty()) {
            return new HashSet<>();
        }
        return Arrays.stream(skillsCsv.split("[,;/]"))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Calculates the percentage (0.0 to 100.0) of required skills matched by candidate skills.
     */
    public static double calculateMatchPercentage(String candidateSkillsCsv, String requiredSkillsCsv) {
        Set<String> required = tokenizeSkills(requiredSkillsCsv);
        if (required.isEmpty()) {
            return 100.0;
        }

        Set<String> candidate = tokenizeSkills(candidateSkillsCsv);
        long matchedCount = required.stream().filter(candidate::contains).count();
        return Math.round(((double) matchedCount / required.size()) * 1000.0) / 10.0;
    }

    /**
     * Identifies skills required by the job that are missing from the candidate's profile.
     */
    public static List<String> identifySkillGaps(String candidateSkillsCsv, String requiredSkillsCsv) {
        Set<String> candidate = tokenizeSkills(candidateSkillsCsv);
        Set<String> required = tokenizeSkills(requiredSkillsCsv);

        List<String> gaps = new ArrayList<>();
        for (String req : required) {
            if (!candidate.contains(req)) {
                gaps.add(req);
            }
        }
        return gaps;
    }
}
