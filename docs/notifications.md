# Browser Notifications

Version 2.5 uses the browser Notification API only. Users explicitly request permission from `/settings/notifications`. Unsupported and denied states are explained without repeated prompts.

After a completed dashboard recommendation, the frontend sends the already-selected snapshot and browser-local policy state to `NotificationService`. Eligibility requires notifications enabled, browser permission granted, sufficient score, calendar availability, no selected-window conflict, permitted day/time, remaining daily capacity, and expired cooldown. The backend returns an explanatory decision and schedule time. The frontend cancels the previous timer before scheduling a replacement, preventing obsolete reminders when weather or calendars change.

Delivery uses a stable notification tag and a recommendation fingerprint to prevent duplicates. Clicking focuses the window and opens the dashboard. Daily count, last delivery, and fingerprint are browser-local. No continuous polling, service worker, remote push service, email, SMS, provider token, or notification server is used. Closing the browser cancels pending timers; this is an intentional limitation of the initial browser-only release.

Version 2.6 progressively registers a narrow service worker for notification-click
routing and PWA operation. Page timers remain the scheduling fallback and delivery
is not guaranteed when the browser is closed. Unsupported background APIs are not
required. There is still no remote push server, external delivery service, or
duplicate recommendation scoring. See [PWA](pwa.md).

## Browser actions and permission states

The settings UI distinguishes not requested, allowed, blocked, and unsupported permission states and requests permission only after a user gesture. Where supported, service-worker notifications offer **Start walk** and **Dismiss**. The click handler validates the action and opaque recommendation reference, then opens the dashboard with an intent; it does not mutate history from the worker. Unsupported browsers fall back to an ordinary dashboard click. Browser/OS suppression and background-delivery limits still apply.
