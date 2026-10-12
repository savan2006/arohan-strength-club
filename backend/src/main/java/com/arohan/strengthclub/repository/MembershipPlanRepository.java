package com.arohan.strengthclub.repository;

import com.arohan.strengthclub.entity.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, UUID> {
    List<MembershipPlan> findByActiveTrueOrderByPriceAsc();
    List<MembershipPlan> findAllByOrderByPriceAsc();
    Optional<MembershipPlan> findByNameIgnoreCase(String name);
}
