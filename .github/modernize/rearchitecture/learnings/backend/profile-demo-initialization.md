# Profile Demo Initialization

Repository-backed startup demo initialization should be profile-scoped and must never mutate existing credentials.

## What Happened
Task t3 needed repeatable local/demo seed behavior without SQL files, password overwrites, or credentials in logs.

## Takeaway
Use an `@Profile({"local", "demo"})` application runner, bind credentials through environment-backed properties, encode only passwords for newly created users, and leave existing password hashes untouched. Activation of inactive users must remain an opt-in policy.

## History
- 2026-11-01 (Enterprise E-Commerce-Multi-Vendor Marketplace/t3): initial
- 2026-11-01 (Enterprise E-Commerce-Multi-Vendor Marketplace/t7.1): demo startup is activated only by the explicit `demo` profile; local and production profiles must not run it. Credential properties have no source defaults and startup validation rejects missing values.
