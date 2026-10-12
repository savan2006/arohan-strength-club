package com.arohan.strengthclub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/** Exercise snapshot stored with an attendance session. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "attendance_exercises")
public class AttendanceExercise {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_id", nullable = false)
    private Attendance attendance;

    @Column(name = "source_exercise_id", length = 36)
    private String sourceExerciseId;

    @Column(name = "exercise_name", nullable = false, length = 120)
    private String exerciseName;

    @Column(nullable = false)
    private int sets = 3;

    @Column(nullable = false, length = 30)
    private String reps = "10-12";

    @Column(name = "rest_seconds", nullable = false)
    private int restSeconds = 60;

    @Column(name = "is_completed", nullable = false)
    private boolean completed = false;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "image_url", nullable = false, length = 300)
    private String imageUrl = "";

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
