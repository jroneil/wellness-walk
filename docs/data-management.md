# Data Management

`/settings/data` manages installation-wide goals, retention, and exports. Wellness
Window has no authentication or per-user ownership, so anyone with access to this
installation can access these controls.

Supported retention periods are 30, 90, 180, and 365 days, or indefinite. Walk
activities remain indefinite by default. A scheduled, transactional cleanup removes
eligible rows and logs counts without generating notifications. Provider settings,
calendar selections, encrypted credentials, and OAuth tokens are outside all
history deletion boundaries.

CSV exports use UTF-8, stable columns, quoting, and escaped quotes. Activity,
recommendation, and daily-summary exports are separate. The complete JSON export
includes schema version `2.6`, export timestamp, timezone, activities,
recommendations, goal settings, and retention settings. It excludes passwords,
tokens, ciphertext, credential references, nonces, and encryption metadata.

Destructive operations must be confirmed by the frontend and should state the
exact date range or category. Deleting all Wellness Window history does not remove
calendar-provider configuration. Database administrators should back up retained
data before production deletion or master-key migration.

## Confirmation boundaries

`/settings/data` provides separate confirmation dialogs for recommendation date ranges, notification history, all walk activity, and all Wellness Window history. Each is irreversible and installation-wide. Delete-all requires the exact phrase `DELETE HISTORY`. These operations preserve calendar connections, encrypted provider credentials, provider selections, OAuth configuration, goals, retention settings, and application preferences. Exports remain `no-store` and exclude secrets.
