package com.arohan.strengthclub.controller;

import com.arohan.strengthclub.dto.TrainerDtos.AssignPlanRequest;
import com.arohan.strengthclub.dto.TrainerDtos.MemberDetailResponse;
import com.arohan.strengthclub.dto.TrainerDtos.MemberResponse;
import com.arohan.strengthclub.dto.WorkoutDtos.PlanRequest;
import com.arohan.strengthclub.dto.WorkoutDtos.PlanResponse;
import com.arohan.strengthclub.entity.User;
import com.arohan.strengthclub.exception.ResourceNotFoundException;
import com.arohan.strengthclub.security.CurrentUserService;
import com.arohan.strengthclub.service.TrainerService;
import com.arohan.strengthclub.service.WorkoutService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Endpoints for trainers to manage assigned members and reusable workout plans. */
@RestController
@RequestMapping("/api/trainer")
public class TrainerController {

    private final CurrentUserService currentUsers;
    private final TrainerService trainerService;
    private final WorkoutService workouts;

    public TrainerController(
            CurrentUserService currentUsers,
            TrainerService trainerService,
            WorkoutService workouts) {
        this.currentUsers = currentUsers;
        this.trainerService = trainerService;
        this.workouts = workouts;
    }

    @GetMapping("/members")
    public List<MemberResponse> members(Authentication auth) {
        return trainerService.getMembers(user(auth));
    }

    @GetMapping("/members/{memberId}")
    public MemberDetailResponse member(Authentication auth, @PathVariable UUID memberId) {
        return trainerService.getMemberDetails(user(auth), memberId);
    }

    @PostMapping("/members/{memberId}/assign-plan")
    public PlanResponse assign(
            Authentication auth,
            @PathVariable UUID memberId,
            @Valid @RequestBody AssignPlanRequest body) {
        return trainerService.assignPlan(user(auth), memberId, body.planId());
    }

    @PutMapping("/members/{memberId}/workout-plan")
    public PlanResponse updateMemberPlan(
            Authentication auth,
            @PathVariable UUID memberId,
            @Valid @RequestBody PlanRequest body) {
        return trainerService.updateMemberPlan(user(auth), memberId, body);
    }

    @GetMapping("/plans")
    public List<PlanResponse> plans(Authentication auth) {
        return workouts.getTrainerTemplates(user(auth));
    }

    @PostMapping("/plans")
    public PlanResponse createPlan(Authentication auth, @Valid @RequestBody PlanRequest body) {
        return workouts.createTrainerTemplate(user(auth), body);
    }

    @GetMapping("/plans/{planId}")
    public PlanResponse plan(Authentication auth, @PathVariable UUID planId) {
        return workouts.getTrainerTemplates(user(auth)).stream()
                .filter(candidate -> candidate.id().equals(planId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Workout plan was not found."));
    }

    @PutMapping("/plans/{planId}")
    public PlanResponse updatePlan(
            Authentication auth,
            @PathVariable UUID planId,
            @Valid @RequestBody PlanRequest body) {
        return workouts.updateTrainerTemplate(user(auth), planId, body);
    }

    @DeleteMapping("/plans/{planId}")
    public void deletePlan(Authentication auth, @PathVariable UUID planId) {
        workouts.deleteTrainerTemplate(user(auth), planId);
    }

    private User user(Authentication auth) {
        return currentUsers.requireUser(auth);
    }
}
