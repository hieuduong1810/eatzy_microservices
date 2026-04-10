package com.eatzy.restaurant.designpattern.strategy.ranking;

import com.eatzy.common.service.MapboxService;
import com.eatzy.restaurant.client.InteractionServiceClient;
import com.eatzy.restaurant.client.dto.BatchScoreRequestDTO;
import com.eatzy.restaurant.client.dto.BatchScoreResponseDTO;
import com.eatzy.restaurant.domain.Restaurant;
import com.eatzy.restaurant.domain.RestaurantType;
import com.eatzy.restaurant.domain.res.ResRestaurantMagazineDTO;
import com.eatzy.restaurant.mapper.RestaurantMapper;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PersonalizedRankingStrategy implements RankingStrategy {

    private final RestaurantMapper restaurantMapper;
    private final MapboxService mapboxService;
    private final InteractionServiceClient interactionServiceClient;
    private static final BigDecimal MAX_DISTANCE_KM = new BigDecimal("10.0");

    public PersonalizedRankingStrategy(RestaurantMapper restaurantMapper, MapboxService mapboxService, InteractionServiceClient interactionServiceClient) {
        this.restaurantMapper = restaurantMapper;
        this.mapboxService = mapboxService;
        this.interactionServiceClient = interactionServiceClient;
    }

    @Override
    public List<ResRestaurantMagazineDTO> calculateAndSort(List<Restaurant> restaurants, BigDecimal userLat, BigDecimal userLng, Long userId) {
        List<ResRestaurantMagazineDTO> results = new ArrayList<>();

        // 1. Thu thap thong tin cham diem ca nhan hoa
        Map<Long, Integer> resScores = new HashMap<>();
        Map<Long, Integer> typeScores = new HashMap<>();

        try {
            List<Long> resIds = restaurants.stream().map(Restaurant::getId).collect(Collectors.toList());
            
            Set<Long> allTypeIds = new HashSet<>();
            for (Restaurant r : restaurants) {
                if (r.getRestaurantTypes() != null) {
                    for (RestaurantType type : r.getRestaurantTypes()) {
                        allTypeIds.add(type.getId());
                    }
                }
            }

            BatchScoreRequestDTO req = new BatchScoreRequestDTO(userId, resIds, new ArrayList<>(allTypeIds));
            BatchScoreResponseDTO scoreDTO = interactionServiceClient.getBatchScores(req);

            if (scoreDTO != null) {
                if (scoreDTO.getRestaurantScores() != null) {
                    scoreDTO.getRestaurantScores().forEach(s -> resScores.put(s.getRestaurantId(), s.getScore()));
                }
                if (scoreDTO.getTypeScores() != null) {
                    scoreDTO.getTypeScores().forEach(s -> typeScores.put(s.getTypeId(), s.getScore()));
                }
            }
        } catch (Exception e) {
            log.warn("Loi khi lay diem thong tin ca nhan (Interaction Service): {}", e.getMessage());
        }

        // 2. Tinh toan
        for (Restaurant r : restaurants) {
            if (r.getLatitude() == null || r.getLongitude() == null) {
                continue;
            }

            BigDecimal distance = null;
            if (userLat != null && userLng != null) {
                distance = mapboxService.getDrivingDistance(userLat, userLng, r.getLatitude(), r.getLongitude());
            }

            if (distance == null || distance.compareTo(MAX_DISTANCE_KM) > 0) {
                continue;
            }

            ResRestaurantMagazineDTO dto = restaurantMapper.convertToMagazineDTO(r);
            dto.setDistance(distance);

            // Tinh S_Type (40%)
            double typeScore = 0.0;
            if (r.getRestaurantTypes() != null && !r.getRestaurantTypes().isEmpty()) {
                int totalTypePts = 0;
                for (RestaurantType type : r.getRestaurantTypes()) {
                    totalTypePts += typeScores.getOrDefault(type.getId(), 0);
                }
                typeScore = Math.min(100.0, (totalTypePts / 200.0) * 100.0);
            }

            // Tinh S_Quen (Loyalty) (30%)
            int rawLoyalty = resScores.getOrDefault(r.getId(), 0);
            double loyaltyScore = Math.min(100.0, (rawLoyalty / 50.0) * 100.0);

            // Tinh S_Gan (Distance) (20%)
            double distVal = distance.doubleValue();
            double distanceScore = Math.max(0.0, 100.0 - (distVal * 10.0));

            // Tinh S_Ngon (Quality) (10%)
            double qualityScore = r.getAverageRating() != null ? r.getAverageRating().doubleValue() * 20.0 : 0.0;

            // Final Score
            double finalScore = (typeScore * 0.40) + (loyaltyScore * 0.30) + (distanceScore * 0.20) + (qualityScore * 0.10);

            dto.setTypeScore(typeScore);
            dto.setLoyaltyScore(loyaltyScore);
            dto.setDistanceScore(distanceScore);
            dto.setQualityScore(qualityScore);
            dto.setFinalScore(finalScore);

            results.add(dto);
        }

        // 3. Sap xep giam dan theo diem final (Diem cao nhat len dau)
        results.sort(Comparator.comparing(ResRestaurantMagazineDTO::getFinalScore, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
        
        log.info("PersonalizedRankingStrategy: processed {} restaurants for user {}", results.size(), userId);
        return results;
    }
}
