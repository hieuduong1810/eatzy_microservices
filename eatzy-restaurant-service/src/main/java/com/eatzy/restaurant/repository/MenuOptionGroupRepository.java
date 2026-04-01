package com.eatzy.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.eatzy.restaurant.domain.MenuOptionGroup;

import java.util.List;

@Repository
public interface MenuOptionGroupRepository
        extends JpaRepository<MenuOptionGroup, Long>, JpaSpecificationExecutor<MenuOptionGroup> {
    List<MenuOptionGroup> findByDishId(Long dishId);
}
