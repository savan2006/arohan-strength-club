package com.arohan.strengthclub.dto;

import com.arohan.strengthclub.entity.Attendance;
import com.arohan.strengthclub.entity.AttendanceExercise;
import com.arohan.strengthclub.entity.GymOperatingHour;
import com.arohan.strengthclub.entity.enums.AttendanceStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Response contracts for attendance sessions and facility operating status. */
public final class AttendanceDtos {

    private AttendanceDtos() {
    }

    public record SessionExerciseResponse(
            UUID id,
            String exerciseName,
            int sets,
            String reps,
            int restSeconds,
            boolean completed,
            Instant completedAt,
            String imageUrl,
            int sortOrder) {

        public static SessionExerciseResponse from(AttendanceExercise exercise) {
            return new SessionExerciseResponse(
                    exercise.getId(),
                    exercise.getExerciseName(),
                    exercise.getSets(),
                    exercise.getReps(),
                    exercise.getRestSeconds(),
                    exercise.isCompleted(),
                    exercise.getCompletedAt(),
                    exercise.getImageUrl(),
                    exercise.getSortOrder());
        }
    }

    public record SessionResponse(
            UUID id,
            Instant checkInTime,
            Instant checkOutTime,
            int durationMinutes,
            int estimatedDurationMinutes,
            AttendanceStatus status,
            LocalDate facilityDate,
            Integer workoutDayNumber,
            String workoutDayName,
            List<SessionExerciseResponse> exercises) {

        public static SessionResponse from(Attendance attendance) {
            return new SessionResponse(
                    attendance.getId(),
                    attendance.getCheckInTime(),
                    attendance.getCheckOutTime(),
                    attendance.getDurationMinutes(),
                    attendance.getEstimatedDurationMinutes(),
                    attendance.getStatus(),
                    attendance.getFacilityDate(),
                    attendance.getWorkoutDayNumber(),
                    attendance.getWorkoutDayName(),
                    attendance.getExercises().stream().map(SessionExerciseResponse::from).toList());
        }
    }

    public record GymStatusResponse(
            boolean open,
            String message,
            String dayOfWeek,
            LocalDate date,
            LocalTime openingTime,
            LocalTime closingTime,
            String closureReason,
            String announcement) {
    }

    public record ScheduleDayResponse(
            String dayOfWeek,
            boolean open,
            LocalTime openingTime,
            LocalTime closingTime,
            String notes) {

        public static ScheduleDayResponse from(GymOperatingHour hour) {
            return new ScheduleDayResponse(
                    hour.getDayOfWeek(),
                    hour.isOpen(),
                    hour.getOpeningTime(),
                    hour.getClosingTime(),
                    hour.getNotes());
        }
    }
}
