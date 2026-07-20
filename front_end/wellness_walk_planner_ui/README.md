# Wellness Window UI

Next.js frontend for Wellness Window, an incremental wellness planning app. The current vertical slice lets a user enter a 5-digit United States ZIP code and render live weather returned by the Spring Boot backend.

## Current Features

- Backend health/status check
- ZIP Code weather lookup form
- Friendly validation and service error messages
- Live weather display using the backend weather DTO, including NWS icons and environmental conditions
- Server-side communication with the Spring Boot backend

## Requirements

- Node.js
- npm
- Backend running on `http://localhost:9090`
- Docker and Docker Compose for the whole-app container workflow

The backend translates ZIP codes through Zippopotam.us, then reuses the National Weather Service weather implementation. The backend may also include optional environmental fields such as feels-like temperature, AQI, UV Index, sunrise, sunset, and remaining daylight. The frontend does not request latitude or longitude directly, and browser code does not call the Spring Boot backend directly.

## Configuration

The Next.js server reads the backend URL from:

```bash
BACKEND_URL=http://localhost:9090
```

If the variable is not set, it defaults to `http://localhost:9090`.

This variable is server-only. Do not expose the backend URL through `NEXT_PUBLIC_*` variables.

## Run Locally

Install dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

Open:

```text
http://localhost:3000
```

## Docker Compose

The whole app can be run from the repository root with Docker Compose:

```bash
docker compose up --build
```

Open:

```text
http://localhost:3000
```

The Compose stack starts:

- `backend`: Spring Boot API on `http://localhost:9090`
- `frontend`: Next.js app on `http://localhost:3000`

The frontend container runs with:

```bash
BACKEND_URL=http://backend:9090
```

Use this environment variable if the Next.js server needs to call a different backend URL:

```bash
BACKEND_URL=http://your-backend:9090 docker compose up --build
```

Stop the stack:

```bash
docker compose down
```

The application currently has no active database dependency, so the Compose stack does not start PostgreSQL or any other database service.

## Local Development

Typical local workflow:

```bash
cd back_end/wellness-walk-planner
./mvnw spring-boot:run
```

In a second terminal:

```bash
cd front_end/wellness_walk_planner_ui
npm install
npm run dev
```

## Validation

Run tests:

```bash
npm run test -- --run
```

Create a production build:

```bash
npm run build
```

## Backend Notes

The backend is the source of truth for provider calls and validation responses. It exposes:

```text
GET /api/health/status
GET /api/weather/current/{zip}
```

Supported ZIP input is currently limited to 5-digit United States ZIP codes.

The UI reaches these endpoints from Next.js server-side code:

- health status is fetched while rendering the page
- weather lookup is submitted through a Server Action

## Persistence

This UI does not require a database. The project can add persistence later when a real product requirement needs durable storage.
