## [t4] Added frontend dashboard order integration
- Existing dashboards used page-local loading, error, retry, and pagination state.
- Added independent statistics and order loaders for vendor/admin pages to avoid coupled failures.
- Backend BigDecimal values can be represented as strings in the frontend; display helpers normalize them.
- API auth header was repaired from a malformed existing uncommitted line to Bearer token syntax.
- Learnings consumed: none
