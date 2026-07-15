package com.oneil.wellness.walkplanner.zip.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.oneil.wellness.walkplanner.dto.CurrentWeatherSummary;
import com.oneil.wellness.walkplanner.dto.WeatherResponse;
import com.oneil.wellness.walkplanner.service.WeatherService;
import com.oneil.wellness.walkplanner.config.ZipLookupProperties;
import com.oneil.wellness.walkplanner.zip.client.ZipLookupClient;

class ZipWeatherServiceTest {

    @Test
    void reusesWeatherServiceWithLookedUpCoordinates() {
        ZipCoordinates coordinates = new ZipCoordinates(new BigDecimal("42.7762"), new BigDecimal("-71.0773"));
        WeatherResponse weatherResponse = weatherResponse();
        TestZipLookupClient zipLookupClient = new TestZipLookupClient(coordinates);
        TestWeatherService weatherService = new TestWeatherService(weatherResponse);

        ZipWeatherService service = new ZipWeatherService(zipLookupClient, weatherService);

        WeatherResponse result = service.getCurrentWeather("01830");

        assertThat(result).isSameAs(weatherResponse);
        assertThat(zipLookupClient.requestedZip).isEqualTo("01830");
        assertThat(weatherService.requestedLatitude).isEqualByComparingTo("42.7762");
        assertThat(weatherService.requestedLongitude).isEqualByComparingTo("-71.0773");
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
                null,
                List.of(),
                List.of());
    }

    private static class TestZipLookupClient extends ZipLookupClient {

        private final ZipCoordinates coordinates;
        private String requestedZip;

        TestZipLookupClient(ZipCoordinates coordinates) {
            super(new ZipLookupProperties());
            this.coordinates = coordinates;
        }

        @Override
        public ZipCoordinates lookup(String zipCode) {
            requestedZip = zipCode;
            return coordinates;
        }
    }

    private static class TestWeatherService extends WeatherService {

        private final WeatherResponse response;
        private BigDecimal requestedLatitude;
        private BigDecimal requestedLongitude;

        TestWeatherService(WeatherResponse response) {
            super(null);
            this.response = response;
        }

        @Override
        public WeatherResponse getCurrentWeather(BigDecimal latitude, BigDecimal longitude) {
            requestedLatitude = latitude;
            requestedLongitude = longitude;
            return response;
        }
    }
}
