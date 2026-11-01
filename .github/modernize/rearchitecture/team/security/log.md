# Security Session Log

## [t5] Audited vendor isolation and credential handling
- Security boundary code must be checked both in route configuration and service/repository enforcement.
- Vendor DTOs need item-level filtering and vendor-scoped aggregate amounts; route/repository scoping alone is insufficient.
- Demo profiles require explicit activation and no password fallback because local is the application default profile.
- JWT signing configuration requires external issuer and Base64 secret, and decoder claims are validated.
- Learnings consumed: none.

## [t5.1] Re-audited vendor isolation and demo credential remediation
- Vendor projections and repository aggregates now use vendor-owned line totals; focused tests cover the cross-vendor total case.
- Demo initialization is explicit to the `demo` profile and environment-only credentials are validated before any repository writes.
- Password values remain nonblank-only; unique strong demo passwords should be enforced as deployment hardening.
- Full Maven suite passed 160 tests with no failures; OWASP dependency analysis was blocked by unavailable NVD data/API credentials.
- Learnings consumed: [security/demo-profile-secrets].
