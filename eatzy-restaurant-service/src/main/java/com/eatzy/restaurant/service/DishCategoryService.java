package com.eatzy.restaurant.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.eatzy.restaurant.domain.DishCategory;
import com.eatzy.restaurant.domain.Restaurant;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.restaurant.repository.DishCategoryRepository;
import com.eatzy.common.exception.IdInvalidException;

@Service
public class DishCategoryService {
    private final DishCategoryRepository dishCategoryRepository;
    private final RestaurantService restaurantService;

    public DishCategoryService(DishCategoryRepository dishCategoryRepository, RestaurantService restaurantService) {
        this.dishCategoryRepository = dishCategoryRepository;
        this.restaurantService = restaurantService;
    }

    public boolean existsByNameAndRestaurantId(String name, Long restaurantId) {
        return dishCategoryRepository.existsByNameAndRestaurantId(name, restaurantId);
    }

    public DishCategory getDishCategoryById(Long id) {
        Optional<DishCategory> categoryOpt = this.dishCategoryRepository.findById(id);
        return categoryOpt.orElse(null);
    }

    public List<DishCategory> getDishCategoriesByRestaurantId(Long restaurantId) {
        return this.dishCategoryRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId);
    }

    public DishCategory createDishCategory(DishCategory dishCategory) throws IdInvalidException {
        // check restaurant exists
        if (dishCategory.getRestaurant() != null) {
            Restaurant restaurant = this.restaurantService.getRestaurantById(dishCategory.getRestaurant().getId());
            if (restaurant == null) {
                throw new IdInvalidException("Restaurant not found with id: " + dishCategory.getRestaurant().getId());
            }

            // check duplicate name in same restaurant
            if (this.existsByNameAndRestaurantId(dishCategory.getName(), restaurant.getId())) {
                throw new IdInvalidException(
                        "Category name already exists in this restaurant: " + dishCategory.getName());
            }

            dishCategory.setRestaurant(restaurant);
        } else {
            throw new IdInvalidException("Restaurant is required");
        }

        return dishCategoryRepository.save(dishCategory);
    }

    public DishCategory updateDishCategory(DishCategory dishCategory) throws IdInvalidException {
        // check id
        DishCategory currentCategory = getDishCategoryById(dishCategory.getId());
        if (currentCategory == null) {
            throw new IdInvalidException("Dish category not found with id: " + dishCategory.getId());
        }

        // update fields
        if (dishCategory.getName() != null) {
            // check duplicate name if name is changed
            if (!currentCategory.getName().equals(dishCategory.getName())) {
                if (this.existsByNameAndRestaurantId(dishCategory.getName(), currentCategory.getRestaurant().getId())) {
                    throw new IdInvalidException(
                            "Category name already exists in this restaurant: " + dishCategory.getName());
                }
            }
            currentCategory.setName(dishCategory.getName());
        }
        if (dishCategory.getDisplayOrder() != null) {
            currentCategory.setDisplayOrder(dishCategory.getDisplayOrder());
        }

        return dishCategoryRepository.save(currentCategory);
    }

    public ResultPaginationDTO getAllDishCategories(Specification<DishCategory> spec, Pageable pageable) {
        Page<DishCategory> page = this.dishCategoryRepository.findAll(spec, pageable);
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

    public void deleteDishCategory(Long id) {
        this.dishCategoryRepository.deleteById(id);
    }
}
