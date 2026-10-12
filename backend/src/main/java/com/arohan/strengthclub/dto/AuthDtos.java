package com.arohan.strengthclub.dto;

import com.arohan.strengthclub.entity.User;
import com.arohan.strengthclub.entity.enums.ExperienceLevel;
import com.arohan.strengthclub.entity.enums.FitnessGoal;
import com.arohan.strengthclub.entity.enums.Role;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Request and response contracts for registration, login, and identity. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "[0-9+()\\- ]{8,20}") String phone,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Min(14) @Max(100) Integer age,
            @Min(50) @Max(260) Integer heightCm,
            @DecimalMin("30.0") @DecimalMax("300.0") Double weightKg,
            FitnessGoal fitnessGoal,
            ExperienceLevel experienceLevel,
            @Min(3) @Max(6) Integer plannedDaysPerWeek,
            @Pattern(regexp = "MORNING|AFTERNOON|EVENING|FLEXIBLE") String preferredSchedule) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record UserResponse(
            UUID id,
            String fullName,
            String email,
            String phone,
            Role role,
            String specialization) {

        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRole(),
                    user.getSpecialization());
        }
    }

    public record AuthResponse(String token, UserResponse user) {
    }
}
