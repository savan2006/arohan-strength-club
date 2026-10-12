package com.arohan.strengthclub.controller;

import com.arohan.strengthclub.dto.AdminDtos.MembershipPlanResponse;
import com.arohan.strengthclub.dto.MemberDtos.ConsistencyResponse;
import com.arohan.strengthclub.dto.MemberDtos.DashboardResponse;
import com.arohan.strengthclub.dto.MemberDtos.FitnessLevelResponse;
import com.arohan.strengthclub.dto.MemberDtos.PaymentRequest;
import com.arohan.strengthclub.dto.MemberDtos.PaymentResponse;
import com.arohan.strengthclub.dto.MemberDtos.ProfileResponse;
import com.arohan.strengthclub.dto.MemberDtos.ProfileUpdateRequest;
import com.arohan.strengthclub.dto.MemberDtos.TrainerPreferenceRequest;
import com.arohan.strengthclub.dto.MemberDtos.TrainerSummary;
import com.arohan.strengthclub.dto.WorkoutDtos.PlanRequest;
import com.arohan.strengthclub.dto.WorkoutDtos.PlanResponse;
import com.arohan.strengthclub.dto.WorkoutDtos.WeeklyScheduleRequest;
import com.arohan.strengthclub.entity.User;
import com.arohan.strengthclub.entity.enums.WorkoutSource;
import com.arohan.strengthclub.exception.BusinessRuleException;
import com.arohan.strengthclub.security.CurrentUserService;
import com.arohan.strengthclub.service.AttendanceService;
import com.arohan.strengthclub.service.FitnessLevelService;
import com.arohan.strengthclub.service.MemberService;
import com.arohan.strengthclub.service.MembershipService;
import com.arohan.strengthclub.service.WorkoutService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Provides member-facing profile, membership, attendance summary, and workout operations. */
@RestController
@RequestMapping("/api/member")
public class MemberController {

    private final CurrentUserService currentUsers;
    private final MemberService members;
    private final MembershipService memberships;
    private final WorkoutService workouts;
    private final AttendanceService attendance;
    private final FitnessLevelService fitnessLevels;

    public MemberController(
            CurrentUserService currentUsers,
            MemberService members,
            MembershipService memberships,
            WorkoutService workouts,
            AttendanceService attendance,
            FitnessLevelService fitnessLevels) {
        this.currentUsers = currentUsers;
        this.members = members;
        this.memberships = memberships;
        this.workouts = workouts;
        this.attendance = attendance;
        this.fitnessLevels = fitnessLevels;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(Authentication auth) {
        return members.getDashboard(user(auth));
    }

    @GetMapping("/profile")
    public ProfileResponse profile(Authentication auth) {
        return members.getProfile(user(auth));
    }

    @PutMapping("/profile")
    public ProfileResponse updateProfile(
            Authentication auth,
            @Valid @RequestBody ProfileUpdateRequest body) {
        return members.updateProfile(user(auth), body);
    }

    @GetMapping("/trainers")
    public List<TrainerSummary> trainers() {
        return members.getTrainers();
    }

    @PatchMapping("/trainer-preference")
    public ProfileResponse trainerPreference(
            Authentication auth,
            @RequestBody TrainerPreferenceRequest body) {
        return members.updateTrainerPreference(user(auth), body);
    }

    /** Retained for older clients that still submit trainer preferences with PUT. */
    @PutMapping("/trainer-preference")
    public ProfileResponse trainerPreferenceCompat(
            Authentication auth,
            @RequestBody TrainerPreferenceRequest body) {
        return members.updateTrainerPreference(user(auth), body);
    }

    @GetMapping("/consistency")
    public ConsistencyResponse consistency(Authentication auth) {
        return attendance.getConsistency(user(auth));
    }

    @GetMapping("/fitness-level")
    public FitnessLevelResponse fitnessLevel(Authentication auth) {
        return fitnessLevels.getLevel(user(auth));
    }

    @PostMapping("/fitness-level/check-promotion")
    public FitnessLevelResponse checkFitnessLevelPromotion(Authentication auth) {
        return fitnessLevels.checkPromotion(user(auth));
    }

    @GetMapping("/membership/plans")
    public List<MembershipPlanResponse> plans() {
        return memberships.getActivePlans();
    }

    @PostMapping("/membership/pay")
    public PaymentResponse purchase(
            Authentication auth,
            @Valid @RequestBody PaymentRequest body) {
        return memberships.purchase(user(auth), body);
    }

    @GetMapping("/workout-plan")
    public PlanResponse workout(Authentication auth) {
        return workouts.getActivePlan(user(auth));
    }

    @PostMapping("/workout-plan")
    public PlanResponse createCustomWorkout(
            Authentication auth,
            @Valid @RequestBody PlanRequest body) {
        return workouts.createMemberCustomPlan(user(auth), body);
    }

    @PutMapping("/weekly-schedule")
    public PlanResponse updateWeeklySchedule(
            Authentication auth,
            @Valid @RequestBody WeeklyScheduleRequest body) {
        return workouts.updateWeeklySchedule(user(auth), body);
    }

    @PostMapping("/workout-plan/generate")
    public PlanResponse generateWorkout(Authentication auth) {
        return workouts.generateRecommended(user(auth));
    }

    @PatchMapping("/workout-plan/day/{dayNumber}/toggle-complete")
    public Map<String, String> markDayComplete(
            Authentication auth,
            @PathVariable int dayNumber) {
        workouts.toggleDayCompletion(user(auth), dayNumber);
        return Map.of("message", "Workout completion recorded.");
    }

    @PatchMapping("/workout-source")
    public PlanResponse changeWorkoutSource(
            Authentication auth,
            @RequestParam(defaultValue = "RECOMMENDED") String source) {
        User current = user(auth);
        if ("RECOMMENDED".equalsIgnoreCase(source)) {
            return workouts.generateRecommended(current);
        }
        if ("CUSTOM".equalsIgnoreCase(source)) {
            ProfileResponse profile = members.getProfile(current);
            if (profile.activeWorkoutSource() != WorkoutSource.CUSTOM) {
                throw new BusinessRuleException(
                        "CUSTOM_PLAN_UNAVAILABLE",
                        "Create a custom plan before selecting it.");
            }
        }
        return workouts.getActivePlan(current);
    }

    private User user(Authentication auth) {
        return currentUsers.requireUser(auth);
    }
}
