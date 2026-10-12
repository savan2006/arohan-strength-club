package com.arohan.strengthclub.service;

import com.arohan.strengthclub.entity.FitnessProfile;
import com.arohan.strengthclub.entity.GymOperatingHour;
import com.arohan.strengthclub.entity.MembershipPlan;
import com.arohan.strengthclub.entity.User;
import com.arohan.strengthclub.entity.enums.Role;
import com.arohan.strengthclub.repository.GymOperatingHourRepository;
import com.arohan.strengthclub.repository.MembershipPlanRepository;
import com.arohan.strengthclub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

/** Seeds baseline reference data and an optional administrator configured by environment variables. */
@Component
public class DataInitializer implements CommandLineRunner {

    private final MembershipPlanRepository plans;
    private final GymOperatingHourRepository hours;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminName;

    public DataInitializer(
            MembershipPlanRepository plans,
            GymOperatingHourRepository hours,
            UserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed-admin.email:}") String adminEmail,
            @Value("${app.seed-admin.password:}") String adminPassword,
            @Value("${app.seed-admin.name:AROHAN Administrator}") String adminName) {
        this.plans = plans;
        this.hours = hours;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminName = adminName;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (plans.count() == 0) {
            seedPlans();
        }
        if (hours.count() == 0) {
            seedHours();
        }
        seedConfiguredAdmin();
    }

    private void seedPlans() {
        plans.save(plan(
                "Iron Start",
                1,
                "1499.00",
                "A focused first step into consistent training.",
                List.of("Full gym access", "Personal workout tracker", "Exercise library")));
        plans.save(plan(
                "Forge Quarterly",
                3,
                "3999.00",
                "Build momentum with a 90-day training block.",
                List.of("Full gym access", "Workout plans", "Monthly progress review", "Member attendance insights")));
        plans.save(plan(
                "Summit Annual",
                12,
                "11999.00",
                "A full year built around strength and consistency.",
                List.of("Full gym access", "Workout plans", "Priority trainer request", "Quarterly progress review")));
    }

    private MembershipPlan plan(
            String name,
            int months,
            String price,
            String description,
            List<String> features) {
        MembershipPlan plan = new MembershipPlan();
        plan.setName(name);
        plan.setDurationMonths(months);
        plan.setPrice(new BigDecimal(price));
        plan.setDescription(description);
        plan.setActive(true);
        plan.setFeatures(features);
        return plan;
    }

    private void seedHours() {
        for (String day : List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")) {
            GymOperatingHour hour = new GymOperatingHour();
            hour.setDayOfWeek(day);
            hour.setOpen(true);
            hour.setOpeningTime(LocalTime.of(6, 0));
            hour.setClosingTime(LocalTime.of(22, 0));
            hour.setNotes("Standard hours");
            hours.save(hour);
        }
    }

    private void seedConfiguredAdmin() {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            return;
        }
        if (adminPassword.length() < 12) {
            throw new IllegalStateException(
                    "APP_SEED_ADMIN_PASSWORD must be at least 12 characters when seeding an admin.");
        }

        String normalizedEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(normalizedEmail)) {
            return;
        }

        User admin = new User();
        admin.setFullName(adminName);
        admin.setEmail(normalizedEmail);
        admin.setPhone("0000000000");
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        users.save(admin);
    }
}
