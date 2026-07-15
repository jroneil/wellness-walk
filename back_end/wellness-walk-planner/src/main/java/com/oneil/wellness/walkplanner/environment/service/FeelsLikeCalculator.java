package com.oneil.wellness.walkplanner.environment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

@Service
public class FeelsLikeCalculator {

    public FeelsLikeResult calculate(BigDecimal temperatureFahrenheit, BigDecimal humidity, BigDecimal windSpeedMph) {
        if (temperatureFahrenheit == null) {
            return new FeelsLikeResult(null, "UNAVAILABLE");
        }
        double temperature = temperatureFahrenheit.doubleValue();
        if (humidity != null && temperature >= 80 && humidity.doubleValue() >= 40) {
            return new FeelsLikeResult(round(heatIndex(temperature, humidity.doubleValue())), "HEAT_INDEX");
        }
        if (windSpeedMph != null && temperature <= 50 && windSpeedMph.doubleValue() >= 3) {
            return new FeelsLikeResult(round(windChill(temperature, windSpeedMph.doubleValue())), "WIND_CHILL");
        }
        return new FeelsLikeResult(temperatureFahrenheit.setScale(1, RoundingMode.HALF_UP), "ACTUAL");
    }

    private double heatIndex(double temperature, double humidity) {
        return -42.379
                + 2.04901523 * temperature
                + 10.14333127 * humidity
                - 0.22475541 * temperature * humidity
                - 0.00683783 * temperature * temperature
                - 0.05481717 * humidity * humidity
                + 0.00122874 * temperature * temperature * humidity
                + 0.00085282 * temperature * humidity * humidity
                - 0.00000199 * temperature * temperature * humidity * humidity;
    }

    private double windChill(double temperature, double windSpeed) {
        return 35.74
                + 0.6215 * temperature
                - 35.75 * Math.pow(windSpeed, 0.16)
                + 0.4275 * temperature * Math.pow(windSpeed, 0.16);
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    public record FeelsLikeResult(BigDecimal temperature, String source) {
    }
}
