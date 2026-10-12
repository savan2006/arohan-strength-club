package com.arohan.strengthclub.entity;

import com.arohan.strengthclub.entity.enums.DayType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A workout or recovery day belonging to a plan. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workout_days", uniqueConstraints =
        @UniqueConstraint(name = "uk_plan_day", columnNames = {"plan_id", "day_number"}))
public class WorkoutDay {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private WorkoutPlan plan;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(name = "day_name", nullable = false, length = 24)
    private String dayName;

    @Column(nullable = false, length = 100)
    private String focus = "Full Body";

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false, length = 20)
    private DayType dayType = DayType.WORKOUT;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<WorkoutExercise> exercises = new ArrayList<>();

    public void addExercise(WorkoutExercise exercise) {
        exercises.add(exercise);
        exercise.setDay(this);
    }
}
