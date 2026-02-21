package com.fitnesstracker.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLocationDto {

    private Long id;
    private Long userId;
    private String username;
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private Double altitude;
    private Double speed;
    private Double heading;
    private Double distanceMeters;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
