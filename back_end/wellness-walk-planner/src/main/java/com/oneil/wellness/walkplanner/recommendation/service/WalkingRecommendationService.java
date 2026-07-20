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

import com.oneil.wellness.walkplanner.calendar.dto.CalendarConflictDto;
import com.oneil.wellness.walkplanner.calendar.model.CalendarEvent;
import com.oneil.wellness.walkplanner.calendar.service.CalendarService;
import com.oneil.wellness.walkplanner.calendar.service.ManualCalendarService;
import com.oneil.wellness.walkplanner.config.RecommendationEngineProperties;
import com.oneil.wellness.walkplanner.dto.HourlyForecastPeriod;
import com.oneil.wellness.walkplanner.recommendation.dto.BestWalkingWindowDto;
import com.oneil.wellness.walkplanner.recommendation.dto.IdealWeatherWindowDto;
import com.oneil.wellness.walkplanner.recommendation.dto.PreferredTimeOfDay;
import com.oneil.wellness.walkplanner.recommendation.dto.RainTolerance;
import com.oneil.wellness.walkplanner.recommendation.dto.RecommendationPreferencesDto;
import com.oneil.wellness.walkplanner.recommendation.dto.TemperaturePreference;
import com.oneil.wellness.walkplanner.recommendation.dto.WindTolerance;
import com.oneil.wellness.walkplanner.recommendation.dto.WalkingRecommendationDto;
import com.oneil.wellness.walkplanner.recommendation.engine.RecommendationEngine;
import com.oneil.wellness.walkplanner.recommendation.engine.calendar.AvailabilityAnalyzer;
import com.oneil.wellness.walkplanner.recommendation.engine.model.CandidateWindow;
import com.oneil.wellness.walkplanner.recommendation.engine.model.RecommendationResult;
import com.oneil.wellness.walkplanner.recommendation.engine.preferences.PreferenceScorer;
import com.oneil.wellness.walkplanner.recommendation.engine.util.TimeWindowUtilities;
import com.oneil.wellness.walkplanner.recommendation.model.ScoredWalkingPeriod;
import com.oneil.wellness.walkplanner.recommendation.model.WalkingRating;

@Service
public class WalkingRecommendationService {

    private final WalkingScoreService scoreService;
    private final CalendarService calendarService;
    private final RecommendationEngine recommendationEngine;
    private final Clock clock;

    @Autowired
    public WalkingRecommendationService(WalkingScoreService scoreService, CalendarService calendarService,
            RecommendationEngine recommendationEngine) {
        this(scoreService, calendarService, recommendationEngine, Clock.systemDefaultZone());
    }

    public WalkingRecommendationService(WalkingScoreService scoreService) {
        this(scoreService, new ManualCalendarService(), defaultEngine(Clock.systemDefaultZone()), Clock.systemDefaultZone());
    }

    WalkingRecommendationService(WalkingScoreService scoreService, Clock clock) {
        this(scoreService, new ManualCalendarService(), defaultEngine(clock), clock);
    }

    WalkingRecommendationService(WalkingScoreService scoreService, CalendarService calendarService, Clock clock) {
        this(scoreService, calendarService, defaultEngine(clock), clock);
    }

    WalkingRecommendationService(WalkingScoreService scoreService, CalendarService calendarService,
            RecommendationEngine recommendationEngine, Clock clock) {
        this.scoreService = scoreService;
        this.calendarService = calendarService;
        this.recommendationEngine = recommendationEngine;
        this.clock = clock;
    }

    private static RecommendationEngine defaultEngine(Clock clock) {
        return new RecommendationEngine(new RecommendationEngineProperties(), new TimeWindowUtilities(),
                new AvailabilityAnalyzer(), new PreferenceScorer(), clock);
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
        return bestWindow(periods, preferences, List.of());
    }

    public BestWalkingWindowDto bestWindow(
            List<HourlyForecastPeriod> periods,
            RecommendationPreferencesDto preferences,
            List<CalendarEvent> calendarEvents) {
        RecommendationPreferencesDto normalized = preferences == null ? RecommendationPreferencesDto.defaults() : preferences.normalized();
        RecommendationResult result = recommendationEngine.recommend(periods, normalized,
                calendarEvents == null ? List.of() : calendarEvents);
        if (result.selected() == null) {
            IdealWeatherWindowDto ideal = result.idealWeather() == null ? null : idealWeatherWindow(result.idealWeather());
            return new BestWalkingWindowDto(null, null, 0, WalkingRating.NOT_RECOMMENDED, "Not Recommended",
                    result.noAvailableReason(), List.of(), List.of(result.noAvailableReason()), normalized.walkDurationMinutes(),
                    List.of(), normalized.minimumScore(), true, result.noAvailableReason(), "UNAVAILABLE",
                    result.explanation().reason(), null, ideal, ideal == null ? 0 : ideal.score(), 0, 0, 0,
                    result.explanation().calendarReasons(), result.noAvailableReason());
        }
        CandidateWindow selected = result.selected();
        WalkingRecommendationDto weather = selected.representativePeriod().walkingRecommendation();
        WalkingRating rating = WalkingRating.fromScore(selected.overallScore());
        boolean belowMinimum = selected.overallScore() < normalized.minimumScore();
        IdealWeatherWindowDto ideal = result.idealWeather() != null && !result.idealWeather().available()
                ? idealWeatherWindow(result.idealWeather()) : null;
        List<String> preferenceReasons = new ArrayList<>();
        preferenceReasons.add(normalized.walkDurationMinutes() + "-minute walk window");
        preferenceReasons.addAll(result.explanation().preferenceReasons());
        return new BestWalkingWindowDto(selected.startTime().toString(), selected.endTime().toString(),
                selected.overallScore(), rating, rating.label(), rating.label() + " overall wellness window.",
                weather.reasons().stream().limit(3).toList(), weather.warnings(), normalized.walkDurationMinutes(),
                preferenceReasons, normalized.minimumScore(), belowMinimum,
                belowMinimum ? "Best available window is below your minimum score of " + normalized.minimumScore() + "." : null,
                "AVAILABLE", result.explanation().reason(), null, ideal, selected.weatherScore(), 100,
                selected.preferenceScore(), selected.overallScore(), result.explanation().calendarReasons(), null);
    }

    private IdealWeatherWindowDto idealWeatherWindow(CandidateWindow candidate) {
        return new IdealWeatherWindowDto(candidate.startTime().toString(), candidate.endTime().toString(),
                candidate.weatherScore(), candidate.available() ? "AVAILABLE" : "UNAVAILABLE",
                candidate.conflict() == null ? null : toConflictDto(candidate.conflict()));
    }

    private Optional<CalendarEvent> conflictFor(
            HourlyForecastPeriod period,
            int durationMinutes,
            List<CalendarEvent> events) {
        return parseStartTime(period.startTime())
                .flatMap(start -> calendarService.findBusyConflict(start, start.plusMinutes(durationMinutes), events));
    }

    private IdealWeatherWindowDto idealWeatherWindow(
            HourlyForecastPeriod period,
            int durationMinutes,
            List<CalendarEvent> events) {
        OffsetDateTime start = parseStartTime(period.startTime()).orElseThrow();
        OffsetDateTime end = start.plusMinutes(durationMinutes);
        CalendarConflictDto conflict = calendarService.findBusyConflict(start, end, events)
                .map(this::toConflictDto)
                .orElse(null);
        return new IdealWeatherWindowDto(
                start.toString(),
                end.toString(),
                period.walkingRecommendation().score(),
                conflict == null ? "AVAILABLE" : "UNAVAILABLE",
                conflict);
    }

    private CalendarConflictDto toConflictDto(CalendarEvent event) {
        return new CalendarConflictDto(
                event.id(),
                event.title(),
                event.startTime().toString(),
                event.endTime().toString(),
                event.source() == null ? "MANUAL" : event.source().name());
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
            case LUNCH -> hour >= 11 && hour < 14;
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
