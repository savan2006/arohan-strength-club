package com.arohan.strengthclub.entity;

import com.arohan.strengthclub.entity.enums.ExperienceLevel;
import com.arohan.strengthclub.entity.enums.FitnessGoal;
import com.arohan.strengthclub.entity.enums.MembershipStatus;
import com.arohan.strengthclub.entity.enums.WorkoutSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Member preferences, goals, membership dates, and trainer assignment state. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fitness_profiles", uniqueConstraints =
        @UniqueConstraint(name = "uk_fitness_profile_user", columnNames = "user_id"))
public class FitnessProfile {

    @Id
    @UuidGenerator
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id")
    private User trainer;

    @Column(name = "trainer_requested", nullable = false)
    private boolean trainerRequested = false;

    @Column(name = "wants_trainer", nullable = false)
    private boolean wantsTrainer = false;

    @Column(nullable = false)
    private int age = 25;

    @Column(name = "height_cm", nullable = false)
    private int heightCm = 175;

    @Column(name = "weight_kg", nullable = false)
    private double weightKg = 72.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "fitness_goal", nullable = false, length = 30)
    private FitnessGoal fitnessGoal = FitnessGoal.GENERAL_FITNESS;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", nullable = false, length = 20)
    private ExperienceLevel experienceLevel = ExperienceLevel.BEGINNER;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_level", nullable = false, length = 20)
    private ExperienceLevel currentLevel = ExperienceLevel.BEGINNER;

    @Enumerated(EnumType.STRING)
    @Column(name = "initial_level", nullable = false, length = 20)
    private ExperienceLevel initialLevel = ExperienceLevel.BEGINNER;

    @Column(name = "level_since", nullable = false)
    private Instant levelSince = Instant.now();

    @Column(name = "planned_days_per_week", nullable = false)
    private int plannedDaysPerWeek = 4;

    @Column(name = "preferred_schedule", nullable = false, length = 30)
    private String preferredSchedule = "EVENING";

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false, length = 20)
    private MembershipStatus membershipStatus = MembershipStatus.EXPIRED;

    @Column(name = "membership_start")
    private LocalDate membershipStart;

    @Column(name = "membership_expiry")
    private LocalDate membershipExpiry;

    @Enumerated(EnumType.STRING)
    @Column(name = "active_workout_source", nullable = false, length = 20)
    private WorkoutSource activeWorkoutSource = WorkoutSource.RECOMMENDED;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }
}
