package com.arohan.strengthclub.repository;

import com.arohan.strengthclub.entity.FitnessProfile;
import com.arohan.strengthclub.entity.enums.MembershipStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FitnessProfileRepository extends JpaRepository<FitnessProfile, UUID> {
    // user + trainer are almost always read right after the profile; fetch them in the same query.
    @EntityGraph(attributePaths = {"user", "trainer"})
    Optional<FitnessProfile> findByUser_Id(UUID userId);
    /** Row lock: concurrent purchases by the same member are processed one after another. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from FitnessProfile p where p.user.id = :userId")
    Optional<FitnessProfile> lockByUserId(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = "user")
    List<FitnessProfile> findByTrainer_Id(UUID trainerId);
    long countByMembershipStatus(MembershipStatus status);
    long countByTrainerRequestedTrueAndTrainerIsNull();

    @Query("select p from FitnessProfile p join fetch p.user left join fetch p.trainer")
    List<FitnessProfile> findAllWithUserAndTrainer();

    /** Marks memberships whose term has ended as EXPIRED so counts and badges stay truthful. */
    @Transactional
    @Modifying
    @Query("update FitnessProfile p set p.membershipStatus = com.arohan.strengthclub.entity.enums.MembershipStatus.EXPIRED "
            + "where p.membershipStatus = com.arohan.strengthclub.entity.enums.MembershipStatus.ACTIVE "
            + "and p.membershipExpiry is not null and p.membershipExpiry < :today")
    int expireLapsed(@Param("today") LocalDate today);
}
