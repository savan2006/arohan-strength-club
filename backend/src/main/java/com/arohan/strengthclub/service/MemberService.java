package com.arohan.strengthclub.service;

import com.arohan.strengthclub.dto.AttendanceDtos.GymStatusResponse;
import com.arohan.strengthclub.dto.MemberDtos.*;
import com.arohan.strengthclub.dto.WorkoutDtos.PlanResponse;
import com.arohan.strengthclub.entity.*;
import com.arohan.strengthclub.entity.enums.AttendanceStatus;
import com.arohan.strengthclub.entity.enums.PaymentStatus;
import com.arohan.strengthclub.entity.enums.Role;
import com.arohan.strengthclub.entity.enums.WorkoutSource;
import com.arohan.strengthclub.exception.BusinessRuleException;
import com.arohan.strengthclub.exception.ResourceNotFoundException;
import com.arohan.strengthclub.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class MemberService {
    private final UserRepository users;
    private final FitnessProfileRepository profiles;
    private final AttendanceRepository attendances;
    private final MembershipPaymentRepository payments;
    private final WorkoutService workouts;
    private final AttendanceService attendanceService;
    private final GymService gymService;
    private final MembershipService memberships;
    private final ZoneId zoneId;

    public MemberService(UserRepository users, FitnessProfileRepository profiles, AttendanceRepository attendances, MembershipPaymentRepository payments,
        WorkoutService workouts, AttendanceService attendanceService, GymService gymService, MembershipService memberships) {
        this.users = users; this.profiles = profiles; this.attendances = attendances; this.payments = payments; this.workouts = workouts;
        this.attendanceService = attendanceService; this.gymService = gymService; this.memberships = memberships; this.zoneId = gymService.getZoneId();
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(User user) { return profileResponse(user, requireProfile(user)); }

    @Transactional
    public ProfileResponse updateProfile(User user, ProfileUpdateRequest request) {
        User managed = users.findById(user.getId()).orElseThrow(() -> new ResourceNotFoundException("Account was not found."));
        FitnessProfile profile = requireProfile(managed);
        managed.setFullName(request.fullName().trim()); managed.setPhone(request.phone().trim());
        profile.setAge(request.age()); profile.setHeightCm(request.heightCm()); profile.setWeightKg(request.weightKg());
        profile.setFitnessGoal(request.fitnessGoal());
        // Fitness level is tracked by server-side promotion rules; profile edits cannot raise or demote it directly.
        profile.setExperienceLevel(profile.getCurrentLevel());
        profile.setPlannedDaysPerWeek(request.plannedDaysPerWeek());
        if (request.preferredSchedule() != null && !request.preferredSchedule().isBlank()) profile.setPreferredSchedule(request.preferredSchedule());
        users.save(managed); profiles.save(profile);
        return profileResponse(managed, profile);
    }

    @Transactional
    public DashboardResponse getDashboard(User user) {
        FitnessProfile profile = requireProfile(user);
        LocalDate today = LocalDate.now(zoneId);
        LocalDate monthStart = today.withDayOfMonth(1);
        // One bounded attendance query feeds the visit counters, today's progress and the consistency maths.
        List<Attendance> history = attendanceService.recentHistory(user, today);
        List<Attendance> month = history.stream().filter(a -> !a.getFacilityDate().isBefore(monthStart)).toList();
        int visits = month.size();
        int minutes = month.stream().mapToInt(Attendance::getDurationMinutes).sum();
        boolean checkedIn = attendanceService.isCheckedIn(user);
        PlanResponse plan = workouts.getActivePlan(user);
        long totalExercises = plan.days().stream().mapToLong(d -> d.exercises().size()).sum();
        long completedExercises = history.stream().filter(a -> a.getFacilityDate().equals(today)).flatMap(a -> a.getExercises().stream()).filter(AttendanceExercise::isCompleted).count();
        ConsistencyResponse consistency = attendanceService.buildConsistency(profile, history, today);
        GymStatusResponse gym = gymService.getStatus();
        return new DashboardResponse(user.getFullName().split("\\s+")[0], profile.getMembershipStatus().name(), profile.getMembershipExpiry(),
            visits, minutes, consistency.currentStreak(), profile.getPlannedDaysPerWeek(), consistency.percentage(), checkedIn,
            plan.name(), totalExercises, completedExercises, gym.open() ? "OPEN" : "CLOSED");
    }

    @Transactional(readOnly = true)
    public List<TrainerSummary> getTrainers() {
        return users.findAllByRoleOrderByCreatedAtDesc(Role.TRAINER).stream().map(TrainerSummary::from).toList();
    }

    @Transactional
    public ProfileResponse updateTrainerPreference(User user, TrainerPreferenceRequest request) {
        FitnessProfile profile = requireProfile(user);
        if (!request.wantsTrainer()) {
            profile.setWantsTrainer(false); profile.setTrainerRequested(false); profile.setTrainer(null);
            workouts.resetToRecommended(user);
        } else if (request.trainerId() == null) {
            profile.setWantsTrainer(true); profile.setTrainerRequested(true); profile.setTrainer(null);
            workouts.resetToRecommended(user);
        } else {
            User trainer = users.findById(request.trainerId()).orElseThrow(() -> new ResourceNotFoundException("Trainer was not found."));
            if (trainer.getRole() != Role.TRAINER) throw new BusinessRuleException("TRAINER_REQUIRED", "The selected account is not a trainer.");
            profile.setWantsTrainer(true); profile.setTrainerRequested(false); profile.setTrainer(trainer);
            workouts.prepareTrainerAssignment(user);
        }
        profiles.save(profile);
        return profileResponse(user, profile);
    }

    @Transactional(readOnly = true)
    public List<Attendance> getRecentAttendance(User user) { return attendances.findByUser_IdOrderByCheckInTimeDesc(user.getId()); }

    private ProfileResponse profileResponse(User user, FitnessProfile profile) {
        String currentPlan = payments.findFirstByUser_IdAndStatusOrderByPaymentDateDesc(user.getId(), PaymentStatus.COMPLETED)
                .map(MembershipPayment::getPlanName).orElse(null);
        long durationMonths = payments.sumPlanDurationMonthsByUserIdAndStatus(user.getId(), PaymentStatus.COMPLETED);
        return ProfileResponse.from(user, profile, currentPlan, Math.toIntExact(Math.min(Integer.MAX_VALUE, durationMonths)));
    }

    private FitnessProfile requireProfile(User user) {
        FitnessProfile profile = profiles.findByUser_Id(user.getId()).orElseThrow(() -> new ResourceNotFoundException("Fitness profile was not found."));
        memberships.applyElapsedExpiry(profile);
        return profile;
    }
}
