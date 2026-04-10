package com.eatzy.restaurant.designpattern.strategy.ranking;

import com.eatzy.restaurant.domain.Restaurant;
import com.eatzy.restaurant.domain.res.ResRestaurantMagazineDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * ★ DESIGN PATTERN: Strategy Pattern
 * Interface for ranking restaurants based on different user contexts.
 */
public interface RankingStrategy {
    /**
     * Tinh toan diem va sap xep nha hang.
     */
    List<ResRestaurantMagazineDTO> calculateAndSort(List<Restaurant> restaurants, BigDecimal userLat, BigDecimal userLng, Long userId);
}
