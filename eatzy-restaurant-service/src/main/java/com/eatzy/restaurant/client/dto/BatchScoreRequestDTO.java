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
public class BatchScoreRequestDTO {
    private Long userId;
    private List<Long> restaurantIds;
    private List<Long> typeIds;
}
