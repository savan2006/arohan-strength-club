package com.arohan.strengthclub.service;

import com.arohan.strengthclub.dto.AuthDtos.AuthResponse;
import com.arohan.strengthclub.dto.AuthDtos.LoginRequest;
import com.arohan.strengthclub.dto.AuthDtos.RegisterRequest;
import com.arohan.strengthclub.dto.AuthDtos.UserResponse;
import com.arohan.strengthclub.entity.FitnessProfile;
import com.arohan.strengthclub.entity.User;
import com.arohan.strengthclub.entity.enums.ExperienceLevel;
import com.arohan.strengthclub.entity.enums.FitnessGoal;
import com.arohan.strengthclub.entity.enums.Role;
import com.arohan.strengthclub.exception.BusinessRuleException;
import com.arohan.strengthclub.repository.FitnessProfileRepository;
import com.arohan.strengthclub.repository.UserRepository;
import com.arohan.strengthclub.security.JwtService;
import com.arohan.strengthclub.security.LoginAttemptService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Locale;

/** Handles account registration, credential validation, and user identity lookups. */
@Service
public class AuthService {

    private final UserRepository users;
    private final FitnessProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttempts;
    private final TransactionTemplate tx;
    /** Compared against when the email is unknown so response time does not reveal which emails exist. */
    private final String dummyHash;

    public AuthService(
            UserRepository users,
            FitnessProfileRepository profiles,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LoginAttemptService loginAttempts,
            TransactionTemplate tx) {
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttempts = loginAttempts;
        this.tx = tx;
        this.dummyHash = passwordEncoder.encode("arohan-timing-equaliser");
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        // BCrypt is slow by design: hash before opening the transaction so no pooled connection is pinned.
        String passwordHash = passwordEncoder.encode(request.password());
        User user = tx.execute(status -> createAccount(request, email, passwordHash));
        return new AuthResponse(jwtService.issueToken(user), UserResponse.from(user));
    }

    private User createAccount(RegisterRequest request, String email, String passwordHash) {
        if (users.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException(
                    "EMAIL_ALREADY_REGISTERED",
                    "An account already exists for this email.",
                    HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(request.phone().trim());
        user.setPasswordHash(passwordHash);
        user.setRole(Role.MEMBER);
        users.save(user);

        // Set safe defaults for optional profile fields supplied by older clients.
        FitnessProfile profile = new FitnessProfile();
        profile.setUser(user);
        profile.setAge(request.age() == null ? 25 : request.age());
        profile.setHeightCm(request.heightCm() == null ? 175 : request.heightCm());
        profile.setWeightKg(request.weightKg() == null ? 72 : request.weightKg());
        profile.setFitnessGoal(request.fitnessGoal() == null
                ? FitnessGoal.GENERAL_FITNESS
                : request.fitnessGoal());
        profile.setExperienceLevel(request.experienceLevel() == null
                ? ExperienceLevel.BEGINNER
                : request.experienceLevel());
        profile.setCurrentLevel(profile.getExperienceLevel());
        profile.setInitialLevel(profile.getExperienceLevel());
        profile.setLevelSince(Instant.now());
        profile.setPlannedDaysPerWeek(request.plannedDaysPerWeek() == null
                ? 4
                : request.plannedDaysPerWeek());
        profile.setPreferredSchedule(request.preferredSchedule() == null
                ? "EVENING"
                : request.preferredSchedule());
        profiles.save(profile);
        return user;
    }

    // Deliberately not @Transactional: BCrypt takes ~100ms and must not pin a pooled connection.
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim();
        if (loginAttempts.isBlocked(email)) {
            throw new BusinessRuleException(
                    "TOO_MANY_ATTEMPTS",
                    "Too many failed sign-in attempts. Please wait a few minutes and try again.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
        User user = users.findByEmail(email.toLowerCase(Locale.ROOT))
                .or(() -> users.findByEmailIgnoreCase(email))
                .orElse(null);

        boolean valid = false;
        if (user != null) {
            valid = passwordEncoder.matches(request.password(), user.getPasswordHash());
        } else {
            passwordEncoder.matches(request.password(), dummyHash); // result intentionally ignored
        }
        if (!valid) {
            loginAttempts.recordFailure(email);
            throw new BusinessRuleException(
                    "INVALID_CREDENTIALS",
                    "Email or password is incorrect.",
                    HttpStatus.UNAUTHORIZED);
        }
        loginAttempts.recordSuccess(email);

        return new AuthResponse(jwtService.issueToken(user), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        return UserResponse.from(users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessRuleException(
                        "USER_NOT_FOUND",
                        "Account was not found.",
                        HttpStatus.NOT_FOUND)));
    }
}
