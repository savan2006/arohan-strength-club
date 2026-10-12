package com.arohan.strengthclub.repository;

import com.arohan.strengthclub.entity.Attendance;
import com.arohan.strengthclub.entity.enums.AttendanceStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    List<Attendance> findByUser_IdOrderByCheckInTimeDesc(UUID userId);
    /** Most recent sessions only; avoids loading a member's entire lifetime history. */
    List<Attendance> findByUser_IdOrderByCheckInTimeDesc(UUID userId, Pageable pageable);
    /** Sessions on or after {@code since}; consistency, streaks and levels never need older rows. */
    List<Attendance> findByUser_IdAndFacilityDateGreaterThanEqualOrderByCheckInTimeDesc(UUID userId, LocalDate since);
    List<Attendance> findByUser_IdInAndFacilityDateGreaterThanEqualOrderByCheckInTimeDesc(Collection<UUID> userIds, LocalDate since);
    List<Attendance> findByUser_IdInOrderByCheckInTimeDesc(Collection<UUID> userIds);
    List<Attendance> findByUser_IdAndFacilityDateOrderByCheckInTimeDesc(UUID userId, LocalDate date);
    Optional<Attendance> findFirstByUser_IdAndStatusOrderByCheckInTimeDesc(UUID userId, AttendanceStatus status);
    boolean existsByUser_IdAndFacilityDateAndStatus(UUID userId, LocalDate date, AttendanceStatus status);
    long countByFacilityDate(LocalDate date);
    void deleteByUser_Id(UUID userId);
}
