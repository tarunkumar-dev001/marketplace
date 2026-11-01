# Order API Boundaries

Backend repository predicates and service actor checks are the source of truth for vendor and admin order isolation.

## What Happened
The t2 implementation needed distinct vendor order pagination, vendor-only item projection, and separate marketplace totals without schema changes.

## Takeaway
Use repository joins for vendor order membership, fetch page item data only after distinct pagination, and filter vendor line items in DTO conversion. Validate active vendor/admin records against JWT subject and role in the service.

## History
- 2026-11-01 (Enterprise E-Commerce-Multi-Vendor Marketplace/t2): initial
- 2026-11-01 (Enterprise E-Commerce-Multi-Vendor Marketplace/t7.1): vendor order DTO totals are the sum of vendor-owned line totals, never the customer order aggregate.
