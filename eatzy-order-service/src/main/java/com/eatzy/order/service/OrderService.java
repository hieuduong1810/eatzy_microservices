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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    public OrderService(OrderRepository orderRepository,
            OrderMapper orderMapper,
            OrderEventProducer orderEventProducer,
            DefaultDeliveryFeeCalculator deliveryFeeCalculator,
            MapboxService mapboxService,
            DynamicPricingService dynamicPricingService,
            AuthServiceClient authServiceClient,
            RestaurantServiceClient restaurantServiceClient,
            PaymentServiceClient paymentServiceClient,
            SystemConfigServiceClient systemConfigServiceClient) {
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
        BigDecimal baseFee = getSystemConfigValue("DELIVERY_BASE_FEE", new BigDecimal("15000"));
        BigDecimal baseDistance = getSystemConfigValue("DELIVERY_BASE_DISTANCE", new BigDecimal("3"));
        BigDecimal perKmFee = getSystemConfigValue("DELIVERY_PER_KM_FEE", new BigDecimal("5000"));
        BigDecimal minFee = getSystemConfigValue("DELIVERY_MIN_FEE", new BigDecimal("10000"));

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
    public ResOrderDTO createOrderFromReqDTO(ReqOrderDTO reqOrderDTO, String clientIp, String baseUrl) throws IdInvalidException {
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
        BigDecimal baseFee = getSystemConfigValue("DELIVERY_BASE_FEE", new BigDecimal("15000"));
        BigDecimal baseDistance = getSystemConfigValue("DELIVERY_BASE_DISTANCE", new BigDecimal("3"));
        BigDecimal perKmFee = getSystemConfigValue("DELIVERY_PER_KM_FEE", new BigDecimal("5000"));
        BigDecimal minFee = getSystemConfigValue("DELIVERY_MIN_FEE", new BigDecimal("10000"));

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
                    savedOrder.getDriverId()
            );

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
                
                // If payment was WALLET and it failed (success = false), throw exception to rollback
                if (paymentResult.containsKey("success") && !(Boolean) paymentResult.get("success")) {
                    throw new IdInvalidException(paymentResult.get("message") != null ? (String) paymentResult.get("message") : "Wallet payment failed");
                }
            }
        } catch (IdInvalidException e) {
            throw e; // rethrow to trigger rollback
        } catch (Exception e) {
            log.error("Failed to initiate payment: {}", e.getMessage());
            // Optionally, we could throw an exception to rollback the order:
            // throw new IdInvalidException("Failed to initiate payment: " + e.getMessage());
        }

        return orderDTO;
    }

    // ==================== STATUS TRANSITION METHODS (State Pattern)
    // ====================

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
        return orderMapper.toResOrderDTO(order);
    }

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

        // Find nearby drivers via Auth Service (Adapter Pattern)
        BigDecimal searchRadius = getSystemConfigValue("DRIVER_SEARCH_RADIUS_KM", new BigDecimal("10.0"));
        List<Map<String, Object>> nearbyDrivers = authServiceClient.findNearbyDrivers(restLat, restLng, searchRadius.doubleValue(), 50);
        if (nearbyDrivers.isEmpty())
            throw new IdInvalidException("No drivers found within radius");

        // Assign first available driver
        Map<String, Object> selectedDriver = nearbyDrivers.get(0);
        Long driverId = getLongValue(selectedDriver, "userId");
        if (driverId == null)
            throw new IdInvalidException("Driver user not found");

        order.setDriverId(driverId);
        order.setAssignedAt(Instant.now());
        order = orderRepository.save(order);

        // Update driver status
        authServiceClient.updateDriverStatus(driverId, "UNAVAILABLE");

        publishStatusChanged(order, order.getOrderStatus(), "Tài xế đã được phân công");
        return orderMapper.toResOrderDTO(order);
    }

    @Transactional
    public ResOrderDTO rejectOrderByRestaurant(Long orderId, String cancellationReason) throws IdInvalidException {
        Order order = getOrderById(orderId);
        if (order == null)
            throw new IdInvalidException("Order not found with id: " + orderId);

        String previousStatus = order.getOrderStatus();
        OrderStateMachine.transition(previousStatus, OrderStatus.REJECTED.name());

        order.setOrderStatus(OrderStatus.REJECTED.name());
        order.setCancellationReason(cancellationReason);
        order = orderRepository.save(order);

        publishStatusChanged(order, previousStatus, "Đơn hàng đã bị từ chối bởi nhà hàng");
        return orderMapper.toResOrderDTO(order);
    }

    @Transactional
    public ResOrderDTO cancelOrder(Long orderId, String cancellationReason) throws IdInvalidException {
        Order order = getOrderById(orderId);
        if (order == null)
            throw new IdInvalidException("Order not found with id: " + orderId);

        String previousStatus = order.getOrderStatus();
        OrderStateMachine.validateCancellation(previousStatus);

        order.setOrderStatus(OrderStatus.REJECTED.name());
        order.setCancellationReason(cancellationReason);
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

    @Transactional
    public ResOrderDTO acceptOrderByDriver(Long orderId, Long driverId) throws IdInvalidException {
        Order order = getOrderById(orderId);
        if (order == null)
            throw new IdInvalidException("Order not found with id: " + orderId);

        if (order.getDriverId() == null || !order.getDriverId().equals(driverId)) {
            throw new IdInvalidException("This order is not assigned to driver " + driverId);
        }

        String previousStatus = order.getOrderStatus();
        OrderStateMachine.transition(previousStatus, OrderStatus.DRIVER_ASSIGNED.name());

        order.setOrderStatus(OrderStatus.DRIVER_ASSIGNED.name());
        order.setAssignedAt(null);
        order = orderRepository.save(order);

        publishStatusChanged(order, previousStatus, "Tài xế đã chấp nhận đơn hàng");
        return orderMapper.toResOrderDTO(order);
    }

    @Transactional
    public ResOrderDTO rejectOrderByDriver(Long orderId, Long driverId) throws IdInvalidException {
        Order order = getOrderById(orderId);
        if (order == null)
            throw new IdInvalidException("Order not found with id: " + orderId);

        if (order.getDriverId() == null || !order.getDriverId().equals(driverId)) {
            throw new IdInvalidException("This order is not assigned to you");
        }

        // Set driver back to AVAILABLE
        authServiceClient.updateDriverStatus(driverId, "AVAILABLE");

        // Clear driver assignment, attempt to find another
        order.setDriverId(null);
        order.setAssignedAt(null);
        order = orderRepository.save(order);

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

        // Update driver status back to AVAILABLE
        authServiceClient.updateDriverStatus(driverId, "AVAILABLE");

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

    private BigDecimal getSystemConfigValue(String key, BigDecimal defaultValue) {
        try {
            Map<String, Object> configData = systemConfigServiceClient.getSystemConfigurationByKey(key);
            if (configData != null && configData.get("configValue") != null) {
                return new BigDecimal(configData.get("configValue").toString());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch system config for key: {}. Using default: {}. Reason: {}", 
                    key, defaultValue, e.getMessage());
        }
        return defaultValue;
    }
}
