package user.service.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import user.service.Entity.Activity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Query("DELETE FROM Activity a WHERE a.createdAt < :cutoffDate")
    int deleteByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query("DELETE FROM Activity")
    void deleteAllActivities();
}