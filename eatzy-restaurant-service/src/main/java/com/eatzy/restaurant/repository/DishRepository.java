package com.eatzy.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.eatzy.restaurant.domain.Dish;

import java.util.List;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long>, JpaSpecificationExecutor<Dish> {
    List<Dish> findByRestaurantId(Long restaurantId);

    List<Dish> findByCategoryId(Long categoryId);

    boolean existsByNameAndRestaurantId(String name, Long restaurantId);
}
