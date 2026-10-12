# API reference

Base URL for local work: `http://localhost:8080`. JSON is used for request and response bodies. Protected endpoints accept `Authorization: Bearer <token>`. Success/error payloads use DTO records / JSON maps rather than raw entities.

## Authentication and public catalogue

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Register a member and fitness profile; returns token and user |
| `POST` | `/api/auth/login` | Authenticate an account; returns token and user |
| `GET` | `/api/auth/me` | Resolve the signed-in account |
| `GET` | `/api/health` | API and database health |
| `GET` | `/api/gym/status` | Public opening/closure status |
| `GET` | `/api/membership-plans` | Active plan catalogue |

## Member

All endpoints below require `MEMBER` role.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/member/dashboard` | Member summary and current status |
| `GET`, `PUT` | `/api/member/profile` | Read/update fitness profile |
| `GET` | `/api/member/trainers` | Available trainer accounts |
| `PATCH` | `/api/member/trainer-preference` | Set or remove trainer preference |
| `GET` | `/api/member/consistency` | Visits, completed days, streak and consistency |
| `GET` | `/api/member/workout-plan` | Read active plan (recommended plan is generated when needed) |
| `POST` | `/api/member/workout-plan` | Create a custom member plan |
| `POST` | `/api/member/workout-plan/generate` | Generate/reselect the recommended plan |
| `PATCH` | `/api/member/workout-plan/day/{dayNumber}/toggle-complete` | Record a completed workout day |
| `PATCH` | `/api/member/workout-source?source=RECOMMENDED` | Change active plan source |
| `PUT` | `/api/member/weekly-schedule` | Update seven-day training/recovery split (3–6 workout days) |
| `GET` | `/api/member/fitness-level` | Read monthly consistency history and promotion record |
| `POST` | `/api/member/fitness-level/check-promotion` | Evaluate server-calculated consistency for a level promotion |
| `GET` | `/api/member/attendance` | Attendance history |
| `GET` | `/api/member/attendance/current-session` | Current active session, if any |
| `POST` | `/api/member/attendance/check-in` | Check in during facility hours with valid membership |
| `POST` | `/api/member/attendance/check-out` | Complete an active session |
| `PATCH` | `/api/member/attendance/active/exercise/{exerciseId}/toggle` | Toggle an exercise in the active session |
| `POST` | `/api/member/membership/pay` | **Demo only:** simulate plan activation |

## Synchronisation

Requires an authenticated account. The browser polls with `clientVersion` and sends its per-tab `X-Tab-Session-Id` header. Events are stored in PostgreSQL so changes can be detected across application instances.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/sync/status?clientVersion={id}` | Detect relevant changes made elsewhere; does not return private record data |

## Trainer

Requires `TRAINER` or `ADMIN` role; ownership/assignment checks still apply in service logic.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/trainer/members` | Assigned member roster |
| `GET` | `/api/trainer/members/{memberId}` | Assigned member details |
| `POST` | `/api/trainer/members/{memberId}/assign-plan` | Assign an owned template |
| `PUT` | `/api/trainer/members/{memberId}/workout-plan` | Update a member plan |
| `GET`, `POST` | `/api/trainer/plans` | List/create trainer-owned templates |
| `GET`, `PUT`, `DELETE` | `/api/trainer/plans/{planId}` | Read/update/delete an owned template |

## Admin

Requires `ADMIN` role.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/admin/system-overview` | Operational summary |
| `GET` | `/api/admin/users` | Account directory |
| `PATCH` | `/api/admin/users/{userId}` | Change role/specialization |
| `PATCH` | `/api/admin/users/{userId}/status` | Change membership status |
| `DELETE` | `/api/admin/users/{userId}` | Delete another user (self-delete blocked) |
| `GET`, `PUT` | `/api/admin/schedule` | Read/update weekly operating hours |
| `GET`, `POST` | `/api/admin/closures` | List/create closure dates |
| `PUT`, `DELETE` | `/api/admin/closures/{id}` | Update/delete closure |
| `GET`, `POST` | `/api/admin/plans` | List/create membership plans |
| `PUT` | `/api/admin/plans/{id}` | Update/enable/disable a membership plan |

## Error format

Validation and business errors return JSON with fields similar to:

```json
{
  "timestamp": "2026-01-01T10:30:00Z",
  "error": "ACTIVE_MEMBERSHIP_REQUIRED",
  "message": "An active membership is required for this action."
}
```

Validation errors also include a `details` object keyed by field. Common statuses are `400` for invalid input/rules, `401` for unauthenticated, `403` for forbidden role/access, `404` for missing records, `409` for data conflicts and `500` for unexpected errors. Internal exception details are intentionally not exposed.


## Additions in the final-touch review

| Method | Path | Role | Purpose |
|---|---|---|---|
| PUT | `/api/admin/members/{memberId}/trainer` | ADMIN | Body `{ "trainerId": "<uuid>" }` assigns a trainer (resolves a pending request); `{ "trainerId": null }` removes the trainer. |

`GET /api/admin/users` now also returns `trainerId`, `trainerName`, `trainerRequested` per user.
Error `429 TOO_MANY_ATTEMPTS` is returned by `POST /api/auth/login` after 8 failed attempts for one email within 10 minutes.
Roles are checked against the database on every request (cached 15 s), so role changes and deletions apply immediately.

| POST | `/api/auth/logout` | any signed-in | Revokes the presented token (per server instance; lasts until the token's own expiry). |

`POST /api/member/membership/pay` returns `409 DUPLICATE_PAYMENT` if the same plan was bought in the last 15 seconds. Closures cannot be created or edited to a past date (`CLOSURE_IN_PAST`).
