package com.oneil.wellness.walkplanner.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.oneil.wellness.walkplanner.dto.HourlyForecastPeriod;
import com.oneil.wellness.walkplanner.calendar.model.CalendarEvent;
import com.oneil.wellness.walkplanner.calendar.model.CalendarSource;
import com.oneil.wellness.walkplanner.recommendation.dto.BestWalkingWindowDto;
import com.oneil.wellness.walkplanner.recommendation.dto.PreferredTimeOfDay;
import com.oneil.wellness.walkplanner.recommendation.dto.RainTolerance;
import com.oneil.wellness.walkplanner.recommendation.dto.RecommendationPreferencesDto;
import com.oneil.wellness.walkplanner.recommendation.dto.TemperaturePreference;
import com.oneil.wellness.walkplanner.recommendation.dto.UnitSystem;
import com.oneil.wellness.walkplanner.recommendation.dto.WindTolerance;

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

        assertThat(result.startTime()).isEqualTo("2026-07-15T14:00-04:00");
        assertThat(result.endTime()).isEqualTo("2026-07-15T14:30-04:00");
        assertThat(result.weatherScore()).isEqualTo(100);
        assertThat(result.positiveReasons()).contains("Comfortable feels-like temperature", "Very low chance of rain", "Light wind");
    }

    @Test
    void durationControlsRecommendationEndTime() {
        HourlyForecastPeriod period = withRecommendation(period("2026-07-15T14:00:00-04:00", "70", 0, 5, 45, true));

        BestWalkingWindowDto result = service.bestWindow(List.of(period), preferences(
                45,
                PreferredTimeOfDay.ANY,
                TemperaturePreference.BALANCED,
                RainTolerance.LIGHT_RAIN_OK,
                WindTolerance.MODERATE,
                60,
                UnitSystem.US));

        assertThat(result.endTime()).isEqualTo("2026-07-15T14:45-04:00");
        assertThat(result.durationMinutes()).isEqualTo(45);
        assertThat(result.preferenceReasons()).contains("45-minute walk window");
    }

    @Test
    void busyEventRemovesIdealWeatherAndSelectsNextAvailableWindow() {
        HourlyForecastPeriod ideal = withRecommendation(period("2026-07-15T14:00:00-04:00", "70", 0, 5, 45, true));
        HourlyForecastPeriod next = withRecommendation(period("2026-07-15T15:00:00-04:00", "78", 0, 5, 45, true));
        CalendarEvent meeting = event("meeting", "Planning meeting", "2026-07-15T14:10:00-04:00", "2026-07-15T14:45:00-04:00", true);

        BestWalkingWindowDto result = service.bestWindow(List.of(ideal, next), RecommendationPreferencesDto.defaults(), List.of(meeting));

        assertThat(result.startTime()).isEqualTo("2026-07-15T14:45-04:00");
        assertThat(result.availability()).isEqualTo("AVAILABLE");
        assertThat(result.idealWeatherWindow().availability()).isEqualTo("UNAVAILABLE");
        assertThat(result.idealWeatherWindow().conflictingEvent().title()).isEqualTo("Planning meeting");
    }

    @Test
    void freeEventsDoNotBlockAndFullDurationMustBeAvailable() {
        HourlyForecastPeriod ideal = withRecommendation(period("2026-07-15T14:00:00-04:00", "70", 0, 5, 45, true));
        HourlyForecastPeriod next = withRecommendation(period("2026-07-15T15:00:00-04:00", "78", 0, 5, 45, true));
        CalendarEvent free = event("focus", "Optional focus time", "2026-07-15T14:15:00-04:00", "2026-07-15T14:45:00-04:00", false);

        BestWalkingWindowDto result = service.bestWindow(List.of(ideal, next), preferences(45, PreferredTimeOfDay.ANY,
                TemperaturePreference.BALANCED, RainTolerance.LIGHT_RAIN_OK, WindTolerance.MODERATE, 60, UnitSystem.US), List.of(free));

        assertThat(result.startTime()).isEqualTo("2026-07-15T14:00-04:00");
        assertThat(result.endTime()).isEqualTo("2026-07-15T14:45-04:00");
        assertThat(result.idealWeatherWindow()).isNull();
    }

    @Test
    void preferredTimeBreaksTiesButDoesNotBeatHigherEnvironmentalScore() {
        HourlyForecastPeriod afternoonExcellent = withRecommendation(period("2026-07-15T14:00:00-04:00", "70", 0, 5, 45, true));
        HourlyForecastPeriod eveningLowerScore = withRecommendation(period("2026-07-15T18:00:00-04:00", "78", 0, 5, 45, true));

        BestWalkingWindowDto result = service.bestWindow(List.of(afternoonExcellent, eveningLowerScore), preferences(
                30,
                PreferredTimeOfDay.EVENING,
                TemperaturePreference.BALANCED,
                RainTolerance.LIGHT_RAIN_OK,
                WindTolerance.MODERATE,
                60,
                UnitSystem.US));

        assertThat(result.startTime()).isEqualTo("2026-07-15T14:00-04:00");
        assertThat(result.weatherScore()).isEqualTo(100);
    }

    @Test
    void preferredTimeSelectsMatchingPeriodWhenScoresTie() {
        HourlyForecastPeriod morning = withRecommendation(period("2026-07-15T09:00:00-04:00", "70", 0, 5, 45, true));
        HourlyForecastPeriod afternoon = withRecommendation(period("2026-07-15T14:00:00-04:00", "70", 0, 5, 45, true));

        BestWalkingWindowDto result = service.bestWindow(List.of(morning, afternoon), preferences(
                30,
                PreferredTimeOfDay.AFTERNOON,
                TemperaturePreference.BALANCED,
                RainTolerance.LIGHT_RAIN_OK,
                WindTolerance.MODERATE,
                60,
                UnitSystem.US));

        assertThat(result.startTime()).isEqualTo("2026-07-15T14:00-04:00");
        assertThat(result.preferenceReasons()).contains("Matches your preferred time of day");
    }

    @Test
    void coolerTemperatureCanWinNearTieWithoutChangingScore() {
        HourlyForecastPeriod warmer = withRecommendation(period("2026-07-15T14:00:00-04:00", "70", 0, 5, 45, true));
        HourlyForecastPeriod cooler = withRecommendation(period("2026-07-15T15:00:00-04:00", "66", 15, 5, 45, true));

        BestWalkingWindowDto result = service.bestWindow(List.of(warmer, cooler), preferences(
                30,
                PreferredTimeOfDay.ANY,
                TemperaturePreference.COOLER,
                RainTolerance.LIGHT_RAIN_OK,
                WindTolerance.MODERATE,
                60,
                UnitSystem.METRIC));

        assertThat(result.startTime()).isEqualTo("2026-07-15T14:00-04:00");
        assertThat(result.weatherScore()).isEqualTo(100);
        assertThat(warmer.walkingRecommendation().score()).isEqualTo(100);
        assertThat(cooler.walkingRecommendation().score()).isEqualTo(96);
        assertThat(result.preferenceScore()).isGreaterThan(0);
    }

    @Test
    void seriousWarningsPreventTemperaturePreferenceOverride() {
        HourlyForecastPeriod safe = withRecommendation(period("2026-07-15T14:00:00-04:00", "70", 0, 5, 45, true));
        HourlyForecastPeriod hot = withRecommendation(period("2026-07-15T15:00:00-04:00", "90", 0, 5, 45, true));

        BestWalkingWindowDto result = service.bestWindow(List.of(safe, hot), preferences(
                30,
                PreferredTimeOfDay.ANY,
                TemperaturePreference.WARMER,
                RainTolerance.LIGHT_RAIN_OK,
                WindTolerance.MODERATE,
                60,
                UnitSystem.US));

        assertThat(result.startTime()).isEqualTo("2026-07-15T14:00-04:00");
    }

    @Test
    void minimumScoreAddsMessageWithoutChangingScore() {
        HourlyForecastPeriod fair = withRecommendation(period("2026-07-15T14:00:00-04:00", "85", 30, 12, 70, true));

        BestWalkingWindowDto result = service.bestWindow(List.of(fair), preferences(
                30,
                PreferredTimeOfDay.ANY,
                TemperaturePreference.BALANCED,
                RainTolerance.LIGHT_RAIN_OK,
                WindTolerance.MODERATE,
                90,
                UnitSystem.US));

        assertThat(result.weatherScore()).isEqualTo(fair.walkingRecommendation().score());
        assertThat(result.score()).isEqualTo(result.overallScore());
        assertThat(result.belowMinimumScore()).isTrue();
        assertThat(result.minimumScore()).isEqualTo(90);
        assertThat(result.minimumScoreMessage()).contains("below your minimum score of 90");
    }

    @Test
    void invalidPreferenceNumbersNormalizeToDefaults() {
        RecommendationPreferencesDto normalized = new RecommendationPreferencesDto(
                999,
                null,
                null,
                null,
                null,
                -1,
                null).normalized();

        assertThat(normalized.walkDurationMinutes()).isEqualTo(30);
        assertThat(normalized.minimumScore()).isEqualTo(60);
        assertThat(normalized.unitSystem()).isEqualTo(UnitSystem.US);
    }

    @Test
    void returnsNullWhenNoUpcomingScorablePeriodExists() {
        HourlyForecastPeriod missingTemperature = withRecommendation(period(
                "2026-07-15T14:00:00-04:00",
                null,
                0,
                5,
                40,
                true));

        assertThat(service.bestWindow(List.of(missingTemperature)).noAvailableReason()).contains("No scorable forecast");
    }

    private HourlyForecastPeriod withRecommendation(HourlyForecastPeriod period) {
        return period.withWalkingRecommendation(service.recommendationFor(period));
    }

    private RecommendationPreferencesDto preferences(int duration, PreferredTimeOfDay preferredTimeOfDay,
            TemperaturePreference temperaturePreference, RainTolerance rainTolerance, WindTolerance windTolerance,
            int minimumScore, UnitSystem unitSystem) {
        return new RecommendationPreferencesDto(
                duration,
                preferredTimeOfDay,
                temperaturePreference,
                rainTolerance,
                windTolerance,
                minimumScore,
                unitSystem).normalized();
    }

    private HourlyForecastPeriod period(String startTime, String temperature, int precipitation, int windSpeed,
            int humidity, boolean isDaytime) {
        BigDecimal temperatureValue = temperature == null ? null : new BigDecimal(temperature);
        return new HourlyForecastPeriod(
                startTime,
                temperatureValue,
                temperatureValue,
                "°F",
                "Sunny",
                "https://example.com/icon.png",
                BigDecimal.valueOf(precipitation),
                BigDecimal.valueOf(humidity),
                BigDecimal.valueOf(windSpeed),
                "NW",
                isDaytime,
                temperatureValue,
                temperatureValue == null ? null : "ACTUAL_TEMPERATURE",
                BigDecimal.ONE,
                "Low",
                startTime,
                "Open-Meteo Forecast API",
                BigDecimal.valueOf(25),
                "Good",
                startTime,
                "Open-Meteo Air Quality API",
                "2026-07-15T05:30",
                "2026-07-15T20:30",
                "DAYLIGHT",
                450,
                null);
    }

    private CalendarEvent event(String id, String title, String start, String end, boolean busy) {
        return new CalendarEvent(id, title, OffsetDateTime.parse(start), OffsetDateTime.parse(end), busy, CalendarSource.MANUAL);
    }
}
