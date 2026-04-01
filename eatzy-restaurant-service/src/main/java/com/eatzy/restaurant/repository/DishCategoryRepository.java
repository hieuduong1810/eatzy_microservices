package com.eatzy.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.eatzy.restaurant.domain.DishCategory;

import java.util.List;

@Repository
public interface DishCategoryRepository
        extends JpaRepository<DishCategory, Long>, JpaSpecificationExecutor<DishCategory> {
    List<DishCategory> findByRestaurantIdOrderByDisplayOrderAsc(Long restaurantId);

    boolean existsByNameAndRestaurantId(String name, Long restaurantId);
}
