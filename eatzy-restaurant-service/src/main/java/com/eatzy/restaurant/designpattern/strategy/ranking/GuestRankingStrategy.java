package com.eatzy.restaurant.designpattern.strategy.ranking;

import com.eatzy.common.service.MapboxService;
import com.eatzy.restaurant.domain.Restaurant;
import com.eatzy.restaurant.dto.res.ResRestaurantMagazineDTO;
import com.eatzy.restaurant.mapper.RestaurantMapper;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@Slf4j
public class GuestRankingStrategy implements RankingStrategy {

    private final RestaurantMapper restaurantMapper;
    private final MapboxService mapboxService;
    private static final BigDecimal MAX_DISTANCE_KM = new BigDecimal("10.0");

    public GuestRankingStrategy(RestaurantMapper restaurantMapper, MapboxService mapboxService) {
        this.restaurantMapper = restaurantMapper;
        this.mapboxService = mapboxService;
    }

    @Override
    public List<ResRestaurantMagazineDTO> calculateAndSort(List<Restaurant> restaurants, BigDecimal userLat,
            BigDecimal userLng, Long userId) {
        List<ResRestaurantMagazineDTO> results = new ArrayList<>();

        for (Restaurant r : restaurants) {
            if (r.getLatitude() == null || r.getLongitude() == null) {
                continue;
            }

            BigDecimal distance = null;
            if (userLat != null && userLng != null) {
                distance = mapboxService.getDrivingDistance(userLat, userLng, r.getLatitude(), r.getLongitude());
            }

            // Neu khong the lay duoc khoang cach hoac vuot qua khoang cach toi da thi bo
            // qua
            if (distance == null || distance.compareTo(MAX_DISTANCE_KM) > 0) {
                continue;
            }

            ResRestaurantMagazineDTO dto = restaurantMapper.convertToMagazineDTO(r);
            dto.setDistance(distance);

            // Tinh S_Ngon (Quality Score) cho Guest
            double qualityScore = r.getAverageRating() != null ? r.getAverageRating().doubleValue() * 20.0 : 0.0;
            dto.setQualityScore(qualityScore);

            results.add(dto);
        }

        // Sap xep tang dan theo khoang cach (gan nhat len dau)
        results.sort(Comparator.comparing(ResRestaurantMagazineDTO::getDistance,
                Comparator.nullsLast(Comparator.naturalOrder())));

        log.info("GuestRankingStrategy: processed {} restaurants", results.size());
        return results;
    }
}
