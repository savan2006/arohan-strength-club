package com.arohan.strengthclub.controller;

import com.arohan.strengthclub.dto.AttendanceDtos.SessionResponse;
import com.arohan.strengthclub.entity.User;
import com.arohan.strengthclub.security.CurrentUserService;
import com.arohan.strengthclub.service.AttendanceService;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Member attendance endpoints for check-in, check-out, and exercise tracking. */
@RestController
@RequestMapping("/api/member/attendance")
@Validated
public class AttendanceController {

    private final CurrentUserService currentUsers;
    private final AttendanceService attendanceService;

    public AttendanceController(CurrentUserService currentUsers, AttendanceService attendanceService) {
        this.currentUsers = currentUsers;
        this.attendanceService = attendanceService;
    }

    @GetMapping
    public List<SessionResponse> history(Authentication auth) {
        return attendanceService.getHistory(user(auth));
    }

    @GetMapping("/current-session")
    public Map<String, Object> current(Authentication auth) {
        User currentUser = user(auth);
        return attendanceService.currentSession(currentUser)
                .<Map<String, Object>>map(session -> Map.of("active", true, "session", session))
                .orElseGet(() -> Map.of("active", false, "session", ""));
    }

    @PostMapping("/check-in")
    public SessionResponse checkIn(Authentication auth) {
        return attendanceService.checkIn(user(auth));
    }

    @PostMapping("/check-out")
    public SessionResponse checkOut(Authentication auth) {
        return attendanceService.checkOut(user(auth));
    }

    @PatchMapping("/active/exercise/{exerciseId}/toggle")
    public SessionResponse toggleExercise(
            Authentication auth,
            @PathVariable @NotNull UUID exerciseId) {
        return attendanceService.toggleExercise(user(auth), exerciseId);
    }

    private User user(Authentication auth) {
        return currentUsers.requireUser(auth);
    }
}
