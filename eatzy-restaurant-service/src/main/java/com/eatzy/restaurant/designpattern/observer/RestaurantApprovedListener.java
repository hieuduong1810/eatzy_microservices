package com.eatzy.restaurant.designpattern.observer;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.eatzy.restaurant.designpattern.factory.NotificationFactory;
import com.eatzy.restaurant.domain.Restaurant;

import lombok.extern.slf4j.Slf4j;

/**
 * ★ DESIGN PATTERN #3: Observer Pattern
 * 
 * Class nay la Observer (Nguoi nghe). No tu dong lang nghe RestaurantApprovedEvent
 * va thuc hien hanh dong khi nha hang duoc duyet.
 * 
 * Ket hop voi Factory Pattern (#2) de chon kenh gui thong bao.
 */
@Component
@Slf4j
public class RestaurantApprovedListener {

    private final NotificationFactory notificationFactory;

    public RestaurantApprovedListener(NotificationFactory notificationFactory) {
        this.notificationFactory = notificationFactory;
    }

    /**
     * Ham nay duoc Spring tu dong goi khi RestaurantApprovedEvent duoc phat ra.
     * Khong can RestaurantService goi truc tiep -> Loose Coupling (gop ket loi).
     */
    @EventListener
    public void onRestaurantApproved(RestaurantApprovedEvent event) {
        Restaurant restaurant = event.getRestaurant();
        log.info("🎉 [OBSERVER] Restaurant approved: {} (ID: {})", restaurant.getName(), restaurant.getId());

        // Su dung Factory Pattern de gui thong bao qua kenh EMAIL
        try {
            notificationFactory.getNotifier("EMAIL").send(
                    "owner-" + restaurant.getOwnerId() + "@eatzy.com",
                    "🎊 Chuc mung! Nha hang " + restaurant.getName() + " da duoc duyet!",
                    "Nha hang cua ban da duoc phe duyet va hien thi tren nen tang Eatzy. "
                            + "Ban co the bat dau nhan don hang ngay bay gio!");
        } catch (Exception e) {
            log.error("Failed to send notification for restaurant {}: {}", restaurant.getId(), e.getMessage());
        }
    }
}
