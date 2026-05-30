package com.eatzy.restaurant.service;

import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.restaurant.domain.Restaurant;
import com.eatzy.restaurant.dto.res.ResRestaurantDTO;
import com.eatzy.restaurant.kafka.RestaurantEventProducer;
import com.eatzy.restaurant.mapper.RestaurantMapper;
import com.eatzy.restaurant.repository.RestaurantRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private RestaurantMapper restaurantMapper;
    @Mock
    private RestaurantEventProducer restaurantEventProducer;
    // other deps are not relevant for getRestaurantDTOBySlug
    @InjectMocks
    private RestaurantService restaurantService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getRestaurantDTOBySlug_whenRestaurantExists_publishesEventIfAuthenticated() throws Exception {
        String slug = "pho-bo-gia-truyen-24h";
        Restaurant r = new Restaurant();
        r.setId(10L);
        r.setName("Phở Bò Gia Truyền 24h");
        r.setRestaurantTypes(List.of(com.eatzy.restaurant.domain.RestaurantType.builder().id(7L).name("Noodles").build()));
        when(restaurantRepository.findBySlug(slug)).thenReturn(Optional.of(r));

        ResRestaurantDTO dto = ResRestaurantDTO.builder().id(10L).name("Phở Bò").build();
        when(restaurantMapper.convertToDTO(r)).thenReturn(dto);

        // Prepare SecurityContext with Jwt principal containing user.id claim
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600), Map.of(), Map.of("user", Map.of("id", 123)));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(jwt, "n/a", List.of());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        ResRestaurantDTO result = restaurantService.getRestaurantDTOBySlug(slug);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        verify(restaurantEventProducer, times(1)).publishSearchEvent(eq("RESTAURANT_VIEWED"), eq(123L), eq(10L), anyList());
    }

    @Test
    void getRestaurantDTOBySlug_whenNotFound_throwsIdInvalidException() {
        String slug = "not-found";
        when(restaurantRepository.findBySlug(slug)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> restaurantService.getRestaurantDTOBySlug(slug));
        verifyNoInteractions(restaurantMapper);
    }
}
