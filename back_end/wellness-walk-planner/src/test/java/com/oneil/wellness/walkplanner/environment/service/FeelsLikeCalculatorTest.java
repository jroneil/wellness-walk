package com.oneil.wellness.walkplanner.environment.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class FeelsLikeCalculatorTest {

    private final FeelsLikeCalculator calculator = new FeelsLikeCalculator();

    @Test
    void calculatesHeatIndexInsideDocumentedRange() {
        FeelsLikeCalculator.FeelsLikeResult result = calculator.calculate(
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(70),
                BigDecimal.valueOf(5));

        assertThat(result.method().name()).isEqualTo("HEAT_INDEX");
        assertThat(result.temperature()).isEqualByComparingTo("105.9");
    }

    @Test
    void calculatesWindChillInsideDocumentedRange() {
        FeelsLikeCalculator.FeelsLikeResult result = calculator.calculate(
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(10));

        assertThat(result.method().name()).isEqualTo("WIND_CHILL");
        assertThat(result.temperature()).isEqualByComparingTo("21.2");
    }

    @Test
    void usesActualTemperatureOutsideDocumentedRanges() {
        FeelsLikeCalculator.FeelsLikeResult result = calculator.calculate(
                BigDecimal.valueOf(75),
                BigDecimal.valueOf(70),
                BigDecimal.valueOf(10));

        assertThat(result.method().name()).isEqualTo("ACTUAL_TEMPERATURE");
        assertThat(result.temperature()).isEqualByComparingTo("75.0");
    }
}
