# Wellness Window

Wellness Window helps a user decide when to take a short restorative walk by combining live weather, optional environmental context, and transparent recommendation scoring.

The current application is intentionally weather-first. It does not yet include calendar availability, workload awareness, authentication, notifications, analytics, or server-side persistence.

## Problem Statement

Weather apps provide conditions, but they do not answer the practical question: "Is this a good moment to step away for a walk?" Wellness Window reduces that decision friction with a readable dashboard, explainable scoring, and lightweight user preferences.

## Major Features

- 5-digit US ZIP weather lookup.
- Zippopotam.us ZIP-to-coordinate lookup.
- National Weather Service current, hourly, and seven-day forecast data.
- Official NWS weather icons with accessible fallback placeholders.
- Optional Open-Meteo environmental data for AQI, UV Index, sunrise, and sunset.
- Explainable walking score and best-window selection.
- Hourly forecast cards and seven-day walking outlook.
- Browser-local preferences for default ZIP, duration, comfort preferences, minimum score, and display units.
- Settings page with validation, save confirmation, and reset.
- Docker Compose workflow for the full app.
- Server-side Next.js communication with the backend.

## Architecture

```text
Browser
  -> Next.js App Router frontend
  -> Server Action
  -> Spring Boot API
  -> ZIP lookup provider
  -> National Weather Service
  -> Optional Open-Meteo enrichment
```

The browser stores settings locally but does not call the Spring Boot API directly. Next.js server-side code owns backend communication.

See [Architecture](docs/architecture.md) for details.

## Technology Stack

- Backend: Spring Boot 4.1, Java 21, Maven
- Frontend: Next.js 16, React, TypeScript, Tailwind CSS
- Runtime: Docker Compose
- Persistence: browser localStorage only
- Weather provider: National Weather Service
- Environmental provider: Open-Meteo

## Project Structure

```text
back_end/wellness-walk-planner      Spring Boot backend
front_end/wellness_walk_planner_ui  Next.js frontend
docs                                Product and architecture docs
compose.yaml                        Full-app Docker Compose configuration
```

## Screenshots

No screenshots are currently committed. See `docs/screenshots/README.md` for the expected screenshot set.

## Requirements

- Docker and Docker Compose
- Java 21 for local backend development
- Node.js and npm for local frontend development

## Run With Docker Compose

From the repository root:

```bash
docker compose up --build
```

Open the app:

```text
http://localhost:3000
```

The Compose stack starts:

- `frontend`: Next.js app on `http://localhost:3000`
- `backend`: Spring Boot API on `http://localhost:9090`

Stop the stack:

```bash
docker compose down
```

## Local Development

Start the backend:

```bash
cd back_end/wellness-walk-planner
./mvnw spring-boot:run
```

Start the frontend in another terminal:

```bash
cd front_end/wellness_walk_planner_ui
npm install
npm run dev
```

Open:

```text
http://localhost:3000
```

## Configuration

Backend defaults live in:

```text
back_end/wellness-walk-planner/src/main/resources/application.properties
```

Important defaults:

```properties
server.port=9090
zip.lookup.base-url=https://api.zippopotam.us
weather.nws.base-url=https://api.weather.gov
environment.open-meteo.forecast-base-url=https://api.open-meteo.com/v1/forecast
environment.open-meteo.air-quality-base-url=https://air-quality-api.open-meteo.com/v1/air-quality
```

The Next.js server uses:

```bash
BACKEND_URL=http://localhost:9090
```

For Docker Compose, this is set to `http://backend:9090` so the Next.js container can call the Spring Boot container over the Compose network. The backend URL is not exposed to browser-side code.

Browser code does not call the Spring Boot backend directly. The health status is fetched in a Server Component path, and weather lookups go through a Server Action.

## Recommendation Engine

The current recommendation engine is deterministic weather-based decision support. It scores upcoming hourly periods from 0 to 100 using seven visible categories: feels-like temperature 30, precipitation 20, wind 10, humidity 10, daylight 10, AQI 10, and UV Index 10. The total score is the sum of those category scores; there are no hidden deductions or negative penalties.

Heat Index and Wind Chill are calculated with NOAA formulas only inside their documented ranges; otherwise actual air temperature is used as the feels-like value.

AQI, UV Index, sunrise, and sunset are optional environmental inputs retrieved from Open-Meteo by coordinate. They are kept separate from National Weather Service provider DTOs and may be unavailable without breaking the weather response or walking recommendation.

Missing optional values are shown as unavailable, award zero points for that category, and are explained in the recommendation; missing temperature makes an hour not scorable.

User preferences are stored in browser localStorage and sent as optional query parameters during weather lookup. Preferences do not change the 0-100 environmental score. They affect best-window selection only as transparent ranking guidance: preferred time, rain tolerance, and wind tolerance break ties; cooler or warmer temperature preference may choose a near-tie window when there are no serious weather warnings. The selected walk duration controls the displayed end time, and minimum score adds messaging when the best available window falls below the user's threshold.

This version does not consider calendar availability or operational workload yet, and it is not medical advice.

See [Recommendation Engine](docs/recommendation-engine.md) for scoring boundaries and tie-breaking rules.

## API Endpoints

```text
GET /api/health/status
GET /api/weather/current/{zip}
```

Example:

```text
http://localhost:9090/api/weather/current/01830?walkDurationMinutes=30&preferredTimeOfDay=AFTERNOON&temperaturePreference=BALANCED&rainTolerance=LIGHT_RAIN_OK&windTolerance=MODERATE&minimumScore=60&unitSystem=US
```

See [API Documentation](docs/api.md) for response examples and error responses.

## Validation

Backend:

```bash
cd back_end/wellness-walk-planner
./mvnw test
./mvnw -q -DskipTests package
```

Frontend:

```bash
cd front_end/wellness_walk_planner_ui
npm run test -- --run
npm run build
```

Docker:

```bash
docker compose config
docker compose build
docker compose up --build
```

## Environment Variables

Backend:

| Variable | Default |
| --- | --- |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://127.0.0.1:3000` |
| `ZIP_LOOKUP_BASE_URL` | `https://api.zippopotam.us` |
| `WEATHER_NWS_BASE_URL` | `https://api.weather.gov` |
| `ENVIRONMENT_OPEN_METEO_FORECAST_BASE_URL` | `https://api.open-meteo.com/v1/forecast` |
| `ENVIRONMENT_OPEN_METEO_AIR_QUALITY_BASE_URL` | `https://air-quality-api.open-meteo.com/v1/air-quality` |

Frontend:

| Variable | Default |
| --- | --- |
| `BACKEND_URL` | `http://localhost:9090` locally, `http://backend:9090` in Docker |

## Persistence

User settings are persisted only in browser localStorage under a versioned key and are safely defaulted if missing or malformed. The backend does not store user preferences.

The current application has no active database requirement. Docker Compose does not start PostgreSQL or any other database service. Durable server-side persistence can be added later when there is a real product requirement.

## Known Limitations

- Only 5-digit US ZIP codes are supported.
- Weather availability depends on provider coverage and uptime.
- Open-Meteo environmental enrichment is optional and best-effort.
- Recommendations are not medical advice.
- Calendar and workload inputs are not implemented.
- No screenshots are currently committed.

## Future Roadmap

- Calendar availability integration.
- Workload or operational status integration.
- Server-side user profiles if durable preferences become a real requirement.
- Better screenshot and release documentation.
- More granular frontend component extraction as the UI grows.

## Development Workflow

Use the existing projects; do not create replacement apps. For each phase:

1. Read the PRD and implementation plan.
2. Inspect existing code.
3. Make small, focused changes.
4. Preserve current behavior unless the phase explicitly changes it.
5. Run relevant tests and builds.
6. Update documentation when behavior or contracts change.

AI-assisted development has been used throughout this project. Human-readable docs and tests are kept as the durable source of project intent.

## Documentation

- [Product Requirements](docs/PRD.md)
- [Architecture](docs/architecture.md)
- [API Documentation](docs/api.md)
- [Recommendation Engine](docs/recommendation-engine.md)
- [Developer Guide](docs/developer-guide.md)
- [Implementation Plan](docs/implementation_plan.md)
- [Project Rules](docs/PROJECT_RULES.md)
- [Technology Stack](docs/technology-stack.md)
