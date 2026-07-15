package com.oneil.wellness.walkplanner.recommendation.model;

public enum WalkingRating {
    EXCELLENT("Excellent"),
    GREAT("Great"),
    GOOD("Good"),
    FAIR("Fair"),
    POOR("Poor"),
    NOT_RECOMMENDED("Not Recommended");

    private final String label;

    WalkingRating(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static WalkingRating fromScore(int score) {
        if (score >= 90) {
            return EXCELLENT;
        }
        if (score >= 75) {
            return GREAT;
        }
        if (score >= 60) {
            return GOOD;
        }
        if (score >= 40) {
            return FAIR;
        }
        if (score >= 20) {
            return POOR;
        }
        return NOT_RECOMMENDED;
    }
}
