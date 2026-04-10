package com.eatzy.interaction.repository;

import com.eatzy.interaction.domain.UserRestaurantScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRestaurantScoreRepository extends JpaRepository<UserRestaurantScore, Long> {
    Optional<UserRestaurantScore> findByUserIdAndRestaurantId(Long userId, Long restaurantId);
    List<UserRestaurantScore> findByUserIdOrderByScoreDesc(Long userId, Pageable pageable);
    
    List<UserRestaurantScore> findByUserIdAndRestaurantIdIn(Long userId, List<Long> restaurantIds);
}
