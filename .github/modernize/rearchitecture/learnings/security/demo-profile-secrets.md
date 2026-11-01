# Demo Profile Secrets

Demo profiles must not ship predictable credentials or implicit secret fallbacks.

## What Happened

During Enterprise E-Commerce & Multi-Vendor Marketplace security review t5, demo users were found enabled by the default local profile and backed by source-visible password defaults.

## Takeaway

Require explicit demo-profile activation and environment-provided unique passwords; fail startup when required demo credentials are absent rather than substituting documented defaults.

## History

- 2026-11-01 (Enterprise E-Commerce & Multi-Vendor Marketplace/t5): initial finding for explicit demo profile activation and secret requirements.
