package com.oneil.wellness.walkplanner.recommendation.service;

import java.time.Clock;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oneil.wellness.walkplanner.dto.HourlyForecastPeriod;
import com.oneil.wellness.walkplanner.recommendation.dto.BestWalkingWindowDto;
import com.oneil.wellness.walkplanner.recommendation.dto.PreferredTimeOfDay;
import com.oneil.wellness.walkplanner.recommendation.dto.RainTolerance;
import com.oneil.wellness.walkplanner.recommendation.dto.RecommendationPreferencesDto;
import com.oneil.wellness.walkplanner.recommendation.dto.TemperaturePreference;
import com.oneil.wellness.walkplanner.recommendation.dto.WindTolerance;
import com.oneil.wellness.walkplanner.recommendation.dto.WalkingRecommendationDto;
import com.oneil.wellness.walkplanner.recommendation.model.ScoredWalkingPeriod;

@Service
public class WalkingRecommendationService {

    private final WalkingScoreService scoreService;
    private final Clock clock;

    @Autowired
    public WalkingRecommendationService(WalkingScoreService scoreService) {
        this(scoreService, Clock.systemDefaultZone());
    }

    WalkingRecommendationService(WalkingScoreService scoreService, Clock clock) {
        this.scoreService = scoreService;
        this.clock = clock;
    }

    public WalkingRecommendationDto recommendationFor(HourlyForecastPeriod period) {
        return toDto(scoreService.score(period));
    }

    public Optional<HourlyForecastPeriod> bestPeriod(List<HourlyForecastPeriod> periods) {
        return bestPeriod(periods, RecommendationPreferencesDto.defaults());
    }

    public Optional<HourlyForecastPeriod> bestPeriod(List<HourlyForecastPeriod> periods, RecommendationPreferencesDto preferences) {
        RecommendationPreferencesDto normalizedPreferences = preferences == null
                ? RecommendationPreferencesDto.defaults()
                : preferences.normalized();
        List<HourlyForecastPeriod> candidates = periods.stream()
                .filter(this::isUpcoming)
                .filter(period -> period.walkingRecommendation() != null && period.walkingRecommendation().score() != null)
                .toList();
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        int topScore = candidates.stream()
                .mapToInt(period -> period.walkingRecommendation().score())
                .max()
                .orElse(0);

        return candidates.stream()
                .filter(period -> isWithinPreferenceWindow(period, topScore, normalizedPreferences))
                .min(Comparator
                        .comparing((HourlyForecastPeriod period) -> preferenceRank(period, normalizedPreferences))
                        .thenComparing((HourlyForecastPeriod period) -> period.walkingRecommendation().score(), Comparator.reverseOrder())
                        .thenComparing(period -> parseStartTime(period.startTime()).map(OffsetDateTime::toInstant).orElse(java.time.Instant.MAX)));
    }

    public BestWalkingWindowDto bestWindow(List<HourlyForecastPeriod> periods) {
        return bestWindow(periods, RecommendationPreferencesDto.defaults());
    }

    public BestWalkingWindowDto bestWindow(List<HourlyForecastPeriod> periods, RecommendationPreferencesDto preferences) {
        RecommendationPreferencesDto normalizedPreferences = preferences == null
                ? RecommendationPreferencesDto.defaults()
                : preferences.normalized();
        return bestPeriod(periods, normalizedPreferences)
                .map(period -> {
                    WalkingRecommendationDto recommendation = period.walkingRecommendation();
                    String endTime = parseStartTime(period.startTime())
                            .map(start -> start.plusMinutes(normalizedPreferences.walkDurationMinutes()).toString())
                            .orElse(null);
                    boolean belowMinimumScore = recommendation.score() < normalizedPreferences.minimumScore();
                    return new BestWalkingWindowDto(
                            period.startTime(),
                            endTime,
                            recommendation.score(),
                            recommendation.rating(),
                            recommendation.ratingLabel(),
                            recommendation.ratingLabel() + " weather for a restorative walk.",
                            recommendation.reasons().stream().limit(3).toList(),
                            recommendation.warnings(),
                            normalizedPreferences.walkDurationMinutes(),
                            preferenceReasons(period, normalizedPreferences),
                            normalizedPreferences.minimumScore(),
                            belowMinimumScore,
                            belowMinimumScore
                                    ? "Best available window is below your minimum score of " + normalizedPreferences.minimumScore() + "."
                                    : null);
                })
                .orElse(null);
    }

    public WalkingRecommendationDto toDto(ScoredWalkingPeriod scored) {
        String label = scored.rating() == null ? "Not enough data" : scored.rating().label();
        return new WalkingRecommendationDto(
                scored.startTime(),
                scored.score(),
                scored.rating(),
                label,
                scored.recommended(),
                scored.temperatureScore(),
                scored.precipitationScore(),
                scored.windScore(),
                scored.humidityScore(),
                scored.daylightScore(),
                scored.airQualityScore(),
                scored.uvScore(),
                scored.feelsLikeTemperature(),
                scored.feelsLikeMethod(),
                scored.reasons(),
                scored.warnings());
    }

    private boolean isUpcoming(HourlyForecastPeriod period) {
        return parseStartTime(period.startTime())
                .map(start -> !start.toInstant().isBefore(clock.instant()))
                .orElse(false);
    }

    private boolean isWithinPreferenceWindow(HourlyForecastPeriod period, int topScore, RecommendationPreferencesDto preferences) {
        int score = period.walkingRecommendation().score();
        if (score == topScore) {
            return true;
        }
        if (preferences.temperaturePreference() == TemperaturePreference.BALANCED) {
            return false;
        }
        return !hasSeriousWarning(period) && topScore - score <= 5;
    }

    private int preferenceRank(HourlyForecastPeriod period, RecommendationPreferencesDto preferences) {
        int rank = 0;
        if (matchesPreferredTime(period, preferences.preferredTimeOfDay())) {
            rank -= 4;
        }
        if (matchesTemperaturePreference(period, preferences.temperaturePreference())) {
            rank -= 3;
        }
        if (matchesRainTolerance(period, preferences.rainTolerance())) {
            rank -= 2;
        }
        if (matchesWindTolerance(period, preferences.windTolerance())) {
            rank -= 1;
        }
        if (hasSeriousWarning(period)) {
            rank += 50;
        }
        return rank;
    }

    private List<String> preferenceReasons(HourlyForecastPeriod period, RecommendationPreferencesDto preferences) {
        List<String> reasons = new ArrayList<>();
        reasons.add(preferences.walkDurationMinutes() + "-minute walk window");
        if (matchesPreferredTime(period, preferences.preferredTimeOfDay())) {
            reasons.add("Matches your preferred time of day");
        }
        if (matchesTemperaturePreference(period, preferences.temperaturePreference())) {
            reasons.add(preferences.temperaturePreference() == TemperaturePreference.COOLER
                    ? "Leans toward cooler conditions"
                    : "Leans toward warmer conditions");
        }
        if (matchesRainTolerance(period, preferences.rainTolerance())) {
            reasons.add("Fits your rain tolerance");
        }
        if (matchesWindTolerance(period, preferences.windTolerance())) {
            reasons.add("Fits your wind tolerance");
        }
        return reasons;
    }

    private boolean matchesPreferredTime(HourlyForecastPeriod period, PreferredTimeOfDay preferredTimeOfDay) {
        if (preferredTimeOfDay == null || preferredTimeOfDay == PreferredTimeOfDay.ANY) {
            return false;
        }
        Optional<LocalTime> startTime = parseStartTime(period.startTime()).map(OffsetDateTime::toLocalTime);
        if (startTime.isEmpty()) {
            return false;
        }
        int hour = startTime.get().getHour();
        return switch (preferredTimeOfDay) {
            case MORNING -> hour >= 6 && hour < 12;
            case AFTERNOON -> hour >= 12 && hour < 17;
            case EVENING -> hour >= 17 && hour < 21;
            case ANY -> false;
        };
    }

    private boolean matchesTemperaturePreference(HourlyForecastPeriod period, TemperaturePreference temperaturePreference) {
        if (temperaturePreference == null || temperaturePreference == TemperaturePreference.BALANCED) {
            return false;
        }
        java.math.BigDecimal feelsLike = period.feelsLikeTemperature() != null ? period.feelsLikeTemperature() : period.temperature();
        if (feelsLike == null) {
            return false;
        }
        int comparison = feelsLike.compareTo(java.math.BigDecimal.valueOf(68));
        return temperaturePreference == TemperaturePreference.COOLER ? comparison <= 0 : comparison >= 0;
    }

    private boolean matchesRainTolerance(HourlyForecastPeriod period, RainTolerance rainTolerance) {
        if (rainTolerance == null) {
            return false;
        }
        java.math.BigDecimal precipitation = period.precipitationProbability();
        if (precipitation == null) {
            return false;
        }
        return switch (rainTolerance) {
            case AVOID_RAIN -> precipitation.compareTo(java.math.BigDecimal.TEN) <= 0;
            case LIGHT_RAIN_OK -> precipitation.compareTo(java.math.BigDecimal.valueOf(25)) <= 0;
            case RAIN_OK -> precipitation.compareTo(java.math.BigDecimal.valueOf(50)) <= 0;
        };
    }

    private boolean matchesWindTolerance(HourlyForecastPeriod period, WindTolerance windTolerance) {
        if (windTolerance == null || period.windSpeed() == null) {
            return false;
        }
        return switch (windTolerance) {
            case LOW -> period.windSpeed().compareTo(java.math.BigDecimal.TEN) <= 0;
            case MODERATE -> period.windSpeed().compareTo(java.math.BigDecimal.valueOf(15)) <= 0;
            case HIGH -> period.windSpeed().compareTo(java.math.BigDecimal.valueOf(20)) <= 0;
        };
    }

    private boolean hasSeriousWarning(HourlyForecastPeriod period) {
        WalkingRecommendationDto recommendation = period.walkingRecommendation();
        if (recommendation == null || recommendation.warnings() == null) {
            return false;
        }
        return recommendation.warnings().stream().anyMatch(warning -> List.of(
                "Excessive heat",
                "Freezing conditions",
                "Rain is likely",
                "Strong wind",
                "Hazardous air quality",
                "Poor air quality",
                "High UV").contains(warning));
    }

    private Optional<OffsetDateTime> parseStartTime(String startTime) {
        if (startTime == null || startTime.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(OffsetDateTime.parse(startTime));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }
}
