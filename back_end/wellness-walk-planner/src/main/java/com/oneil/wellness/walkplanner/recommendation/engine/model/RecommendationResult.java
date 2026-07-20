package com.oneil.wellness.walkplanner.recommendation.engine.model;

public record RecommendationResult(
        CandidateWindow selected,
        CandidateWindow idealWeather,
        RecommendationExplanation explanation,
        String noAvailableReason) {
}
