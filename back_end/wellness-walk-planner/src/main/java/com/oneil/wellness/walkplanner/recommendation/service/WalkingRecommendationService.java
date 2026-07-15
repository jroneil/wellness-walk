package com.oneil.wellness.walkplanner.recommendation.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oneil.wellness.walkplanner.dto.HourlyForecastPeriod;
import com.oneil.wellness.walkplanner.recommendation.dto.BestWalkingWindowDto;
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
        return periods.stream()
                .filter(this::isUpcoming)
                .filter(period -> period.walkingRecommendation() != null && period.walkingRecommendation().score() != null)
                .max(Comparator
                        .comparing((HourlyForecastPeriod period) -> period.walkingRecommendation().score())
                        .thenComparing(period -> parseStartTime(period.startTime()).map(OffsetDateTime::toInstant).orElse(java.time.Instant.MAX),
                                Comparator.reverseOrder()));
    }

    public BestWalkingWindowDto bestWindow(List<HourlyForecastPeriod> periods) {
        return bestPeriod(periods)
                .map(period -> {
                    WalkingRecommendationDto recommendation = period.walkingRecommendation();
                    String endTime = parseStartTime(period.startTime())
                            .map(start -> start.plusHours(1).toString())
                            .orElse(null);
                    return new BestWalkingWindowDto(
                            period.startTime(),
                            endTime,
                            recommendation.score(),
                            recommendation.rating(),
                            recommendation.ratingLabel(),
                            recommendation.ratingLabel() + " weather for a restorative walk.",
                            recommendation.reasons().stream().limit(3).toList(),
                            recommendation.warnings());
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
                scored.aqiPenalty(),
                scored.uvPenalty(),
                scored.feelsLikeTemperature(),
                scored.feelsLikeSource(),
                scored.reasons(),
                scored.warnings());
    }

    private boolean isUpcoming(HourlyForecastPeriod period) {
        return parseStartTime(period.startTime())
                .map(start -> !start.toInstant().isBefore(clock.instant()))
                .orElse(false);
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
