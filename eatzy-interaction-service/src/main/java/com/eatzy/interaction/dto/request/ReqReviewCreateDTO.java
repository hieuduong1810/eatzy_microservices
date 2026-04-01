package com.eatzy.interaction.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqReviewCreateDTO {
    private Long id;

    @NotNull(message = "Order is required")
    private OrderDTO order;

    @NotBlank(message = "Review target is required")
    private String reviewTarget;

    private String targetName;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    private String comment;
    private String reply;

    @Getter
    @Setter
    public static class OrderDTO {
        private Long id;
    }
}
