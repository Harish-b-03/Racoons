package com.fitnesstracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.locationtech.jts.geom.Point;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_locations", indexes = {
    @Index(name = "idx_location_geom", columnList = "location"),
    @Index(name = "idx_location_updated_at", columnList = "updated_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

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

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    public Double getLatitude() {
        return location != null ? location.getY() : null;
    }

    @Transient
    public Double getLongitude() {
        return location != null ? location.getX() : null;
    }
}
