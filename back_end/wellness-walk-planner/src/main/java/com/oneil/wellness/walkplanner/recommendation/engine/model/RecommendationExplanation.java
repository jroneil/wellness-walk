package com.oneil.wellness.walkplanner.recommendation.engine.model;

import java.util.List;

public record RecommendationExplanation(String reason, List<String> calendarReasons, List<String> preferenceReasons) {
}
