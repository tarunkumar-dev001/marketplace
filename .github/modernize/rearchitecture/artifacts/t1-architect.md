# t1 - API Contracts and Implementation Boundaries

## Scope

This is a brownfield extension of the existing Spring Boot / React marketplace. The implementation must preserve the current customer order API and all existing uncommitted changes.

## Existing Patterns

- Backend responses wrap payloads in `ApiResponse<T>` (`src/main/java/com/marketplace/dto/ApiResponse.java`).
- Offset pagination uses `page`, `size`, `items`, `totalElements`, and `totalPages`.
- Spring Security derives actor identity from the JWT subject and role claim.
- Existing dashboard pages use page-local loading, error, retry, and pagination state.
- Entities are mapped with JPA; repository methods must use vendor/order joins rather than changing the schema.
- Current `spring.jpa.hibernate.ddl-auto=validate` and Flyway are enabled in local configuration.

## API Contracts

### Pagination and errors

- Maximum page size: 100.
- Invalid page/size returns the existing `400` validation behavior.
- Unsupported actor role returns `403`.
- Unknown order returns `404`.
- Every success response uses `ApiResponse<T>`.
- List ordering is descending by `createdAt`, then descending by `id`.

### Vendor orders

`GET /api/vendor/orders?page=0&size=20`

Authorization: `VENDOR`.

Page response:

```text
ApiResponse<OrderPageResponse>
```

`OrderPageResponse` remains the existing page wrapper with one constraint: only orders containing at least one item whose `vendor_id` equals the authenticated user are returned. Each returned order contains only that vendor's items in `items`.

The service/repository boundary is:

- Resolve and validate the active vendor from JWT subject plus role.
- Query a distinct customer order page by vendor-user ID.
- Fetch matching vendor items only for the page's order IDs.
- Total count must represent distinct matching orders, not line items.

### Vendor statistics

`GET /api/vendor/orders/statistics`

Authorization: `VENDOR`.

```text
VendorOrderStatisticsResponse {
  totalOrders: long
  placedOrders: long
  fulfilledOrders: long
  cancelledOrders: long
  totalUnits: long
  totalSales: BigDecimal
}
```

`totalSales` is the sum of `lineTotal` for vendor-owned items in non-cancelled orders. Statistics count distinct orders containing vendor items and units across vendor-owned items.

### Admin orders

`GET /api/admin/orders?page=0&size=20`

Authorization: `ADMIN`.

`ApiResponse<OrderPageResponse>` returns the complete order, preserving all vendor items and customer information only if the existing `OrderResponse` contract already exposes it. No vendor filtering is applied.

### Admin statistics

`GET /api/admin/orders/statistics`

Authorization: `ADMIN`.

```text
AdminOrderStatisticsResponse {
  totalOrders: long
  placedOrders: long
  fulfilledOrders: long
  cancelledOrders: long
  totalUnits: long
  totalSales: BigDecimal
}
```

`totalSales` is the sum of order totals for non-cancelled orders. Counts are distinct order counts.

### Order statuses

`GET /api/orders/statuses`

Authorization: any authenticated role.

```text
OrderStatusesResponse {
  statuses: OrderStatusResponse[]
}

OrderStatusResponse {
  value: string
  label: string
}
```

The values are derived from `OrderStatus`; labels are human-readable display names. The same endpoint is used by vendor and admin dashboards. Optional equivalent role-prefixed routes must not be implemented without a coordinator decision.

## Frontend Contracts

Add to `frontend/src/types/order.ts`:

- `VendorOrderStatistics`
- `AdminOrderStatistics`
- `OrderStatusOption`
- `OrderStatuses`

Add functions in `frontend/src/api/orders.ts`:

- `getVendorOrders(page, size)`
- `getVendorOrderStatistics()`
- `getOrderStatuses()`

Add admin functions in `frontend/src/api/admin.ts`:

- `getAdminOrders(page, size)`
- `getAdminOrderStatistics()`

Existing customer order functions remain unchanged. VendorDashboardPage and AdminDashboardPage each load statistics and paginated orders independently, expose loading/error/retry states, and reuse the existing pagination controls.

## Security and Data Boundary

- Vendor isolation must be enforced by repository/service predicates, never by frontend filtering.
- Vendor JWT subject and role must match an active vendor record.
- Never trust a vendor ID supplied by request parameters.
- Admin repository operations require an authenticated and authorized admin at service and controller boundaries.
- The status endpoint derives values from the server enum and must not accept status updates.

## Demo Initialization Boundary

Demo initialization is application startup data preparation, owned by backend configuration:

- Active only under Spring profiles `local` or `demo`.
- Credentials come from environment-configurable properties.
- Required credentials must not be emitted to logs or startup output.
- Existing users must never have passwords overwritten.
- Missing demo users are created with canonical lowercase emails and encoded BCrypt passwords.
- Existing demo users are retained, activated only if policy permits, and have no password mutation.
- Use repositories for existence checks and entity creation; do not execute SQL initialization files.
- No migration is added by this task.

Suggested property names:

```text
marketplace.demo.admin-email
marketplace.demo.admin-password
marketplace.demo.vendor-email
marketplace.demo.vendor-password
marketplace.demo.customer-email
marketplace.demo.customer-password
```

Configuration should bind environment equivalents through `${...}` placeholders and use safe defaults appropriate only for local/demo profiles. The implementation must explicitly document the environment names without logging values.

## Persistence Boundary

No schema change is designed. Existing `CustomerOrder`, `OrderItem`, and repository indexes already support vendor filtering on `order_items.vendor_id`. A migration is required only if implementation runtime validation proves an entity/schema mismatch against `spring.jpa.hibernate.ddl-auto=validate`; otherwise retain `V3__add_cart_and_order_tables.sql` and Flyway configuration unchanged.

## Implementation Boundaries

1. Backend: add vendor/admin query methods and repositories, statistics DTOs, status DTO, controller routes, and security authorization rules.
2. Backend: add profile-scoped demo initializer and environment property binding.
3. Frontend: add types and API functions, then dashboard sections.
4. Tester: add focused controller/service/repository tests for pagination, isolation, counts, and demo repeatability.
5. Security: review vendor item filtering, admin authorization, JWT role checks, credential logging, and password immutability.
6. Architect: review resulting routes, DTO names, page shape, security patterns, and no-migration finding.

## Test Evidence Required

- Vendor page excludes orders with no vendor-owned items and trims returned items to that vendor.
- Vendor statistics match repository/service fixture totals and distinct-order counts.
- Admin endpoint includes all orders and marketplace totals.
- Status endpoint matches the server enum.
- Demo initializer is inert outside local/demo and repeatable under local/demo.
- Existing passwords remain byte-for-byte unchanged on repeat initialization.
- Backend build/test suite passes with no schema-validation mismatch.

## Upstream Artifacts Consumed

- None — no dependency artifacts were provided.

## Evidence Mapping

- `board.md` — task scope and phase dependencies.
- `src/main/java/com/marketplace/controller/OrderController.java:21-75` — current JWT actor, pagination, and API response patterns.
- `src/main/java/com/marketplace/controller/AdminController.java:21-75` — admin role/pagination pattern.
- `src/main/java/com/marketplace/security/SecurityConfig.java:36-52` — authorization route patterns.
- `src/main/java/com/marketplace/entity/OrderItem.java:29-52` — vendor ownership field.
- `src/main/java/com/marketplace/repository/CustomerOrderRepository.java:13-31` — existing order repository pattern.
- `src/main/java/com/marketplace/entity/User.java:19-70` — repository-backed user mutation and password hash constraints.
