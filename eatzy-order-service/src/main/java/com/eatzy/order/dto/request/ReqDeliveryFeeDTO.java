package com.eatzy.order.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReqDeliveryFeeDTO {
    @NotNull(message = "Restaurant ID không được để trống")
    private Long restaurantId;

    @NotNull(message = "Delivery latitude không được để trống")
    private Double deliveryLatitude;

    @NotNull(message = "Delivery longitude không được để trống")
    private Double deliveryLongitude;
}
