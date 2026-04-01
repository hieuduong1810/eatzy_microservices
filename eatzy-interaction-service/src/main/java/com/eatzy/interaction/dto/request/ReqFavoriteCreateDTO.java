package com.eatzy.interaction.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqFavoriteCreateDTO {
    private Long id;

    @NotNull(message = "Restaurant is required")
    private RestaurantDTO restaurant;

    @Getter
    @Setter
    public static class RestaurantDTO {
        private Long id;
    }
}
