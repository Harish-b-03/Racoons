package com.fitnesstracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "gps_tracks", indexes = {
    @Index(name = "idx_gps_track_activity_id", columnList = "activity_id"),
    @Index(name = "idx_gps_track_recorded_at", columnList = "recorded_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GpsTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(precision = 10, scale = 2)
    private Double accuracy;

    @Column(precision = 10, scale = 2)
    private Double altitude;

    @Column(precision = 10, scale = 2)
    private Double speed;

    @Column(precision = 10, scale = 2)
    private Double heading;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Transient
    public Double getLatitude() {
        return location != null ? location.getY() : null;
    }

    @Transient
    public Double getLongitude() {
        return location != null ? location.getX() : null;
    }
}
