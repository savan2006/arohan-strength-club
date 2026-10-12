package com.arohan.strengthclub.repository;

import com.arohan.strengthclub.entity.GymClosure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GymClosureRepository extends JpaRepository<GymClosure, UUID> {
    Optional<GymClosure> findByDate(LocalDate date);
    List<GymClosure> findByDateGreaterThanEqualOrderByDateAsc(LocalDate date);
    List<GymClosure> findAllByOrderByDateAsc();
}
