package com.eatzy.cart.service;

import com.eatzy.cart.domain.Cart;
import com.eatzy.cart.domain.CartItem;
import com.eatzy.cart.domain.CartItemOption;
import com.eatzy.cart.dto.req.ReqCartDTO;
import com.eatzy.cart.dto.res.ResCartDTO;
import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.common.util.SecurityUtils;
import com.eatzy.cart.mapper.CartMapper;
import com.eatzy.cart.repository.CartRepository;
import com.eatzy.cart.kafka.CartEventProducer;
import com.eatzy.cart.designpattern.adapter.RestaurantServiceClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartMapper cartMapper;
    private final CartEventProducer cartEventProducer;
    private final RestaurantServiceClient restaurantServiceClient;

    public List<ResCartDTO> getCartsByCustomerId(Long customerId) {
        return cartRepository.findByCustomerIdOrderByIdDesc(customerId).stream()
                .map(cartMapper::convertToResCartDTO)
                .collect(Collectors.toList());
    }

    public ResCartDTO getCartById(Long id) {
        Optional<Cart> cart = cartRepository.findById(id);
        return cart.map(cartMapper::convertToResCartDTO).orElse(null);
    }

    public List<ResCartDTO> getMyCarts() throws com.eatzy.common.exception.IdInvalidException {
        Long customerId = SecurityUtils.getCurrentUserId();
        return cartRepository.findByCustomerIdOrderByIdDesc(customerId).stream()
                .map(cartMapper::convertToResCartDTO)
                .collect(Collectors.toList());
    }

    public ResultPaginationDTO getAllCarts(Specification<Cart> spec, Pageable pageable) {
        Page<Cart> page = cartRepository.findAll(spec, pageable);
        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());
        result.setMeta(meta);
        result.setResult(page.getContent().stream()
                .map(cartMapper::convertToResCartDTO)
                .collect(Collectors.toList()));
        return result;
    }

    @Transactional
    public void deleteCart(Long id) {
        cartRepository.deleteById(id);
    }

    @Transactional
    public ResCartDTO saveOrUpdateCart(ReqCartDTO reqCartDTO) throws com.eatzy.common.exception.IdInvalidException {
        // Need customerId. Since the ReqCartDTO has customer internally?
        // Wait, original reqCartDTO has Customer entity with ID. Let's assume frontend
        // passes customer.id
        if (reqCartDTO.getRestaurant() == null || reqCartDTO.getRestaurant().getId() == null) {
            throw new IllegalArgumentException("Restaurant ID is required");
        }

        Long customerId = SecurityUtils.getCurrentUserId();

        Long restaurantId = reqCartDTO.getRestaurant().getId();

        Cart cart = cartRepository.findByCustomerIdAndRestaurantId(customerId, restaurantId).orElse(null);

        if (cart == null) {
            cart = Cart.builder()
                    .customerId(customerId)
                    .restaurantId(restaurantId)
                    .build();
        } else {
            cart.getCartItems().clear();
        }

        if (reqCartDTO.getCartItems() != null && !reqCartDTO.getCartItems().isEmpty()) {
            for (ReqCartDTO.CartItem reqItem : reqCartDTO.getCartItems()) {
                if (reqItem.getDish() == null || reqItem.getDish().getId() == null) {
                    throw new IllegalArgumentException("Dish is required for cart item");
                }

                CartItem cartItem = CartItem.builder()
                        .cart(cart)
                        .dishId(reqItem.getDish().getId())
                        .quantity(reqItem.getQuantity())
                        .build();

                if (reqItem.getCartItemOptions() != null) {
                    for (ReqCartDTO.CartItem.CartItemOption reqOption : reqItem.getCartItemOptions()) {
                        if (reqOption.getMenuOption() == null || reqOption.getMenuOption().getId() == null) {
                            throw new IllegalArgumentException("Menu option is required");
                        }
                        CartItemOption cartItemOption = CartItemOption.builder()
                                .cartItem(cartItem)
                                .menuOptionId(reqOption.getMenuOption().getId())
                                .build();
                        cartItem.getCartItemOptions().add(cartItemOption);
                    }
                }
                cart.getCartItems().add(cartItem);
            }

            Cart savedCart = cartRepository.save(cart);
            log.info("🛒 User updated cart for restaurant {}", restaurantId);
            
            // Publish ITEM_ADDED event for user scoring
            try {
                java.util.Map<String, Object> restData = restaurantServiceClient.getRestaurantById(restaurantId);
                java.util.List<Long> typeIds = new java.util.ArrayList<>();
                if (restData != null && restData.containsKey("restaurantTypes")) {
                    java.util.List<java.util.Map<String, Object>> types = (java.util.List<java.util.Map<String, Object>>) restData.get("restaurantTypes");
                    for (java.util.Map<String, Object> t : types) {
                        if (t.get("id") != null) {
                            typeIds.add(Long.valueOf(t.get("id").toString()));
                        }
                    }
                }
                cartEventProducer.publishItemAddedEvent(customerId, restaurantId, typeIds);
            } catch (FeignException e) {
                log.warn("Failed to fetch restaurant details for scoring event: {}", e.getMessage());
                cartEventProducer.publishItemAddedEvent(customerId, restaurantId, java.util.Collections.emptyList());
            }

            return cartMapper.convertToResCartDTO(savedCart);
        } else {
            if (cart.getId() != null) {
                cartRepository.delete(cart);
            }
            return null;
        }
    }
}
