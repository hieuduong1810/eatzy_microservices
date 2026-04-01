package com.eatzy.restaurant.domain.req;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReqDishDTO {
    private Long id;
    private DishCategory category;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private int availabilityQuantity;
    private List<MenuOptionGroup> menuOptionGroups;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DishCategory {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuOptionGroup {
        private Long id;
        private String groupName;
        private Integer minChoices;
        private Integer maxChoices;
        private List<MenuOption> menuOptions;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class MenuOption {
            private Long id;
            private String name;
            private BigDecimal priceAdjustment;
            private Boolean isAvailable;
        }
    }

}
