package com.arohan.strengthclub.controller;

import com.arohan.strengthclub.dto.AdminDtos.AdminUserResponse;
import com.arohan.strengthclub.dto.AdminDtos.AssignTrainerRequest;
import com.arohan.strengthclub.dto.AdminDtos.GymClosureRequest;
import com.arohan.strengthclub.dto.AdminDtos.GymClosureResponse;
import com.arohan.strengthclub.dto.AdminDtos.MembershipPlanRequest;
import com.arohan.strengthclub.dto.AdminDtos.MembershipPlanResponse;
import com.arohan.strengthclub.dto.AdminDtos.OverviewResponse;
import com.arohan.strengthclub.dto.AdminDtos.ScheduleUpdateRequest;
import com.arohan.strengthclub.dto.AdminDtos.UpdateUserRequest;
import com.arohan.strengthclub.dto.AttendanceDtos.ScheduleDayResponse;
import com.arohan.strengthclub.entity.User;
import com.arohan.strengthclub.security.AccountStatusService;
import com.arohan.strengthclub.security.CurrentUserService;
import com.arohan.strengthclub.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** HTTP endpoints for administrator-level club operations. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final CurrentUserService currentUsers;
    private final AccountStatusService accountStatus;

    public AdminController(AdminService adminService, CurrentUserService currentUsers,
                           AccountStatusService accountStatus) {
        this.adminService = adminService;
        this.currentUsers = currentUsers;
        this.accountStatus = accountStatus;
    }

    @GetMapping("/system-overview")
    public OverviewResponse overview() {
        return adminService.getOverview();
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users() {
        return adminService.getUsers();
    }

    @PatchMapping({"/users/{userId}", "/users/{userId}/status"})
    public AdminUserResponse updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest body) {
        AdminUserResponse updated = adminService.updateUser(userId, body);
        accountStatus.evict(userId); // role changes must apply to the very next request
        return updated;
    }

    @PutMapping("/members/{memberId}/trainer")
    public AdminUserResponse assignTrainer(
            @PathVariable UUID memberId,
            @RequestBody AssignTrainerRequest body) {
        return adminService.assignTrainer(memberId, body);
    }

    @DeleteMapping("/users/{userId}")
    public Map<String, String> deleteUser(Authentication auth, @PathVariable UUID userId) {
        User currentAdmin = currentUsers.requireUser(auth);
        adminService.deleteUser(userId, currentAdmin);
        accountStatus.evict(userId);
        return Map.of("message", "User account deleted.");
    }

    @GetMapping("/schedule")
    public List<ScheduleDayResponse> schedule() {
        return adminService.getSchedule();
    }

    @PutMapping("/schedule")
    public List<ScheduleDayResponse> updateSchedule(@Valid @RequestBody ScheduleUpdateRequest body) {
        return adminService.updateSchedule(body);
    }

    @GetMapping("/closures")
    public List<GymClosureResponse> closures() {
        return adminService.getClosures();
    }

    @PostMapping("/closures")
    public GymClosureResponse createClosure(
            Authentication auth,
            @Valid @RequestBody GymClosureRequest body) {
        return adminService.createClosure(body, currentUsers.requireUser(auth));
    }

    @PutMapping("/closures/{id}")
    public GymClosureResponse updateClosure(
            @PathVariable UUID id,
            @Valid @RequestBody GymClosureRequest body) {
        return adminService.updateClosure(id, body);
    }

    @DeleteMapping("/closures/{id}")
    public Map<String, String> deleteClosure(@PathVariable UUID id) {
        adminService.deleteClosure(id);
        return Map.of("message", "Closure removed.");
    }

    @GetMapping({"/plans", "/membership-plans"})
    public List<MembershipPlanResponse> plans() {
        return adminService.getAllMembershipPlans();
    }

    @PostMapping({"/plans", "/membership-plans"})
    public MembershipPlanResponse createPlan(@Valid @RequestBody MembershipPlanRequest body) {
        return adminService.createMembershipPlan(body);
    }

    @PutMapping({"/plans/{id}", "/membership-plans/{id}"})
    public MembershipPlanResponse updatePlan(
            @PathVariable UUID id,
            @Valid @RequestBody MembershipPlanRequest body) {
        return adminService.updateMembershipPlan(id, body);
    }
}
