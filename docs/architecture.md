# Wellness Window Architecture

Wellness Window Version 2.4 combines live weather, transparent scoring, browser-local Manual events, read-only CalDAV, and optional read-only Google Calendar. PostgreSQL stores provider metadata and encrypted credential material; the recommendation engine remains unaware of OAuth and databases.

## System Overview

```text
Browser
  -> Next.js dashboard, calendar, and settings UI
  -> localStorage settings and manual events
  -> session-only normalized CalDAV events
  -> Server Action
  -> Spring Boot API
  -> ZIP lookup provider
  -> National Weather Service
  -> Optional Open-Meteo environmental APIs
  -> CalendarService busy-window filtering
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
- `app/calendar/page.tsx`: manual event CRUD and schedule UI.
- `app/calendar/calendar-service.ts`: versioned local persistence and validation behind a frontend service boundary.
- `app/types.ts`: frontend API and settings contracts.

User preferences are stored only in browser localStorage under `wellness-window-settings:v1`. Malformed or incomplete stored values fall back to defaults.
Manual events are stored under `wellness-window-calendar:v2` and are sent only with a recommendation request.

## Backend Architecture

The backend is a Spring Boot 4.1 Java 21 application in `back_end/wellness-walk-planner`.

Key packages:

- `controller`: REST endpoints.
- `service`: application-level weather orchestration.
- `weather.client`: National Weather Service integration and provider parsing.
- `zip.client` and `zip.service`: ZIP-to-coordinate lookup.
- `environment.client`, `environment.service`, `environment.dto`, `environment.model`: optional Open-Meteo AQI, UV, sunrise, and sunset enrichment.
- `recommendation.service`, `recommendation.dto`, `recommendation.model`: scoring and best-window selection.
- `recommendation.engine`: orchestration plus isolated weather, calendar, preference, model, and time-window components.
- `calendar`, `calendar.model`, `calendar.dto`, `calendar.service`: provider contract/registry, normalized events, synchronization, and conflict lookup.
- `calendar.provider.manual` and `calendar.provider.caldav`: Manual and CalDAV provider boundaries.
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

Version 2.1 uses a modular pipeline:

1. `TimeWindowUtilities` generates overlapping candidates every configured 15 or 30 minutes.
2. Each candidate uses the minimum weather score across every hourly period it overlaps.
3. `AvailabilityAnalyzer` merges overlapping and adjacent busy spans and rejects candidates without the full duration free.
4. `PreferenceScorer` produces an explicit score for time, temperature, rain, and wind.
5. `RecommendationEngine` calculates the visible weighted overall score and applies deterministic tie-breaking.
6. The response retains the ideal blocked weather period and conflict when selection moves.

Default weights are weather 72, availability 20, and preferences 8. Configuration is bound from `recommendation.engine.*` properties.

`CalendarProviderRegistry` registers Manual and CalDAV providers. `CalendarSyncService` performs bounded merge/deduplication and partial-failure fallback. Recommendation scoring consumes only normalized events. See [Calendar Providers](calendar-providers.md).

CalDAV discovery resolves `current-user-principal`, `calendar-home-set`, and depth-one calendar collections. Descriptors stay outside recommendation logic. Selected calendars are fetched independently so one failed collection does not discard successful calendars or Manual events. Stable identity includes provider, calendar, UID, and recurrence occurrence.

## Docker Architecture

`compose.yaml` normally starts three services:

- `backend`: Spring Boot API on port `9090`
- `frontend`: Next.js standalone server on port `3000`
- `postgres`: provider metadata and encrypted credential payload persistence

The frontend container calls the backend using:

```text
BACKEND_URL=http://backend:9090
```

CalDAV environment variables are passed to the existing backend, but the normal stack starts with the provider disabled. The optional `caldav-dev` profile adds Radicale and deterministic provisioning only. Root `.env` enables CalDAV on the same backend and points Docker-side traffic to `http://radicale:5232/`. The frontend/backend ports remain `3000`/`9090`; no second interactive backend exists.

`CalendarProvider` implementations normalize events before `CalendarSyncService`. `OAuthCalendarProvider` is an optional capability used only by Google. Flyway migrates PostgreSQL before JPA validation. `ProviderConnectionService` owns durable metadata/selections, while `ProviderCredentialStore` is the only token/credential boundary. Manual events still travel from browser storage with each recommendation request.

## Future Expansion Points

Future work can add:

- Google Calendar, Microsoft Graph, and Apple Calendar providers
- workload or operational status
- authenticated user profiles
- durable server-side settings
- richer notification workflows

Those are intentionally out of scope for the current weather-first version.

## Version 2.5 Proactive Layer

`RecommendationHistoryService` accepts completed recommendation snapshots and persists only significant changes through `HistoryRepository`. It computes summaries and comparisons without calling the recommendation engine. `NotificationService` consumes the same snapshot plus user policy and returns an eligibility decision and schedule time. Browser delivery owns permission and a single timer because this version has no authenticated user or remote push channel. Settings and delivery counters remain browser-local; meaningful recommendation history is installation-local PostgreSQL data.

## Known Limitations

Version 2.6 adds installation-wide `WalkActivity`, `OpportunityOutcome`, goal, and
retention services downstream of recommendation history. Flyway V3 owns their
PostgreSQL schema. The PWA service worker caches only shell/static resources and
routes every API, OAuth, mutation, calendar, and export request network-only.

- Only 5-digit US ZIP codes are supported.
- NWS coverage and provider uptime determine weather availability.
- Open-Meteo data is optional and best-effort.
- Recommendations are wellness decision support, not medical advice.
- Later weekly cards may not have hourly-backed representative scores when NWS hourly data does not extend far enough.

## Version 2.7 interaction architecture

Dashboard and timeline controls call the existing activity/outcome services. `OpportunityExpiryService` periodically selects a maximum of 100 confidently eligible past recommendation snapshots, excludes explicit outcomes and linked active/completed/partial activity, and records `EXPIRED` transactionally. The service worker owns only shell/static caching and sanitized notification navigation; it never scores recommendations, reads provider credentials, or performs destructive backend mutations.
