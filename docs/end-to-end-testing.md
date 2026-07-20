# Browser end-to-end testing

The frontend uses Playwright as its single browser-level test framework. Unit and component tests remain in Vitest; Vitest explicitly excludes `e2e/`.

Install the Chromium test browser once:

```bash
cd front_end/wellness_walk_planner_ui
npx playwright install chromium
```

Run against the development server managed by Playwright:

```bash
npm run test:e2e
```

Or run against an already-started deterministic local or Docker environment:

```bash
E2E_BASE_URL=http://127.0.0.1:3000 npm run test:e2e
```

The PWA suite checks the dashboard, manifest and dedicated PNG icons, service-worker availability, offline route behavior, and the rule that sensitive API and OAuth paths are excluded from cache handling. It does not use live weather providers or Google OAuth credentials. Expand the fixture-backed suite as backend test-data seeding becomes available.

