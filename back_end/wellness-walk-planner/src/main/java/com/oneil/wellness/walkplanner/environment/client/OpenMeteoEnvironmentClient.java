package com.oneil.wellness.walkplanner.environment.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneil.wellness.walkplanner.environment.dto.EnvironmentalForecast;

@Component
public class OpenMeteoEnvironmentClient {

    private static final String FORECAST_BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String AIR_QUALITY_BASE_URL = "https://air-quality-api.open-meteo.com/v1/air-quality";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenMeteoEnvironmentClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public EnvironmentalForecast fetch(BigDecimal latitude, BigDecimal longitude) {
        try {
            ForecastData forecastData = fetchForecastData(latitude, longitude).orElse(ForecastData.empty());
            Map<String, BigDecimal> aqiByTime = fetchAqiData(latitude, longitude).orElse(Map.of());
            Map<LocalDate, BigDecimal> maxAqiByDate = maxAqiByDate(aqiByTime);
            Map<String, EnvironmentalForecast.HourlyEnvironment> hourly = new HashMap<>();

            for (Map.Entry<String, BigDecimal> entry : forecastData.uvByTime().entrySet()) {
                String startTime = entry.getKey();
                LocalDate date = parseDateTime(startTime).map(LocalDateTime::toLocalDate).orElse(null);
                EnvironmentalForecast.DailyEnvironment daily = date != null ? forecastData.dailyByDate().get(date) : null;
                hourly.put(startTime, new EnvironmentalForecast.HourlyEnvironment(
                        startTime,
                        entry.getValue(),
                        aqiByTime.get(startTime),
                        daily != null ? daily.sunrise() : null,
                        daily != null ? daily.sunset() : null,
                        remainingDaylightMinutes(startTime, daily)));
            }

            for (Map.Entry<String, BigDecimal> entry : aqiByTime.entrySet()) {
                hourly.putIfAbsent(entry.getKey(), new EnvironmentalForecast.HourlyEnvironment(
                        entry.getKey(),
                        null,
                        entry.getValue(),
                        null,
                        null,
                        null));
            }

            Map<LocalDate, EnvironmentalForecast.DailyEnvironment> dailyWithAqi = new LinkedHashMap<>();
            forecastData.dailyByDate().forEach((date, daily) -> dailyWithAqi.put(date,
                    new EnvironmentalForecast.DailyEnvironment(
                            date,
                            daily.uvIndexMax(),
                            daily.sunrise(),
                            daily.sunset(),
                            maxAqiByDate.get(date))));

            return new EnvironmentalForecast(hourly, dailyWithAqi);
        } catch (RuntimeException ex) {
            return EnvironmentalForecast.empty();
        }
    }

    Optional<ForecastData> parseForecastResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode hourly = root.path("hourly");
            JsonNode daily = root.path("daily");
            Map<String, BigDecimal> uvByTime = valuesByTime(hourly.path("time"), hourly.path("uv_index"));
            Map<LocalDate, EnvironmentalForecast.DailyEnvironment> dailyByDate = new LinkedHashMap<>();
            JsonNode dates = daily.path("time");
            for (int i = 0; i < dates.size(); i++) {
                LocalDate date = LocalDate.parse(dates.get(i).asText());
                dailyByDate.put(date, new EnvironmentalForecast.DailyEnvironment(
                        date,
                        nullableDecimal(daily.path("uv_index_max"), i),
                        nullableText(daily.path("sunrise"), i),
                        nullableText(daily.path("sunset"), i),
                        null));
            }
            return Optional.of(new ForecastData(uvByTime, dailyByDate));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    Optional<Map<String, BigDecimal>> parseAirQualityResponse(String body) {
        try {
            JsonNode hourly = objectMapper.readTree(body).path("hourly");
            return Optional.of(valuesByTime(hourly.path("time"), hourly.path("us_aqi")));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Optional<ForecastData> fetchForecastData(BigDecimal latitude, BigDecimal longitude) {
        URI uri = URI.create(FORECAST_BASE_URL + "?" + query(Map.of(
                "latitude", latitude.toPlainString(),
                "longitude", longitude.toPlainString(),
                "hourly", "uv_index",
                "daily", "uv_index_max,sunrise,sunset",
                "temperature_unit", "fahrenheit",
                "wind_speed_unit", "mph",
                "timezone", "auto",
                "forecast_days", "7")));
        return send(uri).flatMap(this::parseForecastResponse);
    }

    private Optional<Map<String, BigDecimal>> fetchAqiData(BigDecimal latitude, BigDecimal longitude) {
        URI uri = URI.create(AIR_QUALITY_BASE_URL + "?" + query(Map.of(
                "latitude", latitude.toPlainString(),
                "longitude", longitude.toPlainString(),
                "hourly", "us_aqi",
                "timezone", "auto",
                "forecast_days", "7")));
        return send(uri).flatMap(this::parseAirQualityResponse);
    }

    private Optional<String> send(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 400 ? Optional.empty() : Optional.of(response.body());
        } catch (java.io.IOException ex) {
            return Optional.empty();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private Map<String, BigDecimal> valuesByTime(JsonNode times, JsonNode values) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (int i = 0; i < times.size() && i < values.size(); i++) {
            BigDecimal value = nullableDecimal(values, i);
            if (value != null) {
                result.put(times.get(i).asText(), value);
            }
        }
        return result;
    }

    private Map<LocalDate, BigDecimal> maxAqiByDate(Map<String, BigDecimal> aqiByTime) {
        Map<LocalDate, BigDecimal> result = new HashMap<>();
        aqiByTime.forEach((time, value) -> parseDateTime(time).ifPresent(dateTime -> result.merge(
                dateTime.toLocalDate(),
                value,
                BigDecimal::max)));
        return result;
    }

    private Integer remainingDaylightMinutes(String startTime, EnvironmentalForecast.DailyEnvironment daily) {
        if (daily == null || daily.sunset() == null) {
            return null;
        }
        Optional<LocalDateTime> start = parseDateTime(startTime);
        Optional<LocalDateTime> sunset = parseDateTime(daily.sunset());
        if (start.isEmpty() || sunset.isEmpty()) {
            return null;
        }
        long minutes = Duration.between(start.get(), sunset.get()).toMinutes();
        return (int) Math.max(0, minutes);
    }

    private Optional<LocalDateTime> parseDateTime(String value) {
        try {
            return Optional.of(LocalDateTime.parse(value));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    private BigDecimal nullableDecimal(JsonNode values, int index) {
        if (!values.isArray() || index >= values.size() || values.get(index).isNull()) {
            return null;
        }
        return BigDecimal.valueOf(values.get(index).asDouble()).setScale(1, RoundingMode.HALF_UP);
    }

    private String nullableText(JsonNode values, int index) {
        if (!values.isArray() || index >= values.size() || values.get(index).isNull()) {
            return null;
        }
        return values.get(index).asText();
    }

    private String query(Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    record ForecastData(
            Map<String, BigDecimal> uvByTime,
            Map<LocalDate, EnvironmentalForecast.DailyEnvironment> dailyByDate) {
        static ForecastData empty() {
            return new ForecastData(Map.of(), Map.of());
        }
    }
}
