package com.arohan.strengthclub.service;

import com.arohan.strengthclub.dto.AdminDtos.AssignTrainerRequest;
import com.arohan.strengthclub.dto.AdminDtos.ScheduleDayRequest;
import com.arohan.strengthclub.dto.AdminDtos.ScheduleUpdateRequest;
import com.arohan.strengthclub.dto.AdminDtos.UpdateUserRequest;
import com.arohan.strengthclub.entity.User;
import com.arohan.strengthclub.entity.enums.Role;
import com.arohan.strengthclub.exception.BusinessRuleException;
import com.arohan.strengthclub.repository.AttendanceRepository;
import com.arohan.strengthclub.repository.FitnessProfileRepository;
import com.arohan.strengthclub.repository.GymClosureRepository;
import com.arohan.strengthclub.repository.GymOperatingHourRepository;
import com.arohan.strengthclub.repository.MembershipPaymentRepository;
import com.arohan.strengthclub.repository.MembershipPlanRepository;
import com.arohan.strengthclub.repository.UserRepository;
import com.arohan.strengthclub.repository.WorkoutPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Regression tests for administrative changes that can otherwise leave the system misconfigured. */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository users;
    @Mock private FitnessProfileRepository profiles;
    @Mock private AttendanceRepository attendances;
    @Mock private MembershipPlanRepository plans;
    @Mock private MembershipPaymentRepository payments;
    @Mock private GymOperatingHourRepository hours;
    @Mock private GymClosureRepository closures;
    @Mock private WorkoutPlanRepository workoutPlans;
    @Mock private WorkoutService workoutService;
    @Mock private GymService gymService;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                users,
                profiles,
                attendances,
                plans,
                payments,
                hours,
                closures,
                workoutPlans,
                workoutService,
                gymService);
    }

    @Test
    void refusesToDemoteTheLastAdministrator() {
        UUID userId = UUID.randomUUID();
        User admin = new User();
        admin.setRole(Role.ADMIN);
        when(users.findById(userId)).thenReturn(Optional.of(admin));
        when(users.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThrows(
                BusinessRuleException.class,
                () -> adminService.updateUser(userId, new UpdateUserRequest(Role.MEMBER, null, null)));

        verify(users, never()).save(any(User.class));
    }

    @Test
    void rejectsDuplicateWeekdaysBeforeWritingAnySchedule() {
        ScheduleUpdateRequest request = new ScheduleUpdateRequest(List.of(
                day("Monday"),
                day("Monday"),
                day("Wednesday"),
                day("Thursday"),
                day("Friday"),
                day("Saturday"),
                day("Sunday")));

        assertThrows(BusinessRuleException.class, () -> adminService.updateSchedule(request));
        verifyNoInteractions(hours);
    }

    @Test
    void refusesToAssignATrainerToANonMemberAccount() {
        UUID adminId = UUID.randomUUID();
        User admin = new User();
        admin.setRole(Role.ADMIN);
        when(users.findById(adminId)).thenReturn(Optional.of(admin));

        assertThrows(
                BusinessRuleException.class,
                () -> adminService.assignTrainer(adminId, new AssignTrainerRequest(UUID.randomUUID())));
        verifyNoInteractions(profiles);
    }

    private ScheduleDayRequest day(String name) {
        return new ScheduleDayRequest(name, true, LocalTime.of(6, 0), LocalTime.of(22, 0), "Test hours");
    }
}
