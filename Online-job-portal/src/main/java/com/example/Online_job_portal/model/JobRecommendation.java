package com.example.Online_job_portal.model;

import java.util.ArrayList;
import java.util.List;

public class JobRecommendation {

    private Job job;
    private int matchScore;
    private List<String> matchedSkills = new ArrayList<>();
    private String explanation;

    public JobRecommendation() {
    }

    public JobRecommendation(Job job, int matchScore, List<String> matchedSkills, String explanation) {
        this.job = job;
        this.matchScore = matchScore;
        this.matchedSkills = matchedSkills != null ? matchedSkills : new ArrayList<>();
        this.explanation = explanation;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    public List<String> getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(List<String> matchedSkills) {
        this.matchedSkills = matchedSkills;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
