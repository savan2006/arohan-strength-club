package com.arohan.strengthclub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A dated facility closure and its member-facing announcement. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "gym_closures",
        uniqueConstraints = @UniqueConstraint(name = "uk_gym_closure_date", columnNames = "closure_date"),
        indexes = @Index(name = "idx_gym_closure_date", columnList = "closure_date"))
public class GymClosure {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "closure_date", nullable = false, unique = true)
    private LocalDate date;

    @Column(nullable = false, length = 200)
    private String reason;

    @Column(nullable = false, length = 600)
    private String announcement = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
