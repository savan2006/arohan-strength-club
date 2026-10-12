package com.arohan.strengthclub.service;

import com.arohan.strengthclub.dto.WorkoutDtos.*;
import com.arohan.strengthclub.entity.*;
import com.arohan.strengthclub.entity.enums.*;
import com.arohan.strengthclub.exception.BusinessRuleException;
import com.arohan.strengthclub.exception.ResourceNotFoundException;
import com.arohan.strengthclub.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class WorkoutService {
    private final WorkoutPlanRepository plans;
    private final FitnessProfileRepository profiles;
    private final AttendanceRepository attendances;
    private final UserRepository users;
    private final MembershipService memberships;
    private final ZoneId zoneId;

    public WorkoutService(WorkoutPlanRepository plans, FitnessProfileRepository profiles, AttendanceRepository attendances,
        UserRepository users, MembershipService memberships, GymService gymService) {
        this.plans = plans; this.profiles = profiles; this.attendances = attendances; this.users = users;
        this.memberships = memberships; this.zoneId = gymService.getZoneId();
    }

    @Transactional
    public PlanResponse getActivePlan(User member) {
        return PlanResponse.from(activePlan(member));
    }

    /** The member's active plan entity (generating the recommended one when none exists yet). */
    private WorkoutPlan activePlan(User member) {
        List<WorkoutPlan> active = plans.findByAssignedTo_IdAndActiveTrueOrderByUpdatedAtDesc(member.getId());
        return active.isEmpty() ? generateRecommendedPlan(member) : active.get(0);
    }

    @Transactional
    public PlanResponse generateRecommended(User member) {
        WorkoutPlan generated = generateRecommendedPlan(member);
        // Explicitly choosing the recommended plan must also be reflected in the profile; otherwise the
        // profile kept reporting CUSTOM/TRAINER while the active plan was the recommended one.
        profile(member).setActiveWorkoutSource(WorkoutSource.RECOMMENDED);
        return PlanResponse.from(generated);
    }

    /** Updates the seven-day shape of the active plan without rewriting completed session snapshots. */
    @Transactional
    public PlanResponse updateWeeklySchedule(User member, WeeklyScheduleRequest request) {
        memberships.requireActiveMembership(member);
        if (request == null || request.weekSchedule() == null || request.weekSchedule().size() != 7) {
            throw new BusinessRuleException("INVALID_WEEKLY_SCHEDULE", "A weekly schedule must include all seven days.");
        }
        Set<Integer> suppliedDays = new HashSet<>();
        for (WeeklyDayRequest input : request.weekSchedule()) {
            if (!suppliedDays.add(input.dayNumber())) {
                throw new BusinessRuleException("DUPLICATE_SCHEDULE_DAY", "Each day of the week must appear exactly once.");
            }
        }
        if (suppliedDays.size() != 7) {
            throw new BusinessRuleException("INCOMPLETE_WEEKLY_SCHEDULE", "Use day numbers 1 through 7, Monday to Sunday.");
        }
        long workoutDays = request.weekSchedule().stream().filter(day -> day.dayType() == DayType.WORKOUT).count();
        if (workoutDays < 3 || workoutDays > 6) {
            throw new BusinessRuleException("INVALID_TRAINING_DAY_COUNT", "Choose between three and six training days per week.");
        }

        WorkoutPlan plan = activePlan(member);
        Map<Integer, WorkoutDay> existing = new HashMap<>();
        for (WorkoutDay day : plan.getDays()) existing.put(day.getDayNumber(), day);
        List<WorkoutExercise> exerciseTemplate = plan.getDays().stream()
            .filter(day -> day.getDayType() == DayType.WORKOUT && !day.getExercises().isEmpty())
            .min(Comparator.comparingInt(WorkoutDay::getDayNumber))
            .map(day -> new ArrayList<>(day.getExercises())).orElseGet(ArrayList::new);

        for (WeeklyDayRequest input : request.weekSchedule().stream()
                .sorted(Comparator.comparingInt(WeeklyDayRequest::dayNumber)).toList()) {
            WorkoutDay day = existing.get(input.dayNumber());
            if (day == null) {
                day = new WorkoutDay(); day.setDayNumber(input.dayNumber()); plan.addDay(day); existing.put(input.dayNumber(), day);
            }
            day.setDayName(input.dayName().trim());
            day.setFocus(input.focus().trim());
            day.setDayType(input.dayType());
            if (input.dayType() == DayType.WORKOUT && day.getExercises().isEmpty() && !exerciseTemplate.isEmpty()) {
                for (WorkoutExercise source : exerciseTemplate) {
                    WorkoutExercise copy = new WorkoutExercise();
                    copy.setExerciseName(source.getExerciseName()); copy.setMuscleGroup(source.getMuscleGroup());
                    copy.setSets(source.getSets()); copy.setReps(source.getReps()); copy.setRestSeconds(source.getRestSeconds());
                    copy.setInstructions(source.getInstructions()); copy.setImageUrl(source.getImageUrl());
                    copy.setSortOrder(source.getSortOrder()); day.addExercise(copy);
                }
            }
            if (input.dayType() == DayType.REST) day.getExercises().clear();
        }
        profiles.findByUser_Id(member.getId()).ifPresent(profile -> {
            int trainingDays = (int) plan.getDays().stream().filter(day -> day.getDayType() == DayType.WORKOUT).count();
            profile.setPlannedDaysPerWeek(Math.max(3, Math.min(6, trainingDays)));
            profiles.save(profile);
        });
        return PlanResponse.from(plans.save(plan));
    }

    @Transactional
    public List<WorkoutExercise> getExercisesForPlanDay(User member, java.time.DayOfWeek dayOfWeek) {
        WorkoutPlan plan = activePlan(member);
        int dayNumber = dayOfWeek.getValue();
        WorkoutDay day = plan.getDays().stream().filter(d -> d.getDayNumber() == dayNumber && d.getDayType() == DayType.WORKOUT).findFirst()
            .orElseGet(() -> plan.getDays().stream().filter(d -> d.getDayType() == DayType.WORKOUT).findFirst().orElse(null));
        return day == null ? List.of() : new ArrayList<>(day.getExercises());
    }

    private WorkoutPlan generateRecommendedPlan(User member) {
        FitnessProfile profile = profile(member);
        plans.findByAssignedTo_IdAndActiveTrueOrderByUpdatedAtDesc(member.getId()).forEach(p -> p.setActive(false));
        WorkoutPlan plan = new WorkoutPlan();
        plan.setName("Arohan Recommended Split");
        plan.setDescription("A progressive routine shaped around your training goal and experience.");
        plan.setSource(WorkoutSource.RECOMMENDED); plan.setFitnessGoal(profile.getFitnessGoal());
        plan.setCreatedBy(member); plan.setAssignedTo(member); plan.setActive(true);
        List<TemplateDay> templates = recommendedTemplate(profile.getFitnessGoal(), profile.getPlannedDaysPerWeek());
        int count = 0;
        for (TemplateDay template : templates) {
            if (count >= profile.getPlannedDaysPerWeek()) break;
            WorkoutDay day = new WorkoutDay(); day.setDayNumber(count + 1); day.setDayName(template.dayName());
            day.setFocus(template.focus()); day.setDayType(DayType.WORKOUT);
            int order = 0;
            for (ExerciseTemplate e : template.exercises()) {
                WorkoutExercise item = new WorkoutExercise(); item.setExerciseName(e.name()); item.setMuscleGroup(e.muscle());
                item.setSets(profile.getExperienceLevel() == ExperienceLevel.BEGINNER ? 3 : 4);
                item.setReps(profile.getFitnessGoal() == FitnessGoal.STRENGTH ? "4-6" : "8-12");
                item.setRestSeconds(profile.getFitnessGoal() == FitnessGoal.STRENGTH ? 120 : 60);
                item.setInstructions(e.instructions()); item.setImageUrl(e.image()); item.setSortOrder(order++);
                day.addExercise(item);
            }
            plan.addDay(day); count++;
        }
        for (int i = count; i < 7; i++) {
            WorkoutDay day = new WorkoutDay(); day.setDayNumber(i + 1); day.setDayName(List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").get(i));
            day.setFocus("Recovery"); day.setDayType(DayType.REST); plan.addDay(day);
        }
        return plans.save(plan);
    }

    @Transactional
    public PlanResponse createMemberCustomPlan(User member, PlanRequest request) {
        memberships.requireActiveMembership(member);
        deactivateActivePlans(member);
        WorkoutPlan plan = createPlan(member, member, WorkoutSource.CUSTOM, request);
        FitnessProfile profile = profile(member); profile.setActiveWorkoutSource(WorkoutSource.CUSTOM); profiles.save(profile);
        return PlanResponse.from(plans.save(plan));
    }

    @Transactional
    public PlanResponse createTrainerTemplate(User trainer, PlanRequest request) {
        WorkoutPlan plan = createPlan(trainer, null, WorkoutSource.TRAINER, request);
        plan.setActive(true);
        return PlanResponse.from(plans.save(plan));
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> getTrainerTemplates(User trainer) {
        return plans.findBySourceAndCreatedBy_IdAndAssignedToIsNullOrderByUpdatedAtDesc(WorkoutSource.TRAINER, trainer.getId())
            .stream().map(PlanResponse::from).toList();
    }

    @Transactional
    public PlanResponse updateTrainerTemplate(User trainer, UUID planId, PlanRequest request) {
        WorkoutPlan plan = plans.findById(planId).orElseThrow(() -> new ResourceNotFoundException("Workout plan was not found."));
        if (plan.getSource() != WorkoutSource.TRAINER || !plan.getCreatedBy().getId().equals(trainer.getId())) {
            throw new BusinessRuleException("PLAN_ACCESS_DENIED", "You can only edit workout templates created by your account.", HttpStatus.FORBIDDEN);
        }
        plan.setName(request.name().trim()); plan.setDescription(Objects.requireNonNullElse(request.description(), ""));
        plan.setFitnessGoal(request.fitnessGoal());
        plan.getDays().clear();
        plans.flush(); // delete old days first: Hibernate would otherwise INSERT new days before deleting, breaking uk_plan_day
        addDays(plan, request.days());
        return PlanResponse.from(plans.save(plan));
    }

    @Transactional
    public void deleteTrainerTemplate(User trainer, UUID planId) {
        WorkoutPlan plan = plans.findById(planId).orElseThrow(() -> new ResourceNotFoundException("Workout plan was not found."));
        if (plan.getSource() != WorkoutSource.TRAINER || !plan.getCreatedBy().getId().equals(trainer.getId())) {
            throw new BusinessRuleException("PLAN_ACCESS_DENIED", "You can only delete workout templates created by your account.", HttpStatus.FORBIDDEN);
        }
        plans.delete(plan);
    }

    @Transactional
    public PlanResponse assignTemplate(User trainer, UUID memberId, UUID planId) {
        User member = users.findById(memberId).orElseThrow(() -> new ResourceNotFoundException("Member was not found."));
        if (member.getRole() != Role.MEMBER) throw new BusinessRuleException("MEMBER_REQUIRED", "A workout can only be assigned to a member.");
        FitnessProfile profile = profile(member);
        if (trainer.getRole() != Role.ADMIN && (profile.getTrainer() == null || !profile.getTrainer().getId().equals(trainer.getId()))) {
            throw new BusinessRuleException("TRAINEE_ACCESS_DENIED", "This member is not assigned to your trainer account.", HttpStatus.FORBIDDEN);
        }
        WorkoutPlan template = plans.findById(planId).orElseThrow(() -> new ResourceNotFoundException("Workout template was not found."));
        if (template.getSource() != WorkoutSource.TRAINER || (trainer.getRole() != Role.ADMIN && !template.getCreatedBy().getId().equals(trainer.getId()))) {
            throw new BusinessRuleException("PLAN_ACCESS_DENIED", "You cannot assign this workout template.", HttpStatus.FORBIDDEN);
        }
        deactivateActivePlans(member);
        WorkoutPlan assigned = clonePlan(template, member);
        assigned.setSource(WorkoutSource.TRAINER); assigned.setCreatedBy(template.getCreatedBy()); assigned.setAssignedTo(member); assigned.setActive(true);
        profile.setActiveWorkoutSource(WorkoutSource.TRAINER); profiles.save(profile);
        return PlanResponse.from(plans.save(assigned));
    }

    @Transactional
    public PlanResponse updateAssignedPlan(User trainer, UUID memberId, PlanRequest request) {
        User member = users.findById(memberId).orElseThrow(() -> new ResourceNotFoundException("Member was not found."));
        FitnessProfile profile = profile(member);
        if (trainer.getRole() != Role.ADMIN && (profile.getTrainer() == null || !profile.getTrainer().getId().equals(trainer.getId()))) {
            throw new BusinessRuleException("TRAINEE_ACCESS_DENIED", "This member is not assigned to your trainer account.", HttpStatus.FORBIDDEN);
        }
        deactivateActivePlans(member);
        WorkoutPlan plan = plans.save(createPlan(trainer, member, WorkoutSource.TRAINER, request));
        profile.setActiveWorkoutSource(WorkoutSource.TRAINER); profiles.save(profile);
        return PlanResponse.from(plan);
    }

    @Transactional
    public void resetToRecommended(User member) {
        deactivateActivePlans(member);
        FitnessProfile profile = profile(member);
        profile.setActiveWorkoutSource(WorkoutSource.RECOMMENDED);
        profiles.save(profile);
        generateRecommendedPlan(member);
    }

    @Transactional
    public void prepareTrainerAssignment(User member) {
        deactivateActivePlans(member);
        FitnessProfile profile = profile(member);
        profile.setActiveWorkoutSource(WorkoutSource.TRAINER);
        profiles.save(profile);
    }

    @Transactional
    public void toggleDayCompletion(User member, int dayNumber) {
        memberships.requireActiveMembership(member);
        LocalDate today = LocalDate.now(zoneId);
        if (attendances.existsByUser_IdAndFacilityDateAndStatus(member.getId(), today, AttendanceStatus.COMPLETED)) {
            throw new BusinessRuleException("WORKOUT_ALREADY_COMPLETED_TODAY", "Only one workout can be completed per calendar day.");
        }
        if (attendances.findFirstByUser_IdAndStatusOrderByCheckInTimeDesc(member.getId(), AttendanceStatus.ACTIVE).isPresent()) {
            throw new BusinessRuleException("ACTIVE_SESSION_EXISTS", "Check out of your current session before marking a workout complete.");
        }
        WorkoutPlan plan = activePlan(member);
        WorkoutDay day = plan.getDays().stream().filter(d -> d.getDayNumber() == dayNumber).findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Workout day was not found."));
        if (day.getDayType() == DayType.REST) throw new BusinessRuleException("REST_DAY", "Choose a workout day to record a completed session.");
        Attendance attendance = new Attendance(); attendance.setUser(member); attendance.setFacilityDate(today);
        attendance.setWorkoutDayNumber(day.getDayNumber()); attendance.setWorkoutDayName(day.getDayName());
        attendance.setCheckInTime(Instant.now()); attendance.setCheckOutTime(Instant.now()); attendance.setStatus(AttendanceStatus.COMPLETED);
        attendance.setEstimatedDurationMinutes(45); attendance.setDurationMinutes(0);
        int order = 0;
        for (WorkoutExercise exercise : day.getExercises()) {
            AttendanceExercise record = new AttendanceExercise(); record.setSourceExerciseId(exercise.getId().toString());
            record.setExerciseName(exercise.getExerciseName()); record.setSets(exercise.getSets()); record.setReps(exercise.getReps());
            record.setRestSeconds(exercise.getRestSeconds()); record.setImageUrl(exercise.getImageUrl()); record.setSortOrder(order++);
            record.setCompleted(true); record.setCompletedAt(Instant.now()); attendance.addExercise(record);
        }
        attendances.save(attendance);
    }

    private WorkoutPlan createPlan(User creator, User assigned, WorkoutSource source, PlanRequest request) {
        WorkoutPlan plan = new WorkoutPlan(); plan.setName(request.name().trim());
        plan.setDescription(Objects.requireNonNullElse(request.description(), "")); plan.setSource(source);
        plan.setFitnessGoal(request.fitnessGoal()); plan.setCreatedBy(creator); plan.setAssignedTo(assigned); plan.setActive(true);
        addDays(plan, request.days()); return plan;
    }

    private void addDays(WorkoutPlan plan, List<DayRequest> dayRequests) {
        Set<Integer> dayNumbers = new HashSet<>();
        for (DayRequest input : dayRequests) {
            if (!dayNumbers.add(input.dayNumber())) throw new BusinessRuleException("DUPLICATE_WORKOUT_DAY", "Each workout day number must be unique.");
            WorkoutDay day = new WorkoutDay(); day.setDayNumber(input.dayNumber()); day.setDayName(input.dayName().trim());
            day.setFocus(input.focus().trim()); day.setDayType(input.dayType());
            int order = 0;
            if (input.exercises() != null) for (ExerciseRequest inputExercise : input.exercises()) {
                WorkoutExercise exercise = new WorkoutExercise(); exercise.setExerciseName(inputExercise.exerciseName().trim());
                exercise.setMuscleGroup(inputExercise.muscleGroup().trim()); exercise.setSets(inputExercise.sets());
                exercise.setReps(inputExercise.reps().trim()); exercise.setRestSeconds(inputExercise.restSeconds());
                exercise.setInstructions(Objects.requireNonNullElse(inputExercise.instructions(), ""));
                exercise.setImageUrl(Objects.requireNonNullElse(inputExercise.imageUrl(), "")); exercise.setSortOrder(order++);
                day.addExercise(exercise);
            }
            plan.addDay(day);
        }
    }

    private WorkoutPlan clonePlan(WorkoutPlan source, User member) {
        WorkoutPlan target = new WorkoutPlan(); target.setName(source.getName()); target.setDescription(source.getDescription());
        target.setFitnessGoal(source.getFitnessGoal()); target.setSource(source.getSource()); target.setCreatedBy(source.getCreatedBy());
        target.setAssignedTo(member); target.setActive(true);
        for (WorkoutDay originalDay : source.getDays()) {
            WorkoutDay day = new WorkoutDay(); day.setDayNumber(originalDay.getDayNumber()); day.setDayName(originalDay.getDayName());
            day.setFocus(originalDay.getFocus()); day.setDayType(originalDay.getDayType());
            for (WorkoutExercise original : originalDay.getExercises()) {
                WorkoutExercise exercise = new WorkoutExercise(); exercise.setExerciseName(original.getExerciseName());
                exercise.setMuscleGroup(original.getMuscleGroup()); exercise.setSets(original.getSets()); exercise.setReps(original.getReps());
                exercise.setRestSeconds(original.getRestSeconds()); exercise.setInstructions(original.getInstructions());
                exercise.setImageUrl(original.getImageUrl()); exercise.setSortOrder(original.getSortOrder()); day.addExercise(exercise);
            }
            target.addDay(day);
        }
        return target;
    }

    private void deactivateActivePlans(User member) {
        plans.findByAssignedTo_IdAndActiveTrueOrderByUpdatedAtDesc(member.getId()).forEach(p -> p.setActive(false));
    }

    private FitnessProfile profile(User user) {
        return profiles.findByUser_Id(user.getId()).orElseThrow(() -> new ResourceNotFoundException("Fitness profile was not found."));
    }

    private record ExerciseTemplate(String name, String muscle, String instructions, String image) {}
    private record TemplateDay(String dayName, String focus, List<ExerciseTemplate> exercises) {}

    private List<TemplateDay> recommendedTemplate(FitnessGoal goal, int dayCount) {
        List<TemplateDay> full = List.of(
            new TemplateDay("Monday", "Chest + Triceps", List.of(e("Bench Press", "Chest", "Keep shoulders retracted and lower with control.", "bench-press.svg"), e("Incline Press", "Chest", "Use a controlled range and avoid bouncing.", "incline-press.svg"), e("Tricep Pushdown", "Arms", "Keep elbows close to your torso.", "tricep-pushdown.svg"))),
            new TemplateDay("Tuesday", "Back + Biceps", List.of(e("Lat Pulldown", "Back", "Pull elbows down without swinging.", "lat-pulldown.svg"), e("Cable Row", "Back", "Keep your spine neutral and squeeze shoulder blades.", "cable-row.svg"), e("Bicep Curl", "Arms", "Avoid momentum and keep upper arms still.", "bicep-curl.svg"))),
            new TemplateDay("Wednesday", "Legs + Core", List.of(e("Squats", "Legs", "Brace your core and keep knees tracking with toes.", "squats.svg"), e("Leg Press", "Legs", "Do not lock your knees at the top.", "leg-press.svg"), e("Plank", "Core", "Maintain a straight line from head to heels.", "plank.svg"))),
            new TemplateDay("Thursday", "Shoulders + Core", List.of(e("Shoulder Press", "Shoulders", "Press overhead without arching your lower back.", "shoulder-press.svg"), e("Lateral Raise", "Shoulders", "Raise with control and avoid shrugging.", "lateral-raise.svg"), e("Plank", "Core", "Keep hips level and breathe steadily.", "plank.svg"))),
            new TemplateDay("Friday", "Posterior Chain", List.of(e("Deadlift", "Back + Legs", "Hinge at the hips and keep the bar close.", "deadlift.svg"), e("Lunges", "Legs", "Take a balanced step and keep your torso tall.", "lunges.svg"), e("Pushup", "Chest", "Keep your body aligned and elbows controlled.", "pushup.svg"))),
            new TemplateDay("Saturday", "Full Body", List.of(e("Squats", "Legs", "Brace the trunk before each rep.", "squats.svg"), e("Cable Row", "Back", "Pull evenly without rocking.", "cable-row.svg"), e("Pushup", "Chest", "Use a range you can control.", "pushup.svg")))
        );
        if (goal == FitnessGoal.ENDURANCE || goal == FitnessGoal.FAT_LOSS) {
            return full.stream().map(d -> new TemplateDay(d.dayName(), goal == FitnessGoal.FAT_LOSS ? "Strength + Conditioning · " + d.focus() : "Work Capacity · " + d.focus(), d.exercises())).toList();
        }
        return full;
    }
    private ExerciseTemplate e(String name, String muscle, String instruction, String image) {
        return new ExerciseTemplate(name, muscle, instruction, "/exercises/" + image);
    }
}
