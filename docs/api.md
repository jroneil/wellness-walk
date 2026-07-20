# API Documentation

Base URL for local backend development:

```text
http://localhost:9090
```

## Health

```http
GET /api/health/status
```

Example response:

```json
{
  "applicationName": "Wellness Window",
  "status": "UP",
  "backendTimestamp": "2026-07-15T12:00:00Z",
  "developmentStage": "local"
}
```

## Weather By ZIP

```http
GET /api/weather/current/{zipCode}
```

Example:

```http
GET /api/weather/current/01830
```

With optional preferences:

```http
GET /api/weather/current/01830?walkDurationMinutes=30&preferredTimeOfDay=AFTERNOON&temperaturePreference=BALANCED&rainTolerance=LIGHT_RAIN_OK&windTolerance=MODERATE&minimumScore=60&unitSystem=US
```

### Preference Query Parameters

| Parameter | Values | Default |
| --- | --- | --- |
| `walkDurationMinutes` | `10`, `15`, `20`, `30`, `45`, `60` | `30` |
| `preferredTimeOfDay` | `ANY`, `MORNING`, `AFTERNOON`, `EVENING` | `ANY` |
| `temperaturePreference` | `COOLER`, `BALANCED`, `WARMER` | `BALANCED` |
| `rainTolerance` | `AVOID_RAIN`, `LIGHT_RAIN_OK`, `RAIN_OK` | `LIGHT_RAIN_OK` |
| `windTolerance` | `LOW`, `MODERATE`, `HIGH` | `MODERATE` |
| `minimumScore` | integer `0` through `100` | `60` |
| `unitSystem` | `US`, `METRIC` | `US` |

Invalid optional preference values are normalized to defaults. The backend keeps scoring in canonical Fahrenheit and mph values; the frontend handles metric display.

## Weather Response Shape

Top-level response:

```json
{
  "locationName": "Haverhill, MA",
  "latitude": 42.7856,
  "longitude": -71.0721,
  "current": {},
  "environmentalConditions": {},
  "bestWalkingWindow": {},
  "hourlyForecast": [],
  "weeklyOutlook": []
}
```

### Current Conditions

```json
{
  "temperature": 82.0,
  "temperatureUnit": "°F",
  "feelsLike": 85.0,
  "humidity": 63.0,
  "windSpeed": 8.0,
  "windDirection": "NW",
  "weatherCondition": "Haze",
  "iconUrl": "https://api.weather.gov/icons/land/day/haze,20?size=small",
  "observationTime": "2026-07-15T09:00:00-04:00",
  "dataType": "HOURLY_FORECAST"
}
```

### Environmental Conditions

```json
{
  "actualTemperature": 82.0,
  "feelsLikeTemperature": 85.0,
  "temperatureUnit": "°F",
  "feelsLikeMethod": "HEAT_INDEX",
  "aqi": 70.0,
  "aqiCategory": "Moderate",
  "uvIndex": 3.0,
  "uvCategory": "Moderate",
  "sunrise": "2026-07-15T05:19",
  "sunset": "2026-07-15T20:20",
  "daylightStatus": "DAYLIGHT",
  "remainingDaylightMinutes": 680
}
```

Environmental fields may be `null` or `"Unavailable"` when optional provider data is missing.

### Best Walking Window

```json
{
  "startTime": "2026-07-15T19:00:00-04:00",
  "endTime": "2026-07-15T19:45-04:00",
  "score": 85,
  "rating": "GREAT",
  "ratingLabel": "Great",
  "summary": "Great weather for a restorative walk.",
  "positiveReasons": ["Still warmer than ideal", "Very low chance of rain", "Light wind"],
  "warnings": [],
  "durationMinutes": 45,
  "preferenceReasons": ["45-minute walk window", "Fits your rain tolerance"],
  "minimumScore": 85,
  "belowMinimumScore": false,
  "minimumScoreMessage": null
}
```

### Hourly Forecast Period

Each hourly period includes raw weather, environmental enrichment, and a `walkingRecommendation` object.

```json
{
  "startTime": "2026-07-15T19:00:00-04:00",
  "temperature": 84.0,
  "actualTemperature": 84.0,
  "temperatureUnit": "°F",
  "shortForecast": "Haze",
  "iconUrl": "https://api.weather.gov/icons/land/night/haze?size=small",
  "precipitationProbability": 4.0,
  "humidity": 38.0,
  "windSpeed": 8.0,
  "windDirection": "NW",
  "isDaytime": false,
  "walkingRecommendation": {
    "score": 85,
    "rating": "GREAT",
    "ratingLabel": "Great",
    "recommended": true,
    "temperatureScore": 18,
    "precipitationScore": 20,
    "windScore": 10,
    "humidityScore": 10,
    "daylightScore": 10,
    "airQualityScore": 7,
    "uvScore": 10,
    "reasons": ["Still warmer than ideal", "Very low chance of rain"],
    "warnings": []
  }
}
```

### Weekly Outlook

Weekly cards use NWS daily forecast data plus representative hourly scores when hourly data exists for that day.

```json
{
  "date": "2026-07-15",
  "dayName": "Wednesday",
  "iconUrl": "https://api.weather.gov/icons/land/day/haze,20/haze?size=medium",
  "shortForecast": "Haze",
  "highTemperature": 89.0,
  "lowTemperature": 66.0,
  "temperatureUnit": "°F",
  "precipitationProbability": 22.0,
  "representativeScore": 85,
  "rating": "GREAT",
  "ratingLabel": "Great",
  "bestAvailableTime": "2026-07-15T19:00:00-04:00",
  "summary": "Great walking weather",
  "environmentalWarnings": []
}
```

## Error Responses

The API returns structured error responses for common failures.

| Status | Meaning |
| --- | --- |
| `400` | invalid ZIP or invalid coordinates |
| `404` | ZIP code not found |
| `502` | weather provider unavailable or invalid after lookup |
| `503` | ZIP lookup provider unavailable |

Example:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "ZIP code not found",
  "path": "/api/weather/current/00000"
}
```
