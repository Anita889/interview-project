# Company Data Search Service

A small Spring Boot service that searches UK companies on Companies House, fetches each company's overview, officers and persons with significant control, stores the result, and returns structured JSON.

The service scrapes the public Find and update company information pages because the task asks for scraping. I also looked at the Companies House Developer Hub before implementing it, but did not use the API because that would avoid the core scraping requirement.

## Tech stack

- Java 17
- Spring Boot 3.3
- Jsoup for HTML fetching and parsing
- Spring Data JPA
- PostgreSQL

## How to run

Requirements:

- Java 17 or newer
- Maven 3.9 or newer

Start the application:

```bash
mvn spring-boot:run
```

The API runs on `http://localhost:8080`.

Before starting the app, create a PostgreSQL database:

```sql
CREATE DATABASE company_search;
```

The default connection settings are:

- JDBC URL `jdbc:postgresql://localhost:5432/company_search`
- Username `postgres`
- Password `postgres`

You can override them with environment variables:

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/company_search
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
```

Spring JPA is configured with `ddl-auto=update`, so tables are created and updated automatically on startup.

## Search endpoint

```http
GET /api/companies/search?query=openai
GET /api/companies/search?query=openai&forceRefresh=true
```

Query parameters:

- `query` is required.
- `forceRefresh` is optional and defaults to `false`. When `true`, it bypasses the stored cache and fetches Companies House again.

Example:

```bash
curl "http://localhost:8080/api/companies/search?query=openai"
```

Example response shape:

```json
{
  "query": "openai",
  "cached": false,
  "fetchedAt": "2026-06-02T13:25:40.123Z",
  "count": 1,
  "warnings": [],
  "companies": [
    {
      "companyNumber": "14367667",
      "name": "OPENAI UK LTD",
      "status": "Active",
      "type": "Private limited Company",
      "incorporatedOn": "21 September 2022",
      "dissolvedOn": null,
      "registeredOfficeAddress": "Suite 1, 7th Floor 50 Broadway, London, United Kingdom, SW1H 0BL",
      "natureOfBusiness": [
        "63990 - Other information service activities not elsewhere classified"
      ],
      "officers": [
        {
          "name": "DOE, Jane",
          "role": "Director",
          "appointedOn": "1 January 2024"
        }
      ],
      "personsWithSignificantControl": [
        {
          "name": "Jane Doe",
          "natureOfControl": [
            "Ownership of shares - 75% or more"
          ]
        }
      ]
    }
  ]
}
```

Errors are returned as JSON too. For example, a blank query returns HTTP 400.

## Caching and freshness

The service normalizes the query by trimming it, collapsing repeated whitespace, and lowercasing it. The first search stores a snapshot of the companies, officers, PSC data, and fetch timestamp in PostgreSQL. Repeating the same normalized query returns the stored snapshot without hitting Companies House while the cache entry is fresh.

Freshness is controlled by `companies-house.cache-ttl`, currently `24h`. After that, the next request refreshes the data. `forceRefresh=true` bypasses the cache immediately.

If a cache entry is stale and Companies House is unavailable during the refresh, the service returns the stale cached result with a warning instead of failing the request completely.

## Politeness

The scraper is deliberately conservative:

- Sends a meaningful `User-Agent` from `companies-house.user-agent`.
- Waits `500ms` between Companies House requests.
- Stops after fully fetching at most `100` companies for one query.
- Continues with partial results when officers or PSC pages fail, and returns those issues in `warnings`.

These values are configurable in `src/main/resources/application.properties`.

## Tests

Parser and deduplication tests are under `src/test/java`.

```bash
mvn test
```

## Hardest part

The hardest part is not the Spring API or the database. It is making the scraper tolerant of public HTML that is meant for people, not machines. The parser uses GOV.UK-style label/value structures where possible and has text-based fallbacks for the overview, officers, and PSC pages.

## What I would improve with more time

- Add integration tests with recorded Companies House HTML pages.
- Add a background refresh job for popular stale queries instead of refreshing only on demand.
- Store per-company snapshots separately so overlapping searches can reuse already fetched companies.
- Add rate limiting per caller if this service were exposed publicly.

## References checked

- Companies House public search: https://find-and-update.company-information.service.gov.uk/
- Example company overview/officers/PSC pages under: https://find-and-update.company-information.service.gov.uk/company/13608027
- Companies House Developer Hub: https://developer.company-information.service.gov.uk/
