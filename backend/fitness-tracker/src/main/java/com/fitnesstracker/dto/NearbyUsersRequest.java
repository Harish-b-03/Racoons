package com.fitnesstracker.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyUsersRequest {

    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    private Double longitude;

    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    private Double latitude;

    @Min(value = 1, message = "Radius must be at least 1 meter")
    @Max(value = 50000, message = "Radius cannot exceed 50km")
    @Builder.Default
    private Double radiusMeters = 1000.0;

    @Min(value = 1, message = "Max age must be at least 1 minute")
    @Max(value = 1440, message = "Max age cannot exceed 24 hours")
    @Builder.Default
    private Integer maxAgeMinutes = 30;

    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit cannot exceed 100")
    @Builder.Default
    private Integer limit = 50;
}
