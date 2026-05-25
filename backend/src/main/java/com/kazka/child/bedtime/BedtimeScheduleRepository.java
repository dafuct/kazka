package com.kazka.child.bedtime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BedtimeScheduleRepository extends JpaRepository<BedtimeSchedule, String> {

    Optional<BedtimeSchedule> findByChildProfileId(String childProfileId);

    /**
     * Sweep query — find due schedules. Joins child_profiles + users to enforce:
     *   - child not archived
     *   - user not suspended
     *   - user email verified (we only send to verified addresses)
     * The 1-hour lower bound prevents catastrophic catch-up after a long outage.
     */
    @Query(value = """
            SELECT bs.* FROM bedtime_schedules bs
              JOIN child_profiles cp ON bs.child_profile_id = cp.id
              JOIN users u ON cp.user_id = u.id
            WHERE bs.enabled = TRUE
              AND bs.failed_at IS NULL
              AND bs.next_run_at <= :now
              AND bs.next_run_at > :horizon
              AND cp.archived_at IS NULL
              AND u.suspended_at IS NULL
              AND u.email_verified = TRUE
            """, nativeQuery = true)
    List<BedtimeSchedule> findDueForSweep(@Param("now") Instant now, @Param("horizon") Instant horizon);

    @Transactional
    @Modifying
    @Query("UPDATE BedtimeSchedule bs SET bs.enabled = false WHERE bs.childProfileId IN " +
           "(SELECT cp.id FROM ChildProfile cp WHERE cp.userId = :userId)")
    int disableAllForUser(@Param("userId") String userId);
}
