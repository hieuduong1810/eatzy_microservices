package com.eatzy.interaction.repository;

import com.eatzy.interaction.domain.UserTypeScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTypeScoreRepository extends JpaRepository<UserTypeScore, Long> {
    Optional<UserTypeScore> findByUserIdAndRestaurantTypeId(Long userId, Long restaurantTypeId);
    List<UserTypeScore> findByUserIdOrderByScoreDesc(Long userId, Pageable pageable);
    
    List<UserTypeScore> findByUserIdAndRestaurantTypeIdIn(Long userId, List<Long> typeIds);
}
