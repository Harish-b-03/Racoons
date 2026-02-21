package com.fitnesstracker.service;

import com.fitnesstracker.config.AppConfig;
import com.fitnesstracker.dto.GpsDataDto;
import com.fitnesstracker.dto.NearbyUsersRequest;
import com.fitnesstracker.dto.UserLocationDto;
import com.fitnesstracker.entity.User;
import com.fitnesstracker.entity.UserLocation;
import com.fitnesstracker.exception.ResourceNotFoundException;
import com.fitnesstracker.repository.UserLocationRepository;
import com.fitnesstracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final UserLocationRepository locationRepository;
    private final UserRepository userRepository;
    private final AppConfig appConfig;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    @CacheEvict(value = "userLocation", key = "#gpsData.userId")
    public UserLocationDto updateUserLocation(GpsDataDto gpsData) {
        User user = userRepository.findById(gpsData.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Point point = geometryFactory.createPoint(
                new Coordinate(gpsData.getLongitude(), gpsData.getLatitude())
        );

        UserLocation location = locationRepository.findByUserId(user.getId())
                .orElse(UserLocation.builder()
                        .user(user)
                        .build());

        location.setLocation(point);
        location.setAccuracy(gpsData.getAccuracy());
        location.setAltitude(gpsData.getAltitude());
        location.setSpeed(gpsData.getSpeed());
        location.setHeading(gpsData.getHeading());

        location = locationRepository.save(location);
        log.debug("Updated location for user {}: ({}, {})", 
                user.getId(), gpsData.getLatitude(), gpsData.getLongitude());

        return mapToDto(location, user.getUsername(), null);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "userLocation", key = "#userId")
    public UserLocationDto getUserLocation(Long userId) {
        UserLocation location = locationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found for user"));

        User user = location.getUser();
        return mapToDto(location, user.getUsername(), null);
    }

    @Transactional(readOnly = true)
    public List<UserLocationDto> findNearbyUsers(Long userId, NearbyUsersRequest request) {
        Double radiusMeters = request.getRadiusMeters() != null ? 
                request.getRadiusMeters() : appConfig.getLocation().getDefaultRadiusMeters();
        
        Integer maxAgeMinutes = request.getMaxAgeMinutes() != null ? 
                request.getMaxAgeMinutes() : appConfig.getLocation().getMaxAgeMinutes();
        
        Integer limit = request.getLimit() != null ? request.getLimit() : 50;

        LocalDateTime minUpdatedAt = LocalDateTime.now().minusMinutes(maxAgeMinutes);

        List<Object[]> results = locationRepository.findNearbyUserLocations(
                userId, radiusMeters, minUpdatedAt, limit
        );

        return mapResultsToDto(results);
    }

    @Transactional(readOnly = true)
    public List<UserLocationDto> findNearbyLocations(NearbyUsersRequest request) {
        if (request.getLongitude() == null || request.getLatitude() == null) {
            throw new IllegalArgumentException("Longitude and latitude are required");
        }

        Double radiusMeters = request.getRadiusMeters() != null ? 
                request.getRadiusMeters() : appConfig.getLocation().getDefaultRadiusMeters();
        
        Integer maxAgeMinutes = request.getMaxAgeMinutes() != null ? 
                request.getMaxAgeMinutes() : appConfig.getLocation().getMaxAgeMinutes();
        
        Integer limit = request.getLimit() != null ? request.getLimit() : 50;

        LocalDateTime minUpdatedAt = LocalDateTime.now().minusMinutes(maxAgeMinutes);

        List<Object[]> results = locationRepository.findNearbyLocations(
                request.getLongitude(), request.getLatitude(), 
                radiusMeters, minUpdatedAt, null, limit
        );

        return mapResultsToDto(results);
    }

    @Transactional(readOnly = true)
    public List<UserLocationDto> getActiveLocations(Integer maxAgeMinutes) {
        Integer ageMinutes = maxAgeMinutes != null ? 
                maxAgeMinutes : appConfig.getLocation().getMaxAgeMinutes();
        
        LocalDateTime minUpdatedAt = LocalDateTime.now().minusMinutes(ageMinutes);
        
        List<UserLocation> locations = locationRepository.findActiveLocations(minUpdatedAt);
        
        return locations.stream()
                .map(loc -> mapToDto(loc, loc.getUser().getUsername(), null))
                .toList();
    }

    @Transactional
    @CacheEvict(value = "userLocation", key = "#userId")
    public void deleteUserLocation(Long userId) {
        locationRepository.deleteByUserId(userId);
        log.info("Deleted location for user: {}", userId);
    }

    private List<UserLocationDto> mapResultsToDto(List<Object[]> results) {
        List<UserLocationDto> dtos = new ArrayList<>();
        
        for (Object[] row : results) {
            try {
                UserLocationDto dto = UserLocationDto.builder()
                        .id(((Number) row[0]).longValue())
                        .userId(((Number) row[1]).longValue())
                        .accuracy(row[3] != null ? ((Number) row[3]).doubleValue() : null)
                        .altitude(row[4] != null ? ((Number) row[4]).doubleValue() : null)
                        .speed(row[5] != null ? ((Number) row[5]).doubleValue() : null)
                        .heading(row[6] != null ? ((Number) row[6]).doubleValue() : null)
                        .updatedAt((LocalDateTime) row[8])
                        .distanceMeters(row[9] != null ? ((Number) row[9]).doubleValue() : null)
                        .build();

                if (row[2] != null) {
                    Point point = (Point) row[2];
                    dto.setLatitude(point.getY());
                    dto.setLongitude(point.getX());
                }

                dtos.add(dto);
            } catch (Exception e) {
                log.error("Error mapping location result: {}", e.getMessage(), e);
            }
        }
        
        return dtos;
    }

    private UserLocationDto mapToDto(UserLocation location, String username, Double distance) {
        return UserLocationDto.builder()
                .id(location.getId())
                .userId(location.getUser().getId())
                .username(username)
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .accuracy(location.getAccuracy())
                .altitude(location.getAltitude())
                .speed(location.getSpeed())
                .heading(location.getHeading())
                .distanceMeters(distance)
                .updatedAt(location.getUpdatedAt())
                .build();
    }
}
