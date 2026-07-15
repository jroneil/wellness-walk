package com.oneil.wellness.walkplanner.weather.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneil.wellness.walkplanner.config.WeatherNwsProperties;
import com.oneil.wellness.walkplanner.dto.CurrentWeatherSummary;
import com.oneil.wellness.walkplanner.dto.HourlyForecastPeriod;
import com.oneil.wellness.walkplanner.dto.WeatherResponse;
import com.oneil.wellness.walkplanner.environment.dto.EnvironmentalConditionsDto;
import com.oneil.wellness.walkplanner.environment.dto.EnvironmentalForecast;
import com.oneil.wellness.walkplanner.environment.service.EnvironmentalDataService;
import com.oneil.wellness.walkplanner.environment.service.FeelsLikeCalculator;
import com.oneil.wellness.walkplanner.environment.service.FeelsLikeCalculator.FeelsLikeResult;
import com.oneil.wellness.walkplanner.exception.WeatherServiceException;
import com.oneil.wellness.walkplanner.recommendation.dto.DailyOutlookDto;
import com.oneil.wellness.walkplanner.recommendation.dto.WalkingRecommendationDto;
import com.oneil.wellness.walkplanner.recommendation.service.WalkingRecommendationService;

@Component
public class NwsWeatherClient {

    private final WeatherNwsProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final WalkingRecommendationService walkingRecommendationService;
    private final EnvironmentalDataService environmentalDataService;
    private final FeelsLikeCalculator feelsLikeCalculator;

    public NwsWeatherClient(WeatherNwsProperties properties) {
        this(properties,
                new WalkingRecommendationService(new com.oneil.wellness.walkplanner.recommendation.service.WalkingScoreService()),
                null,
                new FeelsLikeCalculator());
    }

    @Autowired
    public NwsWeatherClient(WeatherNwsProperties properties, WalkingRecommendationService walkingRecommendationService,
            EnvironmentalDataService environmentalDataService, FeelsLikeCalculator feelsLikeCalculator) {
        this.properties = properties;
        this.walkingRecommendationService = walkingRecommendationService;
        this.environmentalDataService = environmentalDataService;
        this.feelsLikeCalculator = feelsLikeCalculator;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofMillis(properties.getConnectionTimeoutMs()))
                .build();
    }

    public WeatherResponse fetchCurrentWeather(BigDecimal latitude, BigDecimal longitude) {
        try {
            PointResponse points = fetchPoints(latitude, longitude);
            String locationName = points.locationName();
            String hourlyForecastUrl = points.forecastHourlyUrl();
            String dailyForecastUrl = points.forecastUrl();
            EnvironmentalForecast environmentalForecast = fetchEnvironmentalForecast(latitude, longitude);
            List<HourlyForecastPeriod> hourlyPeriods = withWalkingRecommendations(
                    enrichHourlyPeriods(fetchHourlyForecastPeriods(hourlyForecastUrl), environmentalForecast));
            List<DailyOutlookDto> weeklyOutlook = buildWeeklyOutlook(fetchDailyForecastPeriods(dailyForecastUrl), hourlyPeriods);
            var observation = Optional.ofNullable(points.observationStationUrl())
                    .flatMap(this::fetchLatestObservation);

            String dataType = observation.isPresent() ? "OBSERVATION" : "HOURLY_FORECAST";
            CurrentWeatherSummary current = buildCurrentWeatherSummary(observation, hourlyPeriods);

            return buildPublicWeatherResponse(locationName, latitude, longitude, hourlyPeriods, weeklyOutlook, dataType, current);
        } catch (WeatherServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WeatherServiceException("Unable to process weather data", ex);
        }
    }

    WeatherResponse buildPublicWeatherResponse(String locationName, BigDecimal latitude, BigDecimal longitude,
            List<HourlyForecastPeriod> hourlyPeriods, List<DailyOutlookDto> weeklyOutlook, String dataType,
            CurrentWeatherSummary current) {
        CurrentWeatherSummary summary = current != null ? current : buildCurrentWeatherSummary(Optional.empty(), hourlyPeriods);
        List<HourlyForecastPeriod> visibleHourlyPeriods = hourlyPeriods.stream().limit(12).toList();
        return new WeatherResponse(
                locationName,
                latitude.setScale(4, RoundingMode.HALF_UP),
                longitude.setScale(4, RoundingMode.HALF_UP),
                summary,
                environmentalConditions(summary, visibleHourlyPeriods),
                walkingRecommendationService.bestWindow(visibleHourlyPeriods),
                visibleHourlyPeriods,
                weeklyOutlook);
    }

    private CurrentWeatherSummary buildCurrentWeatherSummary(Optional<ObservationResponse> observation,
            List<HourlyForecastPeriod> hourlyPeriods) {
        HourlyForecastPeriod firstHour = hourlyPeriods.isEmpty() ? null : hourlyPeriods.get(0);
        BigDecimal temperature = observation
                .map(ObservationResponse::temperatureCelsius)
                .orElseGet(() -> firstHour != null ? firstHour.temperature() : null);
        String temperatureUnit = observation.isPresent() ? "°C" : firstHour != null ? firstHour.temperatureUnit() : "°F";
        BigDecimal feelsLike = observation.map(ObservationResponse::feelsLikeCelsius)
                .orElse(firstHour != null ? firstHour.feelsLikeTemperature() : null);
        BigDecimal humidity = observation.map(ObservationResponse::humidityPercentage)
                .orElse(firstHour != null ? firstHour.humidity() : null);
        BigDecimal windSpeed = observation.map(ObservationResponse::windSpeedMph)
                .orElse(firstHour != null ? firstHour.windSpeed() : null);
        String windDirection = observation.map(ObservationResponse::windDirection)
                .orElse(firstHour != null ? firstHour.windDirection() : null);
        String weatherCondition = observation.map(ObservationResponse::weatherCondition)
                .orElse(firstHour != null ? firstHour.shortForecast() : null);
        String iconUrl = firstHour != null ? firstHour.iconUrl() : null;
        String observationTime = observation.map(ObservationResponse::observationTime)
                .orElse(firstHour != null ? firstHour.startTime() : null);

        return new CurrentWeatherSummary(
                temperature,
                temperatureUnit,
                feelsLike,
                humidity,
                windSpeed,
                windDirection,
                weatherCondition,
                iconUrl,
                observationTime,
                observation.isPresent() ? "OBSERVATION" : "HOURLY_FORECAST");
    }

    private PointResponse fetchPoints(BigDecimal latitude, BigDecimal longitude) {
        URI uri = URI.create(properties.getBaseUrl() + "/points/" + latitude + "," + longitude);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("User-Agent", properties.getUserAgent())
                .header("Accept", "application/geo+json")
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new WeatherServiceException("Weather provider returned an error response");
            }
            JsonNode root = objectMapper.readTree(response.body());
            PointResponse pointResponse = parsePointResponse(root);
            if (pointResponse.forecastHourlyUrl().isBlank()) {
                throw new WeatherServiceException("No hourly forecast available");
            }
            if (pointResponse.forecastUrl().isBlank()) {
                throw new WeatherServiceException("No daily forecast available");
            }
            return pointResponse;
        } catch (JsonProcessingException ex) {
            throw new WeatherServiceException("Malformed weather provider response", ex);
        } catch (java.io.IOException ex) {
            throw new WeatherServiceException("Weather provider unavailable", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new WeatherServiceException("Weather provider request interrupted", ex);
        }
    }

    private List<HourlyForecastPeriod> fetchHourlyForecastPeriods(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", properties.getUserAgent())
                .header("Accept", "application/geo+json")
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new WeatherServiceException("Hourly forecast unavailable");
            }
            JsonNode root = objectMapper.readTree(response.body());
            return parseHourlyForecastResponse(root);
        } catch (JsonProcessingException ex) {
            throw new WeatherServiceException("Malformed weather provider response", ex);
        } catch (java.io.IOException ex) {
            throw new WeatherServiceException("Weather provider unavailable", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new WeatherServiceException("Weather provider request interrupted", ex);
        }
    }

    private Optional<ObservationResponse> fetchLatestObservation(String stationUrl) {
        if (stationUrl == null || stationUrl.isBlank()) {
            return Optional.empty();
        }
        URI uri = URI.create(stationUrl + "/observations/latest");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("User-Agent", properties.getUserAgent())
                .header("Accept", "application/geo+json")
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response.body());
            return Optional.of(parseObservationResponse(root));
        } catch (JsonProcessingException ex) {
            return Optional.empty();
        } catch (java.io.IOException ex) {
            return Optional.empty();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    PointResponse parsePointResponse(JsonNode root) {
        String city = root.path("properties").path("relativeLocation").path("properties").path("city").asText();
        String state = root.path("properties").path("relativeLocation").path("properties").path("state").asText();
        String locationName = state.isBlank() ? city : city + ", " + state;
        String forecastUrl = root.path("properties").path("forecast").asText();
        String forecastHourlyUrl = root.path("properties").path("forecastHourly").asText();
        String observationStationsUrl = root.path("properties").path("observationStations").asText();
        return new PointResponse(locationName, forecastUrl, forecastHourlyUrl, observationStationsUrl);
    }

    List<HourlyForecastPeriod> parseHourlyForecastResponse(JsonNode root) {
        JsonNode periods = root.path("properties").path("periods");
        if (periods.isMissingNode() || !periods.isArray() || periods.size() == 0) {
            throw new WeatherServiceException("Incomplete weather provider response");
        }

        List<HourlyForecastPeriod> results = new ArrayList<>();
        for (JsonNode period : periods) {
            if (period.isMissingNode()) {
                continue;
            }
            String startTime = period.path("startTime").asText();
            if (startTime.isBlank()) {
                continue;
            }
            BigDecimal temperature = convertTemperature(period.path("temperature").decimalValue());
            String temperatureUnit = period.path("temperatureUnit").asText().equalsIgnoreCase("F") ? "°F" : "°C";
            BigDecimal precipitationProbability = parseNullableNumber(period.path("probabilityOfPrecipitation").path("value"));
            BigDecimal humidity = parseNullableNumber(period.path("relativeHumidity").path("value"));
            BigDecimal windSpeed = parseNullableWindSpeed(period.path("windSpeed").asText());
            String windDirection = period.path("windDirection").asText();
            String shortForecast = period.path("shortForecast").asText();
            String iconUrl = period.path("icon").asText(null);
            Boolean isDaytime = period.path("isDaytime").isMissingNode() ? null : period.path("isDaytime").asBoolean();
            results.add(new HourlyForecastPeriod(
                    startTime,
                    temperature,
                    temperatureUnit,
                    shortForecast,
                    iconUrl,
                    precipitationProbability,
                    humidity,
                    windSpeed,
                    windDirection,
                    isDaytime,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null));
        }

        if (results.isEmpty()) {
            throw new WeatherServiceException("Incomplete weather provider response");
        }
        return results;
    }

    private List<DailyForecastPeriod> fetchDailyForecastPeriods(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", properties.getUserAgent())
                .header("Accept", "application/geo+json")
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new WeatherServiceException("Daily forecast unavailable");
            }
            JsonNode root = objectMapper.readTree(response.body());
            return parseDailyForecastResponse(root);
        } catch (JsonProcessingException ex) {
            throw new WeatherServiceException("Malformed weather provider response", ex);
        } catch (java.io.IOException ex) {
            throw new WeatherServiceException("Weather provider unavailable", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new WeatherServiceException("Weather provider request interrupted", ex);
        }
    }

    List<DailyForecastPeriod> parseDailyForecastResponse(JsonNode root) {
        JsonNode periods = root.path("properties").path("periods");
        if (periods.isMissingNode() || !periods.isArray() || periods.size() == 0) {
            throw new WeatherServiceException("Incomplete daily weather provider response");
        }

        List<DailyForecastPeriod> results = new ArrayList<>();
        for (JsonNode period : periods) {
            String startTime = period.path("startTime").asText();
            if (startTime.isBlank()) {
                continue;
            }
            results.add(new DailyForecastPeriod(
                    startTime,
                    period.path("name").asText(),
                    period.path("isDaytime").isMissingNode() ? null : period.path("isDaytime").asBoolean(),
                    convertTemperature(period.path("temperature").decimalValue()),
                    period.path("temperatureUnit").asText().equalsIgnoreCase("F") ? "°F" : "°C",
                    period.path("icon").asText(null),
                    period.path("shortForecast").asText(),
                    parseNullableNumber(period.path("probabilityOfPrecipitation").path("value"))));
        }
        if (results.isEmpty()) {
            throw new WeatherServiceException("Incomplete daily weather provider response");
        }
        return results;
    }

    private List<HourlyForecastPeriod> withWalkingRecommendations(List<HourlyForecastPeriod> hourlyPeriods) {
        return hourlyPeriods.stream()
                .map(period -> period.withWalkingRecommendation(walkingRecommendationService.recommendationFor(period)))
                .toList();
    }

    private EnvironmentalForecast fetchEnvironmentalForecast(BigDecimal latitude, BigDecimal longitude) {
        return environmentalDataService == null
                ? EnvironmentalForecast.empty()
                : environmentalDataService.getEnvironmentalForecast(latitude, longitude);
    }

    private List<HourlyForecastPeriod> enrichHourlyPeriods(List<HourlyForecastPeriod> hourlyPeriods,
            EnvironmentalForecast environmentalForecast) {
        return hourlyPeriods.stream()
                .map(period -> {
                    String key = environmentalKey(period.startTime()).orElse(period.startTime());
                    EnvironmentalForecast.HourlyEnvironment environment = environmentalForecast.hourlyByStartTime().get(key);
                    EnvironmentalForecast.DailyEnvironment daily = parseDate(period.startTime())
                            .map(environmentalForecast.dailyByDate()::get)
                            .orElse(null);
                    FeelsLikeResult feelsLike = feelsLikeCalculator.calculate(period.temperature(), period.humidity(), period.windSpeed());
                    return period.withEnvironmentalData(
                            feelsLike.temperature(),
                            feelsLike.source(),
                            environment != null ? environment.uvIndex() : null,
                            environment != null ? environment.aqi() : null,
                            environment != null && environment.sunrise() != null ? environment.sunrise() : daily != null ? daily.sunrise() : null,
                            environment != null && environment.sunset() != null ? environment.sunset() : daily != null ? daily.sunset() : null,
                            environment != null ? environment.remainingDaylightMinutes() : null);
                })
                .toList();
    }

    private Optional<String> environmentalKey(String startTime) {
        try {
            return Optional.of(OffsetDateTime.parse(startTime).toLocalDateTime().withMinute(0).withSecond(0).withNano(0).toString());
        } catch (DateTimeParseException ex) {
            try {
                return Optional.of(LocalDateTime.parse(startTime).withMinute(0).withSecond(0).withNano(0).toString());
            } catch (DateTimeParseException nested) {
                return Optional.empty();
            }
        }
    }

    private EnvironmentalConditionsDto environmentalConditions(CurrentWeatherSummary current,
            List<HourlyForecastPeriod> visibleHourlyPeriods) {
        HourlyForecastPeriod firstHour = visibleHourlyPeriods.isEmpty() ? null : visibleHourlyPeriods.get(0);
        if (firstHour == null) {
            return new EnvironmentalConditionsDto(
                    current.feelsLike(),
                    "ACTUAL",
                    null,
                    "Unavailable",
                    null,
                    "Unavailable",
                    null,
                    null,
                    null);
        }
        return new EnvironmentalConditionsDto(
                firstHour.feelsLikeTemperature(),
                firstHour.feelsLikeSource(),
                firstHour.aqi(),
                aqiCategory(firstHour.aqi()),
                firstHour.uvIndex(),
                uvCategory(firstHour.uvIndex()),
                firstHour.sunrise(),
                firstHour.sunset(),
                firstHour.remainingDaylightMinutes());
    }

    List<DailyOutlookDto> buildWeeklyOutlook(List<DailyForecastPeriod> dailyPeriods,
            List<HourlyForecastPeriod> hourlyPeriods) {
        Map<LocalDate, DailyOutlookAccumulator> days = new LinkedHashMap<>();
        for (DailyForecastPeriod period : dailyPeriods) {
            parseDate(period.startTime()).ifPresent(date -> days
                    .computeIfAbsent(date, DailyOutlookAccumulator::new)
                    .add(period));
            if (days.size() >= 7 && days.values().stream().allMatch(DailyOutlookAccumulator::hasAnyForecast)) {
                break;
            }
        }

        return days.values().stream()
                .limit(7)
                .map(day -> day.toDto(bestHourlyPeriodForDate(day.date(), hourlyPeriods), hourlyPeriods))
                .toList();
    }

    private Optional<HourlyForecastPeriod> bestHourlyPeriodForDate(LocalDate date, List<HourlyForecastPeriod> hourlyPeriods) {
        return walkingRecommendationService.bestPeriod(hourlyPeriods.stream()
                .filter(period -> parseDate(period.startTime()).map(date::equals).orElse(false))
                .toList());
    }

    private Optional<LocalDate> parseDate(String startTime) {
        if (startTime == null || startTime.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(OffsetDateTime.parse(startTime).toLocalDate());
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    ObservationResponse parseObservationResponse(JsonNode root) {
        JsonNode properties = root.path("properties");
        BigDecimal temperatureCelsius = convertTemperature(properties.path("temperature").path("value").decimalValue());
        BigDecimal feelsLikeCelsius = convertTemperature(properties.path("heatIndex").path("value").decimalValue());
        BigDecimal humidity = properties.path("relativeHumidity").path("value").isMissingNode()
                ? null
                : BigDecimal.valueOf(properties.path("relativeHumidity").path("value").asDouble());
        BigDecimal windSpeedMph = properties.path("windSpeed").path("value").isMissingNode()
                ? null
                : BigDecimal.valueOf(properties.path("windSpeed").path("value").asDouble() * 0.621371);
        String windDirection = properties.path("windDirection").path("value").isMissingNode()
                ? null
                : String.valueOf(properties.path("windDirection").path("value").asInt());
        String weatherCondition = properties.path("textDescription").asText();
        String observationTime = properties.path("timestamp").asText();
        return new ObservationResponse(temperatureCelsius, feelsLikeCelsius, humidity, windSpeedMph, windDirection, weatherCondition, observationTime);
    }

    private BigDecimal convertTemperature(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal parseNullableNumber(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.isNumber() ? BigDecimal.valueOf(node.asDouble()) : null;
    }

    private BigDecimal parseNullableWindSpeed(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String numeric = value.replaceAll("[^0-9.\\-]", "");
        if (numeric.isBlank()) {
            return null;
        }
        return BigDecimal.valueOf(Double.parseDouble(numeric));
    }

    private String aqiCategory(BigDecimal aqi) {
        if (aqi == null) {
            return "Unavailable";
        }
        if (aqi.compareTo(BigDecimal.valueOf(50)) <= 0) {
            return "Excellent";
        }
        if (aqi.compareTo(BigDecimal.valueOf(100)) <= 0) {
            return "Good";
        }
        if (aqi.compareTo(BigDecimal.valueOf(150)) <= 0) {
            return "Moderate";
        }
        return "Poor";
    }

    private String uvCategory(BigDecimal uvIndex) {
        if (uvIndex == null) {
            return "Unavailable";
        }
        if (uvIndex.compareTo(BigDecimal.valueOf(2)) <= 0) {
            return "Low";
        }
        if (uvIndex.compareTo(BigDecimal.valueOf(5)) <= 0) {
            return "Moderate";
        }
        if (uvIndex.compareTo(BigDecimal.valueOf(7)) <= 0) {
            return "High";
        }
        return "Very High";
    }

    record PointResponse(String locationName, String forecastUrl, String forecastHourlyUrl, String observationStationUrl) {
    }

    record DailyForecastPeriod(
            String startTime,
            String name,
            Boolean isDaytime,
            BigDecimal temperature,
            String temperatureUnit,
            String iconUrl,
            String shortForecast,
            BigDecimal precipitationProbability) {
    }

    private record ObservationResponse(
            BigDecimal temperatureCelsius,
            BigDecimal feelsLikeCelsius,
            BigDecimal humidityPercentage,
            BigDecimal windSpeedMph,
            String windDirection,
            String weatherCondition,
            String observationTime) {
    }

    private static class DailyOutlookAccumulator {

        private static final DateTimeFormatter DAY_NAME_FORMATTER = DateTimeFormatter.ofPattern("EEEE", Locale.US);

        private final LocalDate date;
        private DailyForecastPeriod dayPeriod;
        private DailyForecastPeriod nightPeriod;
        private DailyForecastPeriod fallbackPeriod;

        DailyOutlookAccumulator(LocalDate date) {
            this.date = date;
        }

        void add(DailyForecastPeriod period) {
            if (fallbackPeriod == null) {
                fallbackPeriod = period;
            }
            if (Boolean.TRUE.equals(period.isDaytime())) {
                dayPeriod = period;
            } else if (Boolean.FALSE.equals(period.isDaytime())) {
                nightPeriod = period;
            }
        }

        boolean hasAnyForecast() {
            return fallbackPeriod != null;
        }

        LocalDate date() {
            return date;
        }

        DailyOutlookDto toDto(Optional<HourlyForecastPeriod> bestHourlyPeriod, List<HourlyForecastPeriod> hourlyPeriods) {
            DailyForecastPeriod display = dayPeriod != null ? dayPeriod : fallbackPeriod;
            DailyForecastPeriod unitSource = dayPeriod != null ? dayPeriod : nightPeriod != null ? nightPeriod : fallbackPeriod;
            WalkingRecommendationDto recommendation = bestHourlyPeriod
                    .map(HourlyForecastPeriod::walkingRecommendation)
                    .orElse(null);
            Integer score = recommendation != null ? recommendation.score() : null;
            return new DailyOutlookDto(
                    date.toString(),
                    DAY_NAME_FORMATTER.format(date),
                    display != null ? display.iconUrl() : null,
                    display != null ? display.shortForecast() : null,
                    dayPeriod != null ? dayPeriod.temperature() : null,
                    nightPeriod != null ? nightPeriod.temperature() : null,
                    unitSource != null ? unitSource.temperatureUnit() : "°F",
                    representativePrecipitation(),
                    score,
                    recommendation != null ? recommendation.rating() : null,
                    recommendation != null ? recommendation.ratingLabel() : "Not enough hourly data",
                    bestHourlyPeriod.map(HourlyForecastPeriod::startTime).orElse(null),
                    score == null ? "Not enough hourly data" : recommendation.ratingLabel() + " walking weather",
                    environmentalWarnings(hourlyPeriods));
        }

        private BigDecimal representativePrecipitation() {
            BigDecimal dayPrecipitation = dayPeriod != null ? dayPeriod.precipitationProbability() : null;
            if (dayPrecipitation != null) {
                return dayPrecipitation;
            }
            return nightPeriod != null ? nightPeriod.precipitationProbability() : null;
        }

        private List<String> environmentalWarnings(List<HourlyForecastPeriod> hourlyPeriods) {
            List<String> warnings = new ArrayList<>();
            hourlyPeriods.stream()
                    .filter(period -> {
                        try {
                            return OffsetDateTime.parse(period.startTime()).toLocalDate().equals(date);
                        } catch (DateTimeParseException ex) {
                            return false;
                        }
                    })
                    .forEach(period -> {
                        if (period.uvIndex() != null && period.uvIndex().compareTo(BigDecimal.valueOf(8)) >= 0
                                && !warnings.contains("High UV")) {
                            warnings.add("High UV");
                        }
                        if (period.aqi() != null && period.aqi().compareTo(BigDecimal.valueOf(150)) > 0
                                && !warnings.contains("Poor AQI")) {
                            warnings.add("Poor AQI");
                        }
                        BigDecimal feelsLike = period.feelsLikeTemperature() != null ? period.feelsLikeTemperature() : period.temperature();
                        if (feelsLike != null && feelsLike.compareTo(BigDecimal.valueOf(90)) > 0
                                && !warnings.contains("Very Hot")) {
                            warnings.add("Very Hot");
                        }
                        if (period.windSpeed() != null && period.windSpeed().compareTo(BigDecimal.valueOf(20)) > 0
                                && !warnings.contains("Strong Wind")) {
                            warnings.add("Strong Wind");
                        }
                    });
            return warnings.stream().limit(3).toList();
        }
    }
}
