# Windows Workspace Alias

Validation commands in frontends under a workspace containing spaces and an ampersand can require a short path alias.

## What Happened

This project was located at `C:\Users\user\Enterprise E-Commerce & Multi-Vendor Marketplace`. Direct `npm.cmd run lint` failed before Oxlint started because the shell parsed `& Multi-Vendor` as a separate command and Node resolved oxlint relative to `C:\Users\user`. A `subst` alias to `Z:` allowed the same frontend script to execute normally.

## Takeaway

When Windows command resolution is broken by the workspace path, run frontend scripts through a subst alias rather than changing production source or package scripts. Record both the alias setup command and the script exit code.

## History

- 2026-11-01 (ecommerce-marketplace/t8): initial finding
