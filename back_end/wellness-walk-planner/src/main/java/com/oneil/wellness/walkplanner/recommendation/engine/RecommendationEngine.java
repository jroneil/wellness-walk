package com.oneil.wellness.walkplanner.recommendation.engine;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import com.oneil.wellness.walkplanner.calendar.model.CalendarEvent;
import com.oneil.wellness.walkplanner.config.RecommendationEngineProperties;
import com.oneil.wellness.walkplanner.dto.HourlyForecastPeriod;
import com.oneil.wellness.walkplanner.recommendation.dto.RecommendationPreferencesDto;
import com.oneil.wellness.walkplanner.recommendation.engine.calendar.AvailabilityAnalyzer;
import com.oneil.wellness.walkplanner.recommendation.engine.model.CandidateWindow;
import com.oneil.wellness.walkplanner.recommendation.engine.model.RecommendationExplanation;
import com.oneil.wellness.walkplanner.recommendation.engine.model.RecommendationResult;
import com.oneil.wellness.walkplanner.recommendation.engine.preferences.PreferenceScore;
import com.oneil.wellness.walkplanner.recommendation.engine.preferences.PreferenceScorer;
import com.oneil.wellness.walkplanner.recommendation.engine.util.TimeWindowUtilities;

@Service
public class RecommendationEngine {
    private final RecommendationEngineProperties properties;
    private final TimeWindowUtilities timeWindows;
    private final AvailabilityAnalyzer availability;
    private final PreferenceScorer preferences;
    private final Clock clock;

    @Autowired
    public RecommendationEngine(RecommendationEngineProperties properties, TimeWindowUtilities timeWindows,
            AvailabilityAnalyzer availability, PreferenceScorer preferences) {
        this(properties, timeWindows, availability, preferences, Clock.systemDefaultZone());
    }

    public RecommendationEngine(RecommendationEngineProperties properties, TimeWindowUtilities timeWindows,
            AvailabilityAnalyzer availability, PreferenceScorer preferences, Clock clock) {
        this.properties = properties;
        this.timeWindows = timeWindows;
        this.availability = availability;
        this.preferences = preferences;
        this.clock = clock;
    }

    public RecommendationResult recommend(List<HourlyForecastPeriod> periods, RecommendationPreferencesDto requested,
            List<CalendarEvent> events) {
        RecommendationPreferencesDto p = requested == null ? RecommendationPreferencesDto.defaults() : requested.normalized();
        List<CandidateWindow> generated = timeWindows.generateCandidates(periods, p.walkDurationMinutes(),
                properties.getCandidateIntervalMinutes(), clock.instant());
        if (generated.isEmpty()) {
            String reason = "No scorable forecast can cover the requested walk duration.";
            return new RecommendationResult(null, null, new RecommendationExplanation(reason, List.of(), List.of()), reason);
        }
        List<CandidateWindow> analyzed = availability.analyze(generated, events);
        CandidateWindow ideal = analyzed.stream().max(Comparator.comparingInt(CandidateWindow::weatherScore)
                .thenComparing(CandidateWindow::startTime, Comparator.reverseOrder())).orElse(null);
        List<CandidateWindow> scored = analyzed.stream().map(candidate -> {
            PreferenceScore preference = preferences.score(candidate, p);
            int overall = weighted(candidate.weatherScore(), candidate.available() ? 100 : 0, preference.score());
            return candidate.withScores(preference.score(), overall);
        }).toList();
        Optional<CandidateWindow> selected = scored.stream().filter(CandidateWindow::available)
                .max(Comparator.comparingInt(CandidateWindow::overallScore)
                        .thenComparingInt(CandidateWindow::weatherScore)
                        .thenComparingInt(CandidateWindow::preferenceScore)
                        .thenComparing(CandidateWindow::startTime, Comparator.reverseOrder()));
        if (selected.isEmpty()) {
            String reason = "No available window is long enough for the requested duration.";
            return new RecommendationResult(null, ideal,
                    new RecommendationExplanation(reason, List.of("Calendar is busy for every candidate window"), List.of()), reason);
        }
        CandidateWindow chosen = selected.get();
        PreferenceScore preference = preferences.score(chosen, p);
        List<String> calendarReasons = ideal != null && !ideal.available()
                ? List.of("Ideal weather overlaps " + safeTitle(ideal.conflict()), "Selected window is fully available")
                : List.of("Selected window is fully available");
        String reason = ideal != null && !ideal.available() && !ideal.startTime().equals(chosen.startTime())
                ? "Highest available overall wellness score after a calendar conflict."
                : "Highest available overall wellness score.";
        return new RecommendationResult(chosen, ideal,
                new RecommendationExplanation(reason, calendarReasons, preference.reasons()), null);
    }

    private int weighted(int weather, int availabilityScore, int preference) {
        int totalWeight = properties.getWeatherWeight() + properties.getAvailabilityWeight() + properties.getPreferenceWeight();
        if (totalWeight <= 0) return weather;
        double value = weather * properties.getWeatherWeight()
                + availabilityScore * properties.getAvailabilityWeight()
                + preference * properties.getPreferenceWeight();
        return (int) Math.round(value / totalWeight);
    }

    private String safeTitle(CalendarEvent event) {
        return event == null || event.title() == null || event.title().isBlank() ? "a busy event" : event.title();
    }
}
