# Backend Session Log

## [t3] Implemented safe profile demo initialization
- Profile-scoped startup runners must remain inert outside `local` and `demo`; repository existence checks are the source of truth.
- Existing demo users are preserved byte-for-byte unless the activation policy is explicitly enabled; passwords are never rewritten.
- Credentials should be bound through environment variables and startup logs should report counts only.
- Learnings consumed: none; no prior backend learning directory existed.

## [t2] Implemented vendor/admin order APIs
- Vendor pages query distinct orders through repository joins, then fetch only matching line items.
- Active vendor/admin records are validated against JWT subject, role, and repository state at service boundaries.
- Statistics are repository aggregates and totals use non-cancelled order semantics.
- Learnings consumed: profile-demo-initialization.

## [t7.1] Remediated vendor disclosure and demo credentials
- Vendor projections must expose the sum of vendor-owned line totals, not the parent customer order total.
- Demo seeding belongs only to the explicit `demo` profile and requires environment email/password values with no source defaults.
- Existing password hashes remain untouched; the activation policy stays opt-in.
- Learnings consumed: profile-demo-initialization, order-api-boundaries.
