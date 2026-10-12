package com.arohan.strengthclub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Persistent change marker used to detect updates from another tab or device. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sync_events", indexes =
        @Index(name = "idx_sync_event_created_at", columnList = "created_at"))
public class SyncEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(name = "initiator_tab_id", length = 80)
    private String initiatorTabId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
