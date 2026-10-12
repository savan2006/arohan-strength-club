CREATE TABLE users (
    id uuid PRIMARY KEY,
    full_name varchar(120) NOT NULL,
    email varchar(254) NOT NULL UNIQUE,
    phone varchar(20) NOT NULL,
    password_hash varchar(100) NOT NULL,
    role varchar(20) NOT NULL,
    specialization varchar(120) NOT NULL DEFAULT '',
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
CREATE INDEX idx_users_role ON users(role);

CREATE TABLE sync_events (
    id bigserial PRIMARY KEY,
    actor_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    category varchar(40) NOT NULL,
    initiator_tab_id varchar(80),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_sync_event_created_at ON sync_events(created_at);

CREATE TABLE fitness_profiles (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    trainer_id uuid REFERENCES users(id) ON DELETE SET NULL,
    trainer_requested boolean NOT NULL DEFAULT false,
    wants_trainer boolean NOT NULL DEFAULT false,
    age integer NOT NULL,
    height_cm integer NOT NULL,
    weight_kg double precision NOT NULL,
    fitness_goal varchar(30) NOT NULL,
    experience_level varchar(20) NOT NULL,
    current_level varchar(20) NOT NULL,
    initial_level varchar(20) NOT NULL,
    level_since timestamptz NOT NULL,
    planned_days_per_week integer NOT NULL,
    preferred_schedule varchar(30) NOT NULL,
    membership_status varchar(20) NOT NULL,
    membership_start date,
    membership_expiry date,
    active_workout_source varchar(20) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE TABLE fitness_level_promotions (
    id uuid PRIMARY KEY,
    profile_id uuid NOT NULL REFERENCES fitness_profiles(id) ON DELETE CASCADE,
    from_level varchar(20) NOT NULL,
    to_level varchar(20) NOT NULL,
    promoted_at timestamptz NOT NULL,
    reason varchar(240) NOT NULL
);
CREATE INDEX idx_level_promotion_profile_date ON fitness_level_promotions(profile_id, promoted_at DESC);

CREATE TABLE membership_plans (
    id uuid PRIMARY KEY,
    name varchar(80) NOT NULL UNIQUE,
    duration_months integer NOT NULL,
    price numeric(10,2) NOT NULL,
    description varchar(500) NOT NULL DEFAULT '',
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
CREATE TABLE membership_plan_features (
    plan_id uuid NOT NULL REFERENCES membership_plans(id) ON DELETE CASCADE,
    feature varchar(160) NOT NULL,
    feature_order integer NOT NULL,
    PRIMARY KEY(plan_id, feature_order)
);

CREATE TABLE membership_payments (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id),
    plan_id uuid NOT NULL REFERENCES membership_plans(id),
    plan_name varchar(80) NOT NULL,
    plan_duration_months integer NOT NULL,
    amount numeric(10,2) NOT NULL,
    status varchar(20) NOT NULL,
    payment_date timestamptz NOT NULL,
    start_date date NOT NULL,
    expiry_date date NOT NULL
);
CREATE INDEX idx_payment_user_date ON membership_payments(user_id, payment_date);

CREATE TABLE workout_plans (
    id uuid PRIMARY KEY,
    name varchar(120) NOT NULL,
    description varchar(600) NOT NULL DEFAULT '',
    source varchar(20) NOT NULL,
    fitness_goal varchar(30) NOT NULL,
    created_by_id uuid NOT NULL REFERENCES users(id),
    assigned_to_id uuid REFERENCES users(id) ON DELETE CASCADE,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
CREATE INDEX idx_workout_plan_assigned_active ON workout_plans(assigned_to_id, is_active);
CREATE INDEX idx_workout_plan_creator ON workout_plans(created_by_id);
CREATE TABLE workout_days (
    id uuid PRIMARY KEY,
    plan_id uuid NOT NULL REFERENCES workout_plans(id) ON DELETE CASCADE,
    day_number integer NOT NULL,
    day_name varchar(24) NOT NULL,
    focus varchar(100) NOT NULL,
    day_type varchar(20) NOT NULL,
    CONSTRAINT uk_plan_day UNIQUE(plan_id, day_number)
);
CREATE TABLE workout_exercises (
    id uuid PRIMARY KEY,
    day_id uuid NOT NULL REFERENCES workout_days(id) ON DELETE CASCADE,
    exercise_name varchar(120) NOT NULL,
    muscle_group varchar(60) NOT NULL,
    sets integer NOT NULL,
    reps varchar(30) NOT NULL,
    rest_seconds integer NOT NULL,
    instructions varchar(1200) NOT NULL,
    image_url varchar(300) NOT NULL,
    sort_order integer NOT NULL
);

CREATE TABLE attendances (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    check_in_time timestamptz NOT NULL,
    check_out_time timestamptz,
    duration_minutes integer NOT NULL DEFAULT 0,
    estimated_duration_minutes integer NOT NULL DEFAULT 45,
    status varchar(20) NOT NULL,
    facility_date date NOT NULL,
    workout_day_number integer,
    workout_day_name varchar(24)
);
CREATE INDEX idx_attendance_user_date ON attendances(user_id, facility_date);
CREATE INDEX idx_attendance_status ON attendances(status);
-- Enforce the one-session-per-facility-day rule atomically, even under concurrent check-in requests.
CREATE UNIQUE INDEX uq_attendance_one_session_per_day
    ON attendances(user_id, facility_date)
    WHERE status IN ('ACTIVE', 'COMPLETED');
CREATE TABLE attendance_exercises (
    id uuid PRIMARY KEY,
    attendance_id uuid NOT NULL REFERENCES attendances(id) ON DELETE CASCADE,
    source_exercise_id varchar(36),
    exercise_name varchar(120) NOT NULL,
    sets integer NOT NULL,
    reps varchar(30) NOT NULL,
    rest_seconds integer NOT NULL,
    is_completed boolean NOT NULL DEFAULT false,
    completed_at timestamptz,
    image_url varchar(300) NOT NULL,
    sort_order integer NOT NULL
);

CREATE TABLE gym_operating_hours (
    id uuid PRIMARY KEY,
    day_of_week varchar(12) NOT NULL UNIQUE,
    open boolean NOT NULL DEFAULT true,
    opening_time time NOT NULL,
    closing_time time NOT NULL,
    notes varchar(300) NOT NULL DEFAULT ''
);
CREATE TABLE gym_closures (
    id uuid PRIMARY KEY,
    closure_date date NOT NULL UNIQUE,
    reason varchar(200) NOT NULL,
    announcement varchar(600) NOT NULL DEFAULT '',
    created_by_id uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL
);
CREATE INDEX idx_gym_closure_date ON gym_closures(closure_date);
