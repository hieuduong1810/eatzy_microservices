package com.eatzy.restaurant.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchScoreResponseDTO {
    private List<RestaurantScoreDTO> restaurantScores;
    private List<TypeScoreDTO> typeScores;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestaurantScoreDTO {
        private Long restaurantId;
        private Integer score;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeScoreDTO {
        private Long typeId;
        private Integer score;
    }
}
