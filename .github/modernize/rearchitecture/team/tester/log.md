## [t6] Added and ran focused implementation tests
- Added MockMvc coverage for vendor/admin order scopes, statistics, and status payloads.
- Corrected initial status expectations to use the actual contract field `value`, not `code`.
- Docker is unavailable; Playwright remains independently available after downloading Chromium.
- Backend startup is blocked by rejected configured MySQL credentials, so authenticated runtime journeys remain unverified.
- Learnings consumed: (none)

## [t8] Ran final backend suite and frontend lint/build validation
- Focused remediation tests: 24 passed, 0 failed, 0 skipped, exit code 0.
- Complete backend suite: 160 passed, 0 failed, 0 skipped, exit code 0.
- Frontend lint via short alias: 0 errors, 15 warnings, exit code 0.
- Frontend build via short alias: 105 modules transformed, exit code 0.
- Backend startup remains blocked by no datasource URL and no embedded datasource; Docker daemon is unavailable.
- Do not diagnose ampersand-in-path frontend failures as source defects; use a subst alias for scripts whose process resolution mishandles the workspace path.
- Learnings consumed: (none)
