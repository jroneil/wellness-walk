# Wellness Window Architecture

Wellness Window is a weather-first decision-support application for choosing a restorative walking window. The current implementation combines live weather, optional environmental data, transparent scoring, and browser-local user preferences. Calendar and workload integrations remain future expansion points.

## System Overview

```text
Browser
  -> Next.js dashboard and settings UI
  -> Server Action
  -> Spring Boot API
  -> ZIP lookup provider
  -> National Weather Service
  -> Optional Open-Meteo environmental APIs
  -> Explainable recommendation response
```

The browser does not call the Spring Boot backend directly. Interactive weather requests go through a Next.js Server Action, and the Next.js server uses `BACKEND_URL` to reach the backend.

## Frontend Architecture

The frontend is a Next.js 16 App Router application in `front_end/wellness_walk_planner_ui`.

Key files:

- `app/page.tsx`: dashboard shell, status card, settings navigation, weather section.
- `app/weather-section.tsx`: client-side ZIP form, weather display, recommendation UI, hourly cards, weekly outlook.
- `app/weather-actions.ts`: server action for weather requests.
- `app/lib/backend.ts`: server-side backend API client.
- `app/settings/page.tsx`: local preferences form.
- `app/settings/settings.ts`: localStorage defaults, validation, safe parsing, save/reset helpers.
- `app/types.ts`: frontend API and settings contracts.

User preferences are stored only in browser localStorage under `wellness-window-settings:v1`. Malformed or incomplete stored values fall back to defaults.

## Backend Architecture

The backend is a Spring Boot 4.1 Java 21 application in `back_end/wellness-walk-planner`.

Key packages:

- `controller`: REST endpoints.
- `service`: application-level weather orchestration.
- `weather.client`: National Weather Service integration and provider parsing.
- `zip.client` and `zip.service`: ZIP-to-coordinate lookup.
- `environment.client`, `environment.service`, `environment.dto`, `environment.model`: optional Open-Meteo AQI, UV, sunrise, and sunset enrichment.
- `recommendation.service`, `recommendation.dto`, `recommendation.model`: scoring and best-window selection.
- `config`: typed configuration properties.
- `exception`: API exception mapping and weather service errors.

Provider-specific DTOs remain separate from public API DTOs. Public responses are assembled into records under `dto`, `environment.dto`, and `recommendation.dto`.

## Weather Flow

1. User enters a 5-digit US ZIP code.
2. Next.js validates the ZIP format before sending the request.
3. Spring validates the ZIP again.
4. `ZipLookupClient` calls Zippopotam.us for latitude and longitude.
5. `WeatherService` delegates to `NwsWeatherClient`.
6. NWS points endpoint supplies forecast URLs.
7. Hourly forecast drives current fallback data and walking scores.
8. Daily NWS forecast drives seven-day high/low cards and official icons.
9. Open-Meteo enrichment is fetched best-effort.
10. The backend returns a single public `WeatherResponse`.

## ZIP Lookup Flow

```text
GET /api/weather/current/{zip}
  -> WeatherController
  -> ZipWeatherService
  -> ZipLookupClient
  -> WeatherService
```

ZIP lookup errors map to user-facing statuses:

- Invalid format: `400`
- ZIP not found: `404`
- ZIP provider unavailable: `503`
- Weather unavailable after ZIP lookup: `502`

## Environmental Provider Flow

Open-Meteo is used only for optional environmental context:

- Forecast API: hourly UV Index and daily sunrise/sunset.
- Air Quality API: US AQI.

The app does not require an Open-Meteo API key. If environmental data is unavailable, the NWS weather response still succeeds and the recommendation explains unavailable AQI, UV, or daylight context.

## Recommendation Engine

Each scorable hourly period receives a visible 0-100 environmental score:

- Feels-like temperature: 30
- Precipitation probability: 20
- Wind: 10
- Humidity: 10
- Daylight: 10
- AQI: 10
- UV Index: 10

The score is the sum of visible category scores. Preferences do not change the score.

Best-window selection uses:

- highest environmental score first
- preference tie-breakers for time, rain, and wind
- near-tie temperature preference only when safe
- earliest upcoming time as the final deterministic tie-breaker

## Docker Architecture

`compose.yaml` starts two services:

- `backend`: Spring Boot API on port `9090`
- `frontend`: Next.js standalone server on port `3000`

The frontend container calls the backend using:

```text
BACKEND_URL=http://backend:9090
```

No database service is currently part of the runtime because the implemented persistence requirement is browser-local settings only.

## Future Expansion Points

Future work can add:

- calendar availability
- workload or operational status
- authenticated user profiles
- durable server-side settings
- richer notification workflows

Those are intentionally out of scope for the current weather-first version.

## Known Limitations

- Only 5-digit US ZIP codes are supported.
- NWS coverage and provider uptime determine weather availability.
- Open-Meteo data is optional and best-effort.
- Recommendations are wellness decision support, not medical advice.
- Later weekly cards may not have hourly-backed representative scores when NWS hourly data does not extend far enough.
