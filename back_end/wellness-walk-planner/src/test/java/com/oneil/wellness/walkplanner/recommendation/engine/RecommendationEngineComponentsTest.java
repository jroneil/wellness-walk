package com.oneil.wellness.walkplanner.recommendation.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.oneil.wellness.walkplanner.calendar.model.CalendarEvent;
import com.oneil.wellness.walkplanner.calendar.model.CalendarSource;
import com.oneil.wellness.walkplanner.config.RecommendationEngineProperties;
import com.oneil.wellness.walkplanner.dto.HourlyForecastPeriod;
import com.oneil.wellness.walkplanner.recommendation.dto.*;
import com.oneil.wellness.walkplanner.recommendation.engine.calendar.AvailabilityAnalyzer;
import com.oneil.wellness.walkplanner.recommendation.engine.model.CandidateWindow;
import com.oneil.wellness.walkplanner.recommendation.engine.model.RecommendationResult;
import com.oneil.wellness.walkplanner.recommendation.engine.preferences.PreferenceScorer;
import com.oneil.wellness.walkplanner.recommendation.engine.util.TimeWindowUtilities;
import com.oneil.wellness.walkplanner.recommendation.service.WalkingRecommendationService;
import com.oneil.wellness.walkplanner.recommendation.service.WalkingScoreService;

class RecommendationEngineComponentsTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC);
    private final TimeWindowUtilities windows = new TimeWindowUtilities();
    private final AvailabilityAnalyzer availability = new AvailabilityAnalyzer();
    private final PreferenceScorer preferences = new PreferenceScorer();

    @Test
    void generatesOverlappingFifteenMinuteCandidatesAndSupportsThirtyMinuteIntervals() {
        List<HourlyForecastPeriod> periods = List.of(scored(period("2026-07-15T14:00:00-04:00", 70)),
                scored(period("2026-07-15T15:00:00-04:00", 70)));
        assertThat(windows.generateCandidates(periods, 30, 15, clock.instant()))
                .extracting(candidate -> candidate.startTime().toLocalTime().toString())
                .contains("14:00", "14:15", "14:30", "14:45", "15:00", "15:15", "15:30");
        assertThat(windows.generateCandidates(periods, 30, 30, clock.instant())).hasSize(4);
    }

    @Test
    void crossHourCandidateUsesConservativeWeatherScore() {
        List<HourlyForecastPeriod> periods = List.of(scored(period("2026-07-15T14:00:00-04:00", 70)),
                scored(period("2026-07-15T15:00:00-04:00", 85)));
        CandidateWindow crossing = windows.generateCandidates(periods, 30, 15, clock.instant()).stream()
                .filter(candidate -> candidate.startTime().getMinute() == 45).findFirst().orElseThrow();
        assertThat(crossing.weatherScore()).isLessThanOrEqualTo(crossing.representativePeriod().walkingRecommendation().score());
    }

    @Test
    void mergesOverlappingAndAdjacentBusyEvents() {
        List<AvailabilityAnalyzer.BusyPeriod> merged = availability.mergeBusyPeriods(List.of(
                event("a", "A", "2026-07-15T14:00:00-04:00", "2026-07-15T14:30:00-04:00"),
                event("b", "B", "2026-07-15T14:20:00-04:00", "2026-07-15T15:00:00-04:00"),
                event("c", "C", "2026-07-15T15:00:00-04:00", "2026-07-15T15:30:00-04:00")));
        assertThat(merged).singleElement().satisfies(period -> {
            assertThat(period.start()).isEqualTo(OffsetDateTime.parse("2026-07-15T14:00:00-04:00"));
            assertThat(period.end()).isEqualTo(OffsetDateTime.parse("2026-07-15T15:30:00-04:00"));
        });
    }

    @Test
    void preferenceScoreIsExplicitAndLunchIsSupported() {
        CandidateWindow candidate = new CandidateWindow(OffsetDateTime.parse("2026-07-15T12:15:00-04:00"),
                OffsetDateTime.parse("2026-07-15T12:45:00-04:00"), scored(period("2026-07-15T12:00:00-04:00", 70)),
                100, true, null, 0, 0);
        RecommendationPreferencesDto lunch = new RecommendationPreferencesDto(30, PreferredTimeOfDay.LUNCH,
                TemperaturePreference.BALANCED, RainTolerance.LIGHT_RAIN_OK, WindTolerance.MODERATE, 60, UnitSystem.US);
        assertThat(preferences.score(candidate, lunch).score()).isEqualTo(100);
        assertThat(preferences.score(candidate, lunch).reasons()).contains("Matches your preferred time of day");
    }

    @Test
    void returnsMeaningfulResultWhenEveryCandidateIsBusy() {
        RecommendationEngine engine = new RecommendationEngine(new RecommendationEngineProperties(), windows, availability,
                preferences, clock);
        RecommendationResult result = engine.recommend(List.of(scored(period("2026-07-15T14:00:00-04:00", 70))),
                RecommendationPreferencesDto.defaults(), List.of(event("all", "All day",
                        "2026-07-15T00:00:00-04:00", "2026-07-16T00:00:00-04:00")));
        assertThat(result.selected()).isNull();
        assertThat(result.noAvailableReason()).contains("No available window");
    }

    private HourlyForecastPeriod scored(HourlyForecastPeriod period) {
        WalkingRecommendationService service = new WalkingRecommendationService(new WalkingScoreService());
        return period.withWalkingRecommendation(service.recommendationFor(period));
    }

    private CalendarEvent event(String id, String title, String start, String end) {
        return new CalendarEvent(id, title, OffsetDateTime.parse(start), OffsetDateTime.parse(end), true, CalendarSource.MANUAL);
    }

    private HourlyForecastPeriod period(String start, int temperature) {
        BigDecimal value = BigDecimal.valueOf(temperature);
        return new HourlyForecastPeriod(start, value, value, "°F", "Sunny", null, BigDecimal.ZERO,
                BigDecimal.valueOf(45), BigDecimal.valueOf(5), "NW", true, value, "ACTUAL_TEMPERATURE",
                BigDecimal.ONE, "Low", start, "Open-Meteo", BigDecimal.valueOf(25), "Good", start,
                "Open-Meteo", "2026-07-15T05:30", "2026-07-15T20:30", "DAYLIGHT", 450, null);
    }
}
