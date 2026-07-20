package com.oneil.wellness.walkplanner.recommendation.service;

import java.math.BigDecimal;

final class WalkingScorePolicy {

    private WalkingScorePolicy() {
    }

    static int temperatureScore(BigDecimal temperatureFahrenheit) {
        double value = temperatureFahrenheit.doubleValue();
        if (value >= 60 && value <= 72) {
            return 30;
        }
        if ((value >= 55 && value <= 59) || (value >= 73 && value <= 78)) {
            return 26;
        }
        if (value >= 79 && value <= 84) {
            return 18;
        }
        if (value >= 45 && value <= 54) {
            return 15;
        }
        if (value >= 85 && value <= 90) {
            return 8;
        }
        return 0;
    }

    static int precipitationScore(BigDecimal probability) {
        if (probability == null) {
            return 0;
        }
        double value = probability.doubleValue();
        if (value <= 10) {
            return 20;
        }
        if (value <= 25) {
            return 16;
        }
        if (value <= 50) {
            return 8;
        }
        return 0;
    }

    static int windScore(BigDecimal windSpeedMph) {
        if (windSpeedMph == null) {
            return 0;
        }
        double value = windSpeedMph.doubleValue();
        if (value <= 10) {
            return 10;
        }
        if (value <= 15) {
            return 7;
        }
        if (value <= 20) {
            return 3;
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

    static int daylightScore(String daylightStatus) {
        if ("DAYLIGHT".equals(daylightStatus)) {
            return 10;
        }
        if ("TWILIGHT".equals(daylightStatus)) {
            return 5;
        }
        if ("NIGHT".equals(daylightStatus)) {
            return 2;
        }
        return 0;
    }

    static int airQualityScore(BigDecimal aqi) {
        if (aqi == null) {
            return 0;
        }
        double value = aqi.doubleValue();
        if (value <= 50) {
            return 10;
        }
        if (value <= 100) {
            return 7;
        }
        if (value <= 150) {
            return 3;
        }
        return 0;
    }

    static int uvScore(BigDecimal uvIndex) {
        if (uvIndex == null) {
            return 0;
        }
        double value = uvIndex.doubleValue();
        if (value <= 2) {
            return 10;
        }
        if (value <= 5) {
            return 7;
        }
        if (value <= 7) {
            return 3;
        }
        return 0;
    }
}
