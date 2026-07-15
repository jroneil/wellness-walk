package com.oneil.wellness.walkplanner.environment.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.oneil.wellness.walkplanner.environment.dto.EnvironmentalForecast;

class OpenMeteoEnvironmentClientTest {

    private final OpenMeteoEnvironmentClient client = new OpenMeteoEnvironmentClient();

    @Test
    void parsesForecastUvSunriseAndSunset() {
        String body = """
                {
                  "hourly": {
                    "time": ["2026-07-15T08:00", "2026-07-15T09:00"],
                    "uv_index": [1.2, 3.6]
                  },
                  "daily": {
                    "time": ["2026-07-15"],
                    "uv_index_max": [8.4],
                    "sunrise": ["2026-07-15T05:20"],
                    "sunset": ["2026-07-15T20:18"]
                  }
                }
                """;

        OpenMeteoEnvironmentClient.ForecastData result = client.parseForecastResponse(body).orElseThrow();

        assertThat(result.uvByTime().get("2026-07-15T09:00")).isEqualByComparingTo("3.6");
        EnvironmentalForecast.DailyEnvironment day = result.dailyByDate().get(LocalDate.parse("2026-07-15"));
        assertThat(day.uvIndexMax()).isEqualByComparingTo("8.4");
        assertThat(day.sunrise()).isEqualTo("2026-07-15T05:20");
        assertThat(day.sunset()).isEqualTo("2026-07-15T20:18");
    }

    @Test
    void parsesUsAqi() {
        String body = """
                {
                  "hourly": {
                    "time": ["2026-07-15T08:00", "2026-07-15T09:00"],
                    "us_aqi": [42, 118]
                  }
                }
                """;

        Map<String, BigDecimal> result = client.parseAirQualityResponse(body).orElseThrow();

        assertThat(result.get("2026-07-15T08:00")).isEqualByComparingTo("42.0");
        assertThat(result.get("2026-07-15T09:00")).isEqualByComparingTo("118.0");
    }
}
