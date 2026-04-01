package com.eatzy.interaction.designpattern.event;

import com.eatzy.interaction.dto.request.ScoringEventDTO;
import com.eatzy.interaction.service.UserScoringService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class KafkaConsumerService {

    private final UserScoringService userScoringService;

    public KafkaConsumerService(UserScoringService userScoringService) {
        this.userScoringService = userScoringService;
    }

    @KafkaListener(topics = "order_events_topic", groupId = "interaction-group")
    public void consumeOrderEvent(Map<String, Object> event) {
        try {
            String action = (String) event.get("action");
            if ("PLACED".equals(action)) {
                ScoringEventDTO dto = mapToScoringEvent(event);
                userScoringService.trackPlaceOrder(dto);
            }
        } catch (Exception e) {
            log.error("Failed to process order event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "cart_events_topic", groupId = "interaction-group")
    public void consumeCartEvent(Map<String, Object> event) {
        try {
            String action = (String) event.get("action");
            if ("ITEM_ADDED".equals(action)) {
                ScoringEventDTO dto = mapToScoringEvent(event);
                userScoringService.trackAddToCart(dto);
            }
        } catch (Exception e) {
            log.error("Failed to process cart event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "search_events_topic", groupId = "interaction-group")
    public void consumeSearchEvent(Map<String, Object> event) {
        try {
            String action = (String) event.get("action");
            ScoringEventDTO dto = mapToScoringEvent(event);
            
            if ("RESTAURANT_CLICKED".equals(action)) {
                userScoringService.trackSearchRestaurantByNameAndClick(dto);
            } else if ("DISH_CLICKED".equals(action)) {
                userScoringService.trackSearchDishAndClick(dto);
            } else if ("RESTAURANT_VIEWED".equals(action)) {
                userScoringService.trackViewRestaurantDetails(dto);
            }
        } catch (Exception e) {
            log.error("Failed to process search event: {}", e.getMessage());
        }
    }

    private ScoringEventDTO mapToScoringEvent(Map<String, Object> event) {
        Long userId = event.get("userId") != null ? Long.valueOf(event.get("userId").toString()) : null;
        Long restaurantId = event.get("restaurantId") != null ? Long.valueOf(event.get("restaurantId").toString()) : null;
        List<Long> typeIds = null;
        if (event.get("restaurantTypeIds") != null) {
            typeIds = (List<Long>) event.get("restaurantTypeIds");
        }
        return new ScoringEventDTO(userId, restaurantId, typeIds);
    }
}
