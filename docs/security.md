# Security

## Credential storage

`ProviderCredentialStore` is the only supported boundary for CalDAV passwords, OAuth access/refresh tokens, and temporary PKCE verifiers. The local implementation uses AES-256-GCM with a random 96-bit nonce per write. Authenticated additional data binds ciphertext to connection ID, credential name, and version, so moving or modifying a value fails authentication.

`PROVIDER_CREDENTIAL_MASTER_KEY` must be a Base64-encoded 32-byte runtime secret. It is never generated silently, persisted in PostgreSQL, returned through APIs, or committed. Losing or changing it makes existing encrypted credentials unreadable; recover by disconnecting/reconnecting providers or restoring the correct key. Rotation currently occurs per credential value, not for the application master key.

## OAuth protection

- high-entropy state is stored as SHA-256 only;
- state expires, is consumed once, and is locked during callback processing;
- PKCE S256 binds authorization start and code exchange;
- callback and frontend result URLs are fixed allowlisted configuration;
- only calendar-list/events read-only scopes are accepted;
- tokens remain backend-side and never enter cookies or browser storage.

Logs contain provider type, opaque calendar IDs, counts, timing, and sanitized categories only. They must not contain tokens, authorization headers, raw provider payloads, event descriptions, attendee data, or credential-bearing URLs.

## Threat model and limitations

Encryption protects database backups and direct database disclosure when the runtime master key remains separate. It does not protect against compromise of the running backend process, container host, JVM memory, environment, or an administrator with both database and master-key access. The Compose PostgreSQL password is a development default and must be replaced outside local development. This version is a single-installation connection model, not multi-user authentication or tenant isolation.

The backend exposes only application controllers; Actuator is not enabled. CORS remains explicitly allowlisted. OAuth callback state provides CSRF/replay protection, but deploying behind a proxy requires preserving the configured public HTTPS redirect exactly.
