package com.arohan.strengthclub.service;

import com.arohan.strengthclub.dto.MemberDtos.FitnessLevelPromotionResponse;
import com.arohan.strengthclub.dto.MemberDtos.FitnessLevelResponse;
import com.arohan.strengthclub.dto.MemberDtos.MonthlyConsistencyResponse;
import com.arohan.strengthclub.entity.Attendance;
import com.arohan.strengthclub.entity.FitnessLevelPromotion;
import com.arohan.strengthclub.entity.FitnessProfile;
import com.arohan.strengthclub.entity.User;
import com.arohan.strengthclub.entity.enums.AttendanceStatus;
import com.arohan.strengthclub.entity.enums.ExperienceLevel;
import com.arohan.strengthclub.exception.ResourceNotFoundException;
import com.arohan.strengthclub.repository.AttendanceRepository;
import com.arohan.strengthclub.repository.FitnessLevelPromotionRepository;
import com.arohan.strengthclub.repository.FitnessProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Computes completed-calendar-month consistency on the server and records one-way level promotions. */
@Service
public class FitnessLevelService {
    private final FitnessProfileRepository profiles;
    private final AttendanceRepository attendances;
    private final FitnessLevelPromotionRepository promotions;
    private final WorkoutService workouts;
    private final ZoneId zoneId;

    public FitnessLevelService(FitnessProfileRepository profiles, AttendanceRepository attendances,
            FitnessLevelPromotionRepository promotions, WorkoutService workouts, GymService gymService) {
        this.profiles = profiles;
        this.attendances = attendances;
        this.promotions = promotions;
        this.workouts = workouts;
        this.zoneId = gymService.getZoneId();
    }

    @Transactional(readOnly = true)
    public FitnessLevelResponse getLevel(User user) {
        FitnessProfile profile = requireProfile(user);
        List<MonthlyConsistencyResponse> monthly = calculateMonthlyHistory(profile, levelHistory(user));
        List<MonthlyConsistencyResponse> eligibleMonths = monthsSinceCurrentLevel(profile, monthly);
        return response(profile, monthly, false, null, trailingStreak(eligibleMonths, 90.0), trailingStreak(eligibleMonths, 80.0));
    }

    @Transactional
    public FitnessLevelResponse checkPromotion(User user) {
        FitnessProfile profile = requireProfile(user);
        List<MonthlyConsistencyResponse> monthly = calculateMonthlyHistory(profile, levelHistory(user));
        List<MonthlyConsistencyResponse> eligibleMonths = monthsSinceCurrentLevel(profile, monthly);
        int streak90 = trailingStreak(eligibleMonths, 90.0);
        int streak80 = trailingStreak(eligibleMonths, 80.0);
        PromotionRule rule = ruleFor(profile.getCurrentLevel());
        boolean qualifies90 = streak90 >= rule.monthsAt90();
        boolean qualifies80 = streak80 >= rule.monthsAt80();

        if ((qualifies90 || qualifies80) && rule.nextLevel() != null) {
            ExperienceLevel previous = profile.getCurrentLevel();
            ExperienceLevel next = rule.nextLevel();
            String reason = qualifies90
                    ? rule.monthsAt90() + " consecutive completed months at 90%+ consistency"
                    : rule.monthsAt80() + " consecutive completed months at 80%+ consistency";
            Instant now = Instant.now();
            FitnessLevelPromotion event = new FitnessLevelPromotion();
            event.setProfile(profile);
            event.setFromLevel(previous);
            event.setToLevel(next);
            event.setPromotedAt(now);
            event.setReason(reason);
            promotions.save(event);

            profile.setCurrentLevel(next);
            profile.setExperienceLevel(next);
            profile.setLevelSince(now);
            profiles.save(profile);

            // Only auto-generated plans are regenerated. Custom and trainer-assigned work stays intact.
            if (profile.getActiveWorkoutSource() == com.arohan.strengthclub.entity.enums.WorkoutSource.RECOMMENDED) {
                workouts.resetToRecommended(user);
            }
            return response(profile, monthly, true, reason, streak90, streak80);
        }

        String reason = rule.nextLevel() == null
                ? "You are already at the highest fitness level."
                : "Keep building consistency. Promotion is evaluated from complete calendar months at your current level.";
        return response(profile, monthly, false, reason, streak90, streak80);
    }

    /** The monthly view covers 18 completed months, so older sessions are never needed. */
    private List<Attendance> levelHistory(User user) {
        LocalDate since = LocalDate.now(zoneId).minusMonths(19).withDayOfMonth(1);
        return attendances.findByUser_IdAndFacilityDateGreaterThanEqualOrderByCheckInTimeDesc(user.getId(), since);
    }

    private FitnessProfile requireProfile(User user) {
        return profiles.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Fitness profile was not found."));
    }

    private List<MonthlyConsistencyResponse> calculateMonthlyHistory(FitnessProfile profile, List<Attendance> history) {
        LocalDate today = LocalDate.now(zoneId);
        YearMonth lastCompletedMonth = YearMonth.from(today).minusMonths(1);
        YearMonth firstAvailableMonth = YearMonth.from(profile.getCreatedAt().atZone(zoneId).toLocalDate());
        YearMonth lowerBound = lastCompletedMonth.minusMonths(17);
        if (firstAvailableMonth.isAfter(lowerBound)) lowerBound = firstAvailableMonth;
        if (lowerBound.isAfter(lastCompletedMonth)) return List.of();

        Map<YearMonth, Set<LocalDate>> completedDays = new HashMap<>();
        for (Attendance attendance : history) {
            if (attendance.getStatus() != AttendanceStatus.COMPLETED || attendance.getFacilityDate() == null) continue;
            YearMonth month = YearMonth.from(attendance.getFacilityDate());
            if (!month.isBefore(lowerBound) && !month.isAfter(lastCompletedMonth)) {
                completedDays.computeIfAbsent(month, ignored -> new HashSet<>()).add(attendance.getFacilityDate());
            }
        }

        List<MonthlyConsistencyResponse> result = new ArrayList<>();
        for (YearMonth month = lowerBound; !month.isAfter(lastCompletedMonth); month = month.plusMonths(1)) {
            int completed = completedDays.getOrDefault(month, Set.of()).size();
            double expectedRaw = profile.getPlannedDaysPerWeek() * month.lengthOfMonth() / 7.0;
            int expected = Math.max(1, (int) Math.ceil(expectedRaw));
            double percentage = Math.min(100.0, completed * 100.0 / expected);
            result.add(new MonthlyConsistencyResponse(month.toString(), round(percentage), completed, expected));
        }
        return List.copyOf(result);
    }

    private List<MonthlyConsistencyResponse> monthsSinceCurrentLevel(FitnessProfile profile,
            List<MonthlyConsistencyResponse> monthly) {
        YearMonth levelStarted = YearMonth.from(profile.getLevelSince().atZone(zoneId).toLocalDate());
        return monthly.stream().filter(month -> YearMonth.parse(month.month()).isAfter(levelStarted)).toList();
    }

    private int trailingStreak(List<MonthlyConsistencyResponse> months, double threshold) {
        int streak = 0;
        for (int i = months.size() - 1; i >= 0; i--) {
            if (months.get(i).percentage() >= threshold) streak++;
            else break;
        }
        return streak;
    }

    private FitnessLevelResponse response(FitnessProfile profile, List<MonthlyConsistencyResponse> monthly,
            boolean promoted, String reason, int streak90, int streak80) {
        List<FitnessLevelPromotionResponse> history = promotions.findByProfile_IdOrderByPromotedAtDesc(profile.getId())
                .stream().map(p -> new FitnessLevelPromotionResponse(p.getId(), p.getFromLevel(), p.getToLevel(),
                        p.getPromotedAt(), p.getReason())).toList();
        return new FitnessLevelResponse(profile.getCurrentLevel(), profile.getInitialLevel(), profile.getLevelSince(),
                history, monthly, streak90, streak80, promoted, reason);
    }

    private PromotionRule ruleFor(ExperienceLevel level) {
        return switch (level) {
            case BEGINNER -> new PromotionRule(ExperienceLevel.INTERMEDIATE, 6, 8);
            case INTERMEDIATE -> new PromotionRule(ExperienceLevel.ADVANCED, 8, 12);
            case ADVANCED -> new PromotionRule(ExperienceLevel.PRO, 12, 18);
            case PRO -> new PromotionRule(null, Integer.MAX_VALUE, Integer.MAX_VALUE);
        };
    }

    private double round(double value) { return Math.round(value * 10.0) / 10.0; }

    private record PromotionRule(ExperienceLevel nextLevel, int monthsAt90, int monthsAt80) {}
}
