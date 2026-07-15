Act as a senior software engineer performing a production readiness review.

Review the entire project for any placeholder, sample, or temporary implementation.

Search for:

- Dummy data
- Hardcoded business data
- Sample JSON
- Mock users
- Mock passwords
- Fake IDs
- Lorem Ipsum
- TODO implementations
- Stub methods
- Placeholder return values
- Randomly generated data
- Fake API responses
- Temporary configuration
- Test endpoints exposed to production
- Hardcoded URLs
- Hardcoded API keys
- Hardcoded credentials
- Console logging used for debugging
- Commented-out code

Also identify any code that would prevent replacing mock services with production integrations.

For every issue found provide:

- File
- Line or method
- Issue
- Severity (Critical, High, Medium, Low)
- Recommended fix

Do not modify any code.

Produce a Production Readiness Report.

If no issues are found, explicitly state:

"The application contains no obvious dummy data or temporary production blockers."