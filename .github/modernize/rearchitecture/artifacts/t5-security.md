# t5 - Vendor Isolation and Credential Handling Security Review

## Summary

Audit scope: vendor/admin authorization, vendor order and statistics queries, demo initialization, JWT configuration, and environment credential handling. The review found two HIGH findings and one CRITICAL finding.

## Findings

### CRITICAL S1: Known default administrator credentials are enabled by the default local profile

**Owner:** backend

`application.properties:3` selects `local` when no profile is supplied. `DemoDataInitializer.java:18` activates demo seeding in both `local` and `demo`; the initializer creates the admin at `DemoDataInitializer.java:37`.

`DemoProperties.java:11` contains a fixed, source-visible default admin password. Because the property defaults are part of the application configuration and local is the default profile, a normally started instance can create an administrator with predictable credentials unless deployment-specific environment variables override both the admin email and password. This is exploitable if a demo/local instance is reachable beyond a trusted local boundary.

**Evidence:** `src/main/resources/application.properties:3`, `src/main/java/com/marketplace/config/DemoDataInitializer.java:18,37`, `src/main/java/com/marketplace/config/DemoProperties.java:11`

**Recommended ownership:** Backend must make demo profile activation explicit, require unique credentials with no password fallback, and prohibit demo seeding in production.

### HIGH S2: Vendor responses disclose the customer order's entire total amount

**Owner:** backend

The vendor item list is correctly restricted to items owned by the authenticated vendor in `OrderResponse.java:40-43`, and repository queries are vendor-scoped. However, the vendor overload still returns `order.getTotalAmount()` at `OrderResponse.java:37`. That amount is the full customer-order total, which can include products from other vendors. A vendor can therefore infer cross-vendor sales value from every multi-vendor order that contains its products.

**Evidence:** `src/main/java/com/marketplace/dto/OrderResponse.java:37,40-43`; supporting vendor scope: `src/main/java/com/marketplace/repository/CustomerOrderRepository.java`, `src/main/java/com/marketplace/service/impl/OrderServiceImpl.java`

**Recommended ownership:** Backend should calculate a vendor-owned subtotal or omit `totalAmount` from vendor responses. Vendor statistics should likewise be defined as vendor-scoped aggregate value and verified against the same boundary.

### HIGH S3: Demo credentials use source defaults for all roles

**Owner:** backend / devops

`DemoProperties.java:13,15` provide source-known vendor and customer passwords in addition to the admin password in S1. `application.properties:9-14` supplies fixed example credentials and passwords when environment variables are absent. If environment provisioning is incomplete, all demo identities are predictable and reusable. Credential values are not logged, but known-value defaults remain a credential-secrets control failure.

**Evidence:** `src/main/java/com/marketplace/config/DemoProperties.java:11,13,15`, `src/main/resources/application.properties:9-14`, `src/main/java/com/marketplace/config/DemoDataInitializer.java:37-39`

**Recommended ownership:** Backend should remove password defaults and fail startup when required demo secrets are absent; DevOps should enforce secrets outside source control and verify profile-specific deployment configuration.

## Positive Controls

- Admin and vendor HTTP route families are restricted to their roles in `SecurityConfig.java:51-52`.
- Vendor and admin service methods re-load the subject user and verify active status, role, and subject identity.
- Vendor repository paging and statistics queries are constrained to `vendorId`.
- Vendor item DTO mapping filters items by vendor ID.
- Demo passwords are BCrypt encoded only for newly created users, and existing password hashes are preserved.
- JWT issuance is bound to a positive user subject, role, issuer, TTL, and HMAC secret configured outside source control.
- JWT decoding validates issuer and allowed claims.

## Residual Review Areas

- JWT role changes are currently token-version stale until expiration; role downgrade or account-role changes do not immediately revoke already-issued tokens. Backend should consider subject and role consistency against the current active user or short-lived revocation strategy if these roles are security-sensitive.
- The demo initializer's existing-user activation option should remain disabled outside explicitly trusted demo environments.

## Upstream Artifacts Consumed

- `t2-backend.md` — vendor/admin implementation and test evidence.
- `t3-backend.md` — demo initialization and credential contract.

## Evidence Mapping

- Vendor/admin service contract -> `OrderServiceImpl.java` active-user role and vendor/admin checks.
- Vendor repository contract -> `CustomerOrderRepository.java` vendor ID predicates.
- Demo initialization contract -> `DemoDataInitializer.java`, `DemoProperties.java`, and `application.properties`.
- JWT contract -> `JwtConfig.java`, `JwtService.java`, and `SecurityConfig.java`.

## Test Results

- Read-only audit; production files were not modified.
- Test command executed: none. Runtime tests were not run to preserve the requested read-only review boundary.
- Focused dependency tests already reported by upstream artifacts: `mvnw test` passed in t2 and t3.
- Repository changes were not made by this review.

