package com.eatzy.restaurant.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.restaurant.designpattern.adapter.LocationService;
import com.eatzy.restaurant.designpattern.observer.RestaurantApprovedEvent;
import com.eatzy.restaurant.designpattern.strategy.CommissionStrategy;
import com.eatzy.restaurant.domain.Restaurant;
import com.eatzy.restaurant.domain.res.ResRestaurantDTO;
import com.eatzy.restaurant.mapper.RestaurantMapper;
import com.eatzy.restaurant.repository.RestaurantRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final ApplicationEventPublisher eventPublisher; // Observer Pattern
    private final LocationService locationService; // Adapter Pattern
    private final Map<String, CommissionStrategy> strategyMap; // Strategy Pattern
    private final RestaurantMapper restaurantMapper;

    public RestaurantService(RestaurantRepository restaurantRepository,
            ApplicationEventPublisher eventPublisher,
            LocationService locationService,
            Map<String, CommissionStrategy> strategyMap,
            RestaurantMapper restaurantMapper) {
        this.restaurantRepository = restaurantRepository;
        this.eventPublisher = eventPublisher;
        this.locationService = locationService;
        this.strategyMap = strategyMap;
        this.restaurantMapper = restaurantMapper;
    }

    // ==================== CRUD ====================

    public Restaurant getRestaurantById(Long id) {
        return restaurantRepository.findById(id).orElse(null);
    }

    /**
     * Tao nha hang moi.
     * Su dung Builder Pattern (#1) de tao DTO tra ve.
     */
    public ResRestaurantDTO createRestaurant(Restaurant restaurant) throws IdInvalidException {
        if (restaurantRepository.existsByName(restaurant.getName())) {
            throw new IdInvalidException("Restaurant name already exists: " + restaurant.getName());
        }

        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("✅ Created restaurant: {} (ID: {})", saved.getName(), saved.getId());

        // ★ BUILDER PATTERN (#1): Tao DTO bang Builder thay vi setter loi nhoi
        return ResRestaurantDTO.builder()
                .id(saved.getId())
                .ownerId(saved.getOwnerId())
                .name(saved.getName())
                .slug(saved.getSlug())
                .address(saved.getAddress())
                .description(saved.getDescription())
                .latitude(saved.getLatitude())
                .longitude(saved.getLongitude())
                .contactPhone(saved.getContactPhone())
                .status(saved.getStatus())
                .commissionRate(saved.getCommissionRate())
                .averageRating(saved.getAverageRating())
                .schedule(saved.getSchedule())
                .avatarUrl(saved.getAvatarUrl())
                .coverImageUrl(saved.getCoverImageUrl())
                .build();
    }

    public ResRestaurantDTO updateRestaurant(Restaurant restaurant) throws IdInvalidException {
        Restaurant current = getRestaurantById(restaurant.getId());
        if (current == null) {
            throw new IdInvalidException("Restaurant not found with id: " + restaurant.getId());
        }

        if (restaurant.getName() != null)
            current.setName(restaurant.getName());
        if (restaurant.getAddress() != null)
            current.setAddress(restaurant.getAddress());
        if (restaurant.getDescription() != null)
            current.setDescription(restaurant.getDescription());
        if (restaurant.getLatitude() != null)
            current.setLatitude(restaurant.getLatitude());
        if (restaurant.getLongitude() != null)
            current.setLongitude(restaurant.getLongitude());
        if (restaurant.getContactPhone() != null)
            current.setContactPhone(restaurant.getContactPhone());
        if (restaurant.getStatus() != null)
            current.setStatus(restaurant.getStatus());
        if (restaurant.getSchedule() != null)
            current.setSchedule(restaurant.getSchedule());

        Restaurant saved = restaurantRepository.save(current);
        return restaurantMapper.convertToDTO(saved);
    }

    public ResultPaginationDTO getAllRestaurants(Specification<Restaurant> spec, Pageable pageable) {
        Page<Restaurant> page = restaurantRepository.findAll(spec, pageable);
        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());
        result.setMeta(meta);
        result.setResult(page.getContent().stream()
                .map(restaurantMapper::convertToDTO)
                .collect(Collectors.toList()));
        return result;
    }

    public void deleteRestaurant(Long id) {
        restaurantRepository.deleteById(id);
    }

    public ResRestaurantDTO getCurrentOwnerRestaurant(Long ownerId) {
        return restaurantRepository.findByOwnerId(ownerId)
                .map(restaurantMapper::convertToDTO)
                .orElse(null);
    }

    public Restaurant getRestaurantByOwnerId(Long ownerId) {
        return restaurantRepository.findByOwnerId(ownerId).orElse(null);
    }

    // ==================== DESIGN PATTERN ENDPOINTS ====================

    /**
     * ★ OBSERVER PATTERN (#3): Duyet nha hang - phat event de cac Listener tu dong
     * chay.
     */
    public ResRestaurantDTO approveRestaurant(Long id) throws IdInvalidException {
        Restaurant restaurant = getRestaurantById(id);
        if (restaurant == null) {
            throw new IdInvalidException("Restaurant not found with id: " + id);
        }

        restaurant.setStatus("APPROVED");
        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("✅ Restaurant approved: {} (ID: {})", saved.getName(), saved.getId());

        // Ban event -> RestaurantApprovedListener se tu dong bat va gui email
        eventPublisher.publishEvent(new RestaurantApprovedEvent(this, saved));

        return restaurantMapper.convertToDTO(saved);
    }

    /**
     * ★ STRATEGY PATTERN (#4): Tinh hoa hong dua tren loai nha hang.
     * 
     * @param restaurantId ID cua nha hang
     * @param revenue      Tong doanh thu
     * @param tier         Hang muc: "standardCommission", "premiumCommission",
     *                     "freeTrialCommission"
     */
    public BigDecimal calculateCommission(Long restaurantId, BigDecimal revenue, String tier)
            throws IdInvalidException {
        Restaurant restaurant = getRestaurantById(restaurantId);
        if (restaurant == null) {
            throw new IdInvalidException("Restaurant not found with id: " + restaurantId);
        }

        CommissionStrategy strategy = strategyMap.get(tier);
        if (strategy == null) {
            throw new IdInvalidException("Unknown commission tier: " + tier
                    + ". Available: " + strategyMap.keySet());
        }

        BigDecimal commission = strategy.calculateCommission(revenue);
        log.info("💰 [STRATEGY: {}] Restaurant: {} | Revenue: {} | Commission: {}",
                strategy.getStrategyName(), restaurant.getName(), revenue, commission);
        return commission;
    }

    /**
     * ★ ADAPTER PATTERN (#5): Tinh khoang cach tu nguoi dung toi nha hang.
     */
    public BigDecimal calculateDistanceToRestaurant(Long restaurantId,
            BigDecimal userLat, BigDecimal userLng) throws IdInvalidException {
        Restaurant restaurant = getRestaurantById(restaurantId);
        if (restaurant == null) {
            throw new IdInvalidException("Restaurant not found with id: " + restaurantId);
        }
        if (restaurant.getLatitude() == null || restaurant.getLongitude() == null) {
            throw new IdInvalidException("Restaurant does not have location data");
        }

        // Adapter Pattern: Service chi goi locationService.calculateDistance()
        // ma KHONG CAN BIET phia sau dang xai Google Maps hay Mapbox
        return locationService.calculateDistance(
                userLat, userLng,
                restaurant.getLatitude(), restaurant.getLongitude());
    }

    // ==================== HELPER ====================

}
