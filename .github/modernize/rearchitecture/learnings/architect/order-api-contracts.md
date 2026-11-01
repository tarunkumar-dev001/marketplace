# Order API Contracts

The vendor/admin order extension must preserve the existing customer API and be designed around repository-enforced vendor ownership.

## What Happened

This contract was defined for Enterprise E-Commerce & Multi-Vendor Marketplace under task t1. The current order entity stores vendor ownership on each order item, while customer and admin controller patterns already establish response wrapping, offset pagination, and JWT role checks.

## Takeaway

Use role-specific order routes, return vendor-filtered order projections only through service/repository predicates, and keep one shared status endpoint derived from the server enum. Demo account provisioning must be profile-scoped, BCrypt-backed, repository-driven, repeatable, and never overwrite existing password hashes.

## History

- 2026-11-01 (Enterprise E-Commerce & Multi-Vendor Marketplace/t1): initial
