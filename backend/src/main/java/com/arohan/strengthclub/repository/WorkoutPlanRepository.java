package com.arohan.strengthclub.repository;

import com.arohan.strengthclub.entity.WorkoutPlan;
import com.arohan.strengthclub.entity.enums.WorkoutSource;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, UUID> {
    // days + owner are always rendered with the plan; exercises are batch-loaded (default_batch_fetch_size).
    @EntityGraph(attributePaths = {"days", "assignedTo"})
    List<WorkoutPlan> findByAssignedTo_IdAndActiveTrueOrderByUpdatedAtDesc(UUID userId);
    List<WorkoutPlan> findByCreatedBy_IdOrderByUpdatedAtDesc(UUID userId);
    @EntityGraph(attributePaths = "days")
    List<WorkoutPlan> findBySourceAndCreatedBy_IdAndAssignedToIsNullOrderByUpdatedAtDesc(WorkoutSource source, UUID creatorId);
    void deleteByCreatedBy_Id(UUID creatorId);
    void deleteByAssignedTo_Id(UUID userId);
}
