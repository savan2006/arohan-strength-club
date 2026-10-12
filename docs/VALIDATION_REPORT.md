# Source Review and Validation Report

**Review date:** October 11, 2026

## Changes included in this review

- Standardized formatting, imports, and naming across backend configuration, controllers, DTOs, entities, repositories, and core services; added focused comments around non-obvious business and security rules.
- Removed unused service-constructor dependencies and normalized Spring configuration for the server port, JWT secret, and optional environment-file imports.
- Made the JWT signing secret fail closed when missing or set to common placeholders.
- Kept API authentication failures JSON-based and protected unclassified `/api/**` routes by default.
- Added validation for malformed workout-list items and trainer specialization input. Admin schedule updates reject incomplete weeks and duplicate weekdays before changing database records.
- Prevented routine demotion of the last administrator and made trainer-member assignments safer when a trainer is demoted or deleted.
- Reduced trainer-roster database round trips by loading attendance data in batches rather than querying attendance once per member.
- Improved frontend bootstrap latency by requesting identity and section data concurrently, added a bounded API timeout and clearer network errors, corrected role-based navigation, and prevented stale responses from overwriting newly selected sections.
- Removed the duplicate CSS font import; Google Fonts now load through the root layout with preconnect hints.
- Improved keyboard focus visibility, touch targets, form readability, mobile schedule controls, dashboard metric/table typography, and scroll locking behind the mobile navigation drawer. Added support for the operating system's reduced-motion preference.
- Added a branded route error boundary with a retry action and a lightweight loading fallback for portal route transitions.
- Added `npm run typecheck` and `npm run check` (ESLint → TypeScript type-check → Next.js production build) so pre-release checks can be repeated with one command.
- Added regression tests for last-administrator protection and duplicate schedule weekdays.
- Kept the npm dependency lockfile unchanged and excluded generated build output, IDE metadata, dependency folders, and real environment files from the deliverable.

## Checks completed in this environment

- Static Java syntax review: **70 Java source/test files reviewed with no syntax diagnostics reported by the prior parser check**. This does not resolve or validate Spring/JUnit dependencies and is not a successful Java compilation.
- TypeScript/JavaScript parser check: **14 source/configuration files parsed with 0 syntax errors**. This is not a dependency-backed semantic type check.
- JSON validation: **3 JSON files parsed successfully**. The root dependency versions in `frontend/package.json` match `frontend/package-lock.json`.
- CSS parser check: **887 rules and 2,787 declarations parsed successfully with 0 parse errors**.
- Configuration syntax: `backend/pom.xml` parsed as XML and `backend/src/main/resources/application.yml` parsed as YAML.
- Secret-file handling: no real `.env`, `.env.local`, or other environment-specific files are present in the deliverable. The two `.env.example` templates are the only environment files shipped; real environment-file contents were not opened.
- Archive hygiene: generated TypeScript declarations, partial dependency installation, and build output are excluded.

## Checks not completed

Dependency installation could not complete in this environment. `npm ci` timed out; the partial `node_modules` directory was removed from the final copy. The global TypeScript check therefore reported missing React, Node, and schema type packages. The Maven wrapper also could not download Maven 3.9.11 from Maven Central. As a result, the dependency-backed TypeScript type-check, ESLint run, Next.js production build, Maven compilation, and JUnit test execution **remain unverified**. The added unit tests are included but have not been executed here.

Run these commands on a machine with access to the npm and Maven registries:

```bash
# Frontend
cd frontend
npm ci
npm run check

# Backend (from the repository root)
cd ../backend
./mvnw test
```

On Windows PowerShell, use `.\mvnw.cmd test` from the `backend` folder. The application should also be smoke-tested against a dedicated PostgreSQL test database before deployment.

## Known integration and deployment boundaries

- Membership payment activation is a demo simulation, not a live payment integration. Do not accept real payments until a gateway, webhook signature verification, idempotency, and reconciliation are implemented and tested.
- Password reset, email verification, notification delivery, a formal privacy/retention policy, and CI deployment are not represented as implemented features.
- Before public deployment, configure HTTPS, production CORS origins, managed secrets, database backups, monitoring, and appropriate log retention. Review the current Next.js security advisory in `README.md` and install the compatible security update when it is available.
