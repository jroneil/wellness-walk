# Browser support

Wellness Window is a progressive web application. Its weather, calendar, history, and walk-tracking pages work in current Chrome, Edge, Firefox, and Safari releases, but browser capabilities differ.

| Capability | Chromium desktop/Android | Firefox | Safari/iOS |
| --- | --- | --- | --- |
| Install prompt controlled by the app | Yes, when install criteria are met | No; use browser install controls | No; use **Add to Home Screen** |
| Standalone installed display | Yes | Platform-dependent | Yes |
| Browser notifications | Yes | Yes | Supported for installed web apps on recent iOS/iPadOS |
| Notification action buttons | Feature-detected | Varies | Limited; normal click fallback |
| Offline application shell | Yes | Yes | Yes, subject to browser cache eviction |

The app requests notification permission only after a user action. Delivery is controlled by both browser and operating-system settings and is never guaranteed. If action buttons are unavailable, clicking a notification opens the dashboard. An action-capable notification passes only a validated action and an opaque recommendation reference; it does not contain calendar credentials or OAuth tokens.

The Install action appears only when the browser supplies `beforeinstallprompt` and the app is not already standalone. Other browsers should use their page or share menu. Dismissing the action suppresses it for the current browser session.

Offline mode serves the cached application shell and data already held by the open page. Weather and calendar information may be stale. Backend mutations—including starting, completing, skipping, dismissing, synchronizing, and deleting—are disabled or fail visibly until connectivity returns. The service worker never queues destructive operations and excludes API, OAuth callback, calendar, credential, and export traffic from caching.

