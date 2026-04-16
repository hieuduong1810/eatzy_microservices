package com.eatzy.order.service;

import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.common.event.OrderCreatedEvent;
import com.eatzy.common.event.OrderStatusChangedEvent;
import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.order.designpattern.adapter.AuthServiceClient;
import com.eatzy.order.designpattern.adapter.PaymentServiceClient;
import com.eatzy.order.designpattern.adapter.PaymentServiceClient.CalculateDiscountReq;
import com.eatzy.order.designpattern.adapter.RestaurantServiceClient;
import com.eatzy.order.designpattern.adapter.SystemConfigServiceClient;

import com.eatzy.common.service.MapboxService;
import com.eatzy.common.service.RedisGeoService;
import com.eatzy.order.designpattern.state.OrderStateMachine;
import com.eatzy.order.designpattern.state.OrderStatus;
import com.eatzy.order.designpattern.template.DefaultDeliveryFeeCalculator;
import com.eatzy.order.domain.Order;
import com.eatzy.order.domain.OrderItem;
import com.eatzy.order.domain.OrderItemOption;
import com.eatzy.order.dto.request.ReqOrderDTO;
import com.eatzy.order.dto.response.ResDeliveryFeeDTO;
import com.eatzy.order.dto.response.ResOrderDTO;
import com.eatzy.order.kafka.OrderEventProducer;
import com.eatzy.order.mapper.OrderMapper;
import com.eatzy.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core Order Service - Uses:
 * - State Pattern for status transitions
 * - Strategy Pattern for delivery fee calculation
 * - Observer/Pub-Sub for publishing Kafka events
 * - Adapter Pattern for calling external services
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderEventProducer orderEventProducer;
    private final DefaultDeliveryFeeCalculator deliveryFeeCalculator;
    private final MapboxService mapboxService;
    private final DynamicPricingService dynamicPricingService;
    private final AuthServiceClient authServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final SystemConfigServiceClient systemConfigServiceClient;
    private final RedisGeoService redisGeoService;
    private final RedisRejectionService redisRejectionService;
    private final OrderEarningsSummaryService orderEarningsSummaryService;

    public OrderService(OrderRepository orderRepository,
            OrderMapper orderMapper,
            OrderEventProducer orderEventProducer,
            DefaultDeliveryFeeCalculator deliveryFeeCalculator,
            MapboxService mapboxService,
            DynamicPricingService dynamicPricingService,
            AuthServiceClient authServiceClient,
            RestaurantServiceClient restaurantServiceClient,
            PaymentServiceClient paymentServiceClient,
            SystemConfigServiceClient systemConfigServiceClient,
            RedisRejectionService redisRejectionService,
            RedisGeoService redisGeoService,
            OrderEarningsSummaryService orderEarningsSummaryService) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.orderEventProducer = orderEventProducer;
        this.deliveryFeeCalculator = deliveryFeeCalculator;
        this.mapboxService = mapboxService;
        this.dynamicPricingService = dynamicPricingService;
        this.authServiceClient = authServiceClient;
        this.restaurantServiceClient = restaurantServiceClient;
        this.paymentServiceClient = paymentServiceClient;
        this.systemConfigServiceClient = systemConfigServiceClient;
        this.redisRejectionService = redisRejectionService;
        this.redisGeoService = redisGeoService;
        this.orderEarningsSummaryService = orderEarningsSummaryService;
    }

    // ==================== QUERY METHODS ====================

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    public ResOrderDTO getOrderDTOById(Long id) {
        Order order = getOrderById(id);
        return order != null ? orderMapper.toResOrderDTO(order) : null;
    }

    public ResultPaginationDTO getAllOrdersDTO(Specification<Order> spec, Pageable pageable) {
        Page<Order> page = orderRepository.findAll(spec, pageable);
        return buildPaginationResult(page, pageable);
    }

    public ResultPaginationDTO getOrdersDTOByRestaurantIdWithSpec(Long restaurantId, Specification<Order> spec,
            Pageable pageable) {
        Specification<Order> baseSpec = (root, query, cb) -> cb.equal(root.get("restaurantId"), restaurantId);
        Specification<Order> combinedSpec = spec != null ? baseSpec.and(spec) : baseSpec;
        Page<Order> page = orderRepository.findAll(combinedSpec, pageable);
        return buildPaginationResult(page, pageable);
    }

    public ResultPaginationDTO getOrdersDTOByCustomerIdWithSpec(Long customerId, Specification<Order> spec,
            Pageable pageable) {
        Specification<Order> baseSpec = (root, query, cb) -> cb.equal(root.get("customerId"), customerId);
        Specification<Order> combinedSpec = spec != null ? baseSpec.and(spec) : baseSpec;
        Page<Order> page = orderRepository.findAll(combinedSpec, pageable);
        return buildPaginationResult(page, pageable);
    }

    public ResultPaginationDTO getOrdersDTOByDriverIdWithSpec(Long driverId, Specification<Order> spec,
            Pageable pageable) {
        Specification<Order> baseSpec = (root, query, cb) -> cb.equal(root.get("driverId"), driverId);
        Specification<Order> combinedSpec = spec != null ? baseSpec.and(spec) : baseSpec;
        Page<Order> page = orderRepository.findAll(combinedSpec, pageable);
        return buildPaginationResult(page, pageable);
    }

    public List<ResOrderDTO> getOrdersDTOByRestaurantId(Long restaurantId) {
        return orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId).stream()
                .map(orderMapper::toResOrderDTO).collect(Collectors.toList());
    }

    public List<ResOrderDTO> getOrdersDTOByCustomerId(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(orderMapper::toResOrderDTO).collect(Collectors.toList());
    }

    public List<ResOrderDTO> getOrdersDTOByDriverId(Long driverId) {
        return orderRepository.findByDriverIdOrderByCreatedAtDesc(driverId).stream()
                .map(orderMapper::toResOrderDTO).collect(Collectors.toList());
    }

    public ResOrderDTO getActiveOrderDTOByDriverId(Long driverId) {
        Order order = orderRepository.findFirstByDriverIdAndOrderStatusIn(driverId, Arrays.asList("DRIVER_ASSIGNED", "READY", "PICKED_UP", "ARRIVED"));
        return order != null ? orderMapper.toResOrderDTO(order) : null;
    }

    public List<ResOrderDTO> getOrdersDTOByRestaurantIdAndStatus(Long restaurantId, String status) {
        return orderRepository.findByRestaurantIdAndOrderStatus(restaurantId, status).stream()
                .map(orderMapper::toResOrderDTO).collect(Collectors.toList());
    }

    public ResDeliveryFeeDTO getDeliveryFee(Long restaurantId, BigDecimal deliveryLatitude,
            BigDecimal deliveryLongitude) throws IdInvalidException {
        // Get restaurant info via Adapter
        Map<String, Object> restData = restaurantServiceClient.getRestaurantById(restaurantId);
        if (restData == null) {
            throw new IdInvalidException("Restaurant not found with id: " + restaurantId);
        }

        BigDecimal restLat = getBigDecimalValue(restData, "latitude");
        BigDecimal restLng = getBigDecimalValue(restData, "longitude");
        if (restLat == null || restLng == null) {
            throw new IdInvalidException("Restaurant location is required to calculate delivery fee");
        }

        // Fetch configs
        BigDecimal baseFee = getSystemConfigValue("DELIVERY_BASE_FEE");
        BigDecimal baseDistance = getSystemConfigValue("DELIVERY_BASE_DISTANCE");
        BigDecimal perKmFee = getSystemConfigValue("DELIVERY_PER_KM_FEE");
        BigDecimal minFee = getSystemConfigValue("DELIVERY_MIN_FEE");

        // Get distance via Mapbox
        BigDecimal distance = mapboxService.getDrivingDistance(restLat, restLng, deliveryLatitude, deliveryLongitude);

        if (distance == null) {
            log.warn("Failed to get driving distance from Mapbox, using base fee");
            return new ResDeliveryFeeDTO(baseFee, null, BigDecimal.ONE, baseFee, baseDistance, perKmFee);
        }

        // Get surge multiplier from dynamic pricing service
        BigDecimal surgeMultiplier = dynamicPricingService.getSurgeMultiplier(restLat, restLng);

        // Calculate delivery fee using Template Method
        BigDecimal deliveryFee = deliveryFeeCalculator.calculate(
                distance, baseFee, baseDistance, perKmFee, surgeMultiplier, minFee);

        ResDeliveryFeeDTO response = new ResDeliveryFeeDTO();
        response.setDeliveryFee(deliveryFee.setScale(2, RoundingMode.HALF_UP));
        response.setDistance(distance.setScale(2, RoundingMode.HALF_UP));
        response.setSurgeMultiplier(surgeMultiplier.setScale(2, RoundingMode.HALF_UP));
        response.setBaseFee(baseFee);
        response.setBaseDistance(baseDistance);
        response.setPerKmFee(perKmFee);
        return response;
    }

    // ==================== CREATE ORDER (Facade-like orchestration)
    // ====================

    @Transactional(rollbackFor = Exception.class)
    public ResOrderDTO createOrderFromReqDTO(ReqOrderDTO reqOrderDTO, String clientIp, String baseUrl)
            throws IdInvalidException {
        // 1. Validate customer via REST (Adapter Pattern)
        if (reqOrderDTO.getCustomer() == null || reqOrderDTO.getCustomer().getId() == null) {
            throw new IdInvalidException("Customer is required");
        }
        Map<String, Object> customerData = authServiceClient.getUserById(reqOrderDTO.getCustomer().getId());
        if (customerData == null) {
            throw new IdInvalidException("Customer not found with id: " + reqOrderDTO.getCustomer().getId());
        }

        // 2. Validate restaurant via REST (Adapter Pattern)
        if (reqOrderDTO.getRestaurant() == null || reqOrderDTO.getRestaurant().getId() == null) {
            throw new IdInvalidException("Restaurant is required");
        }
        Map<String, Object> restData = restaurantServiceClient.getRestaurantById(reqOrderDTO.getRestaurant().getId());
        if (restData == null) {
            throw new IdInvalidException("Restaurant not found with id: " + reqOrderDTO.getRestaurant().getId());
        }

        // 3. Build Order entity (Builder Pattern)
        Order order = Order.builder()
                .customerId(reqOrderDTO.getCustomer().getId())
                .restaurantId(reqOrderDTO.getRestaurant().getId())
                .orderStatus(OrderStatus.PENDING.name())
                .deliveryAddress(reqOrderDTO.getDeliveryAddress())
                .deliveryLatitude(reqOrderDTO.getDeliveryLatitude() != null
                        ? BigDecimal.valueOf(reqOrderDTO.getDeliveryLatitude())
                        : null)
                .deliveryLongitude(reqOrderDTO.getDeliveryLongitude() != null
                        ? BigDecimal.valueOf(reqOrderDTO.getDeliveryLongitude())
                        : null)
                .specialInstructions(reqOrderDTO.getSpecialInstructions())
                .paymentMethod(reqOrderDTO.getPaymentMethod())
                .paymentStatus(reqOrderDTO.getPaymentStatus() != null ? reqOrderDTO.getPaymentStatus() : "UNPAID")
                .createdAt(Instant.now())
                .build();

        // Set driver if provided
        if (reqOrderDTO.getDriver() != null && reqOrderDTO.getDriver().getId() != null) {
            order.setDriverId(reqOrderDTO.getDriver().getId());
        }

        // Set voucher IDs
        if (reqOrderDTO.getVouchers() != null && !reqOrderDTO.getVouchers().isEmpty()) {
            List<Long> voucherIds = reqOrderDTO.getVouchers().stream()
                    .filter(v -> v.getId() != null)
                    .map(ReqOrderDTO.Voucher::getId)
                    .collect(Collectors.toList());
            order.setVoucherIds(voucherIds);
        }

        // 4. Calculate delivery fee using real driving distance & dynamic pricing
        BigDecimal baseFee = getSystemConfigValue("DELIVERY_BASE_FEE");
        BigDecimal baseDistance = getSystemConfigValue("DELIVERY_BASE_DISTANCE");
        BigDecimal perKmFee = getSystemConfigValue("DELIVERY_PER_KM_FEE");
        BigDecimal minFee = getSystemConfigValue("DELIVERY_MIN_FEE");

        BigDecimal restLat = getBigDecimalValue(restData, "latitude");
        BigDecimal restLng = getBigDecimalValue(restData, "longitude");
        BigDecimal deliveryFee = baseFee;

        if (restLat != null && restLng != null && order.getDeliveryLatitude() != null
                && order.getDeliveryLongitude() != null) {

            BigDecimal distance = mapboxService.getDrivingDistance(restLat, restLng,
                    order.getDeliveryLatitude(), order.getDeliveryLongitude());

            if (distance != null) {
                BigDecimal surgeMultiplier = dynamicPricingService.getSurgeMultiplier(restLat, restLng);
                deliveryFee = deliveryFeeCalculator.calculate(distance, baseFee, baseDistance, perKmFee,
                        surgeMultiplier, minFee);
            }
        }

        // Validate delivery fee
        if (reqOrderDTO.getDeliveryFee() != null) {
            BigDecimal clientFee = reqOrderDTO.getDeliveryFee().setScale(0, RoundingMode.HALF_UP);
            BigDecimal serverFee = deliveryFee.setScale(0, RoundingMode.HALF_UP);
            if (clientFee.compareTo(serverFee) != 0) {
                throw new IdInvalidException("Phí giao hàng đã thay đổi. Vui lòng tải lại trang. (Giá cũ: " + clientFee
                        + " VND, Giá mới: " + serverFee + " VND)");
            }
        }
        order.setDeliveryFee(deliveryFee);

        // 5. Save order first
        Order savedOrder = orderRepository.save(order);

        // 6. Create order items
        BigDecimal subtotal = BigDecimal.ZERO;
        if (reqOrderDTO.getOrderItems() != null && !reqOrderDTO.getOrderItems().isEmpty()) {
            List<OrderItem> orderItems = new ArrayList<>();
            for (ReqOrderDTO.OrderItem reqItem : reqOrderDTO.getOrderItems()) {
                if (reqItem.getDish() == null || reqItem.getDish().getId() == null) {
                    throw new IdInvalidException("Dish is required for order item");
                }
                // Validate dish via REST
                Map<String, Object> dishData = restaurantServiceClient.getDishById(reqItem.getDish().getId());
                if (dishData == null) {
                    throw new IdInvalidException("Dish not found with id: " + reqItem.getDish().getId());
                }
                if (reqItem.getQuantity() == null || reqItem.getQuantity() <= 0) {
                    throw new IdInvalidException("Quantity must be greater than 0");
                }

                BigDecimal dishPrice = getBigDecimalValue(dishData, "price");
                BigDecimal itemPrice = dishPrice.multiply(new BigDecimal(reqItem.getQuantity()));

                OrderItem orderItem = OrderItem.builder()
                        .order(savedOrder)
                        .dishId(reqItem.getDish().getId())
                        .quantity(reqItem.getQuantity())
                        .build();

                // Process options
                if (reqItem.getOrderItemOptions() != null && !reqItem.getOrderItemOptions().isEmpty()) {
                    List<OrderItemOption> itemOptions = new ArrayList<>();
                    for (ReqOrderDTO.OrderItem.OrderItemOption reqOption : reqItem.getOrderItemOptions()) {
                        if (reqOption.getMenuOption() == null || reqOption.getMenuOption().getId() == null) {
                            throw new IdInvalidException("Menu option is required");
                        }
                        Map<String, Object> menuOptionData = restaurantServiceClient
                                .getMenuOptionById(reqOption.getMenuOption().getId());
                        if (menuOptionData == null) {
                            throw new IdInvalidException(
                                    "Menu option not found with id: " + reqOption.getMenuOption().getId());
                        }

                        BigDecimal optionPrice = getBigDecimalValue(menuOptionData, "priceAdjustment");
                        String optionName = getStringValue(menuOptionData, "name");

                        OrderItemOption itemOption = OrderItemOption.builder()
                                .orderItem(orderItem)
                                .menuOptionId(reqOption.getMenuOption().getId())
                                .optionName(optionName)
                                .priceAtPurchase(optionPrice)
                                .build();
                        itemOptions.add(itemOption);
                        itemPrice = itemPrice.add(optionPrice.multiply(new BigDecimal(reqItem.getQuantity())));
                    }
                    orderItem.setOrderItemOptions(itemOptions);
                }

                orderItem.setPriceAtPurchase(itemPrice);
                subtotal = subtotal.add(itemPrice);
                orderItems.add(orderItem);
            }
            savedOrder.setOrderItems(orderItems);
        }

        // 7. Set final amounts
        savedOrder.setSubtotal(subtotal);

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (savedOrder.getVoucherIds() != null && !savedOrder.getVoucherIds().isEmpty()) {
            try {
                CalculateDiscountReq req = new CalculateDiscountReq(
                        savedOrder.getVoucherIds(),
                        subtotal,
                        savedOrder.getRestaurantId(),
                        savedOrder.getCustomerId(),
                        deliveryFee);
                discountAmount = paymentServiceClient.calculateVoucherDiscount(req);
            } catch (Exception e) {
                // Ignore error, just keep discount 0
            }
        }

        savedOrder.setDiscountAmount(discountAmount);

        savedOrder.setTotalAmount(subtotal.add(deliveryFee).subtract(discountAmount));

        savedOrder = orderRepository.save(savedOrder);

        // 8. Publish Kafka event (Observer/Pub-Sub Pattern)
        orderEventProducer.publishOrderCreated(new OrderCreatedEvent(
                savedOrder.getId(), savedOrder.getCustomerId(), savedOrder.getRestaurantId(),
                savedOrder.getTotalAmount(), savedOrder.getOrderStatus()));

        // 9. Track user scoring for placing order (Interaction Service)
        orderEventProducer.publishTrackPlaceOrder(savedOrder.getCustomerId(), savedOrder.getRestaurantId());

        // 10. Process payment based on payment method via Adapter Pattern
        ResOrderDTO orderDTO = orderMapper.toResOrderDTO(savedOrder);
        try {
            PaymentServiceClient.ReqPaymentInitiateDTO paymentReq = new PaymentServiceClient.ReqPaymentInitiateDTO(
                    savedOrder.getId(),
                    savedOrder.getCustomerId(),
                    savedOrder.getTotalAmount(),
                    savedOrder.getPaymentMethod(),
                    clientIp,
                    baseUrl,
                    savedOrder.getDriverId());

            Map<String, Object> paymentResult = paymentServiceClient.initiatePayment(paymentReq);

            if (paymentResult != null) {
                if (paymentResult.containsKey("paymentUrl")) {
                    orderDTO.setVnpayPaymentUrl((String) paymentResult.get("paymentUrl"));
                }
                if (paymentResult.containsKey("status")) {
                    String newStatus = (String) paymentResult.get("status");
                    if (!savedOrder.getPaymentStatus().equals(newStatus)) {
                        savedOrder.setPaymentStatus(newStatus);
                        orderRepository.save(savedOrder);
                        orderDTO.setPaymentStatus(newStatus);
                    }
                }

                // If payment was WALLET and it failed (success = false), throw exception to
                // rollback
                if (paymentResult.containsKey("success") && !(Boolean) paymentResult.get("success")) {
                    throw new IdInvalidException(
                            paymentResult.get("message") != null ? (String) paymentResult.get("message")
                                    : "Wallet payment failed");
                }
            }
        } catch (IdInvalidException e) {
            throw e; // rethrow to trigger rollback
        } catch (Exception e) {
            log.error("Failed to initiate payment: {}", e.getMessage());
            // Throw an exception to rollback the order instead of swallowing it
            throw new IdInvalidException("Failed to initiate payment: " + e.getMessage());
        }

        return orderDTO;
    }

    // ==================== STATUS TRANSITION METHODS (State Pattern)
    // ====================

    /**
     * Restaurant accepts order → status becomes PREPARING → automatically assign
     * driver.
     * Matches eatzy_backend: acceptOrderByRestaurant calls assignDriver after
     * accepting.
     */
    @Transactional
    public ResOrderDTO acceptOrderByRestaurant(Long orderId) throws IdInvalidException {
        Order order = getOrderById(orderId);
        if (order == null)
            throw new IdInvalidException("Order not found with id: " + orderId);

        String previousStatus = order.getOrderStatus();
        OrderStateMachine.transition(previousStatus, OrderStatus.PREPARING.name());

        order.setOrderStatus(OrderStatus.PREPARING.name());
        order.setPreparingAt(Instant.now());
        order = orderRepository.save(order);

        publishStatusChanged(order, previousStatus, "Đơn hàng đã được chấp nhận và đang được chuẩn bị");

        // Automatically assign driver after restaurant accepts (matches eatzy_backend
        // logic)
        try {
            assignDriver(orderId);
        } catch (Exception e) {
            log.warn("Failed to assign driver immediately for order {}: {}", orderId, e.getMessage());
            // Don't fail the accept — driver will be assigned later by cleanup service
        }

        return orderMapper.toResOrderDTO(order);
    }

    /**
     * Assign the closest available driver to an order using Redis GEO + SQL
     * validation + Mapbox.
     * Matches eatzy_backend 3-step logic:
     * 1. Find nearby drivers via Redis GEO (fast spatial search)
     * 2. Query SQL to validate business rules (COD limit, status)
     * 3. Find closest driver using Mapbox API for real driving distance
     * 4. Set driver status to UNAVAILABLE
     */
    @Transactional
    public ResOrderDTO assignDriver(Long orderId) throws IdInvalidException {
        Order order = getOrderById(orderId);
        if (order == null)
            throw new IdInvalidException("Order not found with id: " + orderId);

        // Get restaurant location for finding nearby drivers
        Map<String, Object> restData = restaurantServiceClient.getRestaurantById(order.getRestaurantId());
        if (restData == null)
            throw new IdInvalidException("Restaurant not found");

        BigDecimal restLat = getBigDecimalValue(restData, "latitude");
        BigDecimal restLng = getBigDecimalValue(restData, "longitude");
        if (restLat == null || restLng == null)
            throw new IdInvalidException("Restaurant location is required");

        // STEP 1: Find nearby drivers via Redis GEO (matches eatzy_backend)
        BigDecimal searchRadius = getSystemConfigValue("DRIVER_SEARCH_RADIUS_KM");
        log.info("🔍 Step 1: Searching drivers using Redis GEO within {} km of restaurant", searchRadius);

        GeoResults<GeoLocation<Object>> geoResults = redisGeoService.findNearbyDrivers(
                restLat, restLng, searchRadius.doubleValue(), 50);

        if (geoResults == null || geoResults.getContent().isEmpty())
            throw new IdInvalidException("No drivers found within " + searchRadius + " km radius");

        List<Map<String, Object>> nearbyDrivers = convertGeoResultsToDriverList(geoResults);
        log.info("📍 Found {} drivers in Redis GEO within radius", nearbyDrivers.size());

        // STEP 2: Query SQL to validate business rules (COD limit, status)
        List<Long> nearbyDriverIds = nearbyDrivers.stream()
                .map(d -> getLongValue(d, "userId"))
                .filter(id -> id != null)
                .collect(Collectors.toList());

        BigDecimal minCodLimit = "COD".equals(order.getPaymentMethod()) ? order.getTotalAmount() : null;
        if (minCodLimit != null) {
            log.info("💰 Step 2: Validating COD limit >= {} for {} drivers", minCodLimit, nearbyDriverIds.size());
        } else {
            log.info("💳 Step 2: Validating online payment readiness for {} drivers", nearbyDriverIds.size());
        }

        List<Long> validDriverIds = authServiceClient.validateDriversByIds(nearbyDriverIds, minCodLimit);

        if (validDriverIds.isEmpty())
            throw new IdInvalidException("No qualified drivers found (failed business rules validation)");

        log.info("✅ {} drivers passed status/COD validation", validDriverIds.size());

        // STEP 2b: Check wallet balance > 0 via payment-service (matches eatzy_backend)
        try {
            List<Long> driversWithBalance = paymentServiceClient.validateDriverWalletBalances(validDriverIds);
            validDriverIds = driversWithBalance;
            log.info("💰 {} drivers have positive wallet balance", validDriverIds.size());
        } catch (Exception e) {
            log.warn("Failed to validate wallet balances, skipping balance check: {}", e.getMessage());
        }

        if (validDriverIds.isEmpty())
            throw new IdInvalidException("No qualified drivers found (all drivers have zero wallet balance)");

        // Filter nearbyDrivers to only include validated ones
        final List<Long> finalValidDriverIds = validDriverIds;
        List<Map<String, Object>> candidateDrivers = nearbyDrivers.stream()
                .filter(d -> {
                    Long id = getLongValue(d, "userId");
                    return id != null && finalValidDriverIds.contains(id);
                })
                .collect(Collectors.toList());

        // STEP 3: Find closest driver using Mapbox API for real driving distance
        log.info("🚗 Step 3: Calculating real driving distances using Mapbox API");
        Long closestDriverId = null;
        BigDecimal shortestDistance = null;

        for (Map<String, Object> driverData : candidateDrivers) {
            Long driverId = getLongValue(driverData, "userId");
            BigDecimal driverLat = getBigDecimalValue(driverData, "latitude");
            BigDecimal driverLng = getBigDecimalValue(driverData, "longitude");

            if (driverId == null || driverLat == null || driverLng == null)
                continue;

            BigDecimal drivingDistance = mapboxService.getDrivingDistance(
                    driverLat, driverLng, restLat, restLng);

            if (drivingDistance == null) {
                log.warn("Failed to get driving distance from Mapbox for driver {}", driverId);
                continue;
            }

            if (shortestDistance == null || drivingDistance.compareTo(shortestDistance) < 0) {
                shortestDistance = drivingDistance;
                closestDriverId = driverId;
            }
        }

        if (closestDriverId == null) {
            throw new IdInvalidException("Failed to calculate driving distance to available drivers");
        }

        log.info("🎯 Assigned driver {} (distance: {} km) to order {}", closestDriverId, shortestDistance, orderId);

        order.setDriverId(closestDriverId);
        order.setAssignedAt(Instant.now());
        order = orderRepository.save(order);

        // Update driver status to UNAVAILABLE (also removes from Redis GEO)
        try {
            authServiceClient.updateDriverStatus(closestDriverId, "UNAVAILABLE");
            log.info("🔴 Set driver {} status to UNAVAILABLE after assignment", closestDriverId);
        } catch (Exception e) {
            log.error("Failed to update driver {} status to UNAVAILABLE: {}", closestDriverId, e.getMessage());
        }

        publishStatusChanged(order, order.getOrderStatus(), "Tài xế đã được phân công");
        return orderMapper.toResOrderDTO(order);
    }

    /**
     * Restaurant rejects order.
     * Matches eatzy_backend: includes refund logic for already-paid orders
     * (WALLET/VNPAY).
     */
    @Transactional
    public ResOrderDTO rejectOrderByRestaurant(Long orderId, String cancellationReason) throws IdInvalidException {
        Order order = getOrderById(orderId);
        if (order == null)
            throw new IdInvalidException("Order not found with id: " + orderId);

        String previousStatus = order.getOrderStatus();
        OrderStateMachine.transition(previousStatus, OrderStatus.REJECTED.name());

        order.setOrderStatus(OrderStatus.REJECTED.name());
        order.setCancellationReason(cancellationReason);

        // If payment was already made (WALLET or VNPAY), process refund (matches
        // eatzy_backend)
        processRefundIfNeeded(order);

        order = orderRepository.save(order);

        publishStatusChanged(order, previousStatus, "Đơn hàng đã bị từ chối bởi nhà hàng");
        return orderMapper.toResOrderDTO(order);
    }

    /**
     * Customer cancels order.
     * Matches eatzy_backend: includes refund logic for already-paid orders
     * (WALLET/VNPAY).
     */
    @Transactional
    public ResOrderDTO cancelOrder(Long orderId, String cancellationReason) throws IdInvalidException {
        Order order = getOrderById(orderId);
        if (order == null)
            throw new IdInvalidException("Order not found with id: " + orderId);

        String previousStatus = order.getOrderStatus();
        OrderStateMachine.validateCancellation(previousStatus);

        order.setOrderStatus(OrderStatus.REJECTED.name());
        order.setCancellationReason(cancellationReason);

        // If payment was already made (WALLET or VNPAY), process refund
        processRefundIfNeeded(order);

        order = orderRepository.save(order);
        publishStatusChanged(order, previousStatus, "Đơn hàng đã bị hủy");
        return orderMapper.toResOrderDTO(order);
    }

    @Transactional
    public ResOrderDTO markOrderAsReady(Long orderId) throws IdInvalidException {
        Order order = getOrderById(orderId);
        if (order == null)
            throw new IdInvalidException("Order not found with id: " + orderId);

        String previousStatus = order.getOrderStatus();
        OrderStateMachine.transition(previousStatus, OrderStatus.READY.name());

        order.setOrderStatus(OrderStatus.READY.name());
        order = orderRepository.save(order);

        publishStatusChanged(order, previousStatus, "Đơn hàng đã sẵn sàng");
        return orderMapper.toResOrderDTO(order);
    }

    /**
     * Driver accepts order (internal method used by both manual accept and
     * auto-accept).
     * Matches eatzy_backend: internalAcceptOrderByDriver
     * - If COD payment: call processCODPaymentOnDelivery
     * - Set status to DRIVER_ASSIGNED
     * - Clear assignedAt
     */
    @Transactional
    public ResOrderDTO acceptOrderByDriver(Long orderId, Long driverId) throws IdInvalidException {
        Order order = getOrderById(orderId);
        if (order == null)
            throw new IdInvalidException("Order not found with id: " + orderId);

        if (order.getDriverId() == null || !order.getDriverId().equals(driverId)) {
            throw new IdInvalidException("This order is not assigned to driver " + driverId);
        }

        // If COD payment, process COD payment on delivery (matches eatzy_backend)
        if ("COD".equals(order.getPaymentMethod())) {
            try {
                Map<String, Object> paymentResult = paymentServiceClient.processCODPaymentOnDelivery(
                        order.getId(), driverId, order.getTotalAmount());
                if (paymentResult != null && paymentResult.containsKey("success")
                        && !(Boolean) paymentResult.get("success")) {
                    throw new IdInvalidException(paymentResult.get("message") != null
                            ? (String) paymentResult.get("message")
                            : "COD payment processing failed");
                }
            } catch (IdInvalidException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to process COD payment for order {}: {}", orderId, e.getMessage());
                throw new IdInvalidException("Failed to process COD payment: " + e.getMessage());
            }
        }

        String previousStatus = order.getOrderStatus();

        // Update status to DRIVER_ASSIGNED and clear assignedAt (matches eatzy_backend)
        order.setOrderStatus(OrderStatus.DRIVER_ASSIGNED.name());
        order.setAssignedAt(null); // Clear assignedAt after successful acceptance
        order = orderRepository.save(order);

        publishStatusChanged(order, previousStatus, "Tài xế đã chấp nhận đơn hàng");
        return orderMapper.toResOrderDTO(order);
    }

    /**
     * Driver rejects order → find replacement driver.
     * Matches eatzy_backend: rejectOrderByDriver
     * 1. Set current driver to AVAILABLE
     * 2. Save rejection to Redis
     * 3. Find nearby drivers excluding rejected ones
     * 4. Use Mapbox to find closest driver
     * 5. Assign new driver or set driver to null if none found
     */
    @Transactional
    public ResOrderDTO rejectOrderByDriver(Long orderId, Long driverId) throws IdInvalidException {
        Order order = getOrderById(orderId);
        if (order == null)
            throw new IdInvalidException("Order not found with id: " + orderId);

        if (order.getDriverId() == null || !order.getDriverId().equals(driverId)) {
            throw new IdInvalidException("This order is not assigned to you");
        }

        // Save rejection to Redis (matches eatzy_backend)
        redisRejectionService.addRejectedDriver(orderId, driverId);
        log.info("💾 Saved driver {} rejection for order {} to Redis", driverId, orderId);

        // Set the rejecting driver's status back to AVAILABLE (matches eatzy_backend)
        try {
            authServiceClient.updateDriverStatus(driverId, "AVAILABLE");
            log.info("🟢 Set driver {} status to AVAILABLE after rejecting order {}", driverId, orderId);
        } catch (Exception e) {
            log.error("Failed to update driver {} profile status to AVAILABLE: {}", driverId, e.getMessage());
        }

        // Get list of all rejected driver IDs for this order from Redis
        List<Long> rejectedDriverIds = redisRejectionService.getRejectedDriverIds(orderId);

        // Get restaurant location
        Map<String, Object> restData = restaurantServiceClient.getRestaurantById(order.getRestaurantId());
        if (restData == null) {
            throw new IdInvalidException("Restaurant location is required to find drivers");
        }
        BigDecimal restLat = getBigDecimalValue(restData, "latitude");
        BigDecimal restLng = getBigDecimalValue(restData, "longitude");
        if (restLat == null || restLng == null) {
            throw new IdInvalidException("Restaurant location is required to find drivers");
        }

        // Search radius
        BigDecimal radiusKm = getSystemConfigValue("DRIVER_SEARCH_RADIUS_KM");

        log.info("🔍 Searching for alternative drivers via Redis GEO (excluding {} rejected drivers)",
                rejectedDriverIds.size());

        // STEP 1: Find nearby drivers via Redis GEO
        GeoResults<GeoLocation<Object>> geoResults = redisGeoService.findNearbyDrivers(
                restLat, restLng, radiusKm.doubleValue(), 100);

        if (geoResults == null || geoResults.getContent().isEmpty()) {
            log.warn("No alternative drivers found via Redis GEO");
            order.setDriverId(null);
            order.setAssignedAt(null);
            order = orderRepository.save(order);
            return orderMapper.toResOrderDTO(order);
        }

        List<Map<String, Object>> nearbyDrivers = convertGeoResultsToDriverList(geoResults);

        // Filter out rejected drivers
        List<Long> nearbyDriverIds = nearbyDrivers.stream()
                .map(d -> getLongValue(d, "userId"))
                .filter(id -> id != null && !rejectedDriverIds.contains(id))
                .collect(Collectors.toList());

        log.info("📍 Found {} available drivers (after excluding rejected)", nearbyDriverIds.size());

        if (nearbyDriverIds.isEmpty()) {
            order.setDriverId(null);
            order.setAssignedAt(null);
            order = orderRepository.save(order);
            return orderMapper.toResOrderDTO(order);
        }

        // STEP 2: Query SQL to validate business rules (COD limit, status)
        BigDecimal minCodLimit = "COD".equals(order.getPaymentMethod()) ? order.getTotalAmount() : null;
        List<Long> validDriverIds = authServiceClient.validateDriversByIds(nearbyDriverIds, minCodLimit);

        if (validDriverIds.isEmpty()) {
            log.warn("No qualified drivers found after SQL validation");
            order.setDriverId(null);
            order.setAssignedAt(null);
            order = orderRepository.save(order);
            return orderMapper.toResOrderDTO(order);
        }

        log.info("✅ {} drivers passed SQL validation", validDriverIds.size());

        // STEP 2b: Check wallet balance > 0 via payment-service (matches eatzy_backend)
        try {
            List<Long> driversWithBalance = paymentServiceClient.validateDriverWalletBalances(validDriverIds);
            validDriverIds = driversWithBalance;
            log.info("💰 {} drivers have positive wallet balance", validDriverIds.size());
        } catch (Exception e) {
            log.warn("Failed to validate wallet balances, skipping balance check: {}", e.getMessage());
        }

        if (validDriverIds.isEmpty()) {
            log.warn("No qualified drivers found (all drivers have zero wallet balance)");
            order.setDriverId(null);
            order.setAssignedAt(null);
            order = orderRepository.save(order);
            return orderMapper.toResOrderDTO(order);
        }

        // Filter nearbyDrivers to only include validated ones
        final List<Long> finalValidDriverIds = validDriverIds;
        List<Map<String, Object>> candidateDrivers = nearbyDrivers.stream()
                .filter(d -> {
                    Long id = getLongValue(d, "userId");
                    return id != null && finalValidDriverIds.contains(id);
                })
                .collect(Collectors.toList());

        // STEP 3: Find closest driver using Mapbox API (matches eatzy_backend)
        Long closestDriverId = null;
        BigDecimal shortestDistance = null;

        for (Map<String, Object> driverData : candidateDrivers) {
            Long candidateId = getLongValue(driverData, "userId");
            BigDecimal driverLat = getBigDecimalValue(driverData, "latitude");
            BigDecimal driverLng = getBigDecimalValue(driverData, "longitude");

            if (candidateId == null || driverLat == null || driverLng == null)
                continue;

            BigDecimal drivingDistance = mapboxService.getDrivingDistance(
                    driverLat, driverLng, restLat, restLng);

            if (drivingDistance == null) {
                log.warn("Failed to get Mapbox distance for driver {}", candidateId);
                continue;
            }

            if (shortestDistance == null || drivingDistance.compareTo(shortestDistance) < 0) {
                shortestDistance = drivingDistance;
                closestDriverId = candidateId;
            }
        }

        if (closestDriverId != null) {
            // Assign to next driver
            order.setDriverId(closestDriverId);
            order.setAssignedAt(Instant.now());
            log.info("🎯 Reassigned order {} to driver {}", orderId, closestDriverId);

            // Set the new driver's status to UNAVAILABLE (also removes from Redis GEO)
            try {
                authServiceClient.updateDriverStatus(closestDriverId, "UNAVAILABLE");
                log.info("🔴 Set driver {} status to UNAVAILABLE after reassignment", closestDriverId);
            } catch (Exception e) {
                log.error("Failed to update driver {} status to UNAVAILABLE: {}", closestDriverId, e.getMessage());
            }
        } else {
            // Failed to calculate distance for all candidates
            order.setDriverId(null);
            order.setAssignedAt(null);
            log.warn("Failed to find closest driver for order {} - Mapbox distance calculation failed", orderId);
        }

        order = orderRepository.save(order);

        // Notify new driver about order assignment
        if (order.getDriverId() != null) {
            publishStatusChanged(order, order.getOrderStatus(), "Tài xế mới đã được phân công");
        }

        return orderMapper.toResOrderDTO(order);
    }

    @Transactional
    public ResOrderDTO markOrderAsPickedUp(Long orderId, Long driverId) throws IdInvalidException {
        Order order = getOrderById(orderId);
        if (order == null)
            throw new IdInvalidException("Order not found with id: " + orderId);

        if (order.getDriverId() == null || !order.getDriverId().equals(driverId)) {
            throw new IdInvalidException("This order is not assigned to you");
        }

        String previousStatus = order.getOrderStatus();
        OrderStateMachine.transition(previousStatus, OrderStatus.PICKED_UP.name());

        order.setOrderStatus(OrderStatus.PICKED_UP.name());
        order = orderRepository.save(order);

        publishStatusChanged(order, previousStatus, "Tài xế đã nhận đơn hàng và đang giao");
        return orderMapper.toResOrderDTO(order);
    }

    @Transactional
    public ResOrderDTO markOrderAsArrived(Long orderId, Long driverId) throws IdInvalidException {
        Order order = getOrderById(orderId);
        if (order == null)
            throw new IdInvalidException("Order not found with id: " + orderId);

        if (order.getDriverId() == null || !order.getDriverId().equals(driverId)) {
            throw new IdInvalidException("This order is not assigned to you");
        }

        String previousStatus = order.getOrderStatus();
        OrderStateMachine.transition(previousStatus, OrderStatus.ARRIVED.name());

        order.setOrderStatus(OrderStatus.ARRIVED.name());
        order = orderRepository.save(order);

        publishStatusChanged(order, previousStatus, "Tài xế đã đến nơi giao hàng!");
        return orderMapper.toResOrderDTO(order);
    }

    @Transactional
    public ResOrderDTO markOrderAsDelivered(Long orderId, Long driverId) throws IdInvalidException {
        Order order = getOrderById(orderId);
        if (order == null)
            throw new IdInvalidException("Order not found with id: " + orderId);

        if (order.getDriverId() == null || !order.getDriverId().equals(driverId)) {
            throw new IdInvalidException("This order is not assigned to you");
        }

        String previousStatus = order.getOrderStatus();
        OrderStateMachine.transition(previousStatus, OrderStatus.DELIVERED.name());

        order.setOrderStatus(OrderStatus.DELIVERED.name());
        order.setDeliveredAt(Instant.now());

        if ("COD".equals(order.getPaymentMethod())) {
            order.setPaymentStatus("PAID");
        }
        order = orderRepository.save(order);

        publishStatusChanged(order, previousStatus, "Đơn hàng đã được giao thành công!");

        // Update driver's completed trips count and set status to AVAILABLE (matches
        // eatzy_backend)
        try {
            authServiceClient.incrementCompletedTrips(driverId);
            log.info("🏁 Incremented completed trips for driver {}", driverId);
        } catch (Exception e) {
            log.error("Failed to increment completed trips for driver {}: {}", driverId, e.getMessage());
        }

        // Update driver status back to AVAILABLE
        try {
            authServiceClient.updateDriverStatus(driverId, "AVAILABLE");
            log.info("🟢 Set driver {} status to AVAILABLE after delivery", driverId);

            // Fetch profile and publish event so they can be assigned the next order
            // automatically
            Map<String, Object> profile = authServiceClient.getDriverProfileByUserId(driverId);
            if (profile != null && profile.containsKey("currentLatitude") && profile.containsKey("currentLongitude")) {
                BigDecimal lat = new BigDecimal(profile.get("currentLatitude").toString());
                BigDecimal lng = new BigDecimal(profile.get("currentLongitude").toString());
                orderEventProducer
                        .publishDriverOnlineEvent(new com.eatzy.common.event.DriverOnlineEvent(driverId, lat, lng));
            }
        } catch (Exception e) {
            log.error("Failed to update driver {} status to AVAILABLE or publish event: {}", driverId, e.getMessage());
        }

        // Create earnings summary when order is delivered (matches eatzy_backend)
        try {
            orderEarningsSummaryService.createEarningsSummary(orderId);
            log.info("💰 Created earnings summary for delivered order {}", orderId);
        } catch (Exception e) {
            log.error("Failed to create earnings summary for order {}: {}", orderId, e.getMessage());
        }

        return orderMapper.toResOrderDTO(order);
    }

    @Transactional
    public ResOrderDTO updateOrder(Order orderUpdate) throws IdInvalidException {
        Order current = getOrderById(orderUpdate.getId());
        if (current == null)
            throw new IdInvalidException("Order not found with id: " + orderUpdate.getId());

        if (orderUpdate.getOrderStatus() != null) {
            current.setOrderStatus(orderUpdate.getOrderStatus());
            if ("DELIVERED".equals(orderUpdate.getOrderStatus()) && current.getDeliveredAt() == null) {
                current.setDeliveredAt(Instant.now());
            }
        }
        if (orderUpdate.getDriverId() != null)
            current.setDriverId(orderUpdate.getDriverId());
        if (orderUpdate.getDeliveryAddress() != null)
            current.setDeliveryAddress(orderUpdate.getDeliveryAddress());
        if (orderUpdate.getDeliveryLatitude() != null)
            current.setDeliveryLatitude(orderUpdate.getDeliveryLatitude());
        if (orderUpdate.getDeliveryLongitude() != null)
            current.setDeliveryLongitude(orderUpdate.getDeliveryLongitude());
        if (orderUpdate.getSpecialInstructions() != null)
            current.setSpecialInstructions(orderUpdate.getSpecialInstructions());
        if (orderUpdate.getSubtotal() != null)
            current.setSubtotal(orderUpdate.getSubtotal());
        if (orderUpdate.getDeliveryFee() != null)
            current.setDeliveryFee(orderUpdate.getDeliveryFee());
        if (orderUpdate.getTotalAmount() != null)
            current.setTotalAmount(orderUpdate.getTotalAmount());
        if (orderUpdate.getPaymentMethod() != null)
            current.setPaymentMethod(orderUpdate.getPaymentMethod());
        if (orderUpdate.getPaymentStatus() != null)
            current.setPaymentStatus(orderUpdate.getPaymentStatus());
        if (orderUpdate.getCancellationReason() != null)
            current.setCancellationReason(orderUpdate.getCancellationReason());

        current = orderRepository.save(current);
        return orderMapper.toResOrderDTO(current);
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Process refund if order was already paid via WALLET or VNPAY.
     * Matches eatzy_backend refund logic in cancelOrder and
     * rejectOrderByRestaurant.
     */
    private void processRefundIfNeeded(Order order) {
        if ("PAID".equals(order.getPaymentStatus()) &&
                ("WALLET".equals(order.getPaymentMethod()) || "VNPAY".equals(order.getPaymentMethod()))) {
            try {
                paymentServiceClient.processRefund(order.getId(), order.getCustomerId(), order.getTotalAmount());
                order.setPaymentStatus("REFUNDED");
                log.info("💰 Processed refund for order {} (method: {}, amount: {})",
                        order.getId(), order.getPaymentMethod(), order.getTotalAmount());
            } catch (Exception e) {
                log.error("Failed to process refund for order {}: {}", order.getId(), e.getMessage());
            }
        }
    }

    private void publishStatusChanged(Order order, String previousStatus, String message) {
        orderEventProducer.publishOrderStatusChanged(new OrderStatusChangedEvent(
                order.getId(), previousStatus, order.getOrderStatus(),
                order.getCustomerId(), order.getRestaurantId(), order.getDriverId(), message));
    }

    private ResultPaginationDTO buildPaginationResult(Page<Order> page, Pageable pageable) {
        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());
        result.setMeta(meta);
        result.setResult(page.getContent().stream()
                .map(orderMapper::toResOrderDTO)
                .collect(Collectors.toList()));
        return result;
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null)
            return null;
        if (value instanceof BigDecimal)
            return (BigDecimal) value;
        return new BigDecimal(value.toString());
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null)
            return null;
        if (value instanceof Long)
            return (Long) value;
        if (value instanceof Number)
            return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    /**
     * Convert Redis GeoResults to a list of driver data maps.
     * Each map contains: userId (Long), latitude (BigDecimal), longitude
     * (BigDecimal), distance (Double in km).
     */
    private List<Map<String, Object>> convertGeoResultsToDriverList(GeoResults<GeoLocation<Object>> geoResults) {
        List<Map<String, Object>> drivers = new ArrayList<>();
        for (GeoResult<GeoLocation<Object>> result : geoResults.getContent()) {
            GeoLocation<Object> location = result.getContent();
            String driverIdStr = location.getName().toString();

            Map<String, Object> driverMap = new HashMap<>();
            try {
                driverMap.put("userId", Long.parseLong(driverIdStr));
            } catch (NumberFormatException e) {
                log.warn("Invalid driver ID in Redis GEO: {}", driverIdStr);
                continue;
            }

            // GeoLocation point: x = longitude, y = latitude
            if (location.getPoint() != null) {
                driverMap.put("latitude", BigDecimal.valueOf(location.getPoint().getY()));
                driverMap.put("longitude", BigDecimal.valueOf(location.getPoint().getX()));
            }

            // Distance in km
            if (result.getDistance() != null) {
                driverMap.put("distance", result.getDistance().getValue());
            }

            drivers.add(driverMap);
        }
        return drivers;
    }

    private BigDecimal getSystemConfigValue(String key) {
        try {
            Map<String, Object> configData = systemConfigServiceClient.getSystemConfigurationByKey(key);
            if (configData != null && configData.get("configValue") != null) {
                return new BigDecimal(configData.get("configValue").toString());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch system config for key: {}. Reason: {}",
                    key, e.getMessage());
        }
        return null;
    }

    @Transactional
    public boolean findAndAssignNextOrderForSpecificDriver(Long driverId, BigDecimal currentLat,
            BigDecimal currentLng) {
        try {
            // STEP 1: Find all PREPARING orders without driver (ordered by oldest first)
            List<Order> preparingOrders = orderRepository
                    .findByOrderStatusAndDriverIdIsNullOrderByPreparingAtAsc("PREPARING");

            if (preparingOrders.isEmpty()) {
                log.info("No PREPARING orders available for driver {}", driverId);
                return false;
            }

            log.info("📋 Found {} PREPARING orders without driver", preparingOrders.size());

            // Get driver profile to check COD limit
            Map<String, Object> profileData = authServiceClient.getDriverProfileByUserId(driverId);
            BigDecimal codLimit = null;
            if (profileData != null) {
                codLimit = getBigDecimalValue(profileData, "codLimit");
            }

            // Get search radius
            BigDecimal radiusKm = getSystemConfigValue("DRIVER_SEARCH_RADIUS_KM");

            // STEP 2: Validate each order against business rules and find the first
            // suitable one
            for (Order order : preparingOrders) {
                try {
                    Map<String, Object> restData = restaurantServiceClient.getRestaurantById(order.getRestaurantId());
                    BigDecimal restLat = getBigDecimalValue(restData, "latitude");
                    BigDecimal restLng = getBigDecimalValue(restData, "longitude");

                    if (restLat == null || restLng == null)
                        continue;

                    if ("COD".equals(order.getPaymentMethod())) {
                        if (codLimit == null || codLimit.compareTo(order.getTotalAmount()) < 0) {
                            log.info("❌ Order {} (COD: {}) exceeds driver's COD limit ({}), skipping",
                                    order.getId(), order.getTotalAmount(), codLimit);
                            continue;
                        }
                    }

                    BigDecimal distance = mapboxService.getDrivingDistance(currentLat, currentLng, restLat, restLng);
                    if (distance == null)
                        continue;

                    log.info("📍 Order {} - Distance to restaurant: {} km (Max radius: {} km)",
                            order.getId(), distance, radiusKm);

                    if (distance.compareTo(radiusKm) > 0)
                        continue;

                    // Order is suitable! Check wallet balance before assignment
                    try {
                        List<Long> driverIds = java.util.Collections.singletonList(driverId);
                        List<Long> driversWithBalance = paymentServiceClient.validateDriverWalletBalances(driverIds);
                        if (driversWithBalance.isEmpty()) {
                            log.warn("❌ Driver {} has zero wallet balance, skipping order assignment", driverId);
                            return false; // driver can't accept any order
                        }
                    } catch (Exception e) {
                        log.warn("Failed to validate wallet balances, skipping balance check: {}", e.getMessage());
                    }

                    log.info("✅ Order {} passed all validations! Assigning driver {}", order.getId(), driverId);

                    order.setDriverId(driverId);
                    order.setAssignedAt(Instant.now());
                    order = orderRepository.save(order);

                    authServiceClient.updateDriverStatus(driverId, "UNAVAILABLE");
                    log.info("🔴 Set driver {} status to UNAVAILABLE after order assignment", driverId);

                    publishStatusChanged(order, "PREPARING", "Tài xế đã được phân công");

                    return true;
                } catch (Exception e) {
                    log.error("Error processing order {} for driver {}: {}", order.getId(), driverId, e.getMessage());
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error finding next order for specific driver {}: {}", driverId, e.getMessage());
            return false;
        }
    }
}
