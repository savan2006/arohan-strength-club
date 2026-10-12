-- Ensure the attendance exercise snapshot table exists.
-- This migration is safe if V1 already created the table.

CREATE TABLE IF NOT EXISTS attendance_exercises (
                                                    id UUID PRIMARY KEY,

                                                    attendance_id UUID NOT NULL
                                                    REFERENCES attendances(id)
    ON DELETE CASCADE,

    source_exercise_id VARCHAR(36),

    exercise_name VARCHAR(120) NOT NULL,

    sets INTEGER NOT NULL,

    reps VARCHAR(30) NOT NULL,

    rest_seconds INTEGER NOT NULL,

    is_completed BOOLEAN NOT NULL DEFAULT FALSE,

    completed_at TIMESTAMPTZ,

    image_url VARCHAR(300) NOT NULL,

    sort_order INTEGER NOT NULL
    );