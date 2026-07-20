package com.oneil.wellness.walkplanner.recommendation.dto;

public record RecommendationPreferencesDto(
        Integer walkDurationMinutes,
        PreferredTimeOfDay preferredTimeOfDay,
        TemperaturePreference temperaturePreference,
        RainTolerance rainTolerance,
        WindTolerance windTolerance,
        Integer minimumScore,
        UnitSystem unitSystem) {

    private static final int DEFAULT_DURATION_MINUTES = 30;
    private static final int DEFAULT_MINIMUM_SCORE = 60;

    public static RecommendationPreferencesDto defaults() {
        return new RecommendationPreferencesDto(
                DEFAULT_DURATION_MINUTES,
                PreferredTimeOfDay.ANY,
                TemperaturePreference.BALANCED,
                RainTolerance.LIGHT_RAIN_OK,
                WindTolerance.MODERATE,
                DEFAULT_MINIMUM_SCORE,
                UnitSystem.US);
    }

    public RecommendationPreferencesDto normalized() {
        RecommendationPreferencesDto defaults = defaults();
        return new RecommendationPreferencesDto(
                validDuration(walkDurationMinutes) ? walkDurationMinutes : defaults.walkDurationMinutes,
                preferredTimeOfDay != null ? preferredTimeOfDay : defaults.preferredTimeOfDay,
                temperaturePreference != null ? temperaturePreference : defaults.temperaturePreference,
                rainTolerance != null ? rainTolerance : defaults.rainTolerance,
                windTolerance != null ? windTolerance : defaults.windTolerance,
                validMinimumScore(minimumScore) ? minimumScore : defaults.minimumScore,
                unitSystem != null ? unitSystem : defaults.unitSystem);
    }

    private boolean validDuration(Integer value) {
        return value != null && (value == 10 || value == 15 || value == 20 || value == 30 || value == 45 || value == 60);
    }

    private boolean validMinimumScore(Integer value) {
        return value != null && value >= 0 && value <= 100;
    }
}
