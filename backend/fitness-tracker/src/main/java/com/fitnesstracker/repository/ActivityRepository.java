package com.fitnesstracker.repository;

import com.fitnesstracker.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    
    Page<Activity> findByUserId(Long userId, Pageable pageable);
    
    List<Activity> findByUserIdAndStatus(Long userId, Activity.Status status);
    
    Optional<Activity> findByIdAndUserId(Long id, Long userId);
    
    @Query("SELECT a FROM Activity a WHERE a.user.id = :userId AND a.startedAt BETWEEN :startDate AND :endDate")
    List<Activity> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT a FROM Activity a WHERE a.user.id = :userId AND a.activityType = :type")
    Page<Activity> findByUserIdAndActivityType(
        @Param("userId") Long userId,
        @Param("type") Activity.ActivityType type,
        Pageable pageable
    );
}
