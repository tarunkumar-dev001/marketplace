# Dashboard Order API Integration

Load order statistics, statuses, and paginated order lists independently so one dashboard data failure does not suppress the others.

## What Happened
Vendor and admin dashboard implementations in project t4 required separate loading and retry state for statistics and paginated orders. API responses are wrapped in `ApiResponse<T>`, so each API function unwraps `data` through the existing helper.

## Takeaway
Keep new dashboard order loaders independent, use the existing page fields for pagination, derive display labels through the shared status endpoint, and do not filter vendor-owned items in the browser. Money values may arrive as JSON strings and must be normalized at display time.

## History
- 2026-11-01 (t4-frontend): initial
