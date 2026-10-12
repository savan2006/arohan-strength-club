package com.arohan.strengthclub.dto;

import com.arohan.strengthclub.entity.WorkoutDay;
import com.arohan.strengthclub.entity.WorkoutExercise;
import com.arohan.strengthclub.entity.WorkoutPlan;
import com.arohan.strengthclub.entity.enums.DayType;
import com.arohan.strengthclub.entity.enums.FitnessGoal;
import com.arohan.strengthclub.entity.enums.WorkoutSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Request and response DTOs for workout plans, days, and exercise prescriptions. */
public final class WorkoutDtos {

    private WorkoutDtos() {
    }

    public record ExerciseRequest(
            @NotBlank @Size(max = 120) String exerciseName,
            @NotBlank @Size(max = 60) String muscleGroup,
            @Min(1) @Max(20) int sets,
            @NotBlank @Size(max = 30) String reps,
            @Min(0) @Max(600) int restSeconds,
            @Size(max = 1200) String instructions,
            @Size(max = 300) String imageUrl) {
    }

    public record DayRequest(
            @Min(1) @Max(7) int dayNumber,
            @NotBlank @Size(max = 24) String dayName,
            @NotBlank @Size(max = 100) String focus,
            @NotNull DayType dayType,
            List<@NotNull @Valid ExerciseRequest> exercises) {
    }

    public record WeeklyDayRequest(
            @Min(1) @Max(7) int dayNumber,
            @NotBlank @Size(max = 24) String dayName,
            @NotBlank @Size(max = 100) String focus,
            @NotNull DayType dayType) {
    }

    public record WeeklyScheduleRequest(
            @NotNull @Size(min = 7, max = 7) List<@NotNull @Valid WeeklyDayRequest> weekSchedule) {
    }

    public record PlanRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 600) String description,
            @NotNull FitnessGoal fitnessGoal,
            @NotEmpty @Size(max = 7) List<@NotNull @Valid DayRequest> days) {
    }

    public record ExerciseResponse(
            UUID id,
            String exerciseName,
            String muscleGroup,
            int sets,
            String reps,
            int restSeconds,
            String instructions,
            String imageUrl,
            int sortOrder) {

        public static ExerciseResponse from(WorkoutExercise exercise) {
            return new ExerciseResponse(
                    exercise.getId(),
                    exercise.getExerciseName(),
                    exercise.getMuscleGroup(),
                    exercise.getSets(),
                    exercise.getReps(),
                    exercise.getRestSeconds(),
                    exercise.getInstructions(),
                    exercise.getImageUrl(),
                    exercise.getSortOrder());
        }
    }

    public record DayResponse(
            UUID id,
            int dayNumber,
            String dayName,
            String focus,
            DayType dayType,
            List<ExerciseResponse> exercises) {

        public static DayResponse from(WorkoutDay day) {
            return new DayResponse(
                    day.getId(),
                    day.getDayNumber(),
                    day.getDayName(),
                    day.getFocus(),
                    day.getDayType(),
                    day.getExercises().stream().map(ExerciseResponse::from).toList());
        }
    }

    public record PlanResponse(
            UUID id,
            String name,
            String description,
            WorkoutSource source,
            FitnessGoal fitnessGoal,
            boolean active,
            UUID assignedToId,
            String assignedToName,
            Instant updatedAt,
            List<DayResponse> days) {

        public static PlanResponse from(WorkoutPlan plan) {
            return new PlanResponse(
                    plan.getId(),
                    plan.getName(),
                    plan.getDescription(),
                    plan.getSource(),
                    plan.getFitnessGoal(),
                    plan.isActive(),
                    plan.getAssignedTo() == null ? null : plan.getAssignedTo().getId(),
                    plan.getAssignedTo() == null ? null : plan.getAssignedTo().getFullName(),
                    plan.getUpdatedAt(),
                    plan.getDays().stream().map(DayResponse::from).toList());
        }
    }
}
