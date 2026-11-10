# Gejekoushian’s storefront API

Spring Boot / Java 17 REST API with Spring Security, MongoDB, S3 image storage and server-owned PayPal checkout. Companion: [shrek-front](https://github.com/dminasian1999/shrek-front).

## Local development

Use JDK 17+ and the included Maven wrapper. A JRE alone cannot compile the application. Start a local MongoDB **replica set**, required for checkout inventory transactions:

```sh
docker compose up -d --wait mongo
bash ./mvnw spring-boot:run
```

Defaults connect to `mongodb://localhost:27017/sevan_local?replicaSet=rs0` on port 8080. A native MongoDB 7 replica set is also supported. This audit ran native MongoDB; the Compose file is a setup convenience. No production data is seeded and no privileged default account is created. Register a normal account through the frontend, then provision moderator/admin roles through your controlled database/admin process.

`.env.example` lists the configuration; Spring Boot does not automatically read `.env`. Export values in your shell or configure them in Render. Missing S3/PayPal credentials do not block browsing or local startup. Image uploads and payments require their own configured integrations.

```sh
bash ./mvnw verify
# Optional tests against a dedicated, local transaction-capable database:
AUDIT_INTEGRATION_MONGODB_URI='mongodb://localhost:27017/sevan_audit_integration?replicaSet=rs0' bash ./mvnw -Dtest=CheckoutMongoIntegrationTest test
```

Normal tests use mocks and need no external account. The optional integration tests use unique synthetic records and a mocked PayPal gateway with actual Mongo transactions. Use a dedicated local database.

## Configuration

| Variable | Purpose |
| --- | --- |
| `MONGODB_URI` | MongoDB URI; replica set/Atlas transactions required for payment capture |
| `PORT` | HTTP port, default 8080 |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated exact frontend origins; defaults to localhost and the two supplied Vercel domains |
| `PAYPAL_MODE` | `sandbox` by default; explicitly `live` for production credentials |
| `PAYPAL_CLIENT_ID`, `PAYPAL_CLIENT_SECRET` | Backend gateway credentials; never send the secret to the frontend |
| `CHECKOUT_PAYMENTS_ENABLED` | Default false; enable only after sandbox verification and required migrations |
| `CHECKOUT_CURRENCY` | USD catalog currency; the current storefront displays USD |
| `CHECKOUT_SHIPPING_EXCHANGE_RATE` | Explicit conversion from shipping-table ILS into catalog USD; zero/missing fails closed |
| `ACCESS_KEY`, `SECRET_KEY`, `BUCKET_NAME` | S3 credentials/bucket for image upload; AWS default credential chain supported |

Product `weight` is **grams**; inventory is `sizeQuantities: [{size, quantity}]`. Shipping country and rate collections must be populated with validated tariffs. The imports require an administrator and only accept the bundled CSV filenames. Verify actual tariffs before accepting orders; the checked-in CSVs are not a promise of current postal pricing. Unknown destinations, invalid weights and missing tariffs produce errors rather than free shipping.

## API map

| API | Permission / behavior |
| --- | --- |
| `GET /posts`, `/post/{id}`, `/posts/category/{category}` | Public catalog |
| `POST /posts/search`, `/posts/wishList` | Public bounded/validated search or product lookup |
| `POST /post/{author}`, `PUT/DELETE /post/{id}`, `/post/file/*` | Moderator/administrator catalog and validated JPEG/PNG/GIF upload (under 5 MB) |
| `POST /users/register`, `/users/login` | Registration; login uses explicit Basic Authorization |
| `PUT /users`, `/users/password` | Authenticated profile/password; current password is authenticated |
| `/users/{username}/cartList/**`, `/wishList/**`, `/users/address/{login}` | Owner only; canonical product/variant validation and optimistic cart version checks |
| `/users/{username}/roles/{role}`, `GET /users` | Administrator only |
| `GET /parser/countries`, `/shippingCost/{country}/{weight}` | Public delivery availability/quote |
| `POST /parser*/*` | Administrator-only, bundled shipping imports |
| `POST /checkOut` | Owner creates pending order from server cart and `{shippingAddress}` |
| `POST /api/paypal/orders` | Owner creates gateway payment using local `{orderId}` |
| `POST /api/paypal/orders/{paypalOrderId}/capture` | Owner capture with amount/currency/reference verification, transactional stock and recorded PAID state |
| `GET /ordersByUser/{userId}`, `/order/{orderId}` | Owner or administrator; other users do not see order data |
| `GET /orders-all` | Administrator only |

Authentication is stateless HTTP Basic with explicit Authorization headers and no session cookies; production requires HTTPS. Never use credentials from the production database for local checks. `/saveOrder/**` and `/users/payment/capture` are denied legacy bypasses. Raw card-saving returns 410; recovery request returns 503 until reliable email delivery exists. Error JSON contains safe messages instead of stack traces.

## Deploy and migrate

Deploy both repository PRs together, keeping payments disabled during migration.

1. Back up the database. Rotate/remove any old **admin/admin** account created by the previous startup code; removing the bootstrap code does not remove existing accounts. Review privileged roles and rotate compromised credentials.
2. Purge legacy stored `paymentMethod.cvv` data and obsolete recovery tokens in a reviewed, backed-up migration. The API now redacts CVV from older documents and no longer saves raw card data.
3. Ensure every sellable product has a positive finite price and grams weight, and valid nonnegative per-size stock. Legacy `quantity` fields were ignored by the old backend; populate `sizeQuantities` deliberately. Verify shipping tariffs, country groups, currency and the configured conversion rate.
4. Use a MongoDB replica set/Atlas with transaction permissions. Existing orders may lack new `currencyCode`, `shippingCost`, `paypalOrderId` and `inventoryReserved` fields; review historical paid records separately. Never infer that a legacy `PAID` record represents a verified gateway capture.
5. Configure matching PayPal sandbox client IDs in both deployments, CORS, S3 bucket policy and credentials. Run approved, declined, timeout/retry and sold-out sandbox scenarios. Enable `CHECKOUT_PAYMENTS_ENABLED` only after those pass; choose live mode explicitly before real payments.

Capture reserves stock transactionally before charging. A short persisted capture lease blocks concurrent requests. Retries use the same provider idempotency key and reconcile provider-completed payments before charging again. Definitive declines release stock; uncertain network/provider results retain reservations to avoid overselling. **Reconciliation is required:** inspect pending orders with `inventoryReserved=true`, query the matching PayPal order/capture, and reconcile completed payments before releasing any reservation. Do not clear stock reservations blindly. Monitor failures and reconcile worker crashes/long-lived pending captures operationally.

Database changes here are additive; prices/totals now use decimal order totals and current catalog prices instead of trusting client JSON. Profile/address/password updates are field scoped; cart changes use `cartVersion` compare-and-set so concurrent saves cannot overwrite payment cleanup.

## Dependency follow-ups

The audit upgrades Spring Boot within 3.5, Jackson/Netty/Tomcat/Log4j patch lines and AWS SDK v1 to its final release. OSV advisory matches for 86 resolved runtime coordinates fell from 136 to four, all in Spring Framework 6.2.19 MVC/WebFlux. The affected functional router, SSE/view-fragment and XSLT view features are not used by these REST controllers; the advisory matches remain and require a supported Spring major migration when feasible. AWS SDK v1 has been past support since December 2025 and should move to v2 separately. The old PayPal Checkout SDK has no newer compatible Central release; a maintained SDK migration is also a follow-up. These are not claims of a complete dependency security certification.
