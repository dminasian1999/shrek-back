# Audit implementation notes — November 10, 2026

The paired branches are published as `codex/full-stack-audit`: [frontend](https://github.com/dminasian1999/shrek-front/tree/codex/full-stack-audit) and [backend](https://github.com/dminasian1999/shrek-back/tree/codex/full-stack-audit). GitHub access was restored after reconnecting the integration. Deploy this backend first, then the frontend, after the migration steps in README; keep payments disabled until sandbox verification passes.

## Most important changes

- Anonymous/cross-user catalog, wishlist, account and order operations are restricted by method and ownership. Arbitrary legacy payment/order bypasses are denied.
- Removed the `admin/admin` startup bootstrap. Registration validates input and uses insert-only storage to prevent duplicate races. Raw card storage is disabled; old CVVs are redacted from responses. Recovery tokens are single use; email recovery explicitly reports unavailable until delivery exists.
- Cart items use canonical catalog prices and valid variants/quantities. Field-scoped profile writes and cart compare-and-set prevent unrelated concurrent saves from replacing checkout/cart updates.
- Checkout creates PENDING server snapshots with verified prices, grams weight and destination shipping. Currency conversion is explicit. Browser identity/status/amount JSON cannot create a paid order.
- PayPal create/capture checks ownership and amount/currency/reference, uses deterministic idempotency keys, verifies approval, reserves inventory transactionally, and records paid state with selective cart cleanup and a recalculated remaining cart total. Unknown capture outcomes keep stock reserved for reconciliation; known declines release it.
- Catalog search uses literal bounded filters, validated sort/price/date inputs and coherent date bounds. Shipping no longer overwrites EcoPost prices with EMS or treats unsupported destinations/weights as free.
- Image uploads use the configured bucket, unique keys, size/dimension/type validation and closed streams. No arbitrary caller file paths are read by CSV imports.

## Validation

`mvn verify` passes40 tests including2 opt-in real MongoDB7 replica-set integration tests. The real database tests verify canonical totals, actual stock commit/rollback, paid recording, idempotence, and selective cart cleanup/remaining total; PayPalService is mocked so no external payment occurs. Java17 target compiled with JDK21. Default runs skip the2 explicitly opt-in database tests.

Resolved runtime dependency OSV matches:136 before,4 after, across86 coordinates. See README for remaining Spring advisories and unsupported AWS/PayPal SDK migration work. A successful build does not prove every dependency advisory is unreachable.

The public deployed API at `https://shrek-back.onrender.com/posts` first timed out, then returned503. Production database, PayPal sandbox/live, S3, mail, and Render/Vercel configuration were not accessed with credentials. No production records, messages, uploads or payments were created.
