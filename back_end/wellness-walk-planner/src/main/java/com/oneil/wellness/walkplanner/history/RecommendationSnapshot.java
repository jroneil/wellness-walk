package com.oneil.wellness.walkplanner.history;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

public record RecommendationSnapshot(Instant timestamp, int score, OffsetDateTime recommendedStart,
        OffsetDateTime recommendedEnd, Double temperature, Double wind, Double humidity, Double aqi, Double uv,
        boolean calendarAvailable, List<String> providerSources, String reasonSummary) {
    public RecommendationSnapshot {
        timestamp = timestamp == null ? Instant.now() : timestamp;
        providerSources = providerSources == null ? List.of() : providerSources.stream().filter(v -> v != null && !v.isBlank()).distinct().sorted().toList();
        reasonSummary = reasonSummary == null || reasonSummary.isBlank() ? "Recommendation updated." : reasonSummary.trim();
    }
}
