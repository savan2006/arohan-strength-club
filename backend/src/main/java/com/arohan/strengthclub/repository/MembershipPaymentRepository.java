package com.arohan.strengthclub.repository;

import com.arohan.strengthclub.entity.MembershipPayment;
import com.arohan.strengthclub.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MembershipPaymentRepository extends JpaRepository<MembershipPayment, UUID> {
    List<MembershipPayment> findByUser_IdOrderByPaymentDateDesc(UUID userId);
    java.util.Optional<MembershipPayment> findFirstByUser_IdAndStatusOrderByPaymentDateDesc(UUID userId, PaymentStatus status);
    @Query("select coalesce(sum(p.planDurationMonths), 0) from MembershipPayment p where p.user.id = :userId and p.status = :status")
    long sumPlanDurationMonthsByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") PaymentStatus status);
    void deleteByUser_Id(UUID userId);
}
