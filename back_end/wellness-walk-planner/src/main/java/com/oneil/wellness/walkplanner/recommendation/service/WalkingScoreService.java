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
        int daylightScore = WalkingScorePolicy.daylightScore(period.daylightStatus());
        int airQualityScore = WalkingScorePolicy.airQualityScore(period.aqi());
        int uvScore = WalkingScorePolicy.uvScore(period.uvIndex());
        int total = temperatureScore + precipitationScore + windScore + humidityScore + daylightScore
                + airQualityScore + uvScore;
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
                airQualityScore,
                uvScore,
                scoringTemperature,
                period.feelsLikeMethod(),
                reasons(period, scoringTemperature, temperatureScore, precipitationScore, windScore, humidityScore,
                        daylightScore, airQualityScore, uvScore),
                warnings(period));
    }

    private List<String> reasons(HourlyForecastPeriod period, BigDecimal scoringTemperature, int temperatureScore,
            int precipitationScore, int windScore, int humidityScore, int daylightScore, int airQualityScore,
            int uvScore) {
        List<String> reasons = new ArrayList<>();
        addTemperatureReason(reasons, period, scoringTemperature, temperatureScore);
        addPrecipitationReason(reasons, period.precipitationProbability(), precipitationScore);
        addWindReason(reasons, period.windSpeed(), windScore);
        addHumidityReason(reasons, period.humidity(), humidityScore);
        addDaylightReason(reasons, period.daylightStatus(), daylightScore);
        addAqiReason(reasons, period.aqi(), period.aqiCategory(), airQualityScore);
        addUvReason(reasons, period.uvIndex(), period.uvCategory(), uvScore);
        return reasons;
    }

    private void addDaylightReason(List<String> reasons, String daylightStatus, int daylightScore) {
        if (daylightScore == 10) {
            reasons.add("Full daylight");
        } else if (daylightScore == 5) {
            reasons.add("Civil twilight");
        } else if (daylightScore == 2) {
            reasons.add("Nighttime limits daylight");
        } else if (daylightStatus == null || "UNKNOWN".equals(daylightStatus)) {
            reasons.add("Sunrise and sunset unavailable");
        }
    }

    private void addTemperatureReason(List<String> reasons, HourlyForecastPeriod period, BigDecimal temperature, int score) {
        if ("HEAT_INDEX".equals(period.feelsLikeMethod())) {
            reasons.add("Feels like " + temperature.toPlainString() + "°F because of heat and humidity");
            return;
        }
        if ("WIND_CHILL".equals(period.feelsLikeMethod())) {
            reasons.add("Feels like " + temperature.toPlainString() + "°F because of wind chill");
            return;
        }
        if (score == 30) {
            reasons.add("Comfortable feels-like temperature");
        } else if (score >= 18 && temperature.compareTo(BigDecimal.valueOf(72)) > 0) {
            reasons.add("Still warmer than ideal");
        } else if (score >= 15) {
            reasons.add("Cooler than ideal");
        } else {
            reasons.add("Feels-like temperature outside the preferred range");
        }
    }

    private void addAqiReason(List<String> reasons, BigDecimal aqi, String category, int score) {
        if (aqi == null) {
            reasons.add("Air quality unavailable");
        } else if (score == 10) {
            reasons.add("Good air quality");
        } else if (score == 7) {
            reasons.add("Moderate air quality");
        } else if (score == 3) {
            reasons.add(category != null ? category + " air quality" : "Air quality may be unhealthy for sensitive groups");
        } else {
            reasons.add("Poor air quality");
        }
    }

    private void addUvReason(List<String> reasons, BigDecimal uvIndex, String category, int score) {
        if (uvIndex == null) {
            reasons.add("UV data unavailable");
        } else if (score == 10) {
            reasons.add("Low UV");
        } else if (score == 7) {
            reasons.add("Moderate UV exposure");
        } else if (score == 3) {
            reasons.add("High UV exposure");
        } else {
            reasons.add(category != null ? category + " UV exposure" : "Very high UV exposure");
        }
    }

    private void addPrecipitationReason(List<String> reasons, BigDecimal probability, int score) {
        if (probability == null) {
            reasons.add("Rain chance unavailable");
        } else if (score == 20) {
            reasons.add("Very low chance of rain");
        } else if (score >= 16) {
            reasons.add("Some chance of rain");
        } else if (score >= 8) {
            reasons.add("Elevated rain chance");
        } else {
            reasons.add("Rain is likely");
        }
    }

    private void addWindReason(List<String> reasons, BigDecimal windSpeed, int score) {
        if (windSpeed == null) {
            reasons.add("Wind speed unavailable");
        } else if (score == 10) {
            reasons.add("Light wind");
        } else if (score >= 7) {
            reasons.add("Moderate wind");
        } else if (score >= 3) {
            reasons.add("Breezy conditions");
        } else {
            reasons.add("Strong wind");
        }
    }

    private void addHumidityReason(List<String> reasons, BigDecimal humidity, int score) {
        if (humidity == null) {
            reasons.add("Humidity unavailable");
        } else if (score == 10) {
            reasons.add("Comfortable humidity");
        } else if (score >= 7) {
            reasons.add("Moderately humid");
        } else if (score >= 3) {
            reasons.add("High humidity");
        } else {
            reasons.add("Very high humidity");
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
        if (period.aqi() != null && period.aqi().compareTo(BigDecimal.valueOf(300)) > 0) {
            warnings.add("Hazardous air quality");
        } else if (period.aqi() != null && period.aqi().compareTo(BigDecimal.valueOf(150)) > 0) {
            warnings.add("Poor air quality");
        } else if (period.aqi() != null && period.aqi().compareTo(BigDecimal.valueOf(100)) > 0) {
            warnings.add("Air quality may be unhealthy for sensitive groups");
        }
        if (period.uvIndex() != null && period.uvIndex().compareTo(BigDecimal.valueOf(8)) >= 0) {
            warnings.add("High UV");
            warnings.add("Sun protection recommended");
        } else if (period.uvIndex() != null && period.uvIndex().compareTo(BigDecimal.valueOf(6)) >= 0) {
            warnings.add("Sun protection recommended");
        }
        if ("NIGHT".equals(period.daylightStatus())) {
            warnings.add("Limited daylight");
        }
        return warnings;
    }
}
