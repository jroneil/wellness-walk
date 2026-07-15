# Architecture

## Backend

Spring Boot 4.1

Packages

config
controller
dto
environment
exception
model
engine
repository
service

## Frontend

Next.js 16

```text
app/
    Server Components
    Server Actions
    Client Components for interactive forms
    types/
```

## Recommendation Flow

Weather
      │
Calendar
      │
Operations
      │
Recommendation Engine
      │
REST API
      │
Next.js Dashboard

## Current Weather Flow

The current weather vertical slice is ZIP-first for the user experience.

```text
Browser
      │
Next.js WeatherSection client form
      │
Next.js server action
      │
Spring Boot GET /api/weather/current/{zip}
      │
ZIP validation
      │
Zippopotam.us ZIP lookup
      │
WeatherService
      │
National Weather Service client
      │
Optional Open-Meteo environmental lookup
      │
WeatherResponse
```

The browser does not call the Spring Boot backend directly. Backend communication is centralized in server-only Next.js code under `app/lib/backend.ts`, and interactive weather requests use the `getCurrentWeather` server action.

### ZIP Lookup Provider

The backend uses `https://api.zippopotam.us/us/{zip}` to translate a 5-digit United States ZIP code into latitude and longitude. This provider requires no API key.

Provider-specific response DTOs live under the backend `zip/dto` package and are not returned to the frontend. The `zip/client` package owns HTTP communication with Zippopotam.us, and the `zip/service` package converts a valid ZIP lookup into a call to the existing `WeatherService`.

### National Weather Service Provider

The existing `WeatherService` remains the only application service responsible for retrieving weather. It delegates to the National Weather Service client, which calls the NWS points endpoint and then retrieves current observation data when available, falling back to hourly forecast data.

The NWS points response supplies both `forecastHourly` and `forecast` URLs. The hourly URL is used for current-hour detail and walking score calculations. The daily `forecast` URL is used for seven-day high/low temperatures, daily summaries, and official NWS daily icons. The application does not manually construct gridpoint forecast URLs.

The ZIP flow does not duplicate NWS retrieval logic. It only supplies coordinates to `WeatherService`.

### Environmental Provider

The backend enriches the NWS hourly forecast with optional environmental data from Open-Meteo:

* Forecast API: hourly `uv_index`, daily `uv_index_max`, `sunrise`, and `sunset`.
* Air Quality API: hourly `us_aqi`.

Open-Meteo requires no API key for the public usage used here. These calls are best-effort. If Open-Meteo is unavailable, malformed, or missing a value, the application keeps returning NWS weather and explains AQI, UV, or daylight as unavailable instead of fabricating values.

Provider-specific parsing lives under `environment/client`. Application-facing environmental DTOs live under `environment/dto`. Raw provider response models are not exposed through public API DTOs.

### Weather-Only Walking Recommendations

The first recommendation engine version considers weather and environmental comfort only. It does not use calendar availability, operational workload, authentication, persistence, AI, machine learning, or opaque scoring.

Scoring is deterministic and explainable. The backend scores each scorable hourly forecast period from 0 to 100:

* Feels-like temperature, maximum 40 points: `60-72°F = 40`, `55-59°F or 73-78°F = 35`, `79-84°F = 25`, `45-54°F = 20`, `85-90°F = 10`, below `45°F` or above `90°F = 0`.
* Precipitation probability, maximum 25 points: `0-10% = 25`, `11-25% = 20`, `26-50% = 10`, above `50% = 0`.
* Wind speed, maximum 15 points: `0-10 mph = 15`, `11-15 mph = 10`, `16-20 mph = 5`, above `20 mph = 0`.
* Humidity, maximum 10 points: `30-60% = 10`, `61-75% = 7`, `76-85% = 3`, above `85% = 0`, below `30% = 7`.
* Daylight, maximum 10 points: at least 60 minutes remaining `10`, less than 60 minutes remaining `6`, no daylight remaining `2`. If sunrise/sunset is unavailable, the NWS daytime flag is used as a fallback.
* AQI penalty: `0-50 = 0`, `51-100 = -3`, `101-150 = -8`, `151+ = -15`.
* UV Index penalty: `0-2 = 0`, `3-5 = -3`, `6-7 = -6`, `8+ = -10`.

Feels-like temperature behavior:

* Heat Index is calculated when temperature is at least `80°F` and humidity is at least `40%`.
* Wind Chill is calculated when temperature is at most `50°F` and wind is at least `3 mph`.
* Actual temperature is used when neither NOAA formula applies.
* Heat Index and Wind Chill are not calculated outside those ranges.

Rating ranges:

* `90-100`: Excellent
* `75-89`: Great
* `60-74`: Good
* `40-59`: Fair
* `20-39`: Poor
* `0-19`: Not Recommended

Missing-data behavior:

* Temperature is required. A period without temperature is marked not scorable.
* Missing precipitation, wind, or humidity contributes zero points for that category.
* Missing AQI or UV contributes no penalty and is explained as unavailable.
* Missing sunrise or sunset falls back to the NWS daytime flag for daylight scoring.
* Missing optional values are explained as unavailable; they are not converted to fabricated weather values.
* The UI distinguishes unavailable data from a real zero value.

Best-window behavior:

* Version 1 recommends a one-hour window.
* The best window is the highest-scoring upcoming scorable hour from the exposed hourly forecast.
* Ties choose the earliest start time.
* Already-passed hours are excluded.
* If no upcoming hour can be scored, `bestWalkingWindow` is `null`.

Safety and environmental warning rules:

* Feels-like temperature above `90°F`: Excessive heat
* Feels-like temperature below `32°F`: Freezing conditions
* Precipitation probability above `50%`: Rain is likely
* Wind above `20 mph`: Strong wind
* Humidity above `85%`: Very high humidity
* AQI above `150`: Poor air quality
* UV Index `6-7`: Sun protection recommended
* UV Index `8+`: High UV and sun protection recommended
* No remaining daylight or nighttime fallback: Limited daylight

Weekly outlook behavior:

* Daily high/low temperatures, summary, precipitation, and icon come from the NWS daily forecast response.
* Representative daily walking scores are derived from hourly periods for that calendar day.
* The best scorable hourly period for a day is selected by score, with earliest time as the tie-breaker.
* Environmental warning chips are derived from the day's available hourly periods and kept brief.
* If NWS hourly data does not cover a later day, the daily forecast is still shown, but representative score, rating, and best available time are left empty and labeled "Not enough hourly data."

These recommendations are explainable weather-based decision support. They are not medical advice.

### Configuration

Backend defaults:

```properties
server.port=9090
zip.lookup.base-url=https://api.zippopotam.us
zip.lookup.connection-timeout-ms=3000
zip.lookup.response-timeout-ms=5000
weather.nws.base-url=https://api.weather.gov
```

Frontend defaults:

```text
BACKEND_URL=http://localhost:9090
```

Docker Compose sets the frontend container's server-side backend URL to:

```text
BACKEND_URL=http://backend:9090
```

No backend URL is exposed through `NEXT_PUBLIC_*` variables.

### Error Handling

The ZIP endpoint returns application-level errors for:

* Invalid ZIP format: `400`
* ZIP not found: `404`
* ZIP lookup provider unavailable or timed out: `503`
* Weather provider unavailable after a successful ZIP lookup: `502`

The Next.js server action maps these responses to friendly user-facing messages. Backend failures are returned as renderable state and should not crash page rendering.

### Known Limitations

* Only 5-digit United States ZIP codes are supported.
* ZIP+4 and non-US postal codes are not supported.
* Zippopotam.us may return one representative coordinate for ZIP codes that cover multiple places.
* Weather availability still depends on National Weather Service coverage and provider uptime.
* This version recommends walking windows from weather only. Calendar availability and operational workload remain future inputs.
* NWS hourly data may not cover all seven daily forecast cards, so later days may be intentionally unscored.

## Persistence

The current application has no active persistence requirement. There are no datasource properties, database-backed entities, repositories, migrations, or product flows that require PostgreSQL.

PostgreSQL and database-related starter scaffolding are intentionally not included in the runtime configuration. A database can be added later when a real persistence requirement exists, with credentials supplied through environment variables and the database image pinned to a specific version.
