package com.oneil.wellness.walkplanner.recommendation.service;

import java.math.BigDecimal;

final class WalkingScorePolicy {

    private WalkingScorePolicy() {
    }

    static int temperatureScore(BigDecimal temperatureFahrenheit) {
        double value = temperatureFahrenheit.doubleValue();
        if (value >= 60 && value <= 72) {
            return 40;
        }
        if ((value >= 55 && value <= 59) || (value >= 73 && value <= 78)) {
            return 35;
        }
        if (value >= 79 && value <= 84) {
            return 25;
        }
        if (value >= 45 && value <= 54) {
            return 20;
        }
        if (value >= 85 && value <= 90) {
            return 10;
        }
        return 0;
    }

    static int precipitationScore(BigDecimal probability) {
        if (probability == null) {
            return 0;
        }
        double value = probability.doubleValue();
        if (value <= 10) {
            return 25;
        }
        if (value <= 25) {
            return 20;
        }
        if (value <= 50) {
            return 10;
        }
        return 0;
    }

    static int windScore(BigDecimal windSpeedMph) {
        if (windSpeedMph == null) {
            return 0;
        }
        double value = windSpeedMph.doubleValue();
        if (value <= 10) {
            return 15;
        }
        if (value <= 15) {
            return 10;
        }
        if (value <= 20) {
            return 5;
        }
        return 0;
    }

    static int humidityScore(BigDecimal humidity) {
        if (humidity == null) {
            return 0;
        }
        double value = humidity.doubleValue();
        if (value >= 30 && value <= 60) {
            return 10;
        }
        if (value < 30 || value <= 75) {
            return 7;
        }
        if (value <= 85) {
            return 3;
        }
        return 0;
    }

    static int daylightScore(Boolean isDaytime) {
        return Boolean.TRUE.equals(isDaytime) ? 10 : 2;
    }

    static int daylightScore(Boolean isDaytime, Integer remainingDaylightMinutes) {
        if (remainingDaylightMinutes != null) {
            if (remainingDaylightMinutes <= 0) {
                return 2;
            }
            return remainingDaylightMinutes >= 60 ? 10 : 6;
        }
        return daylightScore(isDaytime);
    }

    static int aqiPenalty(BigDecimal aqi) {
        if (aqi == null) {
            return 0;
        }
        double value = aqi.doubleValue();
        if (value <= 50) {
            return 0;
        }
        if (value <= 100) {
            return 3;
        }
        if (value <= 150) {
            return 8;
        }
        return 15;
    }

    static int uvPenalty(BigDecimal uvIndex) {
        if (uvIndex == null) {
            return 0;
        }
        double value = uvIndex.doubleValue();
        if (value <= 2) {
            return 0;
        }
        if (value <= 5) {
            return 3;
        }
        if (value <= 7) {
            return 6;
        }
        return 10;
    }
}
