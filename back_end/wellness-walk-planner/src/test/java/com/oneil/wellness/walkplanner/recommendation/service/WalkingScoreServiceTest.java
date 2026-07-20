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
    void scoresFeelsLikeTemperatureBoundaries(String temperature, int expectedScore) {
        ScoredWalkingPeriod result = service.score(period(new BigDecimal(temperature), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(40), true));

        assertThat(result.temperatureScore()).isEqualTo(expectedScore);
    }

    static Stream<Arguments> temperatureCases() {
        return Stream.of(
                Arguments.of("60", 30),
                Arguments.of("72", 30),
                Arguments.of("55", 26),
                Arguments.of("59", 26),
                Arguments.of("73", 26),
                Arguments.of("78", 26),
                Arguments.of("79", 18),
                Arguments.of("84", 18),
                Arguments.of("45", 15),
                Arguments.of("54", 15),
                Arguments.of("85", 8),
                Arguments.of("90", 8),
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
                Arguments.of(BigDecimal.ZERO, 20),
                Arguments.of(BigDecimal.TEN, 20),
                Arguments.of(BigDecimal.valueOf(11), 16),
                Arguments.of(BigDecimal.valueOf(25), 16),
                Arguments.of(BigDecimal.valueOf(26), 8),
                Arguments.of(BigDecimal.valueOf(50), 8),
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
                Arguments.of(BigDecimal.ZERO, 10),
                Arguments.of(BigDecimal.TEN, 10),
                Arguments.of(BigDecimal.valueOf(11), 7),
                Arguments.of(BigDecimal.valueOf(15), 7),
                Arguments.of(BigDecimal.valueOf(16), 3),
                Arguments.of(BigDecimal.valueOf(20), 3),
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

    @ParameterizedTest
    @MethodSource("daylightCases")
    void scoresDaylightBoundaries(String daylightStatus, int expectedScore) {
        ScoredWalkingPeriod result = service.score(period(BigDecimal.valueOf(70), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(40), true, daylightStatus));

        assertThat(result.daylightScore()).isEqualTo(expectedScore);
    }

    static Stream<Arguments> daylightCases() {
        return Stream.of(
                Arguments.of("DAYLIGHT", 10),
                Arguments.of("TWILIGHT", 5),
                Arguments.of("NIGHT", 2),
                Arguments.of("UNKNOWN", 0),
                Arguments.of(null, 0));
    }

    @ParameterizedTest
    @MethodSource("aqiCases")
    void scoresAirQualityBoundaries(BigDecimal aqi, int expectedScore) {
        ScoredWalkingPeriod result = service.score(withEnvironment(
                period(BigDecimal.valueOf(70), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(40), true),
                null,
                aqi,
                "DAYLIGHT"));

        assertThat(result.airQualityScore()).isEqualTo(expectedScore);
    }

    static Stream<Arguments> aqiCases() {
        return Stream.of(
                Arguments.of(BigDecimal.ZERO, 10),
                Arguments.of(BigDecimal.valueOf(50), 10),
                Arguments.of(BigDecimal.valueOf(51), 7),
                Arguments.of(BigDecimal.valueOf(100), 7),
                Arguments.of(BigDecimal.valueOf(101), 3),
                Arguments.of(BigDecimal.valueOf(150), 3),
                Arguments.of(BigDecimal.valueOf(151), 0),
                Arguments.of(null, 0));
    }

    @ParameterizedTest
    @MethodSource("uvCases")
    void scoresUvBoundaries(BigDecimal uvIndex, int expectedScore) {
        ScoredWalkingPeriod result = service.score(withEnvironment(
                period(BigDecimal.valueOf(70), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(40), true),
                uvIndex,
                null,
                "DAYLIGHT"));

        assertThat(result.uvScore()).isEqualTo(expectedScore);
    }

    static Stream<Arguments> uvCases() {
        return Stream.of(
                Arguments.of(BigDecimal.ZERO, 10),
                Arguments.of(BigDecimal.valueOf(2), 10),
                Arguments.of(BigDecimal.valueOf(3), 7),
                Arguments.of(BigDecimal.valueOf(5), 7),
                Arguments.of(BigDecimal.valueOf(6), 3),
                Arguments.of(BigDecimal.valueOf(7), 3),
                Arguments.of(BigDecimal.valueOf(8), 0),
                Arguments.of(null, 0));
    }

    @Test
    void missingTemperatureIsNotScorableAndOptionalMissingValuesAreExplained() {
        ScoredWalkingPeriod missingTemperature = service.score(period(null, null, null, null, true));
        ScoredWalkingPeriod missingOptional = service.score(period(BigDecimal.valueOf(70), null, null, null, true, "UNKNOWN"));

        assertThat(missingTemperature.score()).isNull();
        assertThat(missingTemperature.reasons()).contains("Temperature unavailable");
        assertThat(missingOptional.score()).isEqualTo(30);
        assertThat(missingOptional.precipitationScore()).isZero();
        assertThat(missingOptional.windScore()).isZero();
        assertThat(missingOptional.humidityScore()).isZero();
        assertThat(missingOptional.daylightScore()).isZero();
        assertThat(missingOptional.airQualityScore()).isZero();
        assertThat(missingOptional.uvScore()).isZero();
        assertThat(missingOptional.reasons()).contains(
                "Rain chance unavailable",
                "Wind speed unavailable",
                "Humidity unavailable",
                "Sunrise and sunset unavailable",
                "Air quality unavailable",
                "UV data unavailable");
    }

    @Test
    void usesFeelsLikeTemperatureAndExplainsHeatIndex() {
        ScoredWalkingPeriod result = service.score(withEnvironment(
                period(BigDecimal.valueOf(86), BigDecimal.ZERO, BigDecimal.valueOf(5), BigDecimal.valueOf(70), true),
                BigDecimal.valueOf(97),
                "HEAT_INDEX",
                null,
                null,
                "DAYLIGHT"));

        assertThat(result.temperatureScore()).isZero();
        assertThat(result.feelsLikeTemperature()).isEqualByComparingTo("97");
        assertThat(result.reasons()).contains("Feels like 97°F because of heat and humidity");
        assertThat(result.warnings()).contains("Excessive heat");
    }

    @Test
    void usesFeelsLikeTemperatureAndExplainsWindChill() {
        ScoredWalkingPeriod result = service.score(withEnvironment(
                period(BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.valueOf(10), BigDecimal.valueOf(50), true),
                BigDecimal.valueOf(24),
                "WIND_CHILL",
                null,
                null,
                "DAYLIGHT"));

        assertThat(result.temperatureScore()).isZero();
        assertThat(result.reasons()).contains("Feels like 24°F because of wind chill");
        assertThat(result.warnings()).contains("Freezing conditions");
    }

    @Test
    void totalEqualsSevenVisibleCategoryScores() {
        ScoredWalkingPeriod result = service.score(withEnvironment(
                period(BigDecimal.valueOf(70), BigDecimal.ZERO, BigDecimal.valueOf(5), BigDecimal.valueOf(45), true),
                BigDecimal.valueOf(8),
                BigDecimal.valueOf(151),
                "DAYLIGHT"));

        int visibleTotal = result.temperatureScore()
                + result.precipitationScore()
                + result.windScore()
                + result.humidityScore()
                + result.daylightScore()
                + result.airQualityScore()
                + result.uvScore();

        assertThat(result.airQualityScore()).isZero();
        assertThat(result.uvScore()).isZero();
        assertThat(result.score()).isEqualTo(80);
        assertThat(result.score()).isEqualTo(visibleTotal);
        assertThat(result.score()).isBetween(0, 100);
        assertThat(result.reasons()).contains("Poor air quality", "Very High UV exposure");
        assertThat(result.warnings()).contains("Poor air quality", "High UV", "Sun protection recommended");
    }

    @Test
    void calculatesRatingAndWarningsWithoutHiddenDeductions() {
        ScoredWalkingPeriod result = service.score(withEnvironment(
                period(BigDecimal.valueOf(91), BigDecimal.valueOf(70), BigDecimal.valueOf(25), BigDecimal.valueOf(90), false, "NIGHT"),
                BigDecimal.valueOf(9),
                BigDecimal.valueOf(120),
                "NIGHT"));

        assertThat(result.score()).isEqualTo(5);
        assertThat(result.rating()).isEqualTo(WalkingRating.NOT_RECOMMENDED);
        assertThat(result.warnings()).containsExactly(
                "Excessive heat",
                "Rain is likely",
                "Strong wind",
                "Very high humidity",
                "Air quality may be unhealthy for sensitive groups",
                "High UV",
                "Sun protection recommended",
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

    private HourlyForecastPeriod withEnvironment(HourlyForecastPeriod period, BigDecimal uvIndex, BigDecimal aqi,
            String daylightStatus) {
        return withEnvironment(period, period.temperature(), "ACTUAL_TEMPERATURE", uvIndex, aqi, daylightStatus);
    }

    private HourlyForecastPeriod withEnvironment(HourlyForecastPeriod period, BigDecimal feelsLikeTemperature,
            String feelsLikeMethod, BigDecimal uvIndex, BigDecimal aqi, String daylightStatus) {
        return period.withEnvironmentalData(
                feelsLikeTemperature,
                feelsLikeMethod,
                uvIndex,
                uvIndex == null ? "Unavailable" : uvIndex.compareTo(BigDecimal.valueOf(7)) > 0 ? "Very High" : "Low",
                uvIndex == null ? null : "2026-07-15T13:00",
                uvIndex == null ? null : "Open-Meteo Forecast API",
                aqi,
                aqi == null ? "Unavailable" : aqi.compareTo(BigDecimal.valueOf(150)) > 0 ? "Unhealthy" : "Moderate",
                aqi == null ? null : "2026-07-15T13:00",
                aqi == null ? null : "Open-Meteo Air Quality API",
                "2026-07-15T05:30",
                "2026-07-15T20:30",
                daylightStatus,
                "DAYLIGHT".equals(daylightStatus) ? 450 : null);
    }

    private HourlyForecastPeriod period(BigDecimal temperature, BigDecimal precipitation, BigDecimal windSpeed,
            BigDecimal humidity, Boolean isDaytime) {
        return period(temperature, precipitation, windSpeed, humidity, isDaytime, "DAYLIGHT");
    }

    private HourlyForecastPeriod period(BigDecimal temperature, BigDecimal precipitation, BigDecimal windSpeed,
            BigDecimal humidity, Boolean isDaytime, String daylightStatus) {
        return new HourlyForecastPeriod(
                "2026-07-15T13:00:00-04:00",
                temperature,
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
                temperature == null ? null : "ACTUAL_TEMPERATURE",
                null,
                "Unavailable",
                null,
                null,
                null,
                "Unavailable",
                null,
                null,
                daylightStatus == null ? null : "2026-07-15T05:30",
                daylightStatus == null ? null : "2026-07-15T20:30",
                daylightStatus,
                "DAYLIGHT".equals(daylightStatus) ? 450 : null,
                null);
    }
}
