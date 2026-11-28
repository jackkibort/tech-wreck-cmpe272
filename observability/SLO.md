# SLO: employee directory search

"Here is how we know it works": the one service-level objective for the journey shown in demo
scenario S1, where an employee signs in and searches the directory.

## The journey

`GET /api/employees`: directory search with filters and paging. It's the first thing every
portal user does after signing in, and it runs over all 300,000 employee records.

## Service-level indicator (SLI)

A request is **good** when the edge records:
- a status below 500 (a 4xx is the caller's problem, not an outage); and
- a response time under **300 ms** (goal G3).

The SLI is good requests ÷ all requests to `/api/employees`, measured at the edge from its JSON
access logs (fields `uri`, `status`, `request_time`).

## Objective

**99% of directory searches are good, over a rolling 28 days.**

## Error budget

The budget is the 1% of requests that may fail or be slow.

| Traffic | Requests in 28 days | Error budget (bad requests allowed) |
|---|---|---|
| Demo week (about 2,000 searches a day) | 56,000 | 560 |
| A small company (about 10,000 a day) | 280,000 | 2,800 |

As time: if every request failed during an outage, 1% of 28 days is about **6 hours 43 minutes**
of total downtime (28 × 24 × 60 × 0.01 = 403 minutes).

**How the budget is used:**
- **Budget left:** releases go out as normal.
- **Budget spent:** releases are paused, except fixes, until the ratio is back above 99%.

## Where it's shown

Grafana → Platform → **Platform health**:
- **"SLO: directory search good-request ratio (28 days)"**: green at 99% or above.
- **"SLO: error budget remaining (28 days)"**: below 0 means the SLO is broken.

## How we check it

- **Load test:** the k6 test in `/perf` runs the same search under load and checks the
  95th-percentile response time against 300 ms (G3). See `/perf/README.md`.
- **Request IDs:** every request carries an `X-Request-ID` from the edge, which the API puts on
  every log line, so any slow or failed search can be traced end to end.
