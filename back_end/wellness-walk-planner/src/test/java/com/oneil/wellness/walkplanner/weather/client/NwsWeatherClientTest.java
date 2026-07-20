package com.oneil.wellness.walkplanner.weather.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneil.wellness.walkplanner.config.WeatherNwsProperties;
import com.oneil.wellness.walkplanner.dto.HourlyForecastPeriod;
import com.oneil.wellness.walkplanner.dto.WeatherResponse;
import com.oneil.wellness.walkplanner.recommendation.dto.DailyOutlookDto;

class NwsWeatherClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NwsWeatherClient client = new NwsWeatherClient(new WeatherNwsProperties());

    @Test
    void mapsHourlyPeriodsWithProviderUnitsAndKeepsMissingValuesNullable() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "properties": {
                    "periods": [
                      {
                        "startTime": "2026-07-14T13:00:00-04:00",
                        "temperature": 72,
                        "temperatureUnit": "F",
                        "shortForecast": "Mostly Sunny",
                        "probabilityOfPrecipitation": { "value": 20 },
                        "relativeHumidity": { "value": 36 },
                        "windSpeed": "12 mph",
                        "windDirection": "NW",
                        "isDaytime": true,
                        "icon": "https://example.com/icon.png"
                      },
                      {
                        "startTime": "2026-07-14T14:00:00-04:00",
                        "temperature": 69,
                        "temperatureUnit": "F",
                        "shortForecast": "Cloudy",
                        "probabilityOfPrecipitation": {},
                        "relativeHumidity": {},
                        "windSpeed": "8 mph",
                        "windDirection": "NE",
                        "isDaytime": false,
                        "icon": "https://example.com/icon-2.png"
                      }
                    ]
                  }
                }
                """);

        List<HourlyForecastPeriod> result = client.parseHourlyForecastResponse(root);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).temperature()).isEqualByComparingTo("72");
        assertThat(result.get(0).temperatureUnit()).isEqualTo("°F");
        assertThat(result.get(0).iconUrl()).isEqualTo("https://example.com/icon.png");
        assertThat(result.get(0).precipitationProbability()).isEqualByComparingTo("20");
        assertThat(result.get(0).humidity()).isEqualByComparingTo("36");
        assertThat(result.get(1).humidity()).isNull();
        assertThat(result.get(1).precipitationProbability()).isNull();
        assertThat(result.get(1).windSpeed()).isEqualByComparingTo("8");
        assertThat(result.get(1).windDirection()).isEqualTo("NE");
    }

    @Test
    void keepsForecastDataSeparateFromObservationLabeling() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "properties": {
                    "periods": [
                      {
                        "startTime": "2026-07-14T13:00:00-04:00",
                        "temperature": 72,
                        "temperatureUnit": "F",
                        "shortForecast": "Mostly Sunny",
                        "probabilityOfPrecipitation": { "value": 10 },
                        "relativeHumidity": { "value": 44 },
                        "windSpeed": "10 mph",
                        "windDirection": "W",
                        "isDaytime": true,
                        "icon": "https://example.com/icon.png"
                      }
                    ]
                  }
                }
                """);

        List<HourlyForecastPeriod> periods = client.parseHourlyForecastResponse(root);
        WeatherResponse response = client.buildPublicWeatherResponse(
                "Haverhill, MA",
                new BigDecimal("42.7762"),
                new BigDecimal("-71.0773"),
                periods,
                List.of(),
                "HOURLY_FORECAST",
                null);

        assertThat(response.current().dataType()).isEqualTo("HOURLY_FORECAST");
        assertThat(response.hourlyForecast()).hasSize(1);
        assertThat(response.current().weatherCondition()).isEqualTo("Mostly Sunny");
        assertThat(response.current().iconUrl()).isEqualTo("https://example.com/icon.png");
    }

    @Test
    void mapsPointResponseWithDailyAndHourlyForecastUrls() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "properties": {
                    "forecast": "https://api.weather.gov/gridpoints/BOX/60,74/forecast",
                    "forecastHourly": "https://api.weather.gov/gridpoints/BOX/60,74/forecast/hourly",
                    "observationStations": "https://api.weather.gov/gridpoints/BOX/60,74/stations",
                    "relativeLocation": {
                      "properties": {
                        "city": "Haverhill",
                        "state": "MA"
                      }
                    }
                  }
                }
                """);

        NwsWeatherClient.PointResponse result = client.parsePointResponse(root);

        assertThat(result.locationName()).isEqualTo("Haverhill, MA");
        assertThat(result.forecastUrl()).isEqualTo("https://api.weather.gov/gridpoints/BOX/60,74/forecast");
        assertThat(result.forecastHourlyUrl()).isEqualTo("https://api.weather.gov/gridpoints/BOX/60,74/forecast/hourly");
    }

    @Test
    void mapsDailyNwsForecastPeriods() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "properties": {
                    "periods": [
                      {
                        "name": "Thursday",
                        "startTime": "2099-07-16T06:00:00-04:00",
                        "isDaytime": true,
                        "temperature": 78,
                        "temperatureUnit": "F",
                        "shortForecast": "Mostly Sunny",
                        "probabilityOfPrecipitation": { "value": 10 },
                        "icon": "https://example.com/day.png"
                      },
                      {
                        "name": "Thursday Night",
                        "startTime": "2099-07-16T18:00:00-04:00",
                        "isDaytime": false,
                        "temperature": 62,
                        "temperatureUnit": "F",
                        "shortForecast": "Partly Cloudy",
                        "probabilityOfPrecipitation": { "value": 20 },
                        "icon": "https://example.com/night.png"
                      }
                    ]
                  }
                }
                """);

        List<NwsWeatherClient.DailyForecastPeriod> result = client.parseDailyForecastResponse(root);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).iconUrl()).isEqualTo("https://example.com/day.png");
        assertThat(result.get(0).precipitationProbability()).isEqualByComparingTo("10");
        assertThat(result.get(1).temperature()).isEqualByComparingTo("62");
    }

    @Test
    void derivesDailyRepresentativeFromHourlyDataAndLeavesLaterDaysUnscored() throws Exception {
        List<NwsWeatherClient.DailyForecastPeriod> dailyPeriods = List.of(
                new NwsWeatherClient.DailyForecastPeriod(
                        "2099-07-16T06:00:00-04:00",
                        "Thursday",
                        true,
                        new BigDecimal("78"),
                        "°F",
                        "https://example.com/day.png",
                        "Mostly Sunny",
                        BigDecimal.TEN),
                new NwsWeatherClient.DailyForecastPeriod(
                        "2099-07-16T18:00:00-04:00",
                        "Thursday Night",
                        false,
                        new BigDecimal("62"),
                        "°F",
                        "https://example.com/night.png",
                        "Partly Cloudy",
                        BigDecimal.valueOf(20)),
                new NwsWeatherClient.DailyForecastPeriod(
                        "2099-07-17T06:00:00-04:00",
                        "Friday",
                        true,
                        new BigDecimal("85"),
                        "°F",
                        "https://example.com/friday.png",
                        "Hot",
                        null));
        List<HourlyForecastPeriod> hourlyPeriods = client.parseHourlyForecastResponse(objectMapper.readTree("""
                {
                  "properties": {
                    "periods": [
                      {
                        "startTime": "2099-07-16T08:00:00-04:00",
                        "temperature": 70,
                        "temperatureUnit": "F",
                        "shortForecast": "Sunny",
                        "probabilityOfPrecipitation": { "value": 0 },
                        "relativeHumidity": { "value": 45 },
                        "windSpeed": "5 mph",
                        "windDirection": "NW",
                        "isDaytime": true,
                        "icon": "https://example.com/icon.png"
                      }
                    ]
                  }
                }
                """)).stream()
                .map(period -> period.withWalkingRecommendation(
                        new com.oneil.wellness.walkplanner.recommendation.service.WalkingRecommendationService(
                                new com.oneil.wellness.walkplanner.recommendation.service.WalkingScoreService())
                                .recommendationFor(period)))
                .toList();

        List<DailyOutlookDto> result = client.buildWeeklyOutlook(dailyPeriods, hourlyPeriods);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).date()).isEqualTo("2099-07-16");
        assertThat(result.get(0).highTemperature()).isEqualByComparingTo("78");
        assertThat(result.get(0).lowTemperature()).isEqualByComparingTo("62");
        assertThat(result.get(0).representativeScore()).isEqualTo(70);
        assertThat(result.get(0).bestAvailableTime()).isEqualTo("2099-07-16T08:00:00-04:00");
        assertThat(result.get(1).representativeScore()).isNull();
        assertThat(result.get(1).ratingLabel()).isEqualTo("Not enough hourly data");
    }
}
