# Wellness Window

Wellness Window is an incremental wellness planning application. The current vertical slice lets a user enter a 5-digit United States ZIP code and view live weather through a Spring Boot backend and Next.js frontend.

## Current Capabilities

- ZIP Code weather lookup
- Zippopotam.us ZIP-to-coordinate lookup
- National Weather Service weather retrieval
- Explainable weather-only walking recommendation with feels-like temperature, AQI, UV, and daylight context
- Seven-day walking outlook with official NWS icons
- Backend health endpoint
- Friendly frontend validation and error messages
- Docker Compose workflow for the full app
- Server-side Next.js communication with the backend

## Project Structure

```text
back_end/wellness-walk-planner      Spring Boot backend
front_end/wellness_walk_planner_ui  Next.js frontend
docs                                Product and architecture docs
compose.yaml                        Full-app Docker Compose configuration
```

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
```

The Next.js server uses:

```bash
BACKEND_URL=http://localhost:9090
```

For Docker Compose, this is set to `http://backend:9090` so the Next.js container can call the Spring Boot container over the Compose network. The backend URL is not exposed to browser-side code.

Browser code does not call the Spring Boot backend directly. The health status is fetched in a Server Component path, and weather lookups go through a Server Action.

## Walking Recommendations

The current recommendation engine is deterministic weather-based decision support. It scores upcoming hourly periods from 0 to 100 using feels-like temperature, precipitation probability, wind, humidity, daylight, AQI, and UV Index. Heat Index and Wind Chill are calculated with NOAA formulas only inside their documented ranges; otherwise actual air temperature is used as the feels-like value.

AQI, UV Index, sunrise, and sunset are optional environmental inputs retrieved from Open-Meteo by coordinate. They are kept separate from National Weather Service provider DTOs and may be unavailable without breaking the weather response or walking recommendation.

Missing optional values are shown as unavailable and explained in the recommendation; missing temperature makes an hour not scorable. Ties choose the earliest upcoming hour.

This version does not consider calendar availability or operational workload yet, and it is not medical advice.

## API Endpoints

```text
GET /api/health/status
GET /api/weather/current/{zip}
```

Example:

```text
http://localhost:9090/api/weather/current/01830
```

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

## Persistence

The current application has no active database requirement. Docker Compose does not start PostgreSQL or any other database service. Persistence can be added later when there is a real product requirement for durable storage.

## Documentation

- [Product Requirements](docs/PRD.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Implementation Plan](docs/implementation_plan.md)
- [Project Rules](docs/PROJECT_RULES.md)
- [Technology Stack](docs/technology-stack.md)
