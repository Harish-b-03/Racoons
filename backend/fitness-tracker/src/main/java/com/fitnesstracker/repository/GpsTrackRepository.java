package com.fitnesstracker.repository;

import com.fitnesstracker.entity.GpsTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GpsTrackRepository extends JpaRepository<GpsTrack, Long> {
    
    List<GpsTrack> findByActivityIdOrderByRecordedAtAsc(Long activityId);
    
    @Query("SELECT g FROM GpsTrack g WHERE g.activity.id = :activityId AND g.recordedAt BETWEEN :startTime AND :endTime ORDER BY g.recordedAt ASC")
    List<GpsTrack> findByActivityIdAndTimeRange(
        @Param("activityId") Long activityId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    @Query(value = """
        SELECT 
            ST_MakeLine(location ORDER BY recorded_at) as route,
            SUM(ST_Distance(location::geography, LAG(location::geography) OVER (ORDER BY recorded_at))) as total_distance
        FROM gps_tracks
        WHERE activity_id = :activityId
        GROUP BY activity_id
        """, nativeQuery = true)
    Object[] calculateRouteAndDistance(@Param("activityId") Long activityId);
}
