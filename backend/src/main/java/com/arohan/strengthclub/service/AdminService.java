package com.arohan.strengthclub.service;

import com.arohan.strengthclub.dto.AdminDtos.AdminUserResponse;
import com.arohan.strengthclub.dto.AdminDtos.AssignTrainerRequest;
import com.arohan.strengthclub.dto.AdminDtos.GymClosureRequest;
import com.arohan.strengthclub.dto.AdminDtos.GymClosureResponse;
import com.arohan.strengthclub.dto.AdminDtos.MembershipPlanRequest;
import com.arohan.strengthclub.dto.AdminDtos.MembershipPlanResponse;
import com.arohan.strengthclub.dto.AdminDtos.OverviewResponse;
import com.arohan.strengthclub.dto.AdminDtos.ScheduleDayRequest;
import com.arohan.strengthclub.dto.AdminDtos.ScheduleUpdateRequest;
import com.arohan.strengthclub.dto.AdminDtos.UpdateUserRequest;
import com.arohan.strengthclub.dto.AttendanceDtos.ScheduleDayResponse;
import com.arohan.strengthclub.entity.FitnessProfile;
import com.arohan.strengthclub.entity.GymClosure;
import com.arohan.strengthclub.entity.GymOperatingHour;
import com.arohan.strengthclub.entity.MembershipPlan;
import com.arohan.strengthclub.entity.User;
import com.arohan.strengthclub.entity.enums.MembershipStatus;
import com.arohan.strengthclub.entity.enums.Role;
import com.arohan.strengthclub.entity.enums.WorkoutSource;
import com.arohan.strengthclub.exception.BusinessRuleException;
import com.arohan.strengthclub.exception.ResourceNotFoundException;
import com.arohan.strengthclub.repository.AttendanceRepository;
import com.arohan.strengthclub.repository.FitnessProfileRepository;
import com.arohan.strengthclub.repository.GymClosureRepository;
import com.arohan.strengthclub.repository.GymOperatingHourRepository;
import com.arohan.strengthclub.repository.MembershipPaymentRepository;
import com.arohan.strengthclub.repository.MembershipPlanRepository;
import com.arohan.strengthclub.repository.UserRepository;
import com.arohan.strengthclub.repository.WorkoutPlanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Coordinates administrator-only user, membership-plan, and facility-setting operations. */
@Service
public class AdminService {

    private final UserRepository users;
    private final FitnessProfileRepository profiles;
    private final AttendanceRepository attendances;
    private final MembershipPlanRepository plans;
    private final MembershipPaymentRepository payments;
    private final GymOperatingHourRepository hours;
    private final GymClosureRepository closures;
    private final WorkoutPlanRepository workoutPlans;
    private final WorkoutService workoutService;
    private final GymService gymService;

    public AdminService(
            UserRepository users,
            FitnessProfileRepository profiles,
            AttendanceRepository attendances,
            MembershipPlanRepository plans,
            MembershipPaymentRepository payments,
            GymOperatingHourRepository hours,
            GymClosureRepository closures,
            WorkoutPlanRepository workoutPlans,
            WorkoutService workoutService,
            GymService gymService) {
        this.users = users;
        this.profiles = profiles;
        this.attendances = attendances;
        this.plans = plans;
        this.payments = payments;
        this.hours = hours;
        this.closures = closures;
        this.workoutPlans = workoutPlans;
        this.workoutService = workoutService;
        this.gymService = gymService;
    }

    @Transactional(readOnly = true)
    public OverviewResponse getOverview() {
        LocalDate today = LocalDate.now(gymService.getZoneId());
        List<Object[]> rows = users.overviewCounts(today);
        Object[] row = rows.isEmpty() ? new Object[5] : rows.get(0);
        return new OverviewResponse(
                count(row[0]), count(row[1]), count(row[2]), count(row[3]), count(row[4]), today);
    }

    private void requireNotPast(LocalDate date) {
        if (date.isBefore(LocalDate.now(gymService.getZoneId()))) {
            throw new BusinessRuleException("CLOSURE_IN_PAST", "A closure cannot be set for a date that has already passed.");
        }
    }

    private static long count(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getUsers() {
        Map<UUID, FitnessProfile> profileByUserId = profiles.findAllWithUserAndTrainer().stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity()));

        return users.findAllByOrderByCreatedAtDesc().stream()
                .map(user -> AdminUserResponse.from(user, profileByUserId.get(user.getId())))
                .toList();
    }

    @Transactional
    public AdminUserResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User was not found."));
        Role currentRole = user.getRole();
        Role requestedRole = request.role();

        // Preserve at least one administrator so routine account edits cannot lock out operations.
        if (currentRole == Role.ADMIN
                && requestedRole != null
                && requestedRole != Role.ADMIN
                && users.countByRole(Role.ADMIN) <= 1) {
            throw new BusinessRuleException(
                    "LAST_ADMIN_REQUIRED",
                    "At least one administrator account must remain active.",
                    HttpStatus.CONFLICT);
        }

        // Detach assigned members only when the account becomes a member, not when promoted to admin.
        if (requestedRole == Role.MEMBER && currentRole != Role.MEMBER) {
            detachTrainerMembers(user);
            workoutPlans.deleteByCreatedBy_Id(userId);
        }

        if (requestedRole != null) {
            user.setRole(requestedRole);
        }
        if (request.specialization() != null) {
            user.setSpecialization(request.specialization().trim());
        }
        users.save(user);

        FitnessProfile profile = profiles.findByUser_Id(userId).orElse(null);
        if (user.getRole() == Role.MEMBER && profile == null) {
            profile = new FitnessProfile();
            profile.setUser(user);
            profiles.save(profile);
        }

        if (profile != null && request.membershipStatus() != null) {
            profile.setMembershipStatus(request.membershipStatus());
            if (request.membershipStatus() == MembershipStatus.EXPIRED) {
                profile.setMembershipExpiry(LocalDate.now(gymService.getZoneId()).minusDays(1));
            } else if (request.membershipStatus() == MembershipStatus.ACTIVE) {
                LocalDate today = LocalDate.now(gymService.getZoneId());
                if (profile.getMembershipExpiry() == null || profile.getMembershipExpiry().isBefore(today)) {
                    profile.setMembershipStart(today);
                    profile.setMembershipExpiry(today.plusMonths(1).minusDays(1));
                }
            }
            profiles.save(profile);
        }

        return AdminUserResponse.from(user, profile);
    }

    /** Assigns (or removes) a member's trainer; this is how pending trainer requests get resolved. */
    @Transactional
    public AdminUserResponse assignTrainer(UUID memberId, AssignTrainerRequest request) {
        User member = users.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member was not found."));
        if (member.getRole() != Role.MEMBER) {
            throw new BusinessRuleException("MEMBER_REQUIRED", "Trainers can only be assigned to member accounts.");
        }
        FitnessProfile profile = profiles.findByUser_Id(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member profile was not found."));

        if (request == null || request.trainerId() == null) {
            if (profile.getTrainer() != null || profile.isTrainerRequested()) {
                profile.setTrainer(null);
                profile.setTrainerRequested(false);
                profile.setWantsTrainer(false);
                profiles.save(profile);
                workoutService.resetToRecommended(member);
            }
            return AdminUserResponse.from(member, profile);
        }

        User trainer = users.findById(request.trainerId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainer was not found."));
        if (trainer.getRole() != Role.TRAINER) {
            throw new BusinessRuleException("TRAINER_REQUIRED", "The selected account is not a trainer.");
        }
        boolean unchanged = profile.getTrainer() != null && profile.getTrainer().getId().equals(trainer.getId());
        profile.setTrainer(trainer);
        profile.setWantsTrainer(true);
        profile.setTrainerRequested(false);
        profiles.save(profile);
        if (!unchanged) {
            workoutService.prepareTrainerAssignment(member);
        }
        return AdminUserResponse.from(member, profile);
    }

    /** Unassigns a trainer's members and puts their routines back on the recommended plan. */
    private void detachTrainerMembers(User trainer) {
        List<FitnessProfile> assignedMembers = profiles.findByTrainer_Id(trainer.getId());
        for (FitnessProfile profile : assignedMembers) {
            profile.setTrainer(null);
            profile.setTrainerRequested(false);
            profile.setWantsTrainer(false);
            profile.setActiveWorkoutSource(WorkoutSource.RECOMMENDED);
            profiles.save(profile);
            workoutService.resetToRecommended(profile.getUser());
        }
    }

    @Transactional
    public void deleteUser(UUID userId, User currentAdmin) {
        if (userId.equals(currentAdmin.getId())) {
            throw new BusinessRuleException(
                    "SELF_DELETE_BLOCKED",
                    "You cannot delete the administrator account currently signed in.");
        }

        User target = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User was not found."));
        // Also handles accounts previously promoted from trainer to admin.
        detachTrainerMembers(target);

        workoutPlans.deleteByAssignedTo_Id(target.getId());
        workoutPlans.deleteByCreatedBy_Id(target.getId());
        attendances.deleteByUser_Id(target.getId());
        payments.deleteByUser_Id(target.getId());
        profiles.findByUser_Id(target.getId()).ifPresent(profiles::delete);
        users.delete(target);
    }

    @Transactional(readOnly = true)
    public List<ScheduleDayResponse> getSchedule() {
        return hours.findAll().stream()
                .sorted(Comparator.comparingInt(
                        hour -> DayOfWeek.valueOf(hour.getDayOfWeek().toUpperCase(Locale.ROOT)).getValue()))
                .map(ScheduleDayResponse::from)
                .toList();
    }

    @Transactional
    public List<ScheduleDayResponse> updateSchedule(ScheduleUpdateRequest request) {
        if (request == null || request.days() == null || request.days().size() != 7) {
            throw new BusinessRuleException(
                    "INVALID_WEEKLY_SCHEDULE",
                    "Provide exactly one schedule entry for each day of the week.");
        }

        // Validate the whole request before writing so an invalid week cannot be partially applied.
        Set<DayOfWeek> suppliedDays = EnumSet.noneOf(DayOfWeek.class);
        for (ScheduleDayRequest input : request.days()) {
            if (input == null || input.openingTime() == null || input.closingTime() == null) {
                throw new BusinessRuleException(
                        "INVALID_WEEKLY_SCHEDULE",
                        "Every schedule day must include opening and closing times.");
            }

            DayOfWeek day = parseDay(input.dayOfWeek());
            if (!suppliedDays.add(day)) {
                throw new BusinessRuleException(
                        "DUPLICATE_SCHEDULE_DAY",
                        "Each day of the week must appear exactly once.");
            }
            if (input.open() && !input.openingTime().isBefore(input.closingTime())) {
                throw new BusinessRuleException(
                        "INVALID_OPERATING_HOURS",
                        "Closing time must be later than opening time for an open day.");
            }
        }

        for (ScheduleDayRequest input : request.days()) {
            String canonicalDay = parseDay(input.dayOfWeek()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            GymOperatingHour hour = hours.findByDayOfWeekIgnoreCase(canonicalDay)
                    .orElseThrow(() -> new ResourceNotFoundException("Schedule day was not found."));
            hour.setOpen(input.open());
            hour.setOpeningTime(input.openingTime());
            hour.setClosingTime(input.closingTime());
            hour.setNotes(input.notes() == null ? "" : input.notes().trim());
            hours.save(hour);
        }
        gymService.invalidateCacheAfterCommit();

        return getSchedule();
    }

    private DayOfWeek parseDay(String value) {
        try {
            return DayOfWeek.valueOf(value == null ? "" : value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("INVALID_DAY", "Schedule day must be Monday through Sunday.");
        }
    }

    @Transactional(readOnly = true)
    public List<GymClosureResponse> getClosures() {
        return closures.findAllByOrderByDateAsc().stream()
                .map(GymClosureResponse::from)
                .toList();
    }

    @Transactional
    public GymClosureResponse createClosure(GymClosureRequest request, User admin) {
        requireNotPast(request.date());
        if (closures.findByDate(request.date()).isPresent()) {
            throw new BusinessRuleException(
                    "CLOSURE_DATE_EXISTS",
                    "A closure is already recorded for that date.",
                    HttpStatus.CONFLICT);
        }

        GymClosure closure = new GymClosure();
        closure.setDate(request.date());
        closure.setReason(request.reason().trim());
        closure.setAnnouncement(request.announcement() == null ? "" : request.announcement().trim());
        closure.setCreatedBy(admin);
        GymClosureResponse saved = GymClosureResponse.from(closures.save(closure));
        gymService.invalidateCacheAfterCommit();
        return saved;
    }

    @Transactional
    public GymClosureResponse updateClosure(UUID id, GymClosureRequest request) {
        requireNotPast(request.date());
        GymClosure closure = closures.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Closure was not found."));

        closures.findByDate(request.date())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessRuleException(
                            "CLOSURE_DATE_EXISTS",
                            "A closure is already recorded for that date.",
                            HttpStatus.CONFLICT);
                });

        closure.setDate(request.date());
        closure.setReason(request.reason().trim());
        closure.setAnnouncement(request.announcement() == null ? "" : request.announcement().trim());
        GymClosureResponse saved = GymClosureResponse.from(closures.save(closure));
        gymService.invalidateCacheAfterCommit();
        return saved;
    }

    @Transactional
    public void deleteClosure(UUID id) {
        GymClosure closure = closures.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Closure was not found."));
        closures.delete(closure);
        gymService.invalidateCacheAfterCommit();
    }

    @Transactional(readOnly = true)
    public List<MembershipPlanResponse> getAllMembershipPlans() {
        return plans.findAllByOrderByPriceAsc().stream()
                .map(MembershipPlanResponse::from)
                .toList();
    }

    @Transactional
    public MembershipPlanResponse createMembershipPlan(MembershipPlanRequest request) {
        if (plans.findByNameIgnoreCase(request.name().trim()).isPresent()) {
            throw new BusinessRuleException(
                    "PLAN_NAME_EXISTS",
                    "A membership plan with this name already exists.",
                    HttpStatus.CONFLICT);
        }

        MembershipPlan plan = new MembershipPlan();
        applyPlanRequest(plan, request);
        return MembershipPlanResponse.from(plans.save(plan));
    }

    @Transactional
    public MembershipPlanResponse updateMembershipPlan(UUID id, MembershipPlanRequest request) {
        MembershipPlan plan = plans.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membership plan was not found."));

        plans.findByNameIgnoreCase(request.name().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessRuleException(
                            "PLAN_NAME_EXISTS",
                            "A membership plan with this name already exists.",
                            HttpStatus.CONFLICT);
                });

        applyPlanRequest(plan, request);
        return MembershipPlanResponse.from(plans.save(plan));
    }

    private void applyPlanRequest(MembershipPlan plan, MembershipPlanRequest request) {
        plan.setName(request.name().trim());
        plan.setDurationMonths(request.durationMonths());
        plan.setPrice(request.price());
        plan.setDescription(request.description() == null ? "" : request.description().trim());
        plan.setActive(request.active());
        plan.setFeatures(request.features() == null
                ? List.of()
                : request.features().stream().map(String::trim).toList());
    }
}
