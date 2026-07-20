# Recommendation Engine

The recommendation engine is deterministic, explainable, and weather-only in the current version. It does not use calendar availability, operational workload, authentication, machine learning, or generated advice.

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
3. Prefer highest environmental score.
4. Apply preference tie-breakers.
5. Choose the earliest upcoming start time.

## Preference Influence

Preferences never change the environmental score. They affect selection and explanation only:

- selected duration controls the displayed end time
- preferred time, rain tolerance, and wind tolerance break ties
- cooler or warmer temperature preference may select a near-tie candidate when the score gap is at most five points and no serious safety warning is present
- minimum score adds a warning message when the best available period is below the threshold
- unit system changes frontend display only

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

## Future Integrations

Calendar availability and workload awareness are planned expansion points. They should be added as separate inputs to the recommendation engine without hiding or replacing the existing environmental score.
