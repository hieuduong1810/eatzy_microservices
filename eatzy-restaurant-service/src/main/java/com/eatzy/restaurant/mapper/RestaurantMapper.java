package com.eatzy.restaurant.mapper;

import org.springframework.stereotype.Component;
import com.eatzy.restaurant.domain.Restaurant;
import com.eatzy.restaurant.domain.res.ResRestaurantDTO;

@Component
public class RestaurantMapper {

    public ResRestaurantDTO convertToDTO(Restaurant r) {
        if (r == null) {
            return null;
        }

        int reviewCount = 0;
        if (r.getOneStarCount() != null) reviewCount += r.getOneStarCount();
        if (r.getTwoStarCount() != null) reviewCount += r.getTwoStarCount();
        if (r.getThreeStarCount() != null) reviewCount += r.getThreeStarCount();
        if (r.getFourStarCount() != null) reviewCount += r.getFourStarCount();
        if (r.getFiveStarCount() != null) reviewCount += r.getFiveStarCount();

        return ResRestaurantDTO.builder()
                .id(r.getId())
                .ownerId(r.getOwnerId())
                .name(r.getName())
                .slug(r.getSlug())
                .address(r.getAddress())
                .description(r.getDescription())
                .latitude(r.getLatitude())
                .longitude(r.getLongitude())
                .contactPhone(r.getContactPhone())
                .status(r.getStatus())
                .commissionRate(r.getCommissionRate())
                .averageRating(r.getAverageRating())
                .reviewCount(reviewCount)
                .schedule(r.getSchedule())
                .avatarUrl(r.getAvatarUrl())
                .coverImageUrl(r.getCoverImageUrl())
                .build();
    }
}
