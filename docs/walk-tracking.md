# Walk Tracking

Version 2.6 records user-confirmed behavior without inferring a walk from browser
activity. A recommendation can start one installation-wide active session. The
backend stores the start timestamp; the browser displays elapsed time without
writing timer ticks. Refreshing or navigating recovers the active session from
`GET /api/walks/active`.

Activity statuses are `ACTIVE`, `COMPLETED`, `PARTIALLY_COMPLETED`, `SKIPPED`,
`DISMISSED`, `EXPIRED`, and `UNKNOWN`. Completed and partial activities represent
explicit user input. Skipped means the user explicitly declined an opportunity;
dismissed means they closed it without saying whether they walked; expired means
the window ended without an action; unknown covers Version 2.5 history or data
that cannot be classified. Unknown is never counted as skipped or completed.

Sources identify where the action began: Dashboard, Notification, History,
Timeline, or Manual Entry. Manual walks require a past completion time, positive
duration up to the configured maximum (480 minutes by default), optional quality,
and notes up to 1,000 characters. Notes are rendered as text, never HTML.

Cancelling an active timer removes the unfinished session and does not claim an
outcome. This release does not detect activity automatically, collect health data,
calculate calories, track GPS, or write to calendars.

## Interaction states and feedback

Future eligible recommendations expose Start, Skip, and Dismiss. An active walk exposes Complete and Cancel. Completion collects completed/partially completed status, a positive duration up to 480 minutes, optional restorative/good/neutral/uncomfortable quality, and optional notes up to 1,000 characters. Failed saves retain entered values. Skip and dismiss never count as completion.
