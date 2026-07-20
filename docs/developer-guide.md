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

The frontend build may need network access because the current Next.js font setup fetches Google Fonts during production build.

## Coding Standards

- Keep backend provider DTOs separate from public DTOs.
- Use constructor injection for Spring components.
- Prefer typed configuration properties over scattered environment reads.
- Keep frontend backend calls inside server-side helpers or Server Actions.
- Keep browser-local settings logic in `app/settings/settings.ts`.
- Do not introduce a database without an active persistence requirement.
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
