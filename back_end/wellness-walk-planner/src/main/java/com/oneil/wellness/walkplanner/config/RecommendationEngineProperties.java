package com.oneil.wellness.walkplanner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "recommendation.engine")
public class RecommendationEngineProperties {
    private int candidateIntervalMinutes = 15;
    private int defaultWalkDurationMinutes = 30;
    private int minimumScore = 60;
    private int weatherWeight = 72;
    private int availabilityWeight = 20;
    private int preferenceWeight = 8;

    public int getCandidateIntervalMinutes() { return candidateIntervalMinutes; }
    public void setCandidateIntervalMinutes(int value) { candidateIntervalMinutes = value == 30 ? 30 : 15; }
    public int getDefaultWalkDurationMinutes() { return defaultWalkDurationMinutes; }
    public void setDefaultWalkDurationMinutes(int value) { defaultWalkDurationMinutes = value; }
    public int getMinimumScore() { return minimumScore; }
    public void setMinimumScore(int value) { minimumScore = value; }
    public int getWeatherWeight() { return weatherWeight; }
    public void setWeatherWeight(int value) { weatherWeight = value; }
    public int getAvailabilityWeight() { return availabilityWeight; }
    public void setAvailabilityWeight(int value) { availabilityWeight = value; }
    public int getPreferenceWeight() { return preferenceWeight; }
    public void setPreferenceWeight(int value) { preferenceWeight = value; }
}
