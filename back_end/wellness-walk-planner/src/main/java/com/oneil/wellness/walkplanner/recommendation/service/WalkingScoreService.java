package com.oneil.wellness.walkplanner.recommendation.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.oneil.wellness.walkplanner.dto.HourlyForecastPeriod;
import com.oneil.wellness.walkplanner.recommendation.model.ScoredWalkingPeriod;
import com.oneil.wellness.walkplanner.recommendation.model.WalkingRating;

@Service
public class WalkingScoreService {

    public ScoredWalkingPeriod score(HourlyForecastPeriod period) {
        BigDecimal scoringTemperature = period.feelsLikeTemperature() != null
                ? period.feelsLikeTemperature()
                : period.temperature();
        if (scoringTemperature == null) {
            return new ScoredWalkingPeriod(
                    period.startTime(),
                    null,
                    null,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of("Temperature unavailable"),
                    List.of());
        }

        int temperatureScore = WalkingScorePolicy.temperatureScore(scoringTemperature);
        int precipitationScore = WalkingScorePolicy.precipitationScore(period.precipitationProbability());
        int windScore = WalkingScorePolicy.windScore(period.windSpeed());
        int humidityScore = WalkingScorePolicy.humidityScore(period.humidity());
        int daylightScore = WalkingScorePolicy.daylightScore(period.isDaytime(), period.remainingDaylightMinutes());
        int aqiPenalty = WalkingScorePolicy.aqiPenalty(period.aqi());
        int uvPenalty = WalkingScorePolicy.uvPenalty(period.uvIndex());
        int total = Math.max(0, temperatureScore + precipitationScore + windScore + humidityScore + daylightScore
                - aqiPenalty - uvPenalty);
        WalkingRating rating = WalkingRating.fromScore(total);

        return new ScoredWalkingPeriod(
                period.startTime(),
                total,
                rating,
                total >= 60,
                temperatureScore,
                precipitationScore,
                windScore,
                humidityScore,
                daylightScore,
                aqiPenalty,
                uvPenalty,
                scoringTemperature,
                period.feelsLikeSource(),
                reasons(period, scoringTemperature, temperatureScore, precipitationScore, windScore, humidityScore,
                        daylightScore, aqiPenalty, uvPenalty),
                warnings(period));
    }

    private List<String> reasons(HourlyForecastPeriod period, BigDecimal scoringTemperature, int temperatureScore,
            int precipitationScore, int windScore, int humidityScore, int daylightScore, int aqiPenalty,
            int uvPenalty) {
        List<String> reasons = new ArrayList<>();
        addTemperatureReason(reasons, period, scoringTemperature, temperatureScore);
        addPrecipitationReason(reasons, period.precipitationProbability(), precipitationScore);
        addWindReason(reasons, period.windSpeed(), windScore);
        addHumidityReason(reasons, period.humidity(), humidityScore);
        addDaylightReason(reasons, daylightScore, period.remainingDaylightMinutes());
        addAqiReason(reasons, period.aqi(), aqiPenalty);
        addUvReason(reasons, period.uvIndex(), uvPenalty);
        return reasons;
    }

    private void addDaylightReason(List<String> reasons, int daylightScore, Integer remainingDaylightMinutes) {
        if (daylightScore == 10) {
            reasons.add(remainingDaylightMinutes == null ? "Daylight available" : "Plenty of daylight remains");
        } else if (daylightScore == 6) {
            reasons.add("Limited daylight remains");
        }
    }

    private void addTemperatureReason(List<String> reasons, HourlyForecastPeriod period, BigDecimal temperature, int score) {
        if ("HEAT_INDEX".equals(period.feelsLikeSource())) {
            reasons.add("Feels like " + temperature.toPlainString() + "°F due to humidity");
            return;
        }
        if ("WIND_CHILL".equals(period.feelsLikeSource())) {
            reasons.add("Feels like " + temperature.toPlainString() + "°F due to wind chill");
            return;
        }
        if (score == 40) {
            reasons.add("Comfortable feels-like temperature");
        } else if (score >= 25 && temperature.compareTo(BigDecimal.valueOf(72)) > 0) {
            reasons.add("Still warmer than ideal");
        } else if (score >= 20) {
            reasons.add("Cooler than ideal");
        }
    }

    private void addAqiReason(List<String> reasons, BigDecimal aqi, int penalty) {
        if (aqi == null) {
            reasons.add("Air quality unavailable");
        } else if (penalty == 0) {
            reasons.add("Good air quality");
        } else {
            reasons.add("Air quality reduced score by " + penalty + " points");
        }
    }

    private void addUvReason(List<String> reasons, BigDecimal uvIndex, int penalty) {
        if (uvIndex == null) {
            reasons.add("UV index unavailable");
        } else if (penalty == 0) {
            reasons.add("Low UV");
        } else {
            reasons.add("UV reduced score by " + penalty + " points");
        }
    }

    private void addPrecipitationReason(List<String> reasons, BigDecimal probability, int score) {
        if (probability == null) {
            reasons.add("Rain chance unavailable");
        } else if (score == 25) {
            reasons.add("Low chance of rain");
        } else if (score >= 20) {
            reasons.add("Some chance of rain");
        }
    }

    private void addWindReason(List<String> reasons, BigDecimal windSpeed, int score) {
        if (windSpeed == null) {
            reasons.add("Wind speed unavailable");
        } else if (score == 15) {
            reasons.add("Light wind");
        } else if (score >= 10) {
            reasons.add("Moderate wind");
        }
    }

    private void addHumidityReason(List<String> reasons, BigDecimal humidity, int score) {
        if (humidity == null) {
            reasons.add("Humidity unavailable");
        } else if (score == 10) {
            reasons.add("Comfortable humidity");
        } else if (score >= 7) {
            reasons.add("Humidity is manageable");
        }
    }

    private List<String> warnings(HourlyForecastPeriod period) {
        List<String> warnings = new ArrayList<>();
        BigDecimal feelsLike = period.feelsLikeTemperature() != null ? period.feelsLikeTemperature() : period.temperature();
        if (feelsLike != null && feelsLike.compareTo(BigDecimal.valueOf(90)) > 0) {
            warnings.add("Excessive heat");
        }
        if (feelsLike != null && feelsLike.compareTo(BigDecimal.valueOf(32)) < 0) {
            warnings.add("Freezing conditions");
        }
        if (period.precipitationProbability() != null
                && period.precipitationProbability().compareTo(BigDecimal.valueOf(50)) > 0) {
            warnings.add("Rain is likely");
        }
        if (period.windSpeed() != null && period.windSpeed().compareTo(BigDecimal.valueOf(20)) > 0) {
            warnings.add("Strong wind");
        }
        if (period.humidity() != null && period.humidity().compareTo(BigDecimal.valueOf(85)) > 0) {
            warnings.add("Very high humidity");
        }
        if (period.aqi() != null && period.aqi().compareTo(BigDecimal.valueOf(150)) > 0) {
            warnings.add("Poor air quality");
        }
        if (period.uvIndex() != null && period.uvIndex().compareTo(BigDecimal.valueOf(8)) >= 0) {
            warnings.add("High UV");
            warnings.add("Sun protection recommended");
        } else if (period.uvIndex() != null && period.uvIndex().compareTo(BigDecimal.valueOf(6)) >= 0) {
            warnings.add("Sun protection recommended");
        }
        if (period.remainingDaylightMinutes() != null && period.remainingDaylightMinutes() <= 0) {
            warnings.add("Limited daylight");
        } else if (period.remainingDaylightMinutes() == null && !Boolean.TRUE.equals(period.isDaytime())) {
            warnings.add("Limited daylight");
        }
        return warnings;
    }
}
