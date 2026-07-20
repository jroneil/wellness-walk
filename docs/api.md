# API Documentation

Base URL for local backend development:

```text
http://localhost:9090
```

## Version 2.6 Activity and Data APIs

```http
POST /api/walks/start
POST /api/walks/{id}/complete
POST /api/walks/{id}/cancel
POST /api/walks/manual
GET /api/walks
GET /api/walks/active
DELETE /api/walks/{id}
POST /api/opportunities/{recommendationHistoryId}/skip
POST /api/opportunities/{recommendationHistoryId}/dismiss
GET|PUT /api/settings/wellness-goals
GET /api/settings/wellness-goals/progress
GET|PUT /api/settings/history-retention
DELETE /api/history/recommendations?from=YYYY-MM-DD&to=YYYY-MM-DD&timezone=UTC
DELETE /api/history/notifications
DELETE /api/history/activities
DELETE /api/history/all
GET /api/history/export/activities.csv
GET /api/history/export/recommendations.csv
GET /api/history/export/daily-summary.csv
GET /api/history/export/all.json
```

Exports set attachment filenames and `Cache-Control: no-store`. They never contain
provider credentials, tokens, passwords, or encryption metadata.

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

## Version 2.7 activity and data endpoints

```http
POST /api/walks/start
POST /api/walks/{id}/complete
POST /api/walks/{id}/cancel
POST /api/opportunities/skip
POST /api/opportunities/dismiss
DELETE /api/history/recommendations?from=YYYY-MM-DD&to=YYYY-MM-DD&timezone=Area/City
DELETE /api/history/notifications
DELETE /api/history/activities
DELETE /api/history/all
```

The window-based skip/dismiss requests accept `opportunityStart` and a constrained source. Duplicate explicit outcomes are rejected. Completion accepts positive duration (maximum 480), completion status, optional perceived quality, and notes (maximum 1,000). History deletion is installation-wide but never deletes calendar-provider configuration or encrypted provider credentials.

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
| `preferredTimeOfDay` | `ANY`, `MORNING`, `LUNCH`, `AFTERNOON`, `EVENING` | `ANY` |
| `temperaturePreference` | `COOLER`, `BALANCED`, `WARMER` | `BALANCED` |
| `rainTolerance` | `AVOID_RAIN`, `LIGHT_RAIN_OK`, `RAIN_OK` | `LIGHT_RAIN_OK` |
| `windTolerance` | `LOW`, `MODERATE`, `HIGH` | `MODERATE` |
| `minimumScore` | integer `0` through `100` | `60` |
| `unitSystem` | `US`, `METRIC` | `US` |

Invalid optional preference values are normalized to defaults. The backend keeps scoring in canonical Fahrenheit and mph values; the frontend handles metric display.

## Availability-Aware Recommendation

```http
POST /api/weather/current/{zipCode}/recommendation
Content-Type: application/json
```

The Version 2 frontend uses this additive endpoint. The existing GET endpoint remains compatible and behaves as though the calendar is empty.

```json
{
  "preferences": {
    "walkDurationMinutes": 30,
    "preferredTimeOfDay": "ANY",
    "temperaturePreference": "BALANCED",
    "rainTolerance": "LIGHT_RAIN_OK",
    "windTolerance": "MODERATE",
    "minimumScore": 60,
    "unitSystem": "US"
  },
  "calendarEvents": [
    {
      "id": "event-1",
      "title": "Planning meeting",
      "startTime": "2026-07-15T19:00:00-04:00",
      "endTime": "2026-07-15T19:30:00-04:00",
      "busy": true,
      "source": "MANUAL"
    }
  ]
}
```

Supported sources are `MANUAL`, `CALDAV`, `GOOGLE`, and `MICROSOFT`; Version 2.2 creates only Manual and CalDAV events.

## Calendar Providers

```http
GET /api/calendar/providers
GET /api/calendar/providers/{type}/status
POST /api/calendar/providers/{type}/test
POST /api/calendar/providers/{type}/disconnect
POST /api/calendar/sync
POST /api/calendar/providers/{type}/discover
GET /api/calendar/providers/{type}/calendars
PUT /api/calendar/providers/{type}/calendars/selection
POST /api/calendar/providers/{type}/sync
GET /api/calendar/providers/{type}/sync-status
GET /api/calendar/providers/{type}/oauth/start
GET /api/calendar/providers/{type}/oauth/callback
POST /api/calendar/providers/{type}/revoke
```

Provider status responses explicitly expose `installationConfigured`, `enabled`, `connected`, `authorizationRequired`, `selectedCalendarCount`, `discoveredCalendarCount`, `lastSuccessfulSyncAt`, `lastAttemptedSyncAt`, `providerStatus`, `safeMessage`, and capabilities. The compatibility `status` field mirrors `providerStatus`. Credentials are never returned. Sync accepts bounded `start`, `end`, and optional `manualEvents`, then returns normalized events, sanitized errors, and `synchronizedAt`. Provider failure can therefore return Manual events plus an error.

Discovery returns provider-neutral calendar descriptors. Selection requests contain only `calendarIds` and persist in PostgreSQL. Sync responses include per-calendar success/failure details. CalDAV errors use structured `providerType`, `code`, `message`, and `timestamp` responses without XML, URLs containing credentials, or server stack traces.
Selections are durable in Version 2.4. OAuth start returns only an authorization URL and expiry. The callback validates state, exchanges the code backend-side, and redirects to a fixed frontend result route. APIs never return access tokens, refresh tokens, client secrets, ciphertext, authorization headers, or raw provider payloads. Google failures use the same structured provider error envelope.

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
  "score": 88,
  "rating": "GREAT",
  "ratingLabel": "Great",
  "summary": "Great weather for a restorative walk.",
  "positiveReasons": ["Still warmer than ideal", "Very low chance of rain", "Light wind"],
  "warnings": [],
  "durationMinutes": 45,
  "preferenceReasons": ["45-minute walk window", "Fits your rain tolerance"],
  "minimumScore": 85,
  "belowMinimumScore": false,
  "minimumScoreMessage": null,
  "availability": "AVAILABLE",
  "selectionReason": "Highest available weather score after a calendar conflict.",
  "conflictingEvent": null,
  "idealWeatherWindow": {
    "startTime": "2026-07-15T19:00:00-04:00",
    "endTime": "2026-07-15T19:45:00-04:00",
    "score": 87,
    "availability": "UNAVAILABLE",
    "conflictingEvent": {
      "eventId": "event-1",
      "title": "Planning meeting",
      "startTime": "2026-07-15T19:00:00-04:00",
      "endTime": "2026-07-15T19:30:00-04:00",
      "source": "MANUAL"
    }
  },
  "weatherScore": 84,
  "availabilityScore": 100,
  "preferenceScore": 75,
  "overallScore": 88,
  "calendarReasons": ["Selected window is fully available"],
  "noAvailableReason": null
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

## Wellness History and Notifications

```http
POST /api/wellness/history
GET /api/wellness/history
POST /api/wellness/notifications/evaluate
```

History accepts a completed provider-neutral `RecommendationSnapshot` and returns the stored snapshot. Near-identical calculations are not inserted. The history response includes snapshots, today/week summaries, and the latest significant comparison. Notification evaluation accepts a snapshot, notification preferences, daily delivery count, and last-delivery time; it returns `eligible`, `scheduleAt`, an explanatory reason, and safe notification copy. Neither endpoint recalculates recommendation scores or exposes calendar provider internals.

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
