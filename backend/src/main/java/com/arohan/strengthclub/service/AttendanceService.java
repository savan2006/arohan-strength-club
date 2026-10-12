package com.arohan.strengthclub.service;

import com.arohan.strengthclub.dto.AttendanceDtos.SessionResponse;
import com.arohan.strengthclub.dto.MemberDtos.ConsistencyResponse;
import com.arohan.strengthclub.entity.Attendance;
import com.arohan.strengthclub.entity.AttendanceExercise;
import com.arohan.strengthclub.entity.FitnessProfile;
import com.arohan.strengthclub.entity.GymOperatingHour;
import com.arohan.strengthclub.entity.User;
import com.arohan.strengthclub.entity.WorkoutExercise;
import com.arohan.strengthclub.entity.enums.AttendanceStatus;
import com.arohan.strengthclub.exception.BusinessRuleException;
import com.arohan.strengthclub.exception.ResourceNotFoundException;
import com.arohan.strengthclub.repository.AttendanceRepository;
import com.arohan.strengthclub.repository.FitnessProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Owns check-in/check-out rules and derives attendance summaries for members. */
@Service
public class AttendanceService {

    /** Sessions returned by the history endpoint. */
    private static final int HISTORY_LIMIT = 90;
    /** Look-back used for consistency and streak maths; older rows can never change the result. */
    private static final int LOOKBACK_DAYS = 400;

    private final AttendanceRepository attendances;
    private final FitnessProfileRepository profiles;
    private final MembershipService memberships;
    private final GymService gymService;
    private final WorkoutService workouts;
    private final ZoneId zoneId;

    public AttendanceService(
            AttendanceRepository attendances,
            FitnessProfileRepository profiles,
            MembershipService memberships,
            GymService gymService,
            WorkoutService workouts) {
        this.attendances = attendances;
        this.profiles = profiles;
        this.memberships = memberships;
        this.gymService = gymService;
        this.workouts = workouts;
        this.zoneId = gymService.getZoneId();
    }

    @Transactional
    public SessionResponse checkIn(User user) {
        memberships.requireActiveMembership(user);
        gymService.requireOpenForCheckIn();

        if (getActive(user).isPresent()) {
            throw new BusinessRuleException("ACTIVE_SESSION_EXISTS", "You already have an active gym session.");
        }

        LocalDate today = LocalDate.now(zoneId);
        if (attendances.existsByUser_IdAndFacilityDateAndStatus(
                user.getId(), today, AttendanceStatus.COMPLETED)) {
            throw new BusinessRuleException(
                    "WORKOUT_ALREADY_COMPLETED_TODAY",
                    "Your workout for today is already recorded.");
        }

        // Snapshot the current prescription so later plan edits cannot rewrite session history.
        List<WorkoutExercise> exerciseList = workouts.getExercisesForPlanDay(user, today.getDayOfWeek());
        Attendance session = new Attendance();
        session.setUser(user);
        session.setFacilityDate(today);
        session.setCheckInTime(Instant.now());
        session.setStatus(AttendanceStatus.ACTIVE);
        session.setEstimatedDurationMinutes(estimateDuration(exerciseList.size()));
        session.setWorkoutDayName(today.getDayOfWeek().getDisplayName(
                java.time.format.TextStyle.FULL,
                Locale.ENGLISH));
        session.setWorkoutDayNumber(today.getDayOfWeek().getValue());

        int order = 0;
        for (WorkoutExercise source : exerciseList) {
            AttendanceExercise exercise = new AttendanceExercise();
            exercise.setSourceExerciseId(source.getId().toString());
            exercise.setExerciseName(source.getExerciseName());
            exercise.setSets(source.getSets());
            exercise.setReps(source.getReps());
            exercise.setRestSeconds(source.getRestSeconds());
            exercise.setImageUrl(source.getImageUrl());
            exercise.setSortOrder(order++);
            session.addExercise(exercise);
        }

        return SessionResponse.from(attendances.save(session));
    }

    @Transactional
    public SessionResponse checkOut(User user) {
        Attendance session = getActive(user)
                .orElseThrow(() -> new BusinessRuleException(
                        "NO_ACTIVE_SESSION",
                        "There is no active gym session to check out."));
        Instant now = Instant.now();
        Instant closeAt = closingInstant(session.getFacilityDate());
        Instant completedAt = closeAt != null && now.isAfter(closeAt) ? closeAt : now;

        session.setCheckOutTime(completedAt);
        session.setDurationMinutes((int) Math.max(
                0,
                ChronoUnit.MINUTES.between(session.getCheckInTime(), completedAt)));
        session.setStatus(AttendanceStatus.COMPLETED);
        return SessionResponse.from(attendances.save(session));
    }

    @Transactional
    public Optional<SessionResponse> currentSession(User user) {
        Optional<Attendance> active = getActive(user);
        if (active.isEmpty()) {
            return Optional.empty();
        }

        Attendance session = active.get();
        Instant closeAt = closingInstant(session.getFacilityDate());
        if (closeAt != null && Instant.now().isAfter(closeAt)) {
            session.setCheckOutTime(closeAt);
            session.setDurationMinutes((int) Math.max(
                    0,
                    ChronoUnit.MINUTES.between(session.getCheckInTime(), closeAt)));
            session.setStatus(AttendanceStatus.COMPLETED);
            attendances.save(session);
            return Optional.empty();
        }

        return Optional.of(SessionResponse.from(session));
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getHistory(User user) {
        return attendances.findByUser_IdOrderByCheckInTimeDesc(user.getId(), PageRequest.of(0, HISTORY_LIMIT)).stream()
                .map(SessionResponse::from)
                .toList();
    }

    @Transactional
    public SessionResponse toggleExercise(User user, UUID exerciseId) {
        memberships.requireActiveMembership(user);
        Attendance session = getActive(user)
                .orElseThrow(() -> new BusinessRuleException(
                        "NO_ACTIVE_SESSION",
                        "Start a gym session before updating exercises."));
        AttendanceExercise exercise = session.getExercises().stream()
                .filter(candidate -> candidate.getId().equals(exerciseId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Exercise was not found in the active session."));

        exercise.setCompleted(!exercise.isCompleted());
        exercise.setCompletedAt(exercise.isCompleted() ? Instant.now() : null);
        return SessionResponse.from(attendances.save(session));
    }

    @Transactional(readOnly = true)
    public ConsistencyResponse getConsistency(User user) {
        FitnessProfile profile = profiles.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Fitness profile was not found."));
        LocalDate today = LocalDate.now(zoneId);
        return buildConsistency(profile, recentHistory(user, today), today);
    }

    /** Sessions inside the consistency look-back window, newest first. */
    @Transactional(readOnly = true)
    public List<Attendance> recentHistory(User user, LocalDate today) {
        return attendances.findByUser_IdAndFacilityDateGreaterThanEqualOrderByCheckInTimeDesc(
                user.getId(), today.minusDays(LOOKBACK_DAYS));
    }

    /**
     * Builds a trainer roster's consistency summaries using one attendance query rather than
     * issuing a separate query for every member.
     */
    @Transactional(readOnly = true)
    public Map<UUID, ConsistencyResponse> getConsistencyForProfiles(List<FitnessProfile> memberProfiles) {
        if (memberProfiles.isEmpty()) {
            return Map.of();
        }

        List<UUID> memberIds = memberProfiles.stream()
                .map(profile -> profile.getUser().getId())
                .distinct()
                .toList();
        Map<UUID, List<Attendance>> historyByMember = new HashMap<>();
        LocalDate since = LocalDate.now(zoneId).minusDays(LOOKBACK_DAYS);
        for (Attendance session : attendances.findByUser_IdInAndFacilityDateGreaterThanEqualOrderByCheckInTimeDesc(memberIds, since)) {
            historyByMember.computeIfAbsent(session.getUser().getId(), ignored -> new ArrayList<>())
                    .add(session);
        }

        LocalDate today = LocalDate.now(zoneId);
        Map<UUID, ConsistencyResponse> summaries = new HashMap<>();
        for (FitnessProfile profile : memberProfiles) {
            UUID memberId = profile.getUser().getId();
            summaries.put(
                    memberId,
                    buildConsistency(profile, historyByMember.getOrDefault(memberId, List.of()), today));
        }
        return Map.copyOf(summaries);
    }

    /** Pure calculation over an already-loaded history; performs no database access. */
    public ConsistencyResponse buildConsistency(
            FitnessProfile profile,
            List<Attendance> history,
            LocalDate today) {
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        int monthlyVisits = (int) history.stream()
                .filter(session -> !session.getFacilityDate().isBefore(monthStart))
                .count();
        int workoutMinutes = history.stream()
                .filter(session -> !session.getFacilityDate().isBefore(monthStart))
                .mapToInt(Attendance::getDurationMinutes)
                .sum();
        int completedWorkouts = (int) history.stream()
                .filter(session -> session.getStatus() == AttendanceStatus.COMPLETED
                        && !session.getFacilityDate().isBefore(weekStart)
                        && !session.getFacilityDate().isAfter(today))
                .count();
        int elapsedScheduledDays = (int) Math.min(
                profile.getPlannedDaysPerWeek(),
                ChronoUnit.DAYS.between(weekStart, today) + 1);
        int expected = Math.max(1, elapsedScheduledDays);
        double percentage = Math.min(100.0, completedWorkouts * 100.0 / expected);

        return new ConsistencyResponse(
                profile.getPlannedDaysPerWeek(),
                completedWorkouts,
                expected,
                Math.round(percentage * 10.0) / 10.0,
                workoutMinutes,
                calculateStreak(history, today),
                monthlyVisits);
    }

    @Transactional
    public boolean isCheckedIn(User user) {
        // Resolving an overdue session may persist its automatic checkout.
        return getActive(user).isPresent();
    }

    /** Closes stale sessions at the facility's recorded closing time before returning an active session. */
    private Optional<Attendance> getActive(User user) {
        Optional<Attendance> active = attendances.findFirstByUser_IdAndStatusOrderByCheckInTimeDesc(
                user.getId(), AttendanceStatus.ACTIVE);
        if (active.isPresent()) {
            Instant closeAt = closingInstant(active.get().getFacilityDate());
            if (closeAt != null && Instant.now().isAfter(closeAt)) {
                Attendance session = active.get();
                session.setCheckOutTime(closeAt);
                session.setDurationMinutes((int) Math.max(
                        0,
                        ChronoUnit.MINUTES.between(session.getCheckInTime(), closeAt)));
                session.setStatus(AttendanceStatus.COMPLETED);
                attendances.save(session);
                return Optional.empty();
            }
        }
        return active;
    }

    private Instant closingInstant(LocalDate date) {
        GymOperatingHour hours = gymService.getHoursFor(date);
        if (hours == null || !hours.isOpen()) {
            return date.atTime(22, 0).atZone(zoneId).toInstant();
        }
        return date.atTime(hours.getClosingTime()).atZone(zoneId).toInstant();
    }

    private int estimateDuration(int exerciseCount) {
        return Math.min(120, Math.max(30, exerciseCount * 8));
    }

    private int calculateStreak(List<Attendance> history, LocalDate today) {
        Set<LocalDate> completedDays = new HashSet<>();
        history.stream()
                .filter(session -> session.getStatus() == AttendanceStatus.COMPLETED)
                .forEach(session -> completedDays.add(session.getFacilityDate()));

        LocalDate cursor = completedDays.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (completedDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
