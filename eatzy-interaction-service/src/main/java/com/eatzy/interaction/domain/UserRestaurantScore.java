package com.eatzy.interaction.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Entity to track user's familiarity/loyalty score with specific restaurants
 * Used for personalized restaurant recommendations
 */
@Entity
@Table(name = "user_restaurant_scores",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "restaurant_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRestaurantScore {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    /**
     * Accumulated score representing user's loyalty/familiarity with this restaurant
     * Higher score = user is more familiar/loyal to this restaurant
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
