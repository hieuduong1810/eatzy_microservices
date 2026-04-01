package com.eatzy.interaction.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoringEventDTO {
    private Long userId;
    private Long restaurantId;
    private List<Long> restaurantTypeIds;
}
