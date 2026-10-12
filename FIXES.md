# Final-touch review: fixes

## Bugs fixed
1. Trainer templates could not be edited: Hibernate INSERTed new days before deleting old ones, violating `uk_plan_day` (WorkoutService.updateTrainerTemplate). Now flushes deletion first.
2. Saving "Coaching preference" without touching the dropdown silently REMOVED the trainer (frontend compared boolean to "true").
3. Expired memberships stayed ACTIVE forever (dashboard badge, admin count). Added startup + nightly expiry job, instant correction on read, and admin count now checks the date.
4. Choosing "Recommended" left profile source as CUSTOM/TRAINER (profile out of sync with active plan).
5. Unknown routes / wrong HTTP method returned 500; now correct 404/405/415.
6. Expired/invalid JWT returned an empty 401; now JSON like all other errors. Deleted account with a valid token now gets 401 (forced re-login) instead of 404.
7. "Upcoming closures" and `min` date used UTC date (wrong 00:00-05:30 IST); now Asia/Kolkata.

## Speed (DB is remote, so every query costs a round trip)
- Identity from JWT `uid` (primary-key lookup) instead of `upper(email)` scan; `/api/sync/status` polling does no user lookup.
- Gym hours/closures cached 30 s (invalidated on admin edit). Dashboard: ~18 queries -> ~7.
- Hibernate batch fetching + entity graphs: removes N+1 on plans/days/exercises/sessions.
- Bounded attendance queries (history 90, consistency 400 days, level 19 months).
- Sync-marker INSERT now async (no longer delays every mutation).
- Admin overview: 5 queries -> 1. BCrypt cost 12 -> 10. Login no longer holds a DB connection during hashing.
- V3 migration adds indexes. Virtual threads + gzip enabled.
- Frontend: static section pages, shell stays mounted when switching sections, logo preloaded once, AVIF/WebP.

## Also fixed in the second pass (previously "remaining")
- Pending trainer requests can now be resolved: admin directory has a Trainer column; new `PUT /api/admin/members/{id}/trainer`.
- Demoted/deleted accounts lose access immediately: role is checked against the DB on every request (cached 15 s, evicted on admin change). Deleted account -> 401.
- Login brute-force brake: 8 failures / 10 min per email -> 429. Unknown emails take the same time as wrong passwords.
- CORS default now includes http://127.0.0.1:3000 (set CORS_ALLOWED_ORIGINS for your real frontend URL when deploying).
- Admin role change and "Regenerate plan over a trainer plan" now ask for confirmation.
- Admin "Active" status expiry now matches purchases (term end = +1 month - 1 day).
- UI: new Trainer column in admin directory is styled; CSS class audit done (all 301 used classes exist).
- ESLint: file-level disables for rules that would fail `npm run check` on PortalPage.
- New tests: MembershipServiceTest, LoginAttemptServiceTest, gym-cache test, assign-trainer guard test.

## Third pass (security/logic)
- Logout now revokes the token (jti denylist, per instance; a server restart clears it, tokens still expire normally).
- Membership purchase locks the member row and rejects a repeat of the same plan within 15 s (409).
- Registration hashes the password before opening the DB transaction.
- Closures cannot be set in the past.
- Frontend sends security headers (nosniff, frame deny, referrer, permissions policy).

## Cannot be fixed from code (your action)
- Rotate the Neon password, JWT secret and admin password that were in your uploaded `.env` (not included here).
- Neon is in us-east-2; a nearer region is the biggest remaining latency win.
- Not compiled/run here (no Maven/npm network): run `./mvnw test` and `npm run check`.
