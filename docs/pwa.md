# Progressive Web App

Version 2.6 supplies a manifest, Wellness Window icon, standalone metadata, service
worker registration, and an offline fallback. Installation support varies by
browser and operating system.

The service worker caches a small versioned application shell and same-origin
static assets. Navigations are network-first and fall back to `/offline`. API
requests, OAuth paths, mutations, calendar payloads, and history exports are never
cached. Activation deletes old Wellness Window caches.

Notification clicks focus an existing window or open the dashboard when the
service-worker API is available. Page-based timers remain the fallback. There is
no push server or external notification service, and background delivery is not
guaranteed. Periodic/background sync is not required and is not used when the
browser does not support an appropriate safe workflow. The backend remains the
source of truth for eligibility; the service worker does not duplicate scoring.

## Installation, updates, and cache versioning

The manifest supplies branded 192×192, 512×512, and maskable 512×512 PNG icons, `/` start URL/scope, standalone display, and theme/background colors. A restrained global Install action appears only when the browser provides `beforeinstallprompt`; standalone mode hides it. A waiting worker produces an **Update available** action so reload is intentional.

`public/sw.js` uses a named versioned cache. Activation deletes older Wellness Window caches. Navigation is network-first with an offline page fallback; static same-origin assets are cached. `/api/`, calendar/credential/export traffic, and OAuth callbacks are never served from the cache. Offline pages may display stale in-memory data, disable backend mutations, and never queue destructive work.
