package com.eatzy.interaction.service;

import com.eatzy.interaction.domain.UserRestaurantScore;
import com.eatzy.interaction.domain.UserTypeScore;
import com.eatzy.interaction.dto.request.ScoringEventDTO;
import com.eatzy.interaction.repository.UserRestaurantScoreRepository;
import com.eatzy.interaction.repository.UserTypeScoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import com.eatzy.interaction.dto.request.BatchScoreRequestDTO;
import com.eatzy.interaction.dto.response.BatchScoreResponseDTO;
import com.eatzy.interaction.dto.response.RestaurantScoreDTO;
import com.eatzy.interaction.dto.response.TypeScoreDTO;

/**
 * Service to track user behavior and update scoring for restaurant recommendations
 */
@Service
@Slf4j
public class UserScoringService {

    private final UserTypeScoreRepository userTypeScoreRepository;
    private final UserRestaurantScoreRepository userRestaurantScoreRepository;

    public UserScoringService(
            UserTypeScoreRepository userTypeScoreRepository,
            UserRestaurantScoreRepository userRestaurantScoreRepository) {
        this.userTypeScoreRepository = userTypeScoreRepository;
        this.userRestaurantScoreRepository = userRestaurantScoreRepository;
    }

    @Transactional
    public void trackSearchRestaurantByNameAndClick(ScoringEventDTO event) {
        updateRestaurantScore(event.getUserId(), event.getRestaurantId(), 2);
        updateTypeScores(event.getUserId(), event.getRestaurantTypeIds(), 2);
        log.info("👤 User {} searched and clicked restaurant {}: +2 points", event.getUserId(), event.getRestaurantId());
    }

    @Transactional
    public void trackSearchDishAndClick(ScoringEventDTO event) {
        updateTypeScores(event.getUserId(), event.getRestaurantTypeIds(), 2);
        log.info("👤 User {} searched dish and clicked restaurant {}: +2 type points", event.getUserId(), event.getRestaurantId());
    }

    @Transactional
    public void trackViewRestaurantDetails(ScoringEventDTO event) {
        updateRestaurantScore(event.getUserId(), event.getRestaurantId(), 1);
        updateTypeScores(event.getUserId(), event.getRestaurantTypeIds(), 1);
        log.info("👤 User {} viewed restaurant {}: +1 point", event.getUserId(), event.getRestaurantId());
    }

    @Transactional
    public void trackAddToCart(ScoringEventDTO event) {
        updateRestaurantScore(event.getUserId(), event.getRestaurantId(), 3);
        updateTypeScores(event.getUserId(), event.getRestaurantTypeIds(), 3);
        log.info("👤 User {} added to cart from restaurant {}: +3 points", event.getUserId(), event.getRestaurantId());
    }

    @Transactional
    public void trackPlaceOrder(ScoringEventDTO event) {
        updateRestaurantScore(event.getUserId(), event.getRestaurantId(), 10);
        updateTypeScores(event.getUserId(), event.getRestaurantTypeIds(), 5);
        log.info("👤 User {} placed order at restaurant {}: +10 restaurant, +5 type points", 
                event.getUserId(), event.getRestaurantId());
    }

    @Transactional
    public void trackRating(ScoringEventDTO event, Integer stars) {
        if (stars == null) return;
        switch (stars) {
            case 5:
                updateRestaurantScore(event.getUserId(), event.getRestaurantId(), 5);
                updateTypeScores(event.getUserId(), event.getRestaurantTypeIds(), 3);
                log.info("⭐⭐⭐⭐⭐ User {} rated restaurant {} 5 stars", event.getUserId(), event.getRestaurantId());
                break;
            case 4:
                updateRestaurantScore(event.getUserId(), event.getRestaurantId(), 3);
                updateTypeScores(event.getUserId(), event.getRestaurantTypeIds(), 1);
                log.info("⭐⭐⭐⭐ User {} rated restaurant {} 4 stars", event.getUserId(), event.getRestaurantId());
                break;
            case 3:
                log.info("⭐⭐⭐ User {} rated restaurant {} 3 stars: no score change", event.getUserId(), event.getRestaurantId());
                break;
            case 2:
                updateRestaurantScore(event.getUserId(), event.getRestaurantId(), -10);
                log.info("⭐⭐ User {} rated restaurant {} 2 stars", event.getUserId(), event.getRestaurantId());
                break;
            case 1:
                updateRestaurantScore(event.getUserId(), event.getRestaurantId(), -50);
                log.info("⭐ User {} rated restaurant {} 1 star", event.getUserId(), event.getRestaurantId());
                break;
            default:
                log.warn("Invalid rating stars: {}", stars);
        }
    }

    private void updateRestaurantScore(Long userId, Long restaurantId, int points) {
        if (restaurantId == null || userId == null) return;
        UserRestaurantScore score = userRestaurantScoreRepository
                .findByUserIdAndRestaurantId(userId, restaurantId)
                .orElseGet(() -> {
                    UserRestaurantScore newScore = new UserRestaurantScore();
                    newScore.setUserId(userId);
                    newScore.setRestaurantId(restaurantId);
                    newScore.setScore(0);
                    return newScore;
                });
        
        score.setScore(score.getScore() + points);
        userRestaurantScoreRepository.save(score);
    }

    private void updateTypeScores(Long userId, List<Long> typeIds, int points) {
        if (userId == null || typeIds == null || typeIds.isEmpty()) return;

        for (Long typeId : typeIds) {
            UserTypeScore score = userTypeScoreRepository
                    .findByUserIdAndRestaurantTypeId(userId, typeId)
                    .orElseGet(() -> {
                        UserTypeScore newScore = new UserTypeScore();
                        newScore.setUserId(userId);
                        newScore.setRestaurantTypeId(typeId);
                        newScore.setScore(0);
                        return newScore;
                    });
            
            score.setScore(score.getScore() + points);
            userTypeScoreRepository.save(score);
        }
    }

    @Transactional(readOnly = true)
    public BatchScoreResponseDTO getBatchScores(BatchScoreRequestDTO request) {
        if (request.getUserId() == null) {
            return new BatchScoreResponseDTO(List.of(), List.of());
        }

        List<RestaurantScoreDTO> restaurantScores = List.of();
        if (request.getRestaurantIds() != null && !request.getRestaurantIds().isEmpty()) {
            restaurantScores = userRestaurantScoreRepository
                    .findByUserIdAndRestaurantIdIn(request.getUserId(), request.getRestaurantIds())
                    .stream()
                    .map(score -> new RestaurantScoreDTO(score.getRestaurantId(), score.getScore()))
                    .collect(Collectors.toList());
        }

        List<TypeScoreDTO> typeScores = List.of();
        if (request.getTypeIds() != null && !request.getTypeIds().isEmpty()) {
            typeScores = userTypeScoreRepository
                    .findByUserIdAndRestaurantTypeIdIn(request.getUserId(), request.getTypeIds())
                    .stream()
                    .map(score -> new TypeScoreDTO(score.getRestaurantTypeId(), score.getScore()))
                    .collect(Collectors.toList());
        }

        return new BatchScoreResponseDTO(restaurantScores, typeScores);
    }
}
