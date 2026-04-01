package com.eatzy.interaction.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Entity to track user's preference score for restaurant types
 * Used for personalized restaurant recommendations
 */
@Entity
@Table(name = "user_type_scores", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "restaurant_type_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTypeScore {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "restaurant_type_id", nullable = false)
    private Long restaurantTypeId;

    /**
     * Accumulated score representing user's preference for this restaurant type
     * Higher score = user likes this type more
     */
    @Column(nullable = false)
    private Integer score = 0;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }
}
