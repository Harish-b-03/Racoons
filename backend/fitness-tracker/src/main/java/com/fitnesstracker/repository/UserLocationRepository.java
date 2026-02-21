package com.fitnesstracker.repository;

import com.fitnesstracker.entity.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {
    
    Optional<UserLocation> findByUserId(Long userId);
    
    @Query(value = """
        SELECT ul.id, ul.user_id, ul.location, ul.accuracy, ul.altitude, 
               ul.speed, ul.heading, ul.created_at, ul.updated_at,
               ST_Distance(ul.location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) as distance
        FROM user_locations ul
        WHERE ul.updated_at > :minUpdatedAt
          AND ST_DWithin(
              ul.location::geography,
              ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
              :radiusMeters
          )
          AND (:excludeUserId IS NULL OR ul.user_id != :excludeUserId)
        ORDER BY distance ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findNearbyLocations(
        @Param("longitude") Double longitude,
        @Param("latitude") Double latitude,
        @Param("radiusMeters") Double radiusMeters,
        @Param("minUpdatedAt") LocalDateTime minUpdatedAt,
        @Param("excludeUserId") Long excludeUserId,
        @Param("limit") Integer limit
    );
    
    @Query(value = """
        SELECT ul.id, ul.user_id, ul.location, ul.accuracy, ul.altitude,
               ul.speed, ul.heading, ul.created_at, ul.updated_at,
               ST_Distance(ul.location::geography, ref.location::geography) as distance
        FROM user_locations ul
        CROSS JOIN user_locations ref
        WHERE ref.user_id = :userId
          AND ul.user_id != :userId
          AND ul.updated_at > :minUpdatedAt
          AND ST_DWithin(
              ul.location::geography,
              ref.location::geography,
              :radiusMeters
          )
        ORDER BY distance ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findNearbyUserLocations(
        @Param("userId") Long userId,
        @Param("radiusMeters") Double radiusMeters,
        @Param("minUpdatedAt") LocalDateTime minUpdatedAt,
        @Param("limit") Integer limit
    );
    
    @Query("SELECT ul FROM UserLocation ul WHERE ul.updatedAt > :minUpdatedAt")
    List<UserLocation> findActiveLocations(@Param("minUpdatedAt") LocalDateTime minUpdatedAt);
    
    void deleteByUserId(Long userId);
}
