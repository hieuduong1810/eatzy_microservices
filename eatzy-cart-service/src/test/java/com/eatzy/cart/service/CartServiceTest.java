package com.eatzy.cart.service;

import com.eatzy.cart.dto.req.ReqCartDTO;
import com.eatzy.cart.dto.res.ResCartDTO;
import com.eatzy.cart.mapper.CartMapper;
import com.eatzy.cart.repository.CartRepository;
import com.eatzy.cart.kafka.CartEventProducer;
import com.eatzy.cart.designpattern.adapter.RestaurantServiceClient;
import com.eatzy.cart.domain.Cart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartMapper cartMapper;
    @Mock
    private CartEventProducer cartEventProducer;
    @Mock
    private RestaurantServiceClient restaurantServiceClient;

    @InjectMocks
    private CartService cartService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void saveOrUpdateCart_createsCartAndPublishesEvent_withRestaurantTypes() throws Exception {
        // prepare security context
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600), Map.of("alg", "none"), Map.of("user", Map.of("id", 42)));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(jwt, "n/a", List.of());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        ReqCartDTO req = ReqCartDTO.builder()
                .restaurant(new ReqCartDTO.Restaurant(100L))
                .cartItems(List.of(
                        ReqCartDTO.CartItem.builder()
                                .dish(new ReqCartDTO.CartItem.Dish(555L))
                                .quantity(2)
                                .build()
                ))
                .build();

        when(cartRepository.findByCustomerIdAndRestaurantId(42L, 100L)).thenReturn(java.util.Optional.empty());

        // simulate save returns Cart with id
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        ResCartDTO expected = ResCartDTO.builder().id(1L).build();
        when(cartMapper.convertToResCartDTO(any())).thenReturn(expected);

        // restaurant service returns restaurantTypes
        when(restaurantServiceClient.getRestaurantById(100L)).thenReturn(Map.of("restaurantTypes", List.of(Map.of("id", 7))));

        ResCartDTO result = cartService.saveOrUpdateCart(req);

        assertThat(result).isEqualTo(expected);
        verify(cartEventProducer, times(1)).publishItemAddedEvent(eq(42L), eq(100L), anyList());
    }

    @Test
    void saveOrUpdateCart_whenRestaurantServiceReturnsNull_publishesEventWithEmptyTypes() throws Exception {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600), Map.of("alg", "none"), Map.of("user", Map.of("id", 99)));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(jwt, "n/a", List.of());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        ReqCartDTO req = ReqCartDTO.builder()
                .restaurant(new ReqCartDTO.Restaurant(200L))
                .cartItems(List.of(
                        ReqCartDTO.CartItem.builder()
                                .dish(new ReqCartDTO.CartItem.Dish(777L))
                                .quantity(1)
                                .build()
                ))
                .build();

        when(cartRepository.findByCustomerIdAndRestaurantId(99L, 200L)).thenReturn(java.util.Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartMapper.convertToResCartDTO(any())).thenReturn(ResCartDTO.builder().id(2L).build());
        when(restaurantServiceClient.getRestaurantById(200L)).thenReturn(null);

        ResCartDTO result = cartService.saveOrUpdateCart(req);

        assertThat(result).isNotNull();
        verify(cartEventProducer, times(1)).publishItemAddedEvent(eq(99L), eq(200L), eq(java.util.Collections.emptyList()));
    }
}
