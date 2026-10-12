# Deployment checklist

## Before first start

- [ ] Use a clean Neon database and review every Flyway migration.
- [ ] Replace every placeholder in `backend/.env` and `frontend/.env.local`.
- [ ] Generate a unique JWT secret of at least 32 bytes and an admin bootstrap password of at least 12 characters.
- [ ] Restrict `CORS_ALLOWED_ORIGINS` to exact production frontend origins (comma-separated, no wildcard).
- [ ] Confirm the backend database URL uses TLS (`sslmode=require`) and the credentials are for the intended Neon database.
- [ ] Use the provider's direct/appropriate JDBC connection for migrations; if using a pooled connection, verify it supports Flyway migration operations.

## Security and operations

- [ ] Put secrets in the hosting provider's secret manager; do not commit `.env` files or put private secrets in `NEXT_PUBLIC_*` variables.
- [ ] Serve both applications over HTTPS, disable public debug interfaces and keep Actuator exposure limited to health/info.
- [ ] Recheck the official [Next.js upstream-dependency security update](https://nextjs.org/blog/upcoming-nextjs-security-update-october-2026) announced for 14 October 2026; apply compatible patched versions before production and rerun frontend validation.
- [ ] Configure database backups/restore tests, log retention, uptime monitoring and alerts.
- [ ] Tune HikariCP pool size to the Neon compute limits and deployment replica count.
- [ ] Rotate the seed admin password after first sign-in; remove/clear bootstrap environment variables after creating the admin if your operational process permits it.
- [ ] Test JWT expiration, invalid signatures, role denial, ownership boundaries and user deletion paths.
- [ ] Add rate limiting at the edge/API for authentication routes, plus password reset/email verification before accepting public registrations at scale.
- [ ] Configure privacy/retention/consent terms for personal and fitness profile data.

## Commercial readiness

- [ ] Replace the simulated membership activation endpoint with a payment provider's hosted checkout and signed webhook flow.
- [ ] Add payment idempotency, refund/cancellation records and reconciliation reports.
- [ ] Add email/SMS notification delivery for membership expiry, trainer requests and closure announcements if required.

## Release validation

- [ ] `cd backend && ./mvnw clean verify`
- [ ] `cd frontend && npm install && npm run lint && npm run build` (commit the generated lockfile for repeatable CI installs)
- [ ] Run role-specific happy-path and denial tests against a PostgreSQL test database.
- [ ] Test check-in/check-out around opening/closing time, a special closure, midnight/time-zone boundaries and duplicate concurrent check-ins.
- [ ] Test mobile layouts and keyboard focus states at narrow viewports.
- [ ] Review generated dependency/SBOM and vulnerability reports before production.
