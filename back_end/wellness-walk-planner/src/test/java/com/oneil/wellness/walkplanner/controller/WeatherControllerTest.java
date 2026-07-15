package com.oneil.wellness.walkplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneil.wellness.walkplanner.dto.CurrentWeatherSummary;
import com.oneil.wellness.walkplanner.dto.WeatherResponse;
import com.oneil.wellness.walkplanner.exception.WeatherServiceException;
import com.oneil.wellness.walkplanner.recommendation.dto.BestWalkingWindowDto;
import com.oneil.wellness.walkplanner.recommendation.dto.DailyOutlookDto;
import com.oneil.wellness.walkplanner.recommendation.model.WalkingRating;
import com.oneil.wellness.walkplanner.service.WeatherService;
import com.oneil.wellness.walkplanner.zip.service.ZipCodeNotFoundException;
import com.oneil.wellness.walkplanner.zip.service.ZipLookupException;
import com.oneil.wellness.walkplanner.zip.service.ZipWeatherService;

class WeatherControllerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void rejectsCoordinatesOutsideValidRange() {
        WeatherController controller = new WeatherController(null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.currentWeather(new BigDecimal("91"), new BigDecimal("-77.0352")));

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void returnsWeatherForValidZip() {
        WeatherResponse response = weatherResponse();
        WeatherController controller = new WeatherController(null, new TestZipWeatherService(response));

        WeatherResponse result = controller.currentWeatherByZip("01830");

        assertThat(result).isSameAs(response);
    }

    @Test
    void publicResponseSerializesRecommendationAndWeeklyOutlook() throws Exception {
        String json = objectMapper.writeValueAsString(weatherResponse());

        assertThat(json).contains("\"bestWalkingWindow\"");
        assertThat(json).contains("\"score\":88");
        assertThat(json).contains("\"rating\":\"GREAT\"");
        assertThat(json).contains("\"weeklyOutlook\"");
        assertThat(json).contains("\"summary\":\"Great walking weather\"");
    }

    @Test
    void rejectsInvalidZipFormat() {
        WeatherController controller = new WeatherController(null, new TestZipWeatherService(weatherResponse()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.currentWeatherByZip("18A30"));

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getReason()).contains("5-digit ZIP");
    }

    @Test
    void returnsNotFoundForUnknownZip() {
        WeatherController controller = new WeatherController(null,
                new TestZipWeatherService(new ZipCodeNotFoundException("not found")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.currentWeatherByZip("00000"));

        assertThat(ex.getStatusCode().value()).isEqualTo(404);
        assertThat(ex.getReason()).contains("not found");
    }

    @Test
    void returnsServiceUnavailableForZipProviderFailure() {
        WeatherController controller = new WeatherController(null,
                new TestZipWeatherService(new ZipLookupException("timeout")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.currentWeatherByZip("01830"));

        assertThat(ex.getStatusCode().value()).isEqualTo(503);
        assertThat(ex.getReason()).contains("temporarily unavailable");
    }

    @Test
    void returnsBadGatewayForWeatherFailureAfterZipLookup() {
        WeatherController controller = new WeatherController(null,
                new TestZipWeatherService(new WeatherServiceException("weather down")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.currentWeatherByZip("01830"));

        assertThat(ex.getStatusCode().value()).isEqualTo(502);
        assertThat(ex.getReason()).contains("Weather unavailable");
    }

    private WeatherResponse weatherResponse() {
        return new WeatherResponse(
                "Haverhill, MA",
                new BigDecimal("42.7762"),
                new BigDecimal("-71.0773"),
                new CurrentWeatherSummary(
                        new BigDecimal("20.0"),
                        "°C",
                        null,
                        null,
                        null,
                        null,
                        "Clear",
                        null,
                        "2026-07-14T12:00:00Z",
                        "HOURLY_FORECAST"),
                null,
                new BestWalkingWindowDto(
                        "2026-07-14T13:00:00Z",
                        "2026-07-14T14:00:00Z",
                        88,
                        WalkingRating.GREAT,
                        "Great",
                        "Great weather for a restorative walk.",
                        List.of("Low chance of rain"),
                        List.of()),
                List.of(),
                List.of(new DailyOutlookDto(
                        "2026-07-14",
                        "Tuesday",
                        "https://example.com/icon.png",
                        "Sunny",
                        new BigDecimal("78"),
                        new BigDecimal("62"),
                        "°F",
                        BigDecimal.ZERO,
                        88,
                        WalkingRating.GREAT,
                        "Great",
                        "2026-07-14T13:00:00Z",
                        "Great walking weather",
                        List.of())));
    }

    private static class TestZipWeatherService extends ZipWeatherService {

        private final WeatherResponse response;
        private final RuntimeException exception;

        TestZipWeatherService(WeatherResponse response) {
            super(null, null);
            this.response = response;
            this.exception = null;
        }

        TestZipWeatherService(RuntimeException exception) {
            super(null, null);
            this.response = null;
            this.exception = exception;
        }

        @Override
        public WeatherResponse getCurrentWeather(String zipCode) {
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }
}
