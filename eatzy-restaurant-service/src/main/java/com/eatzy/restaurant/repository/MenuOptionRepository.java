package com.eatzy.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.eatzy.restaurant.domain.MenuOption;

import java.util.List;

@Repository
public interface MenuOptionRepository extends JpaRepository<MenuOption, Long>, JpaSpecificationExecutor<MenuOption> {
    List<MenuOption> findByMenuOptionGroupId(Long groupId);
}
