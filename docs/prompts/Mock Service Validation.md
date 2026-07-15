Review the project for accidental dummy data.

The following are ALLOWED:

- MockWeatherService
- MockCalendarService
- MockOperationsService

provided they are isolated behind interfaces and clearly identified as mock implementations.

Everything else should be treated as temporary code.

Verify:

- Mock implementations are isolated.
- Business logic does not depend on hardcoded values.
- Controllers are unaware of mock implementations.
- Production services can replace mock services without changing controllers or the UI.

Report any violations.

Do not modify code.