package com.oneil.wellness.walkplanner.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.oneil.wellness.walkplanner.dto.HourlyForecastPeriod;
import com.oneil.wellness.walkplanner.recommendation.dto.BestWalkingWindowDto;

class WalkingRecommendationServiceTest {

    private final WalkingRecommendationService service = new WalkingRecommendationService(
            new WalkingScoreService(),
            Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void tieBreakingChoosesEarliestUpcomingHourAndExcludesPastPeriods() {
        HourlyForecastPeriod pastExcellent = withRecommendation(period("2026-07-15T07:00:00-04:00", "70", 0, 5, 45, true));
        HourlyForecastPeriod laterGreat = withRecommendation(period("2026-07-15T15:00:00-04:00", "70", 0, 5, 45, true));
        HourlyForecastPeriod earlierGreat = withRecommendation(period("2026-07-15T14:00:00-04:00", "70", 0, 5, 45, true));

        BestWalkingWindowDto result = service.bestWindow(List.of(pastExcellent, laterGreat, earlierGreat));

        assertThat(result.startTime()).isEqualTo("2026-07-15T14:00:00-04:00");
        assertThat(result.endTime()).isEqualTo("2026-07-15T15:00-04:00");
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.positiveReasons()).contains("Comfortable feels-like temperature", "Low chance of rain", "Light wind");
    }

    @Test
    void returnsNullWhenNoUpcomingScorablePeriodExists() {
        HourlyForecastPeriod missingTemperature = withRecommendation(new HourlyForecastPeriod(
                "2026-07-15T14:00:00-04:00",
                null,
                "°F",
                "Sunny",
                null,
                BigDecimal.ZERO,
                BigDecimal.valueOf(40),
                BigDecimal.valueOf(5),
                "NW",
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));

        assertThat(service.bestWindow(List.of(missingTemperature))).isNull();
    }

    private HourlyForecastPeriod withRecommendation(HourlyForecastPeriod period) {
        return period.withWalkingRecommendation(service.recommendationFor(period));
    }

    private HourlyForecastPeriod period(String startTime, String temperature, int precipitation, int windSpeed,
            int humidity, boolean isDaytime) {
        return new HourlyForecastPeriod(
                startTime,
                new BigDecimal(temperature),
                "°F",
                "Sunny",
                "https://example.com/icon.png",
                BigDecimal.valueOf(precipitation),
                BigDecimal.valueOf(humidity),
                BigDecimal.valueOf(windSpeed),
                "NW",
                isDaytime,
                new BigDecimal(temperature),
                "ACTUAL",
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
