package com.arohan.strengthclub.repository;

import com.arohan.strengthclub.entity.SyncEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SyncEventRepository extends JpaRepository<SyncEvent, Long> {
    Optional<SyncEvent> findTopByOrderByIdDesc();
    List<SyncEvent> findByIdGreaterThanOrderByIdAsc(long id);
    long deleteByCreatedAtBefore(Instant cutoff);
}
