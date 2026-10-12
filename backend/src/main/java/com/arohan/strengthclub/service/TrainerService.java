package com.arohan.strengthclub.service;

import com.arohan.strengthclub.dto.AttendanceDtos.SessionResponse;
import com.arohan.strengthclub.dto.MemberDtos.ConsistencyResponse;
import com.arohan.strengthclub.dto.MemberDtos.ProfileResponse;
import com.arohan.strengthclub.dto.TrainerDtos.MemberDetailResponse;
import com.arohan.strengthclub.dto.TrainerDtos.MemberResponse;
import com.arohan.strengthclub.dto.WorkoutDtos.PlanRequest;
import com.arohan.strengthclub.dto.WorkoutDtos.PlanResponse;
import com.arohan.strengthclub.entity.FitnessProfile;
import com.arohan.strengthclub.entity.User;
import com.arohan.strengthclub.entity.enums.Role;
import com.arohan.strengthclub.exception.BusinessRuleException;
import com.arohan.strengthclub.exception.ResourceNotFoundException;
import com.arohan.strengthclub.repository.AttendanceRepository;
import com.arohan.strengthclub.repository.FitnessProfileRepository;
import com.arohan.strengthclub.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Enforces trainer-to-member ownership rules and builds trainer workspace responses. */
@Service
public class TrainerService {

    private final FitnessProfileRepository profiles;
    private final UserRepository users;
    private final AttendanceRepository attendances;
    private final WorkoutService workoutService;
    private final AttendanceService attendanceService;

    public TrainerService(
            FitnessProfileRepository profiles,
            UserRepository users,
            AttendanceRepository attendances,
            WorkoutService workoutService,
            AttendanceService attendanceService) {
        this.profiles = profiles;
        this.users = users;
        this.attendances = attendances;
        this.workoutService = workoutService;
        this.attendanceService = attendanceService;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(User trainer) {
        List<FitnessProfile> assignedMembers = trainer.getRole() == Role.ADMIN
                ? profiles.findAllWithUserAndTrainer()
                : profiles.findByTrainer_Id(trainer.getId());
        List<FitnessProfile> memberProfiles = assignedMembers.stream()
                .filter(profile -> profile.getUser().getRole() == Role.MEMBER)
                .toList();
        Map<UUID, ConsistencyResponse> consistencyByMember =
                attendanceService.getConsistencyForProfiles(memberProfiles);

        return memberProfiles.stream()
                .map(profile -> {
                    User member = profile.getUser();
                    ConsistencyResponse consistency = consistencyByMember.get(member.getId());
                    return new MemberResponse(
                            member.getId(),
                            member.getFullName(),
                            member.getEmail(),
                            member.getPhone(),
                            profile.getFitnessGoal().name(),
                            profile.getExperienceLevel().name(),
                            profile.getMembershipStatus().name(),
                            profile.getPlannedDaysPerWeek(),
                            profile.isTrainerRequested(),
                            consistency.percentage(),
                            member.getCreatedAt());
                })
                .toList();
    }

    @Transactional
    public MemberDetailResponse getMemberDetails(User trainer, UUID memberId) {
        User member = users.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member was not found."));
        FitnessProfile profile = profiles.findByUser_Id(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member profile was not found."));
        requireAccess(trainer, profile);

        List<SessionResponse> recentSessions = attendances
                .findByUser_IdOrderByCheckInTimeDesc(memberId, PageRequest.of(0, 12))
                .stream()
                .map(SessionResponse::from)
                .toList();

        return new MemberDetailResponse(
                ProfileResponse.from(member, profile),
                workoutService.getActivePlan(member),
                recentSessions);
    }

    @Transactional
    public PlanResponse assignPlan(User trainer, UUID memberId, UUID planId) {
        return workoutService.assignTemplate(trainer, memberId, planId);
    }

    @Transactional
    public PlanResponse updateMemberPlan(User trainer, UUID memberId, PlanRequest request) {
        requireMemberAccess(trainer, memberId);
        return workoutService.updateAssignedPlan(trainer, memberId, request);
    }

    private void requireMemberAccess(User trainer, UUID memberId) {
        FitnessProfile profile = profiles.findByUser_Id(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member profile was not found."));
        requireAccess(trainer, profile);
    }

    private void requireAccess(User trainer, FitnessProfile profile) {
        if (profile.getUser().getRole() != Role.MEMBER) {
            throw new BusinessRuleException("MEMBER_REQUIRED", "This account is not a member.");
        }
        boolean assignedToTrainer = profile.getTrainer() != null
                && profile.getTrainer().getId().equals(trainer.getId());
        if (trainer.getRole() != Role.ADMIN && !assignedToTrainer) {
            throw new BusinessRuleException(
                    "TRAINEE_ACCESS_DENIED",
                    "This member is not assigned to your trainer account.",
                    HttpStatus.FORBIDDEN);
        }
    }
}
