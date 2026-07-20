# Wellness Window Troubleshooting

This guide covers common local-development failures in the Docker Compose setup.
Run commands from the repository root unless a section says otherwise. Start with
`docker compose ps`, then inspect the log for the service that is not healthy.
Logs may contain URLs and error categories, but Wellness Window intentionally does
not log provider passwords, OAuth tokens, or the provider credential master key.

## 1. Backend fails to start

### Symptoms

- The `backend` container exits immediately or repeatedly restarts.
- `http://localhost:9090` is unavailable.
- The frontend opens on port 3000, but weather or calendar API operations fail.

### Why it happens

Spring Boot must bind its configuration, connect to PostgreSQL, validate and run
Flyway migrations, and construct provider encryption services before port 9090 is
opened. A failure in any of those startup stages stops the process. The frontend
is a separate container, so it can remain visible while the backend is down.

### Diagnose

```bash
docker compose ps
docker compose logs --tail=200 backend
docker compose logs --tail=100 postgres
```

Look near the first `Caused by:` line, not only at the final stack-trace line.
Common causes include:

- Missing or malformed environment variables.
- PostgreSQL not being ready or rejecting the configured username/password.
- Flyway migration validation or SQL failures.
- An invalid provider URL, timeout, or provider configuration.
- A missing, malformed, or changed credential-encryption key.

Check whether a non-secret variable reached the container:

```bash
docker compose exec -T backend printenv CALDAV_ENABLED
```

For secrets, check only whether a value exists—never print it:

```bash
docker compose exec -T backend sh -c 'test -n "$PROVIDER_CREDENTIAL_MASTER_KEY" && echo configured || echo missing'
```

After changing `.env`, replace the backend container. A restart preserves the old
container environment and therefore does not load the new value.

```bash
docker compose up -d --build --force-recreate backend
```

## 2. PostgreSQL password changed

### Why changing `.env` does not change the database password

The official PostgreSQL image uses `POSTGRES_DB`, `POSTGRES_USER`, and
`POSTGRES_PASSWORD` to initialize an **empty** data directory. Wellness Window
stores that directory in the `wellness-postgres-data` Docker volume. On later
starts, PostgreSQL finds an initialized database and does not recreate the role or
change its password. Compose can therefore give the backend a new password while
the existing database still expects the original one.

### Symptoms and diagnosis

The backend exits, and its log or the PostgreSQL log includes an error similar to:

```text
password authentication failed for user "wellness"
```

```bash
docker compose logs --tail=200 backend
docker compose logs --tail=100 postgres
```

### Resolution A: restore the original password

Set `POSTGRES_PASSWORD` in `.env` back to the value used when the volume was first
created, then recreate the backend:

```bash
docker compose up -d --force-recreate backend
```

This is the safest option because it preserves local data.

### Resolution B: recreate disposable development data

If the database contains nothing important:

```bash
docker compose down -v
docker compose up -d
```

> **Warning:** `docker compose down -v` permanently deletes the project's local
> PostgreSQL volume, including provider connections, encrypted credentials,
> calendar selections, and recommendation history. It does not merely reset the
> password.

## 3. Provider Credential Master Key

`PROVIDER_CREDENTIAL_MASTER_KEY` is a Base64-encoded 32-byte key used for
AES-256-GCM encryption of CalDAV credentials and Google OAuth tokens stored in
PostgreSQL. The key stays outside PostgreSQL so a database copy alone cannot
decrypt those secrets.

Generate one for a new installation:

```bash
openssl rand -base64 32
```

Paste the complete output into the repository-root `.env`:

```dotenv
PROVIDER_CREDENTIAL_MASTER_KEY=<generated-value>
```

Do not commit, log, or share this value. Recreate the backend after setting it.

```bash
docker compose up -d --build --force-recreate backend
```

### Why changing the key breaks providers

Existing credential rows were authenticated and encrypted with the previous key.
A new key cannot authenticate or decrypt them. Symptoms include
`CONFIGURATION_REQUIRED`, failed provider connections, credential-authentication
exceptions, or a backend warning about credential encryption.

Restore the original key whenever possible. In development, if the old key is
lost, disconnect and reconnect the provider so credentials are stored with the
new key. For the targeted CalDAV recovery that preserves unrelated database data,
see [Calendar provider credential recovery](../README.md#recover-caldav-after-the-master-key-changed).

In production, do not rotate this key without a planned migration that decrypts
each credential with the old key and re-encrypts it with the new key. Back up and
test the migration before rotation.

## 4. CalDAV cannot connect

### Why it happens

CalDAV requires four separate layers to work: a running server, Docker network
reachability, valid credentials, and successful calendar discovery/selection.
A successful server health check does not prove the backend credentials are valid,
and a successful connection test does not prove calendars have been selected for
synchronization.

### Checklist

1. Start the optional local server profile and confirm it is healthy:

   ```bash
   docker compose --profile caldav-dev up -d
   docker compose --profile caldav-dev ps
   docker compose logs --tail=100 radicale
   ```

   Also confirm the backend remains `Up` and responds on port 9090. A healthy
   Radicale container cannot compensate for a backend that failed during database
   migration or configuration startup.

   ```bash
   docker compose ps
   curl -fsS http://localhost:9090/api/health/status
   ```

2. Confirm `.env` uses the Docker hostname from the backend:

   ```dotenv
   CALDAV_ENABLED=true
   CALDAV_SERVER_URL=http://radicale:5232/
   CALDAV_USERNAME=wellness
   CALDAV_PASSWORD=wellness-dev-only
   ```

3. Test reachability from the backend container without exposing credentials:

   ```bash
   docker compose exec -T backend sh -c 'wget -q -O /dev/null http://radicale:5232/ && echo reachable'
   ```

   A `401 Unauthorized` response can still prove the HTTP server is reachable; it
   means authentication must be checked next. Minimal production images may not
   include `wget`, in which case use provider **Test connection** and backend logs.

4. Verify the username and password. For a real provider, prefer an app-specific
   password when supported. Recreate the backend after changing `.env`.

5. Open `http://localhost:3000/calendar`, run **Test connection**, then
   **Discover calendars**. Select at least one event calendar and save it.

6. Run **Synchronize now** and inspect sanitized errors:

   ```bash
   docker compose logs --tail=200 backend
   ```

7. Verify that the selected calendars are durable. Save a selection in the UI,
   replace the backend container, refresh the Calendar page, and confirm the same
   calendars remain selected:

   ```bash
   docker compose up -d --force-recreate backend
   curl -fsS http://localhost:9090/api/calendar/providers/CALDAV/status
   ```

   Selections are stored in PostgreSQL, so replacing a container should not remove
   them. If they disappear, confirm the backend still points to the same `postgres`
   service and volume, check for an accidental `docker compose down -v`, then run
   discovery again. A provider can be reachable while still contributing no events
   if no event calendar is selected.

For local Radicale, the backend uses `http://radicale:5232/`; a command running on
the host uses `http://localhost:5232/`. See [Calendar Providers](calendar-providers.md).

## 5. Google Calendar OAuth

### Checklist

- The Google Calendar API is enabled in the selected Google Cloud project.
- The OAuth client type is **Web application**.
- `GOOGLE_CALENDAR_ENABLED=true` is set.
- The client ID and client secret come from the same Google Cloud project.
- The consent screen is configured, and a test account is listed when required.
- The authorized backend callback is exactly:

  ```text
  http://localhost:9090/api/calendar/providers/GOOGLE/oauth/callback
  ```

- The backend callback redirects the browser to the fixed Calendar result page;
  there is no separate user-configurable frontend OAuth callback environment value.
- The backend was recreated after `.env` changes.

### Redirect mismatch

Google compares redirect URIs as exact strings. `http` versus `https`,
`localhost` versus `127.0.0.1`, port, capitalization, path, and a trailing slash
all matter. If Google reports `redirect_uri_mismatch`, compare the URI configured
in Google Cloud with `GOOGLE_CALENDAR_REDIRECT_URI` character by character.

If authorization succeeds but discovery fails, verify the Calendar API is enabled,
the read-only scopes were granted, and the master key has not changed. Disconnect
and reconnect after correcting credentials. See [Google Calendar](google-calendar.md).

## 6. Flyway migration failures

### Why they happen

Flyway records every applied migration and its checksum in
`flyway_schema_history`. Startup validation fails when an applied migration file
was edited, versions are duplicated or out of order, SQL is incompatible with the
existing schema, or a previous migration stopped partway through.

### Diagnose and validate

```bash
docker compose logs --tail=250 backend
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "TABLE flyway_schema_history;"'
```

The backend runs Flyway validation automatically at startup. Compare the reported
version and checksum with files under:

```text
back_end/wellness-walk-planner/src/main/resources/db/migration
```

### Resolve safely

- Never edit a migration that has been applied to a shared or retained database.
  Add a new, higher-version migration that corrects the schema.
- Fix duplicate versions or incorrect migration ordering before retrying.
- If only disposable local data is affected, reset the volume as described in
  [Full reset](#10-full-reset).
- Flyway `repair` changes migration-history metadata; it does not repair a broken
  schema. Use it only after backing up the database and proving the schema already
  matches the intended migrations. Record why the repair is safe. Do not use it to
  hide a checksum mismatch caused by modifying an applied migration.

After correction, recreate the backend and check for `Successfully validated` and
`Schema ... is up to date` in its log.

## 7. Docker networking

`localhost` always means “this machine or container.” Inside the backend container,
`localhost:5432` points back to the backend container, not PostgreSQL. Compose
creates DNS names from service names, so containers communicate using those names:

```text
backend -> jdbc:postgresql://postgres:5432/wellness
backend -> http://radicale:5232/
frontend -> http://backend:9090
```

From the development host, use published ports instead:

```text
Browser/host -> http://localhost:3000
Browser/host -> http://localhost:9090
Host tools   -> http://localhost:5232
```

Diagnose the network and configured URLs with:

```bash
docker compose ps
docker compose exec -T backend printenv DATABASE_URL
docker compose exec -T backend printenv CALDAV_SERVER_URL
docker compose exec -T frontend printenv BACKEND_URL
```

Do not use `printenv` for passwords, OAuth secrets, tokens, or the master key.

## 8. Browser Notifications

Notifications are browser-local timers, not server push. They require explicit
permission and are evaluated only after the dashboard completes a recommendation.
Closing the tab/browser cancels pending timers.

If a notification does not appear, check:

- The browser supports the Notification API.
- Permission is `GRANTED`, not `DEFAULT` or `DENIED`. A denied permission must be
  changed in the browser's site settings; the application cannot override it.
- Notifications are enabled at `http://localhost:3000/settings/notifications`.
- The recommendation meets the minimum score and has no calendar conflict.
- Quiet hours, weekend policy, and working-hours-only settings allow delivery.
- The daily limit has not been reached and the cooldown has expired.
- The recommendation has not already started and an identical recommendation was
  not already delivered.
- The dashboard and browser remain open until the scheduled time.

Saving settings alone does not trigger a notification. Run a recommendation after
saving so eligibility can be evaluated. See [Browser Notifications](notifications.md).

## 9. Browser cache issues

### Symptoms and cause

The UI can appear unchanged after a code update because the browser retains assets,
the running frontend container still contains an older production build, or Next.js
build output was created before the change. Stopping and starting the same container
does not rebuild its image.

Try these steps in order:

1. Hard-refresh the page (`Ctrl+Shift+R` or `Cmd+Shift+R`).
2. Close duplicate tabs and reopen `http://localhost:3000`.
3. Rebuild and replace the frontend container:

   ```bash
   docker compose up -d --build --force-recreate frontend
   docker compose logs --tail=100 frontend
   ```

4. For local `npm run dev`, stop the process and start it again. If a stale Next.js
   build remains, stop the process before removing `.next`, then restart development.

Avoid clearing browser site data casually: manual events, settings, and notification
delivery state are stored in localStorage and will be lost.

## 10. Full reset

Use a full reset only for local development when all persisted PostgreSQL data can
be discarded:

```bash
docker compose down -v
docker compose build --no-cache
docker compose up -d
```

If using local CalDAV, include the profile on startup:

```bash
docker compose --profile caldav-dev up -d
```

> **Data-loss warning:** `down -v` removes local PostgreSQL data, including provider
> metadata, selected calendars, encrypted credentials/tokens, and recommendation
> history. A no-cache build is slower and downloads/rebuilds dependencies; it is
> useful for suspected stale image layers, not as the first response to every error.

The reset does not clear browser localStorage. Clear site data separately only if
you also intend to remove manual events, preferences, and notification state.

## 11. Useful commands

```bash
# Service state and health
docker compose ps

# Recent logs
docker compose logs --tail=200 backend
docker compose logs --tail=200 frontend
docker compose logs --tail=200 postgres
docker compose logs --tail=200 radicale

# Follow a log until interrupted with Ctrl+C
docker compose logs -f backend

# Inspect non-secret backend environment values
docker compose exec -T backend printenv CALDAV_ENABLED
docker compose exec -T backend printenv CALDAV_SERVER_URL

# Open PostgreSQL's interactive shell
docker compose exec postgres psql -U wellness -d wellness

# Run one PostgreSQL query non-interactively using container configuration
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT version();"'

# Replace containers after configuration changes
docker compose up -d --build --force-recreate backend frontend
```

The `radicale` service exists only when the `caldav-dev` profile is active. Never
print or paste secret environment variables into issue reports.

## 12. Frequently asked questions

### Why does the installed app show the offline page instead of old recommendations?

This is intentional. The Version 2.6 service worker caches only the shell and
static assets. Weather, calendar data, history, OAuth responses, mutations, and
exports are sensitive or time-dependent and remain network-only. Restore the
connection and reload to obtain a current recommendation.

### Why did an active walk survive refresh but its timer pause?

The backend persists the meaningful start timestamp, while the UI derives elapsed
time locally. After reconnecting, `GET /api/walks/active` restores the session and
the elapsed display is recalculated; timer ticks are never continuously written.

### Why does changing `POSTGRES_PASSWORD` not work?

The PostgreSQL image reads it only while initializing an empty data volume. An
existing volume already contains the database role and password. Restore the old
value, deliberately change the database role password, or delete disposable local
data and initialize a new volume.

### Why did encrypted credentials stop working after changing the master key?

AES-GCM authenticates ciphertext with the exact key used when it was written. A
different key correctly rejects the old data rather than returning corrupted or
incorrect credentials.

### Can I change the master key later?

Not by simply replacing the environment variable. Production rotation requires a
migration that has both keys temporarily available and re-encrypts every stored
credential. For development, reconnect providers or use the documented targeted
CalDAV reset if the old key is unavailable.

### Can I delete the PostgreSQL volume?

Yes, for disposable development data. Doing so permanently removes all server-side
local state. It does not remove browser-local manual events or settings.

### Do I need to reconnect CalDAV after changing encryption?

Yes, unless you restore the original key. The current username/password must be
stored again with the new key. Follow the targeted recovery procedure rather than
deleting the entire volume when other local database data should be preserved.

### Why does `docker compose restart` ignore my `.env` change?

Restart starts the same container with the environment captured when that container
was created. Use `docker compose up -d --force-recreate <service>` to replace it.

### Install, notification, or offline controls do not appear

Install prompts are browser-controlled and appear only after installability criteria are met; Safari and Firefox may require their manual page/share menu. Notification actions are also browser-dependent. Check DevTools **Application** for `/manifest.webmanifest`, `/sw.js`, and the active worker, clear old site data if a stale worker remains, and reload. Offline mode intentionally disables backend mutations and never reports an unsynchronized operation as successful. See [browser support](browser-support.md).
