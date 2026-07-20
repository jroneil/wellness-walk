package com.oneil.wellness.walkplanner.recommendation.engine.preferences;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.oneil.wellness.walkplanner.dto.HourlyForecastPeriod;
import com.oneil.wellness.walkplanner.recommendation.dto.*;
import com.oneil.wellness.walkplanner.recommendation.engine.model.CandidateWindow;

@Component
public class PreferenceScorer {
    public PreferenceScore score(CandidateWindow candidate, RecommendationPreferencesDto preferences) {
        RecommendationPreferencesDto p = preferences == null ? RecommendationPreferencesDto.defaults() : preferences.normalized();
        HourlyForecastPeriod period = candidate.representativePeriod();
        List<String> reasons = new ArrayList<>();
        int score = 0;
        if (p.preferredTimeOfDay() == PreferredTimeOfDay.ANY || matchesTime(candidate.startTime().toLocalTime(), p.preferredTimeOfDay())) {
            score += 25;
            if (p.preferredTimeOfDay() != PreferredTimeOfDay.ANY) reasons.add("Matches your preferred time of day");
        }
        BigDecimal temperature = period.feelsLikeTemperature() != null ? period.feelsLikeTemperature() : period.temperature();
        if (p.temperaturePreference() == TemperaturePreference.BALANCED || matchesTemperature(temperature, p.temperaturePreference())) {
            score += 25;
            if (p.temperaturePreference() != TemperaturePreference.BALANCED) reasons.add(p.temperaturePreference() == TemperaturePreference.COOLER ? "Leans toward cooler conditions" : "Leans toward warmer conditions");
        }
        if (matchesRain(period.precipitationProbability(), p.rainTolerance())) { score += 25; reasons.add("Fits your rain tolerance"); }
        if (matchesWind(period.windSpeed(), p.windTolerance())) { score += 25; reasons.add("Fits your wind tolerance"); }
        return new PreferenceScore(score, reasons);
    }

    private boolean matchesTime(LocalTime time, PreferredTimeOfDay preferred) {
        int hour = time.getHour();
        return switch (preferred) {
            case MORNING -> hour >= 6 && hour < 12;
            case LUNCH -> hour >= 11 && hour < 14;
            case AFTERNOON -> hour >= 12 && hour < 17;
            case EVENING -> hour >= 17 && hour < 21;
            case ANY -> true;
        };
    }
    private boolean matchesTemperature(BigDecimal value, TemperaturePreference preference) {
        if (value == null) return false;
        int comparison = value.compareTo(BigDecimal.valueOf(68));
        return preference == TemperaturePreference.COOLER ? comparison <= 0 : comparison >= 0;
    }
    private boolean matchesRain(BigDecimal value, RainTolerance tolerance) {
        if (value == null) return false;
        return switch (tolerance) { case AVOID_RAIN -> value.compareTo(BigDecimal.TEN) <= 0; case LIGHT_RAIN_OK -> value.compareTo(BigDecimal.valueOf(25)) <= 0; case RAIN_OK -> value.compareTo(BigDecimal.valueOf(50)) <= 0; };
    }
    private boolean matchesWind(BigDecimal value, WindTolerance tolerance) {
        if (value == null) return false;
        return switch (tolerance) { case LOW -> value.compareTo(BigDecimal.TEN) <= 0; case MODERATE -> value.compareTo(BigDecimal.valueOf(15)) <= 0; case HIGH -> value.compareTo(BigDecimal.valueOf(20)) <= 0; };
    }
}
