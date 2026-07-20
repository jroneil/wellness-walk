I actually like this direction better. It takes what initially sounded like a "weather app" and turns it into something that demonstrates product thinking, architecture, and business awareness. A manager interviewing someone at CyberArk is more likely to remember that.

---

# Product Requirements Document (PRD)

# Wellness Window

### Intelligent Break Recommendation Platform

**Version:** 2.6

**Status:** Draft

---

## Version 2.6 — Recommendation Outcomes

Users may explicitly start, complete, partially complete, skip, dismiss, or
manually record a walk. Existing unclassified history remains unknown. Optional
weekly goals use user-chosen values and neutral language. Installation-wide data
controls provide retention, deletion, and secret-safe exports. PWA support improves
installability and offline explanation without promising background delivery.

The release adds no AI, medical targets, automatic detection, GPS, social ranking,
authentication, calendar writes, or remote notification service.

# Executive Summary

Wellness Window is an intelligent decision-support application that recommends the optimal time during the workday for employees to take a short restorative break.

Unlike traditional weather applications, Wellness Window combines environmental conditions, calendar availability, and operational workload to determine when a break can be taken with minimal business impact.

The platform demonstrates how multiple data sources can be combined into actionable recommendations using a transparent, explainable decision engine.

---

# Business Problem

Knowledge workers frequently remain at their desks for extended periods due to meetings, operational demands, and uncertainty about when stepping away is appropriate.

Existing weather applications provide environmental information but fail to answer the more important question:

> **"When is the best time for me to take a break without negatively impacting my work responsibilities?"**

Employees often check several applications before deciding:

* Weather
* Calendar
* Team workload
* Operational priorities

This creates unnecessary decision fatigue.

---

# Vision

Create an intelligent assistant that recommends the safest and healthiest time to step away from work while considering both employee wellness and business priorities.

---

# Objectives

* Improve employee wellness
* Reduce decision fatigue
* Increase productivity
* Minimize operational disruption
* Demonstrate explainable business decision-making
* Showcase modern software architecture

---

# Target Users

### Primary

* Software Engineers
* Security Engineers
* SOC Analysts
* IT Administrators
* Managers

### Secondary

* Remote Workers
* Hybrid Employees
* Office Staff

---

# User Stories

### Employee

As an employee, I want to know the best time today to take a short walk so I can improve my focus without missing important work.

---

### Security Engineer

As a security engineer, I want the application to avoid recommending breaks during critical operational events.

---

### Manager

As a manager, I want my team to take healthy breaks without reducing operational coverage.

---

### Remote Worker

As a remote worker, I want weather and work schedule combined into one recommendation.

---

# Functional Requirements

## Dashboard

Display

* Current recommendation
* Walk Score
* Recommended time window
* Weather summary
* Calendar summary
* Operational status
* Recommendation explanation

---

## Weather Module

Collect

* Temperature
* Rain probability
* Wind speed
* Humidity
* UV Index
* Air Quality (future)

---

## Calendar Module

Display

* Free/Busy schedule
* Available time blocks
* Minimum available walking window

Meeting details are never stored.

Version 2.2 supports browser-local Manual events and opt-in, read-only CalDAV availability. Only minimum necessary normalized fields are used, and credentials remain backend-side.

Version 2.3 discovers CalDAV principals, calendar homes, and event calendars; supports explicit selection of multiple calendars; and validates read-only synchronization against an isolated Radicale development server.

Version 2.4 durably stores non-secret provider configuration and selections, encrypts provider credentials at rest, and adds optional read-only Google Calendar OAuth. Manual events remain device-local; Google and CalDAV normalize into the same recommendation input.

---

## Operational Awareness Module

Support operational inputs such as:

* Critical alerts
* High-priority incidents
* Maintenance windows
* On-call status
* Focus time
* Quiet hours

The MVP will use mock operational data to demonstrate the concept.

---

## Recommendation Engine

Analyze:

* Weather
* Calendar
* Operational workload

Generate:

* Walk Score (0–100)
* Recommended time window
* Business justification
* Health justification

---

## Explainable Recommendations

Every recommendation must clearly explain why it was selected.

Example:

**Recommended Walk:** 3:15 PM – 3:45 PM

Walk Score: **91**

Reasoning:

* Comfortable temperature
* Low rain probability
* Calendar is free
* No critical operational alerts
* Moderate UV Index

---

# Non-Functional Requirements

* Responsive UI
* Fast response (<2 seconds)
* Mobile-friendly
* Modular architecture
* Secure API design
* Extensible recommendation engine
* Easy integration with additional data sources

---

# Business Rules

## Weather Rules

Preferred temperature:

60–78°F

Rain probability:

Less than 20%

Wind:

Less than 15 mph

---

## Calendar Rules

Minimum availability:

20 minutes

Preferred:

30 minutes

Avoid overlapping meetings.

---

## Operational Rules

Avoid recommendations when:

* Critical incident exists
* User is primary on-call
* Active maintenance window
* High operational workload

Lower score during:

* Multiple high-priority alerts
* Heavy meeting schedule

---

# Decision Engine

Each recommendation receives a weighted score.

| Factor           | Weight |
| ---------------- | -----: |
| Weather          |    30% |
| Calendar         |    25% |
| Operational Load |    30% |
| Health Factors   |    15% |

Highest score becomes the recommendation.

---

# Security Considerations

No meeting content is stored.

Only calendar availability is used.

Operational data is read-only.

Future integrations should support OAuth/OpenID Connect.

No personally sensitive health information is collected.

---

# AI Features (Future)

Generate natural-language explanations.

Example:

> "Conditions are excellent this afternoon. You have a 40-minute availability window with comfortable temperatures and no operational conflicts."

Provide personalized coaching.

Predict recurring wellness opportunities.

Learn preferred walking times.

---

# External Integrations

### MVP

* Mock Weather Service
* Mock Calendar Service
* Mock Operational Service

### Future

* OpenWeather API
* Microsoft Graph
* Google Calendar
* PagerDuty
* Splunk
* Microsoft Sentinel
* ServiceNow

---

# Future Enhancements

Walking history

Break reminders

Fitness tracker integration

Team wellness dashboard

AI personalization

Mobile notifications

Desktop widget

Wearable integration

---

# Technical Architecture

Frontend

* Next.js
* TypeScript
* Tailwind CSS

Backend

* Spring Boot REST API

Data Layer

* None active in the current vertical slice
* PostgreSQL can be added later when durable persistence is required

Services

* Weather Service
* Calendar Service
* Operational Service
* Recommendation Engine

---

# Risks

Weather API availability

Calendar permissions

False recommendations due to incomplete operational data

Balancing employee wellness with operational requirements

---

# Success Metrics

* Recommendation generated in under 2 seconds
* High user satisfaction with recommendation quality
* Increased employee break compliance
* Demonstrates integration of multiple enterprise data sources
* Clean, maintainable architecture suitable for future expansion

---

## Why This Version Stands Out

If this really is a manager's test, this version shows a level of thinking beyond "I can build a weather app." It reframes the request into an enterprise-style decision support system:

* It combines multiple data sources instead of relying on a single API.
* It uses transparent business rules with explainable recommendations.
* It introduces security operations awareness, which is directly relevant to a company like CyberArk.
* It is designed with extensibility in mind, making it easy to replace the mock operational data with real enterprise integrations later.

That demonstrates not just coding ability, but product design, architectural thinking, and an understanding of how to build software that aligns with business needs.

## Version 2.5 Requirements

Version 2.5 makes recommendations proactive while preserving deterministic selection. Meaningful recommendation changes are retained as provider-neutral wellness history. Users can view daily and weekly summaries, conflicts, missed opportunities, change explanations, and hourly opportunity timelines. Optional browser notifications are delivered only for available, conflict-free recommendations above the configured threshold and within notification policy. External calendar access remains read-only; no AI, authentication, email, SMS, or push provider is introduced.

## Version 2.7 completion release

Version 2.7 exposes start, skip, dismiss, cancel, and completion interactions without changing deterministic weather or availability selection. Completion feedback remains non-medical. Eligible past opportunities are classified as expired by a bounded, idempotent scheduled job; calendar conflicts and weather-rejected windows are not. Destructive history actions explain their scope and preserve provider configuration, credentials, goals, and settings.
