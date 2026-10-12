package com.arohan.strengthclub.entity;

import com.arohan.strengthclub.entity.enums.ExperienceLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/** Audit record for a server-calculated fitness-level promotion. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fitness_level_promotions", indexes =
        @Index(name = "idx_level_promotion_profile_date", columnList = "profile_id,promoted_at"))
public class FitnessLevelPromotion {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private FitnessProfile profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_level", nullable = false, length = 20)
    private ExperienceLevel fromLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_level", nullable = false, length = 20)
    private ExperienceLevel toLevel;

    @Column(name = "promoted_at", nullable = false)
    private Instant promotedAt = Instant.now();

    @Column(nullable = false, length = 240)
    private String reason;
}
