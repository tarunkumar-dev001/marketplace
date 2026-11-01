# t3 - Safe Profile Demo Initialization

## Summary
Added repository-backed demo user initialization for the `local` and `demo` Spring profiles. Credentials are bound from environment-backed properties, BCrypt encodes passwords only for newly created users, and existing password hashes are never changed.

## Implementation
- Added `DemoProperties` for environment-configurable demo credentials and activation policy.
- Added `DemoDataInitializer` as a profile-scoped `ApplicationRunner`.
- Added safe startup logging containing only creation and preservation counts.
- Added focused repeatability, activation-policy, credential-validation, profile, role, and email-normalization tests.
- No SQL initializer or migration added.

## Environment Variables
- `MARKETPLACE_DEMO_ADMIN_EMAIL`
- `MARKETPLACE_DEMO_ADMIN_PASSWORD`
- `MARKETPLACE_DEMO_VENDOR_EMAIL`
- `MARKETPLACE_DEMO_VENDOR_PASSWORD`
- `MARKETPLACE_DEMO_CUSTOMER_EMAIL`
- `MARKETPLACE_DEMO_CUSTOMER_PASSWORD`
- `MARKETPLACE_DEMO_ACTIVATE_EXISTING_USERS`

Credential values are not logged. The activation-policy default is `false`.

## Test Results
- Command: `.\mvnw.cmd -Dtest=DemoDataInitializerTests test`
- Passed: 5
- Failed: 0
- Skipped: 0
- Command: `.\mvnw.cmd test`
- Passed: 151
- Failed: 0
- Skipped: 0

## Upstream Artifacts Consumed
- `.../.github/modernize/rearchitecture/artifacts/t1-architect.md` - demo initialization and persistence boundaries.

## Evidence Mapping
- `t1-architect.md#Demo Initialization Boundary` -> `src/main/java/com/marketplace/config/DemoDataInitializer.java`
- `t1-architect.md#Demo Initialization Boundary` -> `src/main/resources/application.properties`
- Demo repeatability requirement -> `src/test/java/com/marketplace/config/DemoDataInitializerTests.java`

## Compatibility Fixes Required by Current Workspace
- Corrected an existing Java ternary compile error in the t2 vendor order controller.
- Updated the existing admin controller unit-test setup to supply the t2 `OrderService` dependency.
