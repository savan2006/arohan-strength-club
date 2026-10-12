# AROHAN STRENGTH CLUB

A full-stack gym membership and training platform built for AROHAN STRENGTH CLUB. The migrated stack is **Next.js + TypeScript**, **Spring Boot + Java**, and **PostgreSQL on Neon**. The interface uses the supplied AROHAN logo and a custom black / off-white / red identity instead of the original FitPulse theme.

## Project structure

```text
AROHAN_STRENGTH_CLUB/
├── frontend/                 # Next.js App Router application
│   ├── public/                # Gym logo, gym image, exercise illustrations
│   ├── src/app/               # Landing, auth and portal routes
│   ├── src/components/        # UI components and role workspaces
│   ├── src/lib/api.ts         # Typed fetch/session utilities
│   ├── .env.local             # Local frontend configuration (safe starter value)
│   └── .env.example
├── backend/                  # Spring Boot REST API
│   ├── src/main/java/com/arohan/strengthclub/
│   │   ├── config/            # Security, CORS and JWT configuration
│   │   ├── controller/        # HTTP / REST endpoints
│   │   ├── dto/               # Request and response contracts
│   │   ├── entity/            # JPA entities and enums
│   │   ├── exception/         # Business exceptions and API error mapping
│   │   ├── repository/        # Spring Data repositories
│   │   ├── security/           # Current-user lookup and token service
│   │   └── service/           # Business rules and transactions
│   ├── src/main/resources/
│   │   ├── db/migration/      # Versioned Flyway SQL migrations
│   │   └── application.yml
│   ├── .env                  # Local backend configuration (placeholders only)
│   ├── .env.example
│   └── pom.xml
└── docs/                     # Architecture, API, deployment and validation notes
```

## Current stack

| Layer | Version / choice |
| --- | --- |
| Frontend | Next.js 16.4.0, React 19.3.0, TypeScript |
| UI | Custom CSS design system, Lucide icons, supplied logo and local exercise art |
| Backend | Spring Boot 4.1.1, Java 21 |
| Persistence | Spring Data JPA, PostgreSQL, Neon-compatible JDBC connection |
| Schema changes | Flyway SQL migrations; Hibernate schema mode is `validate` |
| Authentication | BCrypt password hashing, signed JWTs, Spring Security role checks |
| Operations | Spring Boot Actuator health endpoints, HikariCP connection pool |

Versions are intentionally pinned for the main framework packages. Before upgrading, review the official release notes and run the build/test checks in this repository.

## Included workflows

- Member registration, sign-in, training profile, fitness goals and trainer preference.
- Membership plans, local payment/activation simulation and membership validity checks.
- Recommended workout generation, member-created plans, exercise instructions, editable seven-day training/recovery schedules and workout completion records.
- Trainer directory, coach roster, reusable workout templates and plan assignment restricted to assigned members.
- Facility check-in/out, session exercise tracking, attendance history, monthly visits, consistency/streak metrics and audited server-calculated fitness-level progression.
- Admin overview, account roles, membership status, membership-plan management, weekly operating hours and dated closures.
- Database-backed cross-tab change detection, API-level validation, consistent JSON errors, JWT expiry, CORS configuration and role-based access control.

## 1. Prerequisites

- Node.js 22 LTS recommended (Next.js 16 requires Node.js 20.9 or newer).
- Java 21 JDK.
- A Neon PostgreSQL database, or a compatible local PostgreSQL database.
- Internet access the first time you install npm and Maven dependencies.

## 2. Configure Neon and backend environment

1. Create a PostgreSQL project in Neon and retrieve its connection details.
2. Open `backend/.env` and replace the placeholders. The ZIP contains **no copied production secrets** from the source project. The API deliberately refuses to start with the example JWT placeholder.
3. Use a JDBC URL of this shape (replace every example value):

   ```dotenv
   JDBC_DATABASE_URL=jdbc:postgresql://YOUR-NEON-HOST/YOUR-DATABASE?sslmode=require
   DB_USERNAME=YOUR_NEON_USERNAME
   DB_PASSWORD=YOUR_NEON_PASSWORD
   JWT_SECRET=REPLACE_WITH_A_RANDOM_SECRET_AT_LEAST_32_BYTES_LONG
   JWT_EXPIRATION_MINUTES=720
   CORS_ALLOWED_ORIGINS=http://localhost:3000
   APP_TIMEZONE=Asia/Kolkata
   APP_SEED_ADMIN_EMAIL=admin@your-domain.example
   APP_SEED_ADMIN_PASSWORD=REPLACE_WITH_A_UNIQUE_PASSWORD_AT_LEAST_12_CHARACTERS
   APP_SEED_ADMIN_NAME=AROHAN Administrator
   DB_POOL_MAX=10
   DB_POOL_MIN=2
   ```

4. Generate a unique random JWT secret (at least 32 bytes). Do not reuse the database password as the JWT secret.
5. `APP_SEED_ADMIN_EMAIL` and `APP_SEED_ADMIN_PASSWORD` are optional, but recommended for the first admin. If set, the backend seeds that admin on startup. The password must contain at least 12 characters. **No default admin account/password is shipped.**
6. Flyway creates the schema automatically at backend startup. Use a fresh/empty database for the first startup; do not point this migration at an unrelated database.

The backend `.env` is parsed as a Spring properties file, so keep each setting on its own line and avoid wrapping values in shell-specific syntax.

## 3. Run the Spring Boot API

In a terminal:

```bash
cd backend
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

If Maven Wrapper cannot download Maven, install Maven 3.9.11 or newer and run `mvn spring-boot:run` instead. The API listens at `http://localhost:8080` by default.

Useful endpoints:

- `GET http://localhost:8080/api/health` — database-aware health check.
- `GET http://localhost:8080/api/gym/status` — public current facility status.
- `GET http://localhost:8080/api/membership-plans` — public active membership catalogue.
- `GET http://localhost:8080/actuator/health` — Spring health endpoint.

## 4. Run the Next.js frontend

Edit `frontend/.env.local` if the API is not hosted at `http://localhost:8080`:

```dotenv
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_APP_NAME=AROHAN Strength Club
```

Then run:

```bash
cd frontend
npm ci
npm run dev
```

Open `http://localhost:3000`. Before sharing or deploying a build, run:

```bash
npm run check
```

The `check` script runs ESLint, the TypeScript type-check, and the Next.js production build in sequence. For a production launch after the checks pass, run `npm run start`.

## 5. Recommended first-start smoke test

1. Start the backend and confirm `/api/health` reports healthy database connectivity.
2. Sign in using the admin values seeded through `APP_SEED_ADMIN_EMAIL` and `APP_SEED_ADMIN_PASSWORD`.
3. Check the three seeded membership plans and update hours/closures for the actual club schedule.
4. Register a separate member account; activate a plan using the clearly labelled demo flow. Set a 3–6 day schedule from the workout page and verify the updated weekly pattern.
5. Confirm a member without an active membership cannot check in, then activate the plan and test check-in, exercise completion and check-out. Open a second tab and confirm changes are detected.
6. Create a trainer account using admin role management; assign the trainer to a member, create a template and assign it.
7. Review the consistency history and exercise the fitness-level evaluation endpoint with seeded/test attendance data.
8. Verify `/api/admin/**`, `/api/trainer/**` and `/api/member/**` reject accounts without the required role.

More detail is in [`docs/API.md`](docs/API.md), [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) and [`docs/DEPLOYMENT_CHECKLIST.md`](docs/DEPLOYMENT_CHECKLIST.md).

## Important production notes

- **Payments are simulated, not charged.** The current activation endpoint is a demo workflow; integrate a real payment provider, webhook signature verification and reconciliation before collecting money.
- Configure HTTPS, production CORS origins, secure database credentials, backups, monitoring and a managed secret store before public deployment.
- **Security watch (checked 11 Oct 2026):** Next.js announced an out-of-band update planned for 14 Oct 2026 for vulnerabilities in upstream dependencies. Before public deployment, review the [official advisory](https://nextjs.org/blog/upcoming-nextjs-security-update-october-2026), upgrade to the patched compatible versions when published, and rerun lint/build checks.
- Add automated tests against PostgreSQL (including role checks and concurrent attendance attempts) and run the full frontend/backend builds in an environment with package-registry access. This delivery environment could not download npm/Maven packages, so a full framework build has not been claimed; see [`docs/VALIDATION_REPORT.md`](docs/VALIDATION_REPORT.md).
- Password reset, email verification, notification delivery, payment-provider integration, formal privacy/retention policy and CI deployment are not represented as implemented features.

## Official technical references

- [Next.js 16.4 release notes](https://nextjs.org/blog/next-16-4)
- [Next.js installation requirements](https://nextjs.org/docs/app/getting-started/installation)
- [Spring Boot releases](https://github.com/spring-projects/spring-boot/releases)
- [Spring Security OAuth 2.0 Resource Server / JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Neon connection strings](https://neon.tech/docs/connect/connect-from-any-app)
- [Flyway migrations](https://documentation.red-gate.com/flyway/flyway-concepts/migrations)
