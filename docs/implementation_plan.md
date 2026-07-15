# Implementation Plan

## Wellness Window – Intelligent Break Recommendation Platform

**Version:** 1.0

**Objective**

Build an enterprise-quality decision support application that recommends the optimal time for a user to take a restorative break by combining weather, calendar availability, and operational workload.

The implementation follows an incremental, test-driven approach where every phase results in a working application.

---

# Development Principles

* Implement one feature at a time.
* Maintain a runnable application after every phase.
* Use mock data before external integrations.
* Document architectural decisions.
* Test each phase before proceeding.
* Keep components loosely coupled.
* Follow clean architecture principles.

---

# Existing Project Assumption

This implementation plan assumes the repository already contains:

```text
Spring Boot 4.1 backend
Next.js 16 frontend
docs/
    PRD.md
    implementation_plan.md
```

OpenCode must inspect and reuse the existing structure. It must not initialize replacement projects or create duplicate frontend, backend, or documentation directories.

# OpenCode Execution Rules

For every phase:

* Read `docs/PRD.md` and `docs/implementation_plan.md` first.
* Inspect existing code before creating or modifying files.
* Implement only the requested phase.
* Do not continue into later phases without explicit instruction.
* Prefer small, reviewable changes.
* Do not add dummy data unless the active phase explicitly requires mock data.
* Do not replace working code merely to impose a preferred structure.
* Run the relevant build and tests before finishing.
* Summarize files changed, validation performed, and any unresolved issues.

# Technology Stack

## Frontend

* Next.js
* TypeScript
* Tailwind CSS

## Backend

* Spring Boot
* REST API

## Database

* PostgreSQL (future)
* Mock JSON for MVP

---

# Phase 1 – Existing Project Verification

## Goal

Verify the existing project foundation and document the current baseline before feature development begins.

The Spring Boot 4.1 backend, Next.js 16 frontend, and documentation folder already exist. OpenCode must use the current structure and must not recreate, rename, relocate, or replace the projects unless explicitly instructed.

### Tasks

* Inspect the existing repository structure
* Identify the existing frontend, backend, and documentation locations
* Read the PRD and implementation plan before making changes
* Verify the Spring Boot backend builds and starts
* Verify the Next.js frontend installs, builds, and starts
* Confirm the existing Java, Node.js, package manager, and build-tool versions
* Review existing linting and formatting configuration
* Review existing environment variable handling
* Review the current Git status
* Document any setup problems without restructuring the repository
* Make only the minimum changes needed to establish a working baseline

### Constraints

* Do not create a new Spring Boot project
* Do not create a new Next.js project
* Do not create another docs folder
* Do not replace existing configuration files without first reviewing them
* Do not move existing files or folders
* Do not introduce application features during this phase
* Do not add dummy data during this phase

### Deliverables

* Verified existing frontend
* Verified existing backend
* Verified documentation folder
* Updated README only if current setup instructions are missing or inaccurate
* Baseline verification notes recorded in the documentation

### Exit Criteria

* Backend builds successfully
* Backend starts successfully
* Frontend builds successfully
* Frontend starts successfully
* Existing structure remains intact
* Any required baseline fixes are documented
* Repository is ready for Phase 2

---

# Phase 2 – Architecture Review and Completion

## Goal

Review the existing application structure and add only the missing architecture needed for the application.

### Backend

Create modules

```text
Controller
Service
Recommendation Engine
DTO
Model
Configuration
Mock Repository
```

### Frontend

Create folders

```text
components/
pages/
services/
hooks/
types/
layouts/
```

### Constraints

* Reuse the current project and folder structure
* Do not recreate folders that already exist
* Do not reorganize working code solely to match this document
* Add only missing packages, folders, and architectural boundaries
* Preserve framework conventions used by Spring Boot 4.1 and Next.js 16
* Keep the application runnable after every change

### Deliverables

Existing project structure reviewed and missing architectural elements added

### Exit Criteria

* Required architectural areas exist
* Existing structure remains intact
* Backend and frontend still build and run

---

# Phase 3 – Domain Models

## Goal

Create application models.

### Weather

```text
Temperature
Rain Chance
Wind Speed
Humidity
UV
```

### Calendar

```text
Time Block

Status

Duration
```

### Operational Status

```text
Critical Alerts

High Alerts

Maintenance Window

On Call

Focus Time
```

### Recommendation

```text
Score

Rating

Reasons

Recommended Time

Duration
```

### Exit Criteria

Models compile successfully

---

# Phase 4 – Mock Data Services

## Goal

Remove all hardcoded UI values.

Create services

```text
WeatherService

CalendarService

OperationsService
```

Each service returns realistic mock data.

### Deliverables

Weather JSON

Calendar JSON

Operational JSON

### Exit Criteria

Dashboard populated entirely from services.

---

# Phase 5 – Recommendation Engine

## Goal

Build the application's core business logic.

Create

```text
RecommendationEngine
```

Responsibilities

* Gather all inputs
* Score every available time window
* Select best recommendation
* Generate explanation

Scoring

| Category   | Weight |
| ---------- | -----: |
| Weather    |     30 |
| Calendar   |     25 |
| Operations |     30 |
| Health     |     15 |

### Business Rules

Ideal temperature

60–78°F

Rain

Less than 20%

Wind

Less than 15 MPH

Minimum walk

20 minutes

Critical alerts

Immediate rejection

Primary on-call

Heavy penalty

### Exit Criteria

Recommendation generated successfully

---

# Phase 6 – REST API

## Goal

Expose recommendation services.

Endpoints

```text
GET /weather

GET /calendar

GET /operations

GET /recommendation
```

### Exit Criteria

Endpoints return JSON

---

# Phase 7 – Dashboard UI

## Goal

Create the main application screen.

Sections

```text
Header

Recommendation Card

Walk Score

Reasoning Panel

Weather Summary

Calendar Timeline

Operational Status

Footer
```

### Exit Criteria

Responsive dashboard complete

---

# Phase 8 – Recommendation Details

## Goal

Explain every recommendation.

Display

```text
Walk Score

Positive Factors

Negative Factors

Recommendation

Suggested Time

Estimated Duration
```

Example

```text
Walk Score: 91

✔ Comfortable temperature

✔ Calendar available

✔ Low operational activity

✔ No rain

✔ Low wind
```

### Exit Criteria

User understands why recommendation exists

---

# Phase 9 – Timeline Visualization

## Goal

Visualize the workday.

Timeline

```text
8 AM --------------------6 PM

Weather

Calendar

Operational Load

Recommendation
```

Highlight

Green

Yellow

Red

Best window

### Exit Criteria

Timeline accurately reflects recommendation

---

# Phase 10 – Configuration

Move business rules into configuration.

Example

```text
Ideal Temperature

Maximum Rain

Maximum Wind

Minimum Walk Length

Maximum Alert Count
```

### Exit Criteria

Rules configurable without code changes

---

# Phase 11 – Testing

Unit Tests

Recommendation Engine

Weather Service

Calendar Service

Operations Service

Integration Tests

REST endpoints

UI Tests

Recommendation display

Dashboard rendering

### Exit Criteria

All tests passing

---

# Phase 12 – Polish

Improve

Loading indicators

Error handling

Animations

Responsive design

Accessibility

Dark mode

Refresh button

### Exit Criteria

Production-quality interface

---

# Phase 13 – Replace Mock Services

Replace

Mock Weather

↓

Weather API

Mock Calendar

↓

Microsoft Graph

Google Calendar

Mock Operations

↓

Splunk

PagerDuty

Sentinel

ServiceNow

### Exit Criteria

Real integrations functional

---

# Phase 14 – AI Features (Optional)

Generate

Natural language recommendation

Health coaching

Daily summaries

Historical trends

Personal insights

Example

> "Today between 2:30 PM and 3:00 PM offers the best opportunity for a short walk. Weather is ideal, your calendar is free, and operational workload is low."

---

# Suggested Git Commits

```text
Initial project setup

Create project architecture

Add domain models

Implement mock services

Build recommendation engine

Create REST API

Build dashboard

Add recommendation details

Add timeline visualization

Implement configuration

Add testing

UI polish

Replace mock APIs

AI enhancements
```

---

# Definition of Done

The application is complete when:

* The dashboard loads successfully.
* Weather, calendar, and operational data are displayed.
* A recommendation is generated using business rules.
* Every recommendation includes an explanation.
* The application is responsive and user-friendly.
* Mock services can be replaced with real integrations without changing the UI or recommendation engine.
* The project includes a complete set of documentation (PRD, Architecture, Implementation Plan, README) and can be demonstrated end-to-end.

This implementation plan also gives your friend a strong story during a review: it shows they approached the assignment like an engineering project—with planning, modular design, incremental delivery, and clear separation of concerns—rather than simply writing code until it worked.