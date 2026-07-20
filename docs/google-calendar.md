# Google Calendar

Google Calendar is an optional, read-only provider. Installation configuration belongs to the backend administrator; account authorization belongs to the calendar user. Client secrets and the credential-encryption key must never be entered in the Calendar page or stored in browser storage.

## First-time Google Cloud setup

This walkthrough creates a Google project for local development. You do not need a
service account, API key, public website, or published Google app. Wellness Window
uses an OAuth **Web application** client because its Spring Boot backend receives
Google's sign-in response.

Google occasionally adjusts the console layout. The current area is named
**Google Auth Platform** and contains **Branding**, **Audience**, **Data Access**,
and **Clients** pages.

### Before you begin

You need:

- A Google account with a calendar.
- Wellness Window running locally on ports 3000 and 9090.
- Permission to create a project in [Google Cloud Console](https://console.cloud.google.com/).

Keep this callback address available to copy later:

```text
http://localhost:9090/api/calendar/providers/GOOGLE/oauth/callback
```

It must be copied exactly, with no trailing slash.

### Step 1: Create a Google Cloud project

1. Open [Google Cloud Console](https://console.cloud.google.com/).
2. Sign in, then select the project menu at the top of the page.
3. Select **New Project**.
4. Enter `Wellness Window Local` as the project name.
5. Select **Create**.
6. Make sure the new project is selected before continuing. The selected project
   name appears in the top bar.

The project is only a container for the API and OAuth settings. Creating it does
not give Wellness Window access to your calendar.

### Step 2: Enable the Google Calendar API

1. Open the navigation menu and go to **APIs & Services → Library**.
2. Search for `Google Calendar API`.
3. Open **Google Calendar API** and select **Enable**.

If the page shows **Manage** instead of **Enable**, the API is already enabled.
Enabling the API allows the project to make Calendar API requests after a user
grants permission; it does not grant permission by itself.

### Step 3: Set up the consent screen

The consent screen is the page Google shows when you select **Connect Google
Calendar** in Wellness Window.

1. Open **Google Auth Platform → Branding**.
2. If Google says the Auth Platform is not configured, select **Get Started**.
3. Enter these beginner-friendly local values:

   | Field | Suggested value |
   | --- | --- |
   | App name | `Wellness Window Local` |
   | User support email | Your Google email address |
   | Audience | **External** for a personal Gmail account; **Internal** only if your Google Workspace organization permits it |
   | Contact email | Your Google email address |

4. Accept Google's user-data policy if you agree, then finish creating the app.
5. Leave the app in **Testing** for local development. You do not need to publish it.

If you chose **External**, open **Google Auth Platform → Audience**. Under **Test
users**, select **Add users**, add the same Google account you will use for testing,
and save it. If this step is skipped, Google may show an access-blocked message.

### Step 4: Add the read-only Calendar permissions

1. Open **Google Auth Platform → Data Access**.
2. Select **Add or Remove Scopes**.
3. Add these two scopes:

   ```text
   https://www.googleapis.com/auth/calendar.calendarlist.readonly
   https://www.googleapis.com/auth/calendar.events.readonly
   ```

4. Select **Update** or **Save**.

These permissions let Wellness Window list calendars and read event times for
availability. They do not allow the application to create, edit, or delete Google
Calendar events.

### Step 5: Create the OAuth client

1. Open **Google Auth Platform → Clients**.
2. Select **Create Client**.
3. For **Application type**, choose **Web application**.
4. Name it `Wellness Window Local Backend`.
5. Under **Authorized redirect URIs**, select **Add URI** and paste:

   ```text
   http://localhost:9090/api/calendar/providers/GOOGLE/oauth/callback
   ```

6. Leave **Authorized JavaScript origins** empty. The browser does not call Google
   APIs directly; the backend handles the OAuth callback.
7. Select **Create**.
8. Copy the displayed **Client ID** and **Client secret** somewhere temporary. Do
   not commit or share either value.

The redirect URI is the most common setup mistake. `http` versus `https`, the
hostname, port, capitalization, path, and trailing slash must match exactly.

### Step 6: Add the values to Wellness Window

From the repository root, create `.env` if it does not already exist:

```bash
cp .env.example .env
```

Do not run that command if you already have a configured `.env`, because copying
the example would replace your settings. Add or update these lines instead:

```dotenv
GOOGLE_CALENDAR_ENABLED=true
GOOGLE_CALENDAR_CLIENT_ID=<paste-client-id>
GOOGLE_CALENDAR_CLIENT_SECRET=<paste-client-secret>
GOOGLE_CALENDAR_REDIRECT_URI=http://localhost:9090/api/calendar/providers/GOOGLE/oauth/callback
PROVIDER_CREDENTIAL_MASTER_KEY=<paste-existing-or-new-master-key>
```

If this is a new installation and the master key is still empty, generate one:

```bash
openssl rand -base64 32
```

Paste the complete output after `PROVIDER_CREDENTIAL_MASTER_KEY=`. Keep this value
stable. Changing it later makes previously encrypted provider credentials and
tokens unreadable. See the [Troubleshooting Guide](troubleshooting.md#3-provider-credential-master-key).

### Step 7: Apply the configuration

Rebuild and replace the backend so it receives the new `.env` values:

```bash
docker compose up -d --build --force-recreate backend
docker compose logs --tail=100 backend
```

`docker compose restart backend` is not enough because restarting an existing
container does not reload its environment.

### Step 8: Connect your calendar

1. Open `http://localhost:3000/calendar`.
2. Find **Google Calendar** and select **Connect Google Calendar**.
3. Choose the Google account you added as a test user.
4. Review the read-only permissions and select **Allow**.
5. After returning to Wellness Window, select **Discover calendars**.
6. Select the calendars that should block unavailable walking times and save them.
7. Select **Synchronize now**.

Setup is complete when the provider shows **Connected**, your calendars appear,
and synchronization finishes successfully. If Google reports an unverified app
during local testing, confirm that the app is in Testing and your account is listed
as a test user. Do not bypass a warning for an app or OAuth client you did not
create.

Never commit `.env`, OAuth credentials, or the master key. Production installations
should use Docker secrets, mounted secret files, or an approved server-side secret
manager. Google's official references are [Configure OAuth consent](https://developers.google.com/workspace/guides/configure-oauth-consent)
and [Create OAuth credentials](https://developers.google.com/workspace/guides/create-credentials).

## Authorization and security

The backend callback is implemented at `GET /api/calendar/providers/GOOGLE/oauth/callback`, exactly matching the documented redirect URI. The frontend requests authorization start; the backend creates a 256-bit state value and PKCE S256 verifier, persists only the state hash, expires it after ten minutes, and consumes it once under a database lock. Redirect destinations are fixed server configuration and cannot be supplied by the browser.

Google returns the authorization code to the backend. The backend validates state, exchanges the code, encrypts access and refresh tokens with AES-256-GCM, and redirects to the fixed Calendar result URL. Callback replay and invalid state fail safely. The Calendar page displays a safe result, refreshes provider state, and removes the callback query from the visible URL. Tokens, codes, state, client secrets, and raw Google errors never enter localStorage, sessionStorage, URLs controlled by users, or provider APIs.

Requested scopes are limited to:

```text
https://www.googleapis.com/auth/calendar.calendarlist.readonly
https://www.googleapis.com/auth/calendar.events.readonly
```

The request uses offline access and explicit consent so Google can issue a refresh token. Disconnect deletes local credentials. Revoke calls Google's revocation endpoint best-effort and then deletes local credentials. Neither action modifies Google events.

## Provider workflow

When installation values are absent, status is `DISABLED` and the UI shows setup instructions only. Once installation configuration is complete, status is ready to connect. After consent, users can discover calendars, see primary/read-only indicators, persist selections, synchronize, disconnect, or use the Advanced disclosure to revoke access. Expired or permanently rejected credentials produce `AUTHORIZATION_REQUIRED` and a reconnect action.

Calendar identifiers exposed by the API are opaque hashes. Event normalization keeps only availability fields; descriptions, attendees, conferencing, reminders, and raw provider payloads are discarded.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Google remains `DISABLED` | `GOOGLE_CALENDAR_ENABLED` must be `true`; client ID, client secret, redirect URI, and master key must be present; restart the backend. |
| `redirect_uri_mismatch` | Google Cloud must contain the exact URI above. Scheme, hostname, port, path, case, and trailing slash must match. |
| Access blocked or test-user error | If the consent screen is in Testing, add the Google account as a test user where required. |
| Invalid client | Verify the client ID and secret and confirm both belong to the selected Cloud project. |
| Authorization succeeds but no calendars appear | Enable the Calendar API, grant the read-only scopes, retry discovery, and inspect sanitized backend logs. |
| No refresh token | Confirm offline access and consent behavior. Reconnect if prior consent prevents a refresh token; never paste tokens manually. |

Automated tests use mocked Google endpoints and require no credentials. Live authorization must be validated separately with administrator-supplied credentials. There is no write access, service-account delegation, multi-user authentication, webhook, or incremental sync support.
