package com.arohan.strengthclub.entity;

import com.arohan.strengthclub.entity.enums.FitnessGoal;
import com.arohan.strengthclub.entity.enums.WorkoutSource;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Workout plan created by the system, a member, or a trainer. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "workout_plans", indexes = {
        @Index(name = "idx_workout_plan_assigned_active", columnList = "assigned_to_id,is_active"),
        @Index(name = "idx_workout_plan_creator", columnList = "created_by_id")
})
public class WorkoutPlan {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 600)
    private String description = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkoutSource source = WorkoutSource.RECOMMENDED;

    @Enumerated(EnumType.STRING)
    @Column(name = "fitness_goal", nullable = false, length = 30)
    private FitnessGoal fitnessGoal = FitnessGoal.GENERAL_FITNESS;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC")
    private List<WorkoutDay> days = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public void addDay(WorkoutDay day) {
        days.add(day);
        day.setPlan(this);
    }
}
