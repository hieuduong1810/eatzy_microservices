package com.eatzy.restaurant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.eatzy.restaurant.domain.RestaurantType;

@Repository
public interface RestaurantTypeRepository
        extends JpaRepository<RestaurantType, Long>, JpaSpecificationExecutor<RestaurantType> {
    boolean existsBySlug(String slug);

    List<RestaurantType> findByIdIn(List<Long> ids);
}
