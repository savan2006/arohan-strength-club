package com.arohan.strengthclub.dto;

import com.arohan.strengthclub.entity.FitnessProfile;
import com.arohan.strengthclub.entity.MembershipPayment;
import com.arohan.strengthclub.entity.User;
import com.arohan.strengthclub.entity.enums.ExperienceLevel;
import com.arohan.strengthclub.entity.enums.FitnessGoal;
import com.arohan.strengthclub.entity.enums.MembershipStatus;
import com.arohan.strengthclub.entity.enums.WorkoutSource;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Request and response contracts for member profile, membership, and progress features. */
public final class MemberDtos {

    private MemberDtos() {
    }

    public record ProfileResponse(
            UUID id,
            String fullName,
            String email,
            String phone,
            int age,
            int heightCm,
            double weightKg,
            FitnessGoal fitnessGoal,
            ExperienceLevel experienceLevel,
            ExperienceLevel currentLevel,
            int plannedDaysPerWeek,
            String preferredSchedule,
            MembershipStatus membershipStatus,
            LocalDate membershipStart,
            LocalDate membershipExpiry,
            UUID trainerId,
            String trainerName,
            boolean trainerRequested,
            boolean wantsTrainer,
            WorkoutSource activeWorkoutSource,
            String currentMembershipPlan,
            int accumulatedDurationMonths
    ) {

        public static ProfileResponse from(
                User user,
                FitnessProfile profile
        ) {
            return from(user, profile, null, 0);
        }

        public static ProfileResponse from(
                User user,
                FitnessProfile profile,
                String currentMembershipPlan,
                int accumulatedDurationMonths
        ) {
            return new ProfileResponse(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    profile.getAge(),
                    profile.getHeightCm(),
                    profile.getWeightKg(),
                    profile.getFitnessGoal(),
                    profile.getExperienceLevel(),
                    profile.getCurrentLevel(),
                    profile.getPlannedDaysPerWeek(),
                    profile.getPreferredSchedule(),
                    profile.getMembershipStatus(),
                    profile.getMembershipStart(),
                    profile.getMembershipExpiry(),
                    profile.getTrainer() == null
                            ? null
                            : profile.getTrainer().getId(),
                    profile.getTrainer() == null
                            ? null
                            : profile.getTrainer().getFullName(),
                    profile.isTrainerRequested(),
                    profile.isWantsTrainer(),
                    profile.getActiveWorkoutSource(),
                    currentMembershipPlan,
                    accumulatedDurationMonths
            );
        }
    }

    public record ProfileUpdateRequest(
            @NotBlank
            @Size(max = 120)
            String fullName,

            @NotBlank
            @Pattern(regexp = "[0-9+()\\- ]{8,20}")
            String phone,

            @Min(14)
            @Max(100)
            int age,

            @Min(50)
            @Max(260)
            int heightCm,

            @DecimalMin("30.0")
            @DecimalMax("300.0")
            double weightKg,

            @NotNull
            FitnessGoal fitnessGoal,

            @NotNull
            ExperienceLevel experienceLevel,

            @Min(3)
            @Max(6)
            int plannedDaysPerWeek,

            @Pattern(
                    regexp = "MORNING|AFTERNOON|EVENING|FLEXIBLE"
            )
            String preferredSchedule
    ) {
    }

    public record DashboardResponse(
            String greeting,
            String membershipStatus,
            LocalDate membershipExpiry,
            int monthlyVisits,
            int workoutMinutesThisMonth,
            int streak,
            int plannedDaysPerWeek,
            double consistencyPercent,
            boolean checkedIn,
            String activeWorkoutName,
            long totalExercises,
            long completedExercises,
            String gymStatus
    ) {
    }

    public record TrainerSummary(
            UUID id,
            String fullName,
            String email,
            String specialization
    ) {

        public static TrainerSummary from(User user) {
            return new TrainerSummary(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getSpecialization()
            );
        }
    }

    public record TrainerPreferenceRequest(
            boolean wantsTrainer,
            UUID trainerId
    ) {
    }

    public record PaymentRequest(
            @NotNull UUID planId
    ) {
    }

    public record PaymentResponse(
            UUID id,
            String planName,
            String amount,
            LocalDate startDate,
            LocalDate expiryDate,
            String status
    ) {

        public static PaymentResponse from(MembershipPayment payment) {
            return new PaymentResponse(
                    payment.getId(),
                    payment.getPlanName(),
                    payment.getAmount().toPlainString(),
                    payment.getStartDate(),
                    payment.getExpiryDate(),
                    payment.getStatus().name()
            );
        }
    }

    public record ConsistencyResponse(
            int plannedDaysPerWeek,
            int completedWorkouts,
            int expectedWorkouts,
            double percentage,
            int workoutMinutesThisMonth,
            int currentStreak,
            int visitsThisMonth
    ) {
    }

    public record MonthlyConsistencyResponse(
            String month,
            double percentage,
            int completedWorkouts,
            int expectedWorkouts
    ) {
    }

    public record FitnessLevelPromotionResponse(
            UUID id,
            ExperienceLevel fromLevel,
            ExperienceLevel toLevel,
            java.time.Instant promotedAt,
            String reason
    ) {
    }

    public record FitnessLevelResponse(
            ExperienceLevel currentLevel,
            ExperienceLevel initialLevel,
            java.time.Instant levelSince,
            List<FitnessLevelPromotionResponse> promotionHistory,
            List<MonthlyConsistencyResponse> monthlyHistory,
            int consecutive90,
            int consecutive80,
            boolean promoted,
            String reason
    ) {
    }
}