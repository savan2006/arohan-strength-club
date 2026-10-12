package com.arohan.strengthclub.service;

import com.arohan.strengthclub.dto.AdminDtos.MembershipPlanResponse;
import com.arohan.strengthclub.dto.MemberDtos.PaymentRequest;
import com.arohan.strengthclub.dto.MemberDtos.PaymentResponse;
import com.arohan.strengthclub.entity.FitnessProfile;
import com.arohan.strengthclub.entity.MembershipPayment;
import com.arohan.strengthclub.entity.MembershipPlan;
import com.arohan.strengthclub.entity.User;
import com.arohan.strengthclub.entity.enums.MembershipStatus;
import com.arohan.strengthclub.entity.enums.PaymentStatus;
import com.arohan.strengthclub.exception.BusinessRuleException;
import com.arohan.strengthclub.exception.ResourceNotFoundException;
import com.arohan.strengthclub.repository.FitnessProfileRepository;
import com.arohan.strengthclub.repository.MembershipPaymentRepository;
import com.arohan.strengthclub.repository.MembershipPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/** Manages membership plan listings, simulated purchases, and access checks. */
@Service
public class MembershipService {

    private static final Logger log = LoggerFactory.getLogger(MembershipService.class);

    private final MembershipPlanRepository plans;
    private final MembershipPaymentRepository payments;
    private final FitnessProfileRepository profiles;
    private final ZoneId zoneId;

    public MembershipService(
            MembershipPlanRepository plans,
            MembershipPaymentRepository payments,
            FitnessProfileRepository profiles,
            @Value("${app.timezone:Asia/Kolkata}") String timezone) {
        this.plans = plans;
        this.payments = payments;
        this.profiles = profiles;
        this.zoneId = ZoneId.of(timezone);
    }

    /**
     * Memberships never flip to EXPIRED by themselves; without this the dashboard, admin counts and
     * trainer rosters would keep showing lapsed members as ACTIVE.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Kolkata")
    @EventListener(ApplicationReadyEvent.class)
    public void expireLapsedMemberships() {
        // Not @Transactional on purpose: the repository call runs in its own transaction, so a failure
        // here is fully contained by the catch below (it must never abort application startup).
        try {
            int changed = profiles.expireLapsed(LocalDate.now(zoneId));
            if (changed > 0) log.info("Marked {} lapsed memberships as expired", changed);
        } catch (RuntimeException ex) {
            log.warn("Could not expire lapsed memberships: {}", ex.getMessage());
        }
    }

    /** Reflects an elapsed term immediately for a loaded profile (the nightly job persists it). */
    public void applyElapsedExpiry(FitnessProfile profile) {
        if (profile.getMembershipStatus() == MembershipStatus.ACTIVE
                && profile.getMembershipExpiry() != null
                && profile.getMembershipExpiry().isBefore(LocalDate.now(zoneId))) {
            profile.setMembershipStatus(MembershipStatus.EXPIRED);
        }
    }

    @Transactional(readOnly = true)
    public List<MembershipPlanResponse> getActivePlans() {
        return plans.findByActiveTrueOrderByPriceAsc().stream()
                .map(MembershipPlanResponse::from)
                .toList();
    }

    /** Records a simulated completed payment and extends a currently valid membership. */
    @Transactional
    public PaymentResponse purchase(User user, PaymentRequest request) {
        MembershipPlan plan = plans.findById(request.planId())
                .filter(MembershipPlan::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Membership plan was not found or is no longer available."));
        FitnessProfile profile = profiles.lockByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Fitness profile was not found."));

        // A double click or a retry must not buy the same plan twice.
        payments.findFirstByUser_IdAndStatusOrderByPaymentDateDesc(user.getId(), PaymentStatus.COMPLETED)
                .filter(last -> last.getPlan().getId().equals(plan.getId())
                        && last.getPaymentDate().isAfter(Instant.now().minusSeconds(15)))
                .ifPresent(last -> {
                    throw new BusinessRuleException("DUPLICATE_PAYMENT",
                            "This purchase was just recorded. Please wait a moment before buying again.",
                            HttpStatus.CONFLICT);
                });

        LocalDate today = LocalDate.now(zoneId);
        LocalDate start = today;
        boolean continuousRenewal = profile.getMembershipStatus() == MembershipStatus.ACTIVE
                && profile.getMembershipExpiry() != null
                && !profile.getMembershipExpiry().isBefore(today);
        if (continuousRenewal) {
            start = profile.getMembershipExpiry().plusDays(1);
        }
        LocalDate originalMembershipStart = continuousRenewal && profile.getMembershipStart() != null
                ? profile.getMembershipStart()
                : start;
        LocalDate expiry = start.plusMonths(plan.getDurationMonths()).minusDays(1);

        MembershipPayment payment = new MembershipPayment();
        payment.setUser(user);
        payment.setPlan(plan);
        payment.setPlanName(plan.getName());
        payment.setAmount(plan.getPrice());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPlanDurationMonths(plan.getDurationMonths());
        payment.setStartDate(start);
        payment.setExpiryDate(expiry);
        payments.save(payment);

        profile.setMembershipStatus(MembershipStatus.ACTIVE);
        profile.setMembershipStart(originalMembershipStart);
        profile.setMembershipExpiry(expiry);
        profiles.save(profile);
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public void requireActiveMembership(User user) {
        FitnessProfile profile = profiles.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Fitness profile was not found."));
        LocalDate today = LocalDate.now(zoneId);
        boolean valid = profile.getMembershipStatus() == MembershipStatus.ACTIVE
                && (profile.getMembershipExpiry() == null || !profile.getMembershipExpiry().isBefore(today));
        if (!valid) {
            throw new BusinessRuleException(
                    "ACTIVE_MEMBERSHIP_REQUIRED",
                    "An active membership is required for this action.",
                    HttpStatus.FORBIDDEN);
        }
    }

    @Transactional(readOnly = true)
    public List<MembershipPayment> getPaymentHistory(UUID userId) {
        return payments.findByUser_IdOrderByPaymentDateDesc(userId);
    }
}
