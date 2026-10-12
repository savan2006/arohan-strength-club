package com.arohan.strengthclub.service;

import com.arohan.strengthclub.entity.FitnessProfile;
import com.arohan.strengthclub.entity.enums.MembershipStatus;
import com.arohan.strengthclub.repository.FitnessProfileRepository;
import com.arohan.strengthclub.repository.MembershipPaymentRepository;
import com.arohan.strengthclub.repository.MembershipPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MembershipServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private MembershipService service;

    @BeforeEach
    void setUp() {
        service = new MembershipService(
                Mockito.mock(MembershipPlanRepository.class),
                Mockito.mock(MembershipPaymentRepository.class),
                Mockito.mock(FitnessProfileRepository.class),
                ZONE.getId());
    }

    @Test
    void activeMembershipPastItsExpiryIsReportedAsExpired() {
        FitnessProfile profile = new FitnessProfile();
        profile.setMembershipStatus(MembershipStatus.ACTIVE);
        profile.setMembershipExpiry(LocalDate.now(ZONE).minusDays(1));

        service.applyElapsedExpiry(profile);

        assertEquals(MembershipStatus.EXPIRED, profile.getMembershipStatus());
    }

    @Test
    void membershipExpiringTodayIsStillActive() {
        FitnessProfile profile = new FitnessProfile();
        profile.setMembershipStatus(MembershipStatus.ACTIVE);
        profile.setMembershipExpiry(LocalDate.now(ZONE));

        service.applyElapsedExpiry(profile);

        assertEquals(MembershipStatus.ACTIVE, profile.getMembershipStatus());
    }

    @Test
    void frozenMembershipIsNotTouched() {
        FitnessProfile profile = new FitnessProfile();
        profile.setMembershipStatus(MembershipStatus.FROZEN);
        profile.setMembershipExpiry(LocalDate.now(ZONE).minusDays(30));

        service.applyElapsedExpiry(profile);

        assertEquals(MembershipStatus.FROZEN, profile.getMembershipStatus());
    }
}
