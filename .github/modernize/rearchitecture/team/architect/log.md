## [t1] Defined API contracts and implementation boundaries
- Existing customer order APIs use ApiResponse, offset pagination, JWT subject/role actor checks, and repository-backed authorization.
- Vendor ownership belongs on OrderItem.vendor, so vendor isolation must be enforced by distinct-order and vendor-item repository queries.
- Local/demo initialization should be separate from schema migrations and must never mutate existing passwords.
- Learnings consumed: none
