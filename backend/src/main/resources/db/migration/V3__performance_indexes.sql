-- Performance indexes for the hottest access paths. Safe to re-run (IF NOT EXISTS).

-- Case-insensitive account lookup (Spring Data findByEmailIgnoreCase uses upper(email)).
CREATE INDEX IF NOT EXISTS idx_users_email_upper ON users (upper(email));

-- Child rows are always loaded by their parent id.
CREATE INDEX IF NOT EXISTS idx_workout_exercises_day ON workout_exercises (day_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_attendance_exercises_attendance ON attendance_exercises (attendance_id, sort_order);

-- Attendance: history/active-session/dashboard/overview queries.
CREATE INDEX IF NOT EXISTS idx_attendance_user_checkin ON attendances (user_id, check_in_time DESC);
CREATE INDEX IF NOT EXISTS idx_attendance_user_status ON attendances (user_id, status);
CREATE INDEX IF NOT EXISTS idx_attendance_facility_date ON attendances (facility_date);

-- Trainer roster, pending trainer requests, membership expiry sweep and admin counts.
CREATE INDEX IF NOT EXISTS idx_fitness_profiles_trainer ON fitness_profiles (trainer_id);
CREATE INDEX IF NOT EXISTS idx_fitness_profiles_membership ON fitness_profiles (membership_status, membership_expiry);

-- Foreign key without an index (payments by plan).
CREATE INDEX IF NOT EXISTS idx_payment_plan ON membership_payments (plan_id);
