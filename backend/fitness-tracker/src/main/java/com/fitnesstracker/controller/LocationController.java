package com.fitnesstracker.controller;

import com.fitnesstracker.dto.ApiResponse;
import com.fitnesstracker.dto.GpsDataDto;
import com.fitnesstracker.dto.NearbyUsersRequest;
import com.fitnesstracker.dto.UserLocationDto;
import com.fitnesstracker.entity.User;
import com.fitnesstracker.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping("/update")
    public ResponseEntity<ApiResponse<UserLocationDto>> updateLocation(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody GpsDataDto gpsData) {
        
        log.debug("Location update request from user: {}", user.getId());
        gpsData.setUserId(user.getId());
        
        UserLocationDto location = locationService.updateUserLocation(gpsData);
        return ResponseEntity.ok(ApiResponse.success("Location updated successfully", location));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserLocationDto>> getMyLocation(@AuthenticationPrincipal User user) {
        log.debug("Get location request for user: {}", user.getId());
        UserLocationDto location = locationService.getUserLocation(user.getId());
        return ResponseEntity.ok(ApiResponse.success(location));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserLocationDto>> getUserLocation(@PathVariable Long userId) {
        log.debug("Get location request for user: {}", userId);
        UserLocationDto location = locationService.getUserLocation(userId);
        return ResponseEntity.ok(ApiResponse.success(location));
    }

    @PostMapping("/nearby/me")
    public ResponseEntity<ApiResponse<List<UserLocationDto>>> findNearbyUsers(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody NearbyUsersRequest request) {
        
        log.debug("Find nearby users request from user: {}", user.getId());
        List<UserLocationDto> nearbyUsers = locationService.findNearbyUsers(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Found %d nearby users", nearbyUsers.size()), 
                nearbyUsers
        ));
    }

    @PostMapping("/nearby")
    public ResponseEntity<ApiResponse<List<UserLocationDto>>> findNearbyLocations(
            @Valid @RequestBody NearbyUsersRequest request) {
        
        log.debug("Find nearby locations request for coordinates: ({}, {})", 
                request.getLatitude(), request.getLongitude());
        
        List<UserLocationDto> nearbyUsers = locationService.findNearbyLocations(request);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Found %d nearby users", nearbyUsers.size()), 
                nearbyUsers
        ));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<UserLocationDto>>> getActiveLocations(
            @RequestParam(required = false, defaultValue = "30") Integer maxAgeMinutes) {
        
        log.debug("Get active locations request with max age: {} minutes", maxAgeMinutes);
        List<UserLocationDto> activeLocations = locationService.getActiveLocations(maxAgeMinutes);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Found %d active users", activeLocations.size()), 
                activeLocations
        ));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMyLocation(@AuthenticationPrincipal User user) {
        log.info("Delete location request from user: {}", user.getId());
        locationService.deleteUserLocation(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Location deleted successfully", null));
    }
}
