# Wellness History

`RecommendationHistory` stores installation-local, meaningful recommendation snapshots in PostgreSQL. Fields include capture time, score, recommended interval, temperature, wind, humidity, AQI, UV, calendar availability, provider-neutral source names, and a short explanation. Descriptions, event details, attendees, credentials, and provider payloads are never stored.

A snapshot is inserted only when the selected time, availability, provider sources, explanation, or score by at least three points changes. `/history` shows today's best score/time, opportunities, calendar/weather conflicts, weekly averages, best/most-blocked days, missed opportunities, environmental averages, sources, significant before/current differences, and the persisted timeline.

This version is a single-installation history because authentication and multi-user tenancy are out of scope. Retention pruning and user-specific ownership are future work.
# Version 2.6 Activity Outcomes

Recommendation snapshots do not prove behavior. Version 2.6 separately records
explicit completed, partial, skipped, and dismissed actions; expired and unknown
remain distinct. The previous ambiguous “missed opportunities” concept is replaced
in new UI and analytics by Calendar blocked, Weather rejected, Opportunity expired,
User skipped, User dismissed, and Unknown outcome. See [Walk Tracking](walk-tracking.md)
and [Data Management](data-management.md).

## Version 2.7 outcomes

History preserves explicit completed, partially completed, skipped, dismissed, expired, and unknown meanings. Expiry is automated only for confidently linked eligible windows. Current history views remain bounded by their API periods; richer server-side multi-field filtering and pagination are planned follow-up work.
