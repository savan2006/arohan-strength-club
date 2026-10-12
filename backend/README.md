# AROHAN Strength Club API

Spring Boot REST API for the AROHAN Strength Club web application. See the repository root `README.md` for setup and workflow overview, `../docs/API.md` for endpoints, and `../docs/ARCHITECTURE.md` for package responsibilities.

## Run locally

1. Use Java 21 and set values in `.env` (see `.env.example`).
2. Point `JDBC_DATABASE_URL`, `DB_USERNAME` and `DB_PASSWORD` at an empty Neon/PostgreSQL database.
3. Optionally set `APP_SEED_ADMIN_EMAIL` and a unique `APP_SEED_ADMIN_PASSWORD` (12+ characters) for an initial admin account.
4. Run `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`). Flyway applies the migrations in `db/migration` (V1 schema, V2, V3 performance indexes) on startup. If migration hangs on a Neon `-pooler` host, run it once with the direct (non-pooler) host in `JDBC_DATABASE_URL`.

The default API port is `8080`. Configure CORS using `CORS_ALLOWED_ORIGINS`. Membership payment activation remains a demo-only flow.
