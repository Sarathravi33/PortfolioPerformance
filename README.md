# Portfolio Performance API

A backend REST API that calculates the daily return for a portfolio, validates inputs against business rules, and returns a status decision of `VALID`, `REVIEW_REQUIRED`, or `INVALID_INPUT`.

---

## Approach

### Architecture

The implementation follows a standard Spring Boot layered structure:

```
src/main/java/com/portfolio/performance/
├── controller/
│   └── PerformanceController.java      ← REST endpoint, HTTP status mapping
├── service/
│   ├── DailyReturnService.java         ← Orchestrates validation → calculation → review
│   └── CalculationReviewer.java        ← Custom review agent (REVIEW_REQUIRED logic)
├── validator/
│   └── DailyReturnValidator.java       ← INVALID_INPUT business rule enforcement
├── repository/
│   └── ValuationAuditRepository.java   ← JPA repository; persists each request to H2
├── model/
│   └── ValuationAudit.java             ← JPA entity (audit record)
├── dto/
│   ├── DailyReturnRequest.java         ← Inbound payload
│   └── DailyReturnResponse.java        ← Outbound response
└── PortfolioPerformanceApplication.java

src/test/java/com/portfolio/performance/
├── service/DailyReturnServiceTest.java
├── validator/DailyReturnValidatorTest.java
└── controller/PerformanceControllerIntegrationTest.java
```

### Custom Agent — Calculation Reviewer

`CalculationReviewer` is the project's reusable review agent. It is a stateless Spring `@Component` responsible solely for applying tolerance thresholds after a valid calculation completes:

- Flags `REVIEW_REQUIRED` when the absolute deviation between portfolio return and benchmark return exceeds **5%**.
- Flags `REVIEW_REQUIRED` when the absolute net cash flow exceeds **20%** of begin market value.
- Returns human-readable reason strings that include the actual computed values so reviewers understand exactly what triggered the flag.

This component is deliberately separated from `DailyReturnService` so its rules can be updated or tested independently without touching the calculation formula.

### Request Flow

```
POST /api/performance/daily-return
         │
         ▼
PerformanceController     — structural/Bean Validation checks → 400
         │
         ▼
DailyReturnValidator      — business INVALID_INPUT rules     → 422
         │
         ▼
DailyReturnService        — formula → portfolioReturnPct, excessReturnPct
         │
         ▼
CalculationReviewer       — tolerance checks → VALID or REVIEW_REQUIRED → 200
         │
         ▼
ValuationAuditRepository  — persist audit record to H2
```

### Tech Stack

| Layer | Choice |
|-------|--------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.5 |
| Build | Maven |
| Database | H2 in-memory (audit log) |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation (`spring-boot-starter-validation`) |
| Testing | JUnit 5, Mockito, MockMvc, Spring Boot Test |

---

## Assumptions

1. **`portfolioReturnPct` spelling** — The API contract in the spec shows `portfolioRturnPct` (missing `e`). This implementation uses the correctly spelled `portfolioReturnPct` as the canonical response field. Downstream consumers should adopt the corrected spelling.

2. **`beginMarketValue = 0` and `endMarketValue = 0`** — A portfolio that starts and ends at zero is treated as a zero-return scenario (`portfolioReturnPct = 0`). This passes validation because Rule 4 only fires when begin is 0 and end is non-zero.

3. **Cash-flow review rule when `beginMarketValue = 0`** — The 20% cash-flow threshold (Rule 6) is skipped when `beginMarketValue = 0` to avoid a trivially-true comparison against zero.

4. **Multiple REVIEW_REQUIRED reasons** — Both Rule 5 (excess return) and Rule 6 (cash flow) can fire simultaneously. When they do, both reason strings appear in the `reasons` array; the status remains a single `REVIEW_REQUIRED`.

5. **Multiple INVALID_INPUT reasons** — All four validation rules are evaluated before returning so multiple failure reasons can accumulate in one response.

6. **`processedAt` timestamp** — Set to the server's UTC time at the moment the request is processed. It reflects actual processing time, not the `valuationDate`.

7. **H2 as audit store** — Each processed request (regardless of status) is persisted as a `ValuationAudit` record in H2. The database is in-memory and resets on each application restart. It is intended for development and testing only.

8. **`requestedBy` field** — Accepted and stored in the audit record; not echoed in the response body.

9. **Rounding** — `portfolioReturnPct` and `excessReturnPct` are rounded to four decimal places before serialisation. The JSON serialiser drops trailing zeros (e.g., `2.5000` → `2.5`).

---

## How to Run

### Prerequisites

- Java 21 (JDK)
- Maven 3.9+

### Clone and Install

```bash
git clone <repo-url>
cd portfolio-performance
mvn clean install
```

### Run the Application

```bash
mvn spring-boot:run
```

Server starts on `http://localhost:8080`.

### H2 Console (in-memory DB browser)

Navigate to `http://localhost:8080/h2-console` in your browser.

| Field | Value |
|-------|-------|
| JDBC URL | `jdbc:h2:mem:portfoliodb` |
| Username | `sa` |
| Password | *(leave blank)* |

### Run Tests

```bash
mvn test
```

Runs unit tests (validator, service) and integration tests (MockMvc full round-trip).

---

## API Reference

### `POST /api/performance/daily-return`

#### Request Body

```json
{
  "portfolioId":        "PF-1001",
  "valuationDate":      "2026-06-14",
  "beginMarketValue":   1000000,
  "endMarketValue":     1035000,
  "netCashFlow":        10000,
  "benchmarkReturnPct": 1.8,
  "currency":           "USD",
  "requestedBy":        "advisor01"
}
```

| Field | Type | Required |
|-------|------|----------|
| `portfolioId` | String | Yes |
| `valuationDate` | String (YYYY-MM-DD) | Yes |
| `beginMarketValue` | Number | Yes |
| `endMarketValue` | Number | Yes |
| `netCashFlow` | Number | Yes |
| `benchmarkReturnPct` | Number | Yes |
| `currency` | String | Yes |
| `requestedBy` | String | Yes |

---

#### Response — VALID (HTTP 200)

```json
{
  "portfolioId":        "PF-1001",
  "valuationDate":      "2026-06-14",
  "portfolioReturnPct": 2.5,
  "benchmarkReturnPct": 1.8,
  "excessReturnPct":    0.7,
  "status":             "VALID",
  "reasons":            [],
  "processedAt":        "2026-06-14T10:30:00.000Z"
}
```

#### Response — REVIEW_REQUIRED (HTTP 200)

```json
{
  "portfolioId":        "PF-1002",
  "valuationDate":      "2026-06-14",
  "portfolioReturnPct": 9.0,
  "benchmarkReturnPct": 1.8,
  "excessReturnPct":    7.2,
  "status":             "REVIEW_REQUIRED",
  "reasons": [
    "Portfolio return (9.0000%) deviates from benchmark (1.8%) by 7.2000%, exceeding the 5% threshold."
  ],
  "processedAt":        "2026-06-14T10:30:00.000Z"
}
```

#### Response — INVALID_INPUT (HTTP 422)

```json
{
  "portfolioId":        "PF-1003",
  "valuationDate":      "2026-06-14",
  "portfolioReturnPct": null,
  "benchmarkReturnPct": 1.8,
  "excessReturnPct":    null,
  "status":             "INVALID_INPUT",
  "reasons": [
    "Begin market value cannot be negative."
  ],
  "processedAt":        "2026-06-14T10:30:00.000Z"
}
```

#### Response — Bad Request (HTTP 400)

```json
{
  "error":  "Missing required fields",
  "fields": ["currency"]
}
```

---

## Quick Verification with curl

```bash
# VALID — spec example (portfolioReturnPct = 2.5%, excessReturnPct = 0.7%)
curl -s -X POST http://localhost:8080/api/performance/daily-return \
  -H "Content-Type: application/json" \
  -d '{
    "portfolioId": "PF-1001",
    "valuationDate": "2026-06-14",
    "beginMarketValue": 1000000,
    "endMarketValue": 1035000,
    "netCashFlow": 10000,
    "benchmarkReturnPct": 1.8,
    "currency": "USD",
    "requestedBy": "advisor01"
  }'

# REVIEW_REQUIRED — excess return > 5%
curl -s -X POST http://localhost:8080/api/performance/daily-return \
  -H "Content-Type: application/json" \
  -d '{
    "portfolioId": "PF-1002",
    "valuationDate": "2026-06-14",
    "beginMarketValue": 1000000,
    "endMarketValue": 1090000,
    "netCashFlow": 0,
    "benchmarkReturnPct": 1.8,
    "currency": "USD",
    "requestedBy": "advisor01"
  }'

# REVIEW_REQUIRED — cash flow > 20% of begin market value
curl -s -X POST http://localhost:8080/api/performance/daily-return \
  -H "Content-Type: application/json" \
  -d '{
    "portfolioId": "PF-1003",
    "valuationDate": "2026-06-14",
    "beginMarketValue": 1000000,
    "endMarketValue": 1025000,
    "netCashFlow": 250000,
    "benchmarkReturnPct": 1.8,
    "currency": "USD",
    "requestedBy": "advisor01"
  }'

# INVALID_INPUT — negative begin market value
curl -s -X POST http://localhost:8080/api/performance/daily-return \
  -H "Content-Type: application/json" \
  -d '{
    "portfolioId": "PF-1004",
    "valuationDate": "2026-06-14",
    "beginMarketValue": -500,
    "endMarketValue": 1000,
    "netCashFlow": 0,
    "benchmarkReturnPct": 1.8,
    "currency": "USD",
    "requestedBy": "advisor01"
  }'

# INVALID_INPUT — missing currency
curl -s -X POST http://localhost:8080/api/performance/daily-return \
  -H "Content-Type: application/json" \
  -d '{
    "portfolioId": "PF-1005",
    "valuationDate": "2026-06-14",
    "beginMarketValue": 1000000,
    "endMarketValue": 1035000,
    "netCashFlow": 10000,
    "benchmarkReturnPct": 1.8,
    "requestedBy": "advisor01"
  }'
```
