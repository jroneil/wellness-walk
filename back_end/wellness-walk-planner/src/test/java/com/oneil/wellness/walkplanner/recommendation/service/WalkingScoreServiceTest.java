package com.oneil.wellness.walkplanner.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.oneil.wellness.walkplanner.dto.HourlyForecastPeriod;
import com.oneil.wellness.walkplanner.recommendation.model.ScoredWalkingPeriod;
import com.oneil.wellness.walkplanner.recommendation.model.WalkingRating;

class WalkingScoreServiceTest {

    private final WalkingScoreService service = new WalkingScoreService();

    @ParameterizedTest
    @MethodSource("temperatureCases")
    void scoresTemperatureBoundaries(String temperature, int expectedScore) {
        ScoredWalkingPeriod result = service.score(period(new BigDecimal(temperature), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(40), true));

        assertThat(result.temperatureScore()).isEqualTo(expectedScore);
    }

    static Stream<Arguments> temperatureCases() {
        return Stream.of(
                Arguments.of("60", 40),
                Arguments.of("72", 40),
                Arguments.of("55", 35),
                Arguments.of("59", 35),
                Arguments.of("73", 35),
                Arguments.of("78", 35),
                Arguments.of("79", 25),
                Arguments.of("84", 25),
                Arguments.of("45", 20),
                Arguments.of("54", 20),
                Arguments.of("85", 10),
                Arguments.of("90", 10),
                Arguments.of("44", 0),
                Arguments.of("91", 0));
    }

    @ParameterizedTest
    @MethodSource("precipitationCases")
    void scoresPrecipitationBoundaries(BigDecimal precipitation, int expectedScore) {
        ScoredWalkingPeriod result = service.score(period(BigDecimal.valueOf(70), precipitation, BigDecimal.ZERO,
                BigDecimal.valueOf(40), true));

        assertThat(result.precipitationScore()).isEqualTo(expectedScore);
    }

    static Stream<Arguments> precipitationCases() {
        return Stream.of(
                Arguments.of(BigDecimal.ZERO, 25),
                Arguments.of(BigDecimal.TEN, 25),
                Arguments.of(BigDecimal.valueOf(11), 20),
                Arguments.of(BigDecimal.valueOf(25), 20),
                Arguments.of(BigDecimal.valueOf(26), 10),
                Arguments.of(BigDecimal.valueOf(50), 10),
                Arguments.of(BigDecimal.valueOf(51), 0),
                Arguments.of(null, 0));
    }

    @ParameterizedTest
    @MethodSource("windCases")
    void scoresWindBoundaries(BigDecimal windSpeed, int expectedScore) {
        ScoredWalkingPeriod result = service.score(period(BigDecimal.valueOf(70), BigDecimal.ZERO, windSpeed,
                BigDecimal.valueOf(40), true));

        assertThat(result.windScore()).isEqualTo(expectedScore);
    }

    static Stream<Arguments> windCases() {
        return Stream.of(
                Arguments.of(BigDecimal.ZERO, 15),
                Arguments.of(BigDecimal.TEN, 15),
                Arguments.of(BigDecimal.valueOf(11), 10),
                Arguments.of(BigDecimal.valueOf(15), 10),
                Arguments.of(BigDecimal.valueOf(16), 5),
                Arguments.of(BigDecimal.valueOf(20), 5),
                Arguments.of(BigDecimal.valueOf(21), 0),
                Arguments.of(null, 0));
    }

    @ParameterizedTest
    @MethodSource("humidityCases")
    void scoresHumidityBoundaries(BigDecimal humidity, int expectedScore) {
        ScoredWalkingPeriod result = service.score(period(BigDecimal.valueOf(70), BigDecimal.ZERO, BigDecimal.ZERO,
                humidity, true));

        assertThat(result.humidityScore()).isEqualTo(expectedScore);
    }

    static Stream<Arguments> humidityCases() {
        return Stream.of(
                Arguments.of(BigDecimal.valueOf(30), 10),
                Arguments.of(BigDecimal.valueOf(60), 10),
                Arguments.of(BigDecimal.valueOf(61), 7),
                Arguments.of(BigDecimal.valueOf(75), 7),
                Arguments.of(BigDecimal.valueOf(76), 3),
                Arguments.of(BigDecimal.valueOf(85), 3),
                Arguments.of(BigDecimal.valueOf(86), 0),
                Arguments.of(BigDecimal.valueOf(29), 7),
                Arguments.of(null, 0));
    }

    @Test
    void scoresDaylightAndNighttime() {
        assertThat(service.score(period(BigDecimal.valueOf(70), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(40), true)).daylightScore()).isEqualTo(10);
        assertThat(service.score(period(BigDecimal.valueOf(70), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(40), false)).daylightScore()).isEqualTo(2);
    }

    @Test
    void missingTemperatureIsNotScorableAndOptionalMissingValuesAreExplained() {
        ScoredWalkingPeriod missingTemperature = service.score(period(null, null, null, null, true));
        ScoredWalkingPeriod missingOptional = service.score(period(BigDecimal.valueOf(70), null, null, null, true));

        assertThat(missingTemperature.score()).isNull();
        assertThat(missingTemperature.reasons()).contains("Temperature unavailable");
        assertThat(missingOptional.score()).isEqualTo(50);
        assertThat(missingOptional.precipitationScore()).isZero();
        assertThat(missingOptional.windScore()).isZero();
        assertThat(missingOptional.humidityScore()).isZero();
        assertThat(missingOptional.reasons()).contains("Rain chance unavailable", "Wind speed unavailable", "Humidity unavailable");
    }

    @Test
    void usesFeelsLikeTemperatureAndExplainsHeatIndex() {
        ScoredWalkingPeriod result = service.score(period(BigDecimal.valueOf(86), BigDecimal.ZERO, BigDecimal.valueOf(5),
                BigDecimal.valueOf(70), true)
                        .withEnvironmentalData(BigDecimal.valueOf(97), "HEAT_INDEX", null, null, null, null, null));

        assertThat(result.temperatureScore()).isZero();
        assertThat(result.score()).isEqualTo(57);
        assertThat(result.feelsLikeTemperature()).isEqualByComparingTo("97");
        assertThat(result.reasons()).contains("Feels like 97°F due to humidity");
        assertThat(result.warnings()).contains("Excessive heat");
    }

    @Test
    void scoresAqiAndUvPenaltiesAndExplainsPointRemoval() {
        ScoredWalkingPeriod result = service.score(period(BigDecimal.valueOf(70), BigDecimal.ZERO, BigDecimal.valueOf(5),
                BigDecimal.valueOf(45), true)
                        .withEnvironmentalData(BigDecimal.valueOf(70), "ACTUAL", BigDecimal.valueOf(8),
                                BigDecimal.valueOf(151), "2026-07-15T05:30", "2026-07-15T20:30", 120));

        assertThat(result.aqiPenalty()).isEqualTo(15);
        assertThat(result.uvPenalty()).isEqualTo(10);
        assertThat(result.daylightScore()).isEqualTo(10);
        assertThat(result.score()).isEqualTo(75);
        assertThat(result.reasons()).contains("Air quality reduced score by 15 points", "UV reduced score by 10 points");
        assertThat(result.warnings()).contains("Poor air quality", "High UV", "Sun protection recommended");
    }

    @Test
    void missingAqiUvAndSunriseSunsetDoNotBreakRecommendations() {
        ScoredWalkingPeriod result = service.score(period(BigDecimal.valueOf(70), BigDecimal.ZERO, BigDecimal.valueOf(5),
                BigDecimal.valueOf(45), true));

        assertThat(result.aqiPenalty()).isZero();
        assertThat(result.uvPenalty()).isZero();
        assertThat(result.daylightScore()).isEqualTo(10);
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.reasons()).contains("Air quality unavailable", "UV index unavailable");
    }

    @Test
    void calculatesTotalRatingAndWarnings() {
        ScoredWalkingPeriod result = service.score(period(BigDecimal.valueOf(91), BigDecimal.valueOf(70),
                BigDecimal.valueOf(25), BigDecimal.valueOf(90), false));

        assertThat(result.score()).isEqualTo(2);
        assertThat(result.rating()).isEqualTo(WalkingRating.NOT_RECOMMENDED);
        assertThat(result.warnings()).containsExactly(
                "Excessive heat",
                "Rain is likely",
                "Strong wind",
                "Very high humidity",
                "Limited daylight");
    }

    @ParameterizedTest
    @MethodSource("ratingCases")
    void mapsRatingBoundaries(int score, WalkingRating rating) {
        assertThat(WalkingRating.fromScore(score)).isEqualTo(rating);
    }

    static Stream<Arguments> ratingCases() {
        return Stream.of(
                Arguments.of(100, WalkingRating.EXCELLENT),
                Arguments.of(90, WalkingRating.EXCELLENT),
                Arguments.of(89, WalkingRating.GREAT),
                Arguments.of(75, WalkingRating.GREAT),
                Arguments.of(74, WalkingRating.GOOD),
                Arguments.of(60, WalkingRating.GOOD),
                Arguments.of(59, WalkingRating.FAIR),
                Arguments.of(40, WalkingRating.FAIR),
                Arguments.of(39, WalkingRating.POOR),
                Arguments.of(20, WalkingRating.POOR),
                Arguments.of(19, WalkingRating.NOT_RECOMMENDED),
                Arguments.of(0, WalkingRating.NOT_RECOMMENDED));
    }

    private HourlyForecastPeriod period(BigDecimal temperature, BigDecimal precipitation, BigDecimal windSpeed,
            BigDecimal humidity, Boolean isDaytime) {
        return new HourlyForecastPeriod(
                "2026-07-15T13:00:00-04:00",
                temperature,
                "°F",
                "Sunny",
                "https://example.com/icon.png",
                precipitation,
                humidity,
                windSpeed,
                "NW",
                isDaytime,
                temperature,
                "ACTUAL",
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
