# Recommendation Engine

The recommendation engine is deterministic and explainable. Version 2.4 evaluates weather, normalized Manual/CalDAV/Google availability, and explicit preferences. Persistence, OAuth, token refresh, discovery, and provider errors remain outside scoring.

## Scoring Algorithm

Each scorable hourly period receives a 0-100 score. The score is the sum of visible category scores:

| Category | Max |
| --- | ---: |
| Feels-like temperature | 30 |
| Precipitation probability | 20 |
| Wind speed | 10 |
| Humidity | 10 |
| Daylight | 10 |
| Air Quality Index | 10 |
| UV Index | 10 |

There are no hidden penalties. Missing optional values score zero for their category and are explained.

## Temperature

The engine scores the feels-like temperature:

- `60-72°F`: 30
- `55-59°F` or `73-78°F`: 26
- `79-84°F`: 18
- `45-54°F`: 15
- `85-90°F`: 8
- below `45°F` or above `90°F`: 0

Feels-like method:

- Heat Index is used when temperature is at least `80°F` and humidity is at least `40%`.
- Wind Chill is used when temperature is at most `50°F` and wind is at least `3 mph`.
- Actual temperature is used outside those NOAA formula ranges.

## Precipitation

- `0-10%`: 20
- `11-25%`: 16
- `26-50%`: 8
- above `50%`: 0
- unavailable: 0

## Wind

- `0-10 mph`: 10
- `11-15 mph`: 7
- `16-20 mph`: 3
- above `20 mph`: 0
- unavailable: 0

## Humidity

- `30-60%`: 10
- below `30%` or `61-75%`: 7
- `76-85%`: 3
- above `85%`: 0
- unavailable: 0

## Daylight

Sunrise and sunset come from Open-Meteo when available:

- daylight: 10
- civil twilight: 5
- night: 2
- unavailable: 0

## AQI

- `0-50`: 10
- `51-100`: 7
- `101-150`: 3
- above `150`: 0
- unavailable: 0

## UV Index

- `0-2`: 10
- `3-5`: 7
- `6-7`: 3
- `8+`: 0
- unavailable: 0

## Ratings

- `90-100`: Excellent
- `75-89`: Great
- `60-74`: Good
- `40-59`: Fair
- `20-39`: Poor
- `0-19`: Not Recommended

## Tie Breaking

Best-window selection:

1. Exclude past periods.
2. Exclude periods without a scorable temperature.
3. Prefer the highest overall wellness score.
4. Prefer the highest weather score.
5. Prefer the highest preference score.
6. Choose the earliest upcoming start time.

## Candidate Generation and Availability

Candidate starts are generated every `recommendation.engine.candidate-interval-minutes` (15 by default; 30 is supported). Windows overlap and use the requested walk duration. A window crossing forecast hours uses the minimum weather score of all covered hours so the explanation never overstates conditions.

Availability analysis:

1. Score and rank upcoming weather periods exactly as in Version 1.
2. Create a duration-aware walk window for each candidate and remove any window overlapping a busy calendar event.

Busy events are normalized and overlapping or adjacent spans are merged. This covers fragmented schedules, adjacent meetings, and all-day events. An overlap exists when `event.startTime < walk.endTime` and `event.endTime > walk.startTime`. Free events never block, and the full configured duration must fit.

## Overall Wellness Score

Default visible weights are weather 72%, availability 20%, and preferences 8%. Each component is normalized to 0-100 and the weighted result is rounded to an integer. Availability is also a hard constraint: unavailable candidates cannot win regardless of score.

## Preference Influence

Preferences never change the environmental score. They produce a separate visible 0-100 contribution:

- selected duration controls the displayed end time
- preferred time, rain tolerance, and wind tolerance break ties
- cooler or warmer temperature preference may select a near-tie candidate when the score gap is at most five points and no serious safety warning is present
- minimum score adds a warning message when the best available period is below the threshold
- unit system changes frontend display only
- preferred time supports morning, lunch, afternoon, and evening

## Configuration

`RecommendationEngineProperties` binds candidate interval, default duration, minimum score, and the three scoring weights from `recommendation.engine.*`. Environment-variable overrides are documented in `application.properties`.

## Missing Data Handling

- Missing temperature makes an hour not scorable.
- Missing precipitation, wind, humidity, daylight, AQI, or UV awards zero points for that category.
- Missing values are shown as unavailable and explained in the recommendation.
- The system does not fabricate environmental values.

## Explainability

Every recommendation includes:

- category scores
- reasons for awarded points
- warnings for environmental risks
- preference reasons when preferences influenced the selected window
- availability and the selection reason
- the ideal-weather conflict when calendar filtering changes the result
- the normalized conflict source (`MANUAL`, `CALDAV`, or `GOOGLE`) without private provider payloads

## Future Integrations

Microsoft Graph and workload awareness remain future integrations. Providers register behind `CalendarProvider`; synchronization produces normalized events and cannot hide or replace the environmental score.

## Version 2.5 Consumers

History and notifications are downstream consumers of the selected recommendation. They never rescore hours or alter selection. History stores meaningful changes when score, selected time, availability, sources, or explanation changes. The opportunity timeline uses existing hourly `WalkingRecommendation` scores plus normalized busy events. Notification eligibility checks the selected window's existing score and availability against user policy.

## Version 2.6 Outcomes

Walk activity, goals, and opportunity outcomes remain downstream consumers. They
do not change weather scores, availability filtering, candidate ranking, or
explanations. Completion is explicit user input and is never inferred from a
recommendation snapshot or browser activity.

## Outcome classification

`SKIPPED` means the user explicitly declined an eligible opportunity. `DISMISSED` means the suggestion was removed without claiming whether a walk occurred. `EXPIRED` is assigned only after an eligible, available window ends with no explicit outcome or linked active/completed/partially completed activity. A calendar conflict, a weather-rejected window, and an unknown historical record are distinct and are never inferred as expired. Scoring and tie-breaking are unchanged.
