package com.arohan.strengthclub.repository;

import com.arohan.strengthclub.entity.GymOperatingHour;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GymOperatingHourRepository extends JpaRepository<GymOperatingHour, UUID> {
    Optional<GymOperatingHour> findByDayOfWeekIgnoreCase(String dayOfWeek);
    List<GymOperatingHour> findAllByOrderByDayOfWeekAsc();
}
