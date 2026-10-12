package com.arohan.strengthclub.repository;

import com.arohan.strengthclub.entity.User;
import com.arohan.strengthclub.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @Query("select u.role from User u where u.id = :id")
    Optional<Role> findRoleById(@Param("id") UUID id);

    Optional<User> findByEmailIgnoreCase(String email);

    /** One round trip for the whole admin overview: members, trainers, active memberships, visits, requests. */
    @Query(value = """
            select
              (select count(*) from users where role = 'MEMBER'),
              (select count(*) from users where role = 'TRAINER'),
              (select count(*) from fitness_profiles
                 where membership_status = 'ACTIVE' and (membership_expiry is null or membership_expiry >= :today)),
              (select count(*) from attendances where facility_date = :today),
              (select count(*) from fitness_profiles where trainer_requested = true and trainer_id is null)
            """, nativeQuery = true)
    List<Object[]> overviewCounts(@Param("today") LocalDate today);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findAllByOrderByCreatedAtDesc();
    long countByRole(Role role);
    List<User> findAllByRoleOrderByCreatedAtDesc(Role role);
}
