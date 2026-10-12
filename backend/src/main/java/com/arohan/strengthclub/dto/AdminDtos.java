package com.arohan.strengthclub.dto;

import com.arohan.strengthclub.entity.FitnessProfile;
import com.arohan.strengthclub.entity.GymClosure;
import com.arohan.strengthclub.entity.MembershipPlan;
import com.arohan.strengthclub.entity.User;
import com.arohan.strengthclub.entity.enums.MembershipStatus;
import com.arohan.strengthclub.entity.enums.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Request and response contracts for administration endpoints. */
public final class AdminDtos {

    private AdminDtos() {
    }

    public record OverviewResponse(
            long totalMembers,
            long totalTrainers,
            long activeMemberships,
            long visitsToday,
            long pendingTrainerRequests,
            LocalDate date) {
    }

    public record AdminUserResponse(
            UUID id,
            String fullName,
            String email,
            String phone,
            Role role,
            String specialization,
            MembershipStatus membershipStatus,
            LocalDate membershipStart,
            LocalDate membershipExpiry,
            Instant createdAt,
            UUID trainerId,
            String trainerName,
            boolean trainerRequested) {

        public static AdminUserResponse from(User user, FitnessProfile profile) {
            return new AdminUserResponse(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRole(),
                    user.getSpecialization(),
                    profile == null ? null : profile.getMembershipStatus(),
                    profile == null ? null : profile.getMembershipStart(),
                    profile == null ? null : profile.getMembershipExpiry(),
                    user.getCreatedAt(),
                    profile == null || profile.getTrainer() == null ? null : profile.getTrainer().getId(),
                    profile == null || profile.getTrainer() == null ? null : profile.getTrainer().getFullName(),
                    profile != null && profile.isTrainerRequested());
        }
    }

    public record UpdateUserRequest(
            Role role,
            MembershipStatus membershipStatus,
            @Size(max = 120) String specialization) {
    }

    /** {@code trainerId == null} removes the member's trainer. */
    public record AssignTrainerRequest(UUID trainerId) {
    }

    public record MembershipPlanRequest(
            @NotBlank @Size(max = 80) String name,
            @Min(1) @Max(60) int durationMonths,
            @NotNull @DecimalMin("0.00") @DecimalMax("1000000.00") BigDecimal price,
            @Size(max = 500) String description,
            boolean active,
            @Size(max = 12) List<@NotBlank @Size(max = 160) String> features) {
    }

    public record MembershipPlanResponse(
            UUID id,
            String name,
            int durationMonths,
            BigDecimal price,
            String description,
            boolean active,
            List<String> features) {

        public static MembershipPlanResponse from(MembershipPlan plan) {
            return new MembershipPlanResponse(
                    plan.getId(),
                    plan.getName(),
                    plan.getDurationMonths(),
                    plan.getPrice(),
                    plan.getDescription(),
                    plan.isActive(),
                    plan.getFeatures());
        }
    }

    public record ScheduleDayRequest(
            @NotBlank String dayOfWeek,
            boolean open,
            @NotNull LocalTime openingTime,
            @NotNull LocalTime closingTime,
            @Size(max = 300) String notes) {
    }

    public record ScheduleUpdateRequest(
            @NotEmpty @Size(min = 7, max = 7) List<@Valid ScheduleDayRequest> days) {
    }

    public record GymClosureRequest(
            @NotNull LocalDate date,
            @NotBlank @Size(max = 200) String reason,
            @Size(max = 600) String announcement) {
    }

    public record GymClosureResponse(
            UUID id,
            LocalDate date,
            String reason,
            String announcement,
            Instant createdAt) {

        public static GymClosureResponse from(GymClosure closure) {
            return new GymClosureResponse(
                    closure.getId(),
                    closure.getDate(),
                    closure.getReason(),
                    closure.getAnnouncement(),
                    closure.getCreatedAt());
        }
    }
}
