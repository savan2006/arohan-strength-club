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

import java.util.UUID;

/** An exercise prescription inside a workout day. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workout_exercises")
public class WorkoutExercise {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "day_id", nullable = false)
    private WorkoutDay day;

    @Column(name = "exercise_name", nullable = false, length = 120)
    private String exerciseName;

    @Column(name = "muscle_group", nullable = false, length = 60)
    private String muscleGroup = "Full Body";

    @Column(nullable = false)
    private int sets = 3;

    @Column(nullable = false, length = 30)
    private String reps = "10-12";

    @Column(name = "rest_seconds", nullable = false)
    private int restSeconds = 60;

    @Column(nullable = false, length = 1200)
    private String instructions = "";

    @Column(name = "image_url", nullable = false, length = 300)
    private String imageUrl = "";

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
