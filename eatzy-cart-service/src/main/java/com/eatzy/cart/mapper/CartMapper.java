package com.eatzy.cart.mapper;

import com.eatzy.cart.designpattern.adapter.RestaurantServiceClient;
import com.eatzy.cart.designpattern.decorator.BaseItemPrice;
import com.eatzy.cart.designpattern.decorator.ItemPriceCalculator;
import com.eatzy.cart.designpattern.decorator.MenuOptionDecorator;
import com.eatzy.cart.domain.Cart;
import com.eatzy.cart.domain.CartItem;
import com.eatzy.cart.domain.CartItemOption;
import com.eatzy.cart.dto.res.ResCartDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartMapper {

    private final RestaurantServiceClient restaurantServiceClient;

    public ResCartDTO convertToResCartDTO(Cart cart) {
        if (cart == null) return null;

        ResCartDTO.ResCartDTOBuilder dtoBuilder = ResCartDTO.builder()
                .id(cart.getId());

        // We only have customerId natively. The frontend usually fetches user details separately
        // But to maintain the contract, we populate what we can.
        dtoBuilder.customer(ResCartDTO.Customer.builder()
                .id(cart.getCustomerId())
                .name("Customer_" + cart.getCustomerId()) // Placeholder, since Auth Service doesn't have a public GET /users endpoint in this scope usually. If needed, we could add AuthServiceClient.
                .build());

        // Fetch Restaurant details
        Map<String, Object> restaurantData = restaurantServiceClient.getRestaurantById(cart.getRestaurantId());
        if (restaurantData != null) {
            dtoBuilder.restaurant(ResCartDTO.Restaurant.builder()
                    .id(cart.getRestaurantId())
                    .name(getStringValue(restaurantData, "name"))
                    .address(getStringValue(restaurantData, "address"))
                    .imageUrl(getStringValue(restaurantData, "avatarUrl"))
                    .status(getStringValue(restaurantData, "status"))
                    .build());
        }

        List<ResCartDTO.CartItem> cartItemDtos = new ArrayList<>();
        BigDecimal cartTotal = BigDecimal.ZERO;

        if (cart.getCartItems() != null) {
            for (CartItem item : cart.getCartItems()) {
                ResCartDTO.CartItem.CartItemBuilder itemBuilder = ResCartDTO.CartItem.builder()
                        .id(item.getId())
                        .quantity(item.getQuantity());

                // Fetch Dish details
                Map<String, Object> dishData = restaurantServiceClient.getDishById(item.getDishId());
                BigDecimal dishBasePrice = BigDecimal.ZERO;

                if (dishData != null) {
                    dishBasePrice = getBigDecimalValue(dishData, "price");
                    itemBuilder.dish(ResCartDTO.CartItem.Dish.builder()
                            .id(item.getDishId())
                            .name(getStringValue(dishData, "name"))
                            .price(dishBasePrice)
                            .image(getStringValue(dishData, "imageUrl"))
                            .build());
                }

                // Decorator Pattern: Start with base price
                ItemPriceCalculator priceCalculator = new BaseItemPrice(dishBasePrice);

                List<ResCartDTO.CartItem.CartItemOption> optionDtos = new ArrayList<>();
                if (item.getCartItemOptions() != null) {
                    for (CartItemOption option : item.getCartItemOptions()) {
                        Map<String, Object> menuOptionData = restaurantServiceClient.getMenuOptionById(option.getMenuOptionId());
                        BigDecimal optionPrice = BigDecimal.ZERO;
                        
                        if (menuOptionData != null) {
                            optionPrice = getBigDecimalValue(menuOptionData, "priceAdjustment");
                            optionDtos.add(ResCartDTO.CartItem.CartItemOption.builder()
                                    .id(option.getId())
                                    .menuOption(ResCartDTO.CartItem.CartItemOption.MenuOption.builder()
                                            .id(option.getMenuOptionId())
                                            .name(getStringValue(menuOptionData, "name"))
                                            .priceAdjustment(optionPrice)
                                            .build())
                                    .build());
                        }
                        
                        // Decorator Pattern: Wrap the calculator with the new option price
                        priceCalculator = new MenuOptionDecorator(priceCalculator, optionPrice);
                    }
                }
                itemBuilder.cartItemOptions(optionDtos);

                // Calculate item total using the final decorated calculator
                BigDecimal itemTotalPrice = priceCalculator.calculatePrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                itemBuilder.totalPrice(itemTotalPrice);

                cartTotal = cartTotal.add(itemTotalPrice);
                cartItemDtos.add(itemBuilder.build());
            }
        }

        dtoBuilder.cartItems(cartItemDtos);
        dtoBuilder.cartTotal(cartTotal);

        return dtoBuilder.build();
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(String.valueOf(val));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
