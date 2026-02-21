package com.fitnesstracker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "activities", indexes = {
    @Index(name = "idx_activity_user_id", columnList = "user_id"),
    @Index(name = "idx_activity_started_at", columnList = "started_at"),
    @Index(name = "idx_activity_type", columnList = "activity_type")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "distance_meters", precision = 10, scale = 2)
    private Double distanceMeters;

    @Column(name = "calories_burned", precision = 10, scale = 2)
    private Double caloriesBurned;

    @Column(name = "average_speed", precision = 10, scale = 2)
    private Double averageSpeed;

    @Column(name = "max_speed", precision = 10, scale = 2)
    private Double maxSpeed;

    @Column(name = "average_heart_rate")
    private Integer averageHeartRate;

    @Column(name = "max_heart_rate")
    private Integer maxHeartRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.IN_PROGRESS;

    @Column(length = 500)
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GpsTrack> gpsTrack;

    public enum ActivityType {
        RUNNING, WALKING, CYCLING, HIKING, SWIMMING, GYM, YOGA, OTHER
    }

    public enum Status {
        IN_PROGRESS, COMPLETED, PAUSED, CANCELLED
    }
}
