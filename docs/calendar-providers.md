# Calendar Providers

Wellness Window 2.4 provides Manual, CalDAV, and optional Google Calendar providers behind normalized read-only contracts.

## Architecture

`CalendarProvider` exposes lifecycle, discovery, descriptor listing, selection, bounded per-calendar retrieval, synchronization status, capabilities, and safe disconnect behavior. `CalendarSyncService` merges enabled-provider results, reports each calendar independently, deduplicates by provider + calendar + UID + occurrence identity, and preserves Manual events and successful calendars after partial failure. The recommendation engine receives normalized `CalendarEvent` values only.

The registered providers are Manual Calendar (browser-local CRUD), CalDAV (read-only), and Google Calendar (optional read-only OAuth). Microsoft remains a UI placeholder.

## CalDAV Reference Compatibility

Radicale 3.7.3 is the live-tested reference server. The client uses Basic authentication, WebDAV `PROPFIND`, and bounded CalDAV `calendar-query` `REPORT`. Other servers may work, but compatibility is not claimed without testing.

## Discovery and Selection

Unless `CALDAV_CALENDAR_PATH` overrides discovery, the provider:

1. requests `current-user-principal`/`principal-URL` from the configured server;
2. requests `calendar-home-set` from the principal;
3. enumerates the calendar home at depth one;
4. maps display name, resource type, supported components, privilege set, optional color, and safe description into `CalendarDescriptor` values.

One usable VEVENT calendar auto-selects. Multiple calendars require explicit selection. Selections are durably stored in PostgreSQL and survive backend restart. `CALDAV_CALENDAR_IDS` is bootstrap input and does not overwrite an existing durable choice. Explicit paths remain supported and bypass discovery.

Use HTTPS and an app password where supported. Plain HTTP is accepted only for `localhost`/`127.0.0.1` development.

## Configuration and Credentials

```text
CALDAV_ENABLED=false
CALDAV_SERVER_URL=https://calendar.example.test/
CALDAV_USERNAME=calendar-user
CALDAV_PASSWORD=runtime-app-password
CALDAV_CALENDAR_PATH=/user/calendar/
CALDAV_CALENDAR_IDS=
CALDAV_DEFAULT_TIMEZONE=UTC
CALDAV_LOOKAHEAD_DAYS=7
CALDAV_CONNECTION_TIMEOUT=3s
CALDAV_READ_TIMEOUT=5s
CALDAV_MAX_OCCURRENCES_PER_EVENT=500
CALDAV_MAX_EVENTS_PER_CALENDAR=2000
CALDAV_MAX_RESPONSE_BYTES=2000000
CALDAV_MAX_EXPANSION_DAYS=31
```

Environment credentials are secure runtime overrides/bootstrap values. They are copied into `ProviderCredentialStore` and encrypted with AES-256-GCM. APIs, logs, browser storage, and ordinary entity columns never contain secrets. Persisted non-secret configuration is used after runtime overrides; existing selections are not silently replaced.

Precedence is explicit: non-empty environment server/credential values bootstrap a new connection and act as intentional runtime overrides. If an explicit username or password changes while the same master key is available, the encrypted value is rotated. Empty environment credential values restore encrypted persisted credentials. Persisted calendar selections always survive restart and are not overwritten by credential bootstrap. A missing master key produces `CONFIGURATION_REQUIRED`; it never exposes or logs the password.

## Event Normalization and Privacy

The mapper retains only UID, title, start/end, busy state, source/provider identity, timezone, and all-day state. Descriptions, attendees, email addresses, conference links, and attachments are not part of the application model.

- `TRANSP:TRANSPARENT` is free; absent/opaque transparency is busy.
- `STATUS:CANCELLED` does not block availability.
- Missing summaries become `Busy`.
- UTC values retain UTC; `TZID` values use the named zone.
- Date-only events begin at local midnight in their supplied zone; UTC is the explicit fallback when no zone exists.
- Missing `DTEND` means one day for all-day or one hour for timed events.

## iCalendar Library Decision

Version 2.3 replaces custom recurrence code with ical4j 4.2.5. The library is BSD-3-Clause, actively maintained, Java 17+ compatible, and provides parser, VTIMEZONE, RRULE/RDATE/EXDATE, and bounded recurrence-set support. Version 4.2.5 was selected after recent upstream fixes covering recurrence resource exhaustion, timezone behavior, parser security, and EXDATE UTC handling. The application retains only a small overlay that reconciles detached `RECURRENCE-ID` VEVENT components with the library-generated master recurrence set.

## Recurrence and Timezones

Synchronization always has a bounded range. ical4j handles DAILY, WEEKLY, MONTHLY, YEARLY, INTERVAL, COUNT, UNTIL, common BY* clauses, EXDATE, and RDATE. The mapper applies changed/cancelled detached instances using `RECURRENCE-ID`. Limits bound range days, response bytes, resources, and occurrences per event. Malformed or excessive inputs return sanitized partial errors.

Canonical comparison uses instants represented by `OffsetDateTime`. TZID and VTIMEZONE data are parsed by ical4j; floating values use `CALDAV_DEFAULT_TIMEZONE`, never the JVM default. Tests cover UTC, spring-forward, fall-back, named TZID, and date-only events. All-day events retain provider calendar-date semantics.

## Synchronization and Failure Handling

Synchronization is explicit, not scheduled, and uses a seven-day UI range. Selected calendars are queried independently. Responses include last attempt/success, per-calendar counts/errors, and partial status. External events are browser-session-only; there is no database. Manual events remain available when CalDAV fails.

The dashboard identifies conflict sources as Manual or CalDAV without exposing descriptions or attendees.

Safe logs include provider type, opaque calendar identifier, duration, event count, and error category. Logs exclude credentials, authorization headers, raw XML/iCalendar, attendee data, descriptions, and event titles.

## Radicale Development and Integration

```bash
cp .env.example .env
# Generate a key, then paste its output into PROVIDER_CREDENTIAL_MASTER_KEY in .env.
openssl rand -base64 32
docker compose --profile caldav-dev up -d --build
docker compose --profile caldav-dev ps
docker compose --profile caldav-dev logs backend
docker compose --profile caldav-dev exec backend printenv CALDAV_ENABLED
cd back_end/wellness-walk-planner
./mvnw -Pcaldav-integration verify
```

Set the root `.env` CalDAV values to `true`, `http://radicale:5232/`, `wellness`, `wellness-dev-only`, an empty calendar path/IDs, and `America/New_York` before starting the profile. The profile builds pinned Radicale 3.7.3, provisions Work and Personal calendars, and loads recurrence/all-day/transparent/cancelled fixtures. The existing frontend uses the existing backend at `backend:9090`; no port `9091` service exists. Docker services use `http://radicale:5232/`, while the host-run integration tests use `http://localhost:5232/`. Do not print passwords or the master key. Normal Compose and `./mvnw test` do not require Radicale.

### CalDAV credential recovery

After editing `.env`, recreate the backend with
`docker compose up -d --build --force-recreate backend`; restarting an existing
container does not reload its environment. Check master-key presence without
printing it:

```bash
docker compose exec -T backend sh -c 'test -n "$PROVIDER_CREDENTIAL_MASTER_KEY" && echo configured || echo missing'
```

If CalDAV still reports `CONFIGURATION_REQUIRED`, the PostgreSQL credentials may
have been encrypted with a previous master key. Restore that original key when
possible. If it is permanently unavailable, the targeted recovery procedure in
the README removes only CalDAV's encrypted credential rows, after which discovery
re-encrypts the `.env` username and password with the current key. It preserves
calendar selections and unrelated application data. Do not use
`docker compose down -v` unless all local PostgreSQL data is disposable.

## Google Calendar

Google implements the optional `OAuthCalendarProvider` capability and emits the same normalized events as CalDAV. Installation configuration (OAuth client ID/secret, redirect URI, enabled flag, and encryption key) is server-side. User authorization, discovery, selection, synchronization, disconnect, and revoke are user-facing. The provider status DTO reports installation readiness separately from connection/authorization state and never returns environment values or tokens. See `docs/google-calendar.md`. Microsoft Graph is not implemented.
