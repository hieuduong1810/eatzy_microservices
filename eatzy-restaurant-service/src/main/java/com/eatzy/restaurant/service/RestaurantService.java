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
import com.eatzy.common.service.MapboxService;
import com.eatzy.restaurant.designpattern.observer.RestaurantApprovedEvent;
import com.eatzy.restaurant.designpattern.strategy.commission.CommissionStrategy;
import com.eatzy.restaurant.designpattern.strategy.ranking.GuestRankingStrategy;
import com.eatzy.restaurant.designpattern.strategy.ranking.PersonalizedRankingStrategy;
import com.eatzy.restaurant.domain.Restaurant;
import com.eatzy.restaurant.dto.res.ResRestaurantDTO;
import com.eatzy.restaurant.dto.res.ResRestaurantMagazineDTO;
import com.eatzy.restaurant.client.InteractionServiceClient;
import com.eatzy.restaurant.client.dto.BatchScoreRequestDTO;
import com.eatzy.restaurant.client.dto.BatchScoreResponseDTO;
import com.eatzy.common.util.SecurityUtils;
import com.eatzy.restaurant.mapper.RestaurantMapper;
import com.eatzy.restaurant.repository.RestaurantRepository;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final ApplicationEventPublisher eventPublisher; // Observer Pattern
    private final MapboxService mapboxService;
    private final Map<String, CommissionStrategy> strategyMap; // Strategy Pattern
    private final RestaurantMapper restaurantMapper;
    private final GuestRankingStrategy guestRankingStrategy;
    private final PersonalizedRankingStrategy personalizedRankingStrategy;
    private final InteractionServiceClient interactionServiceClient;

    public RestaurantService(RestaurantRepository restaurantRepository,
            ApplicationEventPublisher eventPublisher,
            MapboxService mapboxService,
            Map<String, CommissionStrategy> strategyMap,
            RestaurantMapper restaurantMapper,
            GuestRankingStrategy guestRankingStrategy,
            PersonalizedRankingStrategy personalizedRankingStrategy,
            InteractionServiceClient interactionServiceClient) {
        this.restaurantRepository = restaurantRepository;
        this.eventPublisher = eventPublisher;
        this.mapboxService = mapboxService;
        this.strategyMap = strategyMap;
        this.restaurantMapper = restaurantMapper;
        this.guestRankingStrategy = guestRankingStrategy;
        this.personalizedRankingStrategy = personalizedRankingStrategy;
        this.interactionServiceClient = interactionServiceClient;
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

        // Service truc tiep goi mapboxService tu common
        return mapboxService.getDrivingDistance(
                userLat, userLng,
                restaurant.getLatitude(), restaurant.getLongitude());
    }

    public ResultPaginationDTO getNearbyRestaurants(BigDecimal lat, BigDecimal lng, String keyword,
            Specification<Restaurant> spec, Pageable pageable) {

        // 0. Xay dung Specification cho keyword (tim kiem theo ten nha hang, mon an hoac danh muc)
        Specification<Restaurant> finalSpec = spec;
        if (keyword != null && !keyword.trim().isEmpty()) {
            String searchKeyword = "%" + keyword.trim().toLowerCase() + "%";
            Specification<Restaurant> searchSpec = (root, query, criteriaBuilder) -> {
                var namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchKeyword);
                
                var dishSubquery = query.subquery(Long.class);
                var dishRoot = dishSubquery.from(Restaurant.class);
                var dishCategoryJoin = dishRoot.join("dishCategories");
                var dishJoin = dishCategoryJoin.join("dishes");
                dishSubquery.select(dishRoot.get("id"))
                        .where(
                                criteriaBuilder.and(
                                        criteriaBuilder.equal(dishRoot.get("id"), root.get("id")),
                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(dishJoin.get("name")), searchKeyword)));
                var dishPredicate = criteriaBuilder.exists(dishSubquery);

                var categorySubquery = query.subquery(Long.class);
                var categoryRoot = categorySubquery.from(Restaurant.class);
                var categoryJoin = categoryRoot.join("dishCategories");
                categorySubquery.select(categoryRoot.get("id"))
                        .where(
                                criteriaBuilder.and(
                                        criteriaBuilder.equal(categoryRoot.get("id"), root.get("id")),
                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(categoryJoin.get("name")), searchKeyword)));
                var categoryPredicate = criteriaBuilder.exists(categorySubquery);

                return criteriaBuilder.or(namePredicate, dishPredicate, categoryPredicate);
            };
            finalSpec = finalSpec != null ? finalSpec.and(searchSpec) : searchSpec;
        }

        // Lay tat ca nha hang de filter va sap xep trong bo nho (giong monolith)
        List<Restaurant> restaurants = finalSpec != null ? restaurantRepository.findAll(finalSpec) : restaurantRepository.findAll();

        // Filter nha hang dang hoat dong
        restaurants = restaurants.stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()) || "OPEN".equals(r.getStatus()))
                .collect(Collectors.toList());

        // 1. Get current userId
        Long userId = null;
        try {
            userId = SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            log.trace("User unauthenticated for nearby restaurants search");
        }

        // 2. Ap dung Design Pattern: Strategy Pattern cho Ranking mac dinh
        List<ResRestaurantMagazineDTO> rankedList;
        if (userId != null) {
            rankedList = personalizedRankingStrategy.calculateAndSort(restaurants, lat, lng, userId);
        } else {
            rankedList = guestRankingStrategy.calculateAndSort(restaurants, lat, lng, null);
        }

        // 2.1. Neu user co truyen param sort (vi du: sort=averageRating,desc), ta se de de sap xep
        if (pageable.getSort().isSorted()) {
            java.util.Comparator<ResRestaurantMagazineDTO> customComparator = null;
            for (org.springframework.data.domain.Sort.Order order : pageable.getSort()) {
                java.util.Comparator<ResRestaurantMagazineDTO> propertyComparator = null;
                switch (order.getProperty()) {
                    case "distance":
                        propertyComparator = java.util.Comparator.comparing(ResRestaurantMagazineDTO::getDistance, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
                        break;
                    case "averageRating":
                        propertyComparator = java.util.Comparator.comparing(ResRestaurantMagazineDTO::getAverageRating, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
                        break;
                    case "finalScore":
                        propertyComparator = java.util.Comparator.comparing(ResRestaurantMagazineDTO::getFinalScore, java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder()));
                        break;
                    case "name":
                        propertyComparator = java.util.Comparator.comparing(ResRestaurantMagazineDTO::getName, String.CASE_INSENSITIVE_ORDER);
                        break;
                }
                
                if (propertyComparator != null) {
                    if (order.isDescending()) {
                        propertyComparator = propertyComparator.reversed();
                    }
                    if (customComparator == null) {
                        customComparator = propertyComparator;
                    } else {
                        customComparator = customComparator.thenComparing(propertyComparator);
                    }
                }
            }
            if (customComparator != null) {
                rankedList.sort(customComparator);
            }
        }

        // 3. SubList Pagination trong Memory
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), rankedList.size());

        List<ResRestaurantMagazineDTO> pagedList = new ArrayList<>();
        if (start < rankedList.size()) {
            pagedList = rankedList.subList(start, end);
        }

        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal((long) rankedList.size());
        meta.setPages((int) Math.ceil((double) rankedList.size() / pageable.getPageSize()));
        result.setMeta(meta);
        result.setResult(pagedList);
        return result;
    }

    public ResRestaurantDTO getRestaurantDTOBySlug(String slug) throws IdInvalidException {
        Restaurant restaurant = restaurantRepository.findBySlug(slug)
                .orElseThrow(() -> new IdInvalidException("Restaurant not found by slug: " + slug));
        return restaurantMapper.convertToDTO(restaurant);
    }

    public void openCurrentRestaurant(Long ownerId) throws IdInvalidException {
        Restaurant restaurant = restaurantRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new IdInvalidException("Owner does not have a restaurant: " + ownerId));
        restaurant.setStatus("OPEN");
        restaurantRepository.save(restaurant);
    }

    public void closeCurrentRestaurant(Long ownerId) throws IdInvalidException {
        Restaurant restaurant = restaurantRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new IdInvalidException("Owner does not have a restaurant: " + ownerId));
        restaurant.setStatus("CLOSED");
        restaurantRepository.save(restaurant);
    }

    // ==================== HELPER ====================

}
