package com.eatzy.restaurant.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.eatzy.restaurant.domain.RestaurantType;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.restaurant.repository.RestaurantTypeRepository;
import com.eatzy.common.exception.IdInvalidException;

@Service
public class RestaurantTypeService {
    private final RestaurantTypeRepository restaurantTypeRepository;

    public RestaurantTypeService(RestaurantTypeRepository restaurantTypeRepository) {
        this.restaurantTypeRepository = restaurantTypeRepository;
    }

    public RestaurantType getRestaurantTypeById(Long id) {
        Optional<RestaurantType> restaurantTypeOpt = this.restaurantTypeRepository.findById(id);
        if (restaurantTypeOpt.isPresent()) {
            return restaurantTypeOpt.get();
        }
        return null;
    }

    public boolean checkRestaurantTypeExists(String slug) {
        return restaurantTypeRepository.existsBySlug(slug);
    }

    public RestaurantType createRestaurantType(RestaurantType restaurantType) {
        return restaurantTypeRepository.save(restaurantType);
    }

    public RestaurantType updateRestaurantType(RestaurantType restaurantType) throws IdInvalidException {
        // check id
        RestaurantType currentRestaurantType = getRestaurantTypeById(restaurantType.getId());
        if (currentRestaurantType == null) {
            throw new IdInvalidException("Restaurant type not found with id: " + restaurantType.getId());
        }

        if (restaurantType.getName() != null) {
            currentRestaurantType.setName(restaurantType.getName());
        }
        if (restaurantType.getSlug() != null) {
            currentRestaurantType.setSlug(restaurantType.getSlug());
        }
        if (restaurantType.getImage() != null) {
            currentRestaurantType.setImage(restaurantType.getImage());
        }
        if (restaurantType.getDisplayOrder() != 0) {
            currentRestaurantType.setDisplayOrder(restaurantType.getDisplayOrder());
        }

        // check exist by slug
        if (restaurantType.getSlug() != null && !restaurantType.getSlug().equals(currentRestaurantType.getSlug())) {
            boolean isSlugExist = checkRestaurantTypeExists(restaurantType.getSlug());
            if (isSlugExist) {
                throw new IdInvalidException("Restaurant type already exists with slug: " + restaurantType.getSlug());
            }
        }

        return restaurantTypeRepository.save(currentRestaurantType);
    }

    public ResultPaginationDTO getAllRestaurantTypes(Specification<RestaurantType> spec, Pageable pageable) {
        Page<RestaurantType> page = this.restaurantTypeRepository.findAll(spec, pageable);
        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());
        result.setMeta(meta);
        result.setResult(page.getContent());
        return result;
    }

    public void deleteRestaurantType(Long id) {
        Optional<RestaurantType> restaurantTypeOpt = this.restaurantTypeRepository.findById(id);
        RestaurantType restaurantType = restaurantTypeOpt.get();
        restaurantType.getRestaurants().forEach(restaurant -> {
            restaurant.getRestaurantTypes().remove(restaurantType);
        });
        this.restaurantTypeRepository.deleteById(id);
    }
}
