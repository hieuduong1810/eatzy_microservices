package com.eatzy.restaurant.mapper;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

import com.eatzy.restaurant.domain.Restaurant;
import com.eatzy.restaurant.domain.DishCategory;
import com.eatzy.restaurant.domain.Dish;
import com.eatzy.restaurant.domain.MenuOptionGroup;
import com.eatzy.restaurant.domain.MenuOption;

import com.eatzy.restaurant.dto.res.ResDishCategoryDTO;
import com.eatzy.restaurant.dto.res.ResDishDTO;
import com.eatzy.restaurant.dto.res.ResMenuOptionDTO;
import com.eatzy.restaurant.dto.res.ResMenuOptionGroupDTO;
import com.eatzy.restaurant.dto.res.ResRestaurantDTO;
import com.eatzy.restaurant.dto.res.ResRestaurantMenuDTO;
import com.eatzy.restaurant.dto.res.ResRestaurantMagazineDTO;
import com.eatzy.restaurant.dto.res.ResRestaurantTypeDTO;

@Component
public class RestaurantMapper {

    public ResRestaurantDTO convertToDTO(Restaurant r) {
        if (r == null) {
            return null;
        }

        List<ResRestaurantTypeDTO> restaurantTypesDTO = null;
        if (r.getRestaurantTypes() != null) {
            restaurantTypesDTO = r.getRestaurantTypes().stream()
                    .map(type -> new ResRestaurantTypeDTO(type.getId(), type.getName()))
                    .collect(Collectors.toList());
        }

        int reviewCount = 0;
        if (r.getOneStarCount() != null)
            reviewCount += r.getOneStarCount();
        if (r.getTwoStarCount() != null)
            reviewCount += r.getTwoStarCount();
        if (r.getThreeStarCount() != null)
            reviewCount += r.getThreeStarCount();
        if (r.getFourStarCount() != null)
            reviewCount += r.getFourStarCount();
        if (r.getFiveStarCount() != null)
            reviewCount += r.getFiveStarCount();

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
                .restaurantTypes(restaurantTypesDTO)
                .build();
    }

    public ResRestaurantMenuDTO convertToMenuDTO(Restaurant r) {
        if (r == null)
            return null;

        List<ResDishCategoryDTO> categoriesDTO = null;
        if (r.getDishCategories() != null) {
            categoriesDTO = r.getDishCategories().stream()
                    .map(cat -> new ResDishCategoryDTO(
                            cat.getId(),
                            cat.getName(),
                            cat.getDishes() != null ? cat.getDishes().stream()
                                    .map(dish -> new ResDishDTO(
                                            dish.getId(),
                                            dish.getName(),
                                            dish.getDescription(),
                                            dish.getPrice(),
                                            dish.getAvailabilityQuantity(),
                                            dish.getImageUrl(),
                                            dish.getMenuOptionGroups() != null ? dish.getMenuOptionGroups().size() : 0,
                                            dish.getMenuOptionGroups() != null ? dish.getMenuOptionGroups().stream()
                                                    .map(group -> new ResMenuOptionGroupDTO(
                                                            group.getId(),
                                                            group.getGroupName(),
                                                            group.getMinChoices(),
                                                            group.getMaxChoices(),
                                                            group.getMenuOptions() != null
                                                                    ? group.getMenuOptions().stream()
                                                                            .map(opt -> new ResMenuOptionDTO(
                                                                                    opt.getId(),
                                                                                    opt.getName(),
                                                                                    opt.getPriceAdjustment(),
                                                                                    opt.getIsAvailable() != null
                                                                                            ? opt.getIsAvailable()
                                                                                            : false))
                                                                            .collect(Collectors.toList())
                                                                    : null))
                                                    .collect(Collectors.toList()) : null))
                                    .collect(Collectors.toList()) : null))
                    .collect(Collectors.toList());
        }

        return new ResRestaurantMenuDTO(
                r.getId(),
                r.getName(),
                categoriesDTO);
    }

    public ResRestaurantMagazineDTO convertToMagazineDTO(Restaurant r) {
        if (r == null)
            return null;

        List<ResRestaurantMagazineDTO.Category> categoriesDTO = null;
        if (r.getDishCategories() != null) {
            categoriesDTO = r.getDishCategories().stream()
                    .map(cat -> new ResRestaurantMagazineDTO.Category(
                            cat.getId(),
                            cat.getName(),
                            cat.getDishes() != null ? cat.getDishes().stream()
                                    .map(dish -> new ResRestaurantMagazineDTO.Category.Dish(
                                            dish.getId(),
                                            dish.getName(),
                                            dish.getDescription(),
                                            dish.getPrice(),
                                            dish.getImageUrl()))
                                    .collect(Collectors.toList()) : null))
                    .collect(Collectors.toList());
        }

        ResRestaurantMagazineDTO dto = new ResRestaurantMagazineDTO();
        dto.setId(r.getId());
        dto.setName(r.getName());
        dto.setSlug(r.getSlug());
        dto.setAddress(r.getAddress());
        dto.setDescription(r.getDescription());
        dto.setAvatarUrl(r.getAvatarUrl());
        dto.setOneStarCount(r.getOneStarCount());
        dto.setTwoStarCount(r.getTwoStarCount());
        dto.setThreeStarCount(r.getThreeStarCount());
        dto.setFourStarCount(r.getFourStarCount());
        dto.setFiveStarCount(r.getFiveStarCount());
        dto.setAverageRating(r.getAverageRating());
        dto.setCategory(categoriesDTO);

        return dto;
    }
}
