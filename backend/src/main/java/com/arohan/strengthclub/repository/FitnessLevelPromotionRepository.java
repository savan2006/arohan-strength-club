package com.arohan.strengthclub.repository;

import com.arohan.strengthclub.entity.FitnessLevelPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface FitnessLevelPromotionRepository extends JpaRepository<FitnessLevelPromotion, UUID> {
    List<FitnessLevelPromotion> findByProfile_IdOrderByPromotedAtDesc(UUID profileId);
}
