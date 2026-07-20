# Developer Guide

## Project Structure

```text
back_end/wellness-walk-planner      Spring Boot backend
front_end/wellness_walk_planner_ui  Next.js frontend
docs                                Product and engineering documentation
compose.yaml                        Full local runtime
```

## Local Backend

```bash
cd back_end/wellness-walk-planner
./mvnw spring-boot:run
```

Backend default port:

```text
http://localhost:9090
```

## Local Frontend

```bash
cd front_end/wellness_walk_planner_ui
npm install
npm run dev
```

Frontend default port:

```text
http://localhost:3000
```

The frontend server calls the backend through `BACKEND_URL`.

## Docker Workflow

From the repository root:

```bash
docker compose up -d --build
docker compose ps
docker compose logs --tail=100
```

Stop the stack:

```bash
docker compose down
```

## Testing

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

Live CalDAV integration (after starting the development profile):

```bash
docker compose --profile caldav-dev up -d --build
docker compose --profile caldav-dev ps
docker compose --profile caldav-dev logs backend
docker compose --profile caldav-dev exec backend printenv CALDAV_ENABLED
cd back_end/wellness-walk-planner
./mvnw -Pcaldav-integration verify
```

The root `.env` must enable CalDAV and point it to `http://radicale:5232/`; generate the required credential key with `openssl rand -base64 32`. The existing backend remains at `http://localhost:9090`, and the frontend continues to call `http://backend:9090`. Host-run integration tests reach Radicale at `http://localhost:5232/`. There is no second backend or port `9091` service. The normal unit suite never requires Docker or network access. Do not print passwords or the master key.

The frontend uses a system font stack and does not fetch Google Fonts during production builds.

Compose starts PostgreSQL and waits for `pg_isready` before the backend. Flyway applies versioned migrations automatically; Hibernate validates the resulting schema. Never use destructive schema reset outside tests. Generate the development encryption key with `openssl rand -base64 32`, keep it outside source control, and preserve it with the database volume or encrypted credentials become unreadable.

## Coding Standards

- Keep backend provider DTOs separate from public DTOs.
- Use constructor injection for Spring components.
- Prefer typed configuration properties over scattered environment reads.
- Keep frontend backend calls inside server-side helpers or Server Actions.
- Keep browser-local settings logic in `app/settings/settings.ts`.
- Use repositories and migrations for durable provider metadata; never bypass `ProviderCredentialStore` for secrets.
- Do not add calendar, workload, authentication, analytics, or notification features during cleanup phases.

## Backend Folder Conventions

- `controller`: HTTP boundary and request validation.
- `service`: application orchestration.
- `client`: external provider communication.
- `dto`: public or provider-specific transfer records.
- `model`: internal enums and scoring models.
- `configuration` or `config`: typed configuration objects.

## Frontend Folder Conventions

- `app/page.tsx`: dashboard route.
- `app/settings/page.tsx`: settings route.
- `app/weather-section.tsx`: interactive weather dashboard.
- `app/lib/backend.ts`: server-side backend client.
- `app/weather-actions.ts`: Server Action boundary.
- `app/types.ts`: shared frontend types.

## Adding a Provider

1. Create a provider-specific client package.
2. Keep raw provider DTOs inside that provider boundary.
3. Map provider data into application DTOs or service models.
4. Add timeouts and graceful failure behavior.
5. Add focused tests with representative provider payloads.
6. Document configuration and failure behavior.

CalDAV credentials bootstrap from environment and are copied into encrypted storage. Use `.env.example` for placeholders, never commit `.env`, and see `docs/calendar-providers.md`, `docs/google-calendar.md`, and `docs/security.md`.

## Extending Scoring

1. Add visible score fields to the recommendation DTO.
2. Keep the total equal to visible category scores.
3. Explain every awarded or removed point.
4. Add tests for boundaries and missing data.
5. Update `docs/recommendation-engine.md`.

## Adding Frontend Components

1. Prefer small components close to the route that owns them.
2. Extract only when reuse or readability improves.
3. Preserve accessible labels, keyboard focus, and responsive behavior.
4. Add tests for user-visible behavior.

## AI-Assisted Development Workflow

This project has been built incrementally with AI assistance. Each phase should:

- read the product and implementation docs first
- inspect existing code before editing
- make small, reviewable changes
- preserve working behavior
- run tests/builds before finishing
- document implementation decisions when they affect future contributors

## Version 2.5 Development

Flyway migration `V2__recommendation_history.sql` owns the history table. Do not enable Hibernate schema generation. Browser notification tests must mock the Notification API and never depend on OS permission dialogs. Backend notification tests use fixed instants for quiet-hours, weekend, threshold, cooldown, and daily-limit rules. History tests verify deduplication and summaries. The browser schedules only after a user-triggered weather refresh; there is no background polling process.

## Version 2.6 Development

Flyway migration `V3__walk_activity_and_data_controls.sql` extends existing data
without modifying V1 or V2. Activity state changes are meaningful writes; elapsed
timer updates stay in the browser. Keep service-worker caching exclusions for all
`/api`, OAuth, mutation, calendar, and export traffic. Data deletion must never
cascade into provider configuration or credentials.

## Version 2.7 validation

Run backend tests/package, frontend Vitest/build/lint, `npm run test:e2e`, `git diff --check`, then rebuild Compose. Playwright setup and deterministic-test guidance are in [end-to-end-testing.md](end-to-end-testing.md). Configure the expiry cadence with `OPPORTUNITY_EXPIRY_CRON`; the default is every 15 minutes.
