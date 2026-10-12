package com.arohan.strengthclub.dto;

import com.arohan.strengthclub.dto.MemberDtos.ProfileResponse;
import com.arohan.strengthclub.dto.WorkoutDtos.PlanResponse;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Data contracts returned by trainer workspace endpoints. */
public final class TrainerDtos {

    private TrainerDtos() {
    }

    public record MemberResponse(
            UUID id,
            String fullName,
            String email,
            String phone,
            String fitnessGoal,
            String experienceLevel,
            String membershipStatus,
            int plannedDaysPerWeek,
            boolean trainerRequested,
            double consistencyPercent,
            Instant createdAt) {
    }

    public record MemberDetailResponse(
            ProfileResponse profile,
            PlanResponse activeWorkout,
            List<AttendanceDtos.SessionResponse> recentAttendance) {
    }

    public record AssignPlanRequest(@NotNull UUID planId) {
    }

    public record MessageResponse(String message) {
    }
}
