package com.eatzy.order.designpattern.facade;

import com.eatzy.common.event.OrderCreatedEvent;
import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.order.designpattern.adapter.PaymentServiceClient;
import com.eatzy.order.designpattern.adapter.PaymentServiceClient.CalculateDiscountReq;
import com.eatzy.order.designpattern.context.OrderCreationContext;
import com.eatzy.order.designpattern.cor.CustomerValidationHandler;
import com.eatzy.order.designpattern.cor.DeliveryFeeValidationHandler;
import com.eatzy.order.designpattern.cor.OrderItemsValidationHandler;
import com.eatzy.order.designpattern.cor.RestaurantValidationHandler;
import com.eatzy.order.designpattern.state.OrderStatus;
import com.eatzy.order.domain.Order;
import com.eatzy.order.domain.OrderItem;
import com.eatzy.order.dto.request.ReqOrderDTO;
import com.eatzy.order.dto.response.ResOrderDTO;
import com.eatzy.order.kafka.OrderEventProducer;
import com.eatzy.order.mapper.OrderMapper;
import com.eatzy.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderCreationFacade {

    private static final Logger log = LoggerFactory.getLogger(OrderCreationFacade.class);

    private final CustomerValidationHandler customerValidationHandler;
    private final RestaurantValidationHandler restaurantValidationHandler;
    private final DeliveryFeeValidationHandler deliveryFeeValidationHandler;
    private final OrderItemsValidationHandler orderItemsValidationHandler;
    
    private final PaymentServiceClient paymentServiceClient;
    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;
    private final OrderMapper orderMapper;

    public OrderCreationFacade(CustomerValidationHandler customerValidationHandler,
                               RestaurantValidationHandler restaurantValidationHandler,
                               DeliveryFeeValidationHandler deliveryFeeValidationHandler,
                               OrderItemsValidationHandler orderItemsValidationHandler,
                               PaymentServiceClient paymentServiceClient,
                               OrderRepository orderRepository,
                               OrderEventProducer orderEventProducer,
                               OrderMapper orderMapper) {
        
        this.customerValidationHandler = customerValidationHandler;
        this.restaurantValidationHandler = restaurantValidationHandler;
        this.deliveryFeeValidationHandler = deliveryFeeValidationHandler;
        this.orderItemsValidationHandler = orderItemsValidationHandler;
        
        this.paymentServiceClient = paymentServiceClient;
        this.orderRepository = orderRepository;
        this.orderEventProducer = orderEventProducer;
        this.orderMapper = orderMapper;

        // Build the Chain of Responsibility
        this.customerValidationHandler
            .setNext(this.restaurantValidationHandler)
            .setNext(this.deliveryFeeValidationHandler)
            .setNext(this.orderItemsValidationHandler);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResOrderDTO createOrder(ReqOrderDTO reqOrderDTO, String clientIp, String baseUrl) throws IdInvalidException {
        // 1. Initialize Context
        OrderCreationContext context = OrderCreationContext.builder()
                .reqOrderDTO(reqOrderDTO)
                .clientIp(clientIp)
                .baseUrl(baseUrl)
                .build();

        // 2. Execute Validation Chain
        customerValidationHandler.handle(context);

        // 3. Build Order Entity (Builder Pattern)
        Order order = Order.builder()
                .customerId(reqOrderDTO.getCustomer().getId())
                .restaurantId(reqOrderDTO.getRestaurant().getId())
                .orderStatus(OrderStatus.PENDING.name())
                .deliveryAddress(reqOrderDTO.getDeliveryAddress())
                .deliveryLatitude(reqOrderDTO.getDeliveryLatitude() != null ? BigDecimal.valueOf(reqOrderDTO.getDeliveryLatitude()) : null)
                .deliveryLongitude(reqOrderDTO.getDeliveryLongitude() != null ? BigDecimal.valueOf(reqOrderDTO.getDeliveryLongitude()) : null)
                .specialInstructions(reqOrderDTO.getSpecialInstructions())
                .paymentMethod(reqOrderDTO.getPaymentMethod())
                .paymentStatus(reqOrderDTO.getPaymentStatus() != null ? reqOrderDTO.getPaymentStatus() : "UNPAID")
                .createdAt(Instant.now())
                .deliveryFee(context.getDeliveryFee())
                .build();

        if (reqOrderDTO.getDriver() != null && reqOrderDTO.getDriver().getId() != null) {
            order.setDriverId(reqOrderDTO.getDriver().getId());
        }

        if (reqOrderDTO.getVouchers() != null && !reqOrderDTO.getVouchers().isEmpty()) {
            List<Long> voucherIds = reqOrderDTO.getVouchers().stream()
                    .filter(v -> v.getId() != null)
                    .map(ReqOrderDTO.Voucher::getId)
                    .collect(Collectors.toList());
            order.setVoucherIds(voucherIds);
        }

        // Must save order first to get ID for OrderItems (Hibernate mapping requires it)
        Order savedOrder = orderRepository.save(order);

        // Map Order back to OrderItems created in the chain
        List<OrderItem> orderItems = context.getOrderItems();
        if (orderItems != null && !orderItems.isEmpty()) {
            for (OrderItem item : orderItems) {
                item.setOrder(savedOrder);
            }
            savedOrder.setOrderItems(orderItems);
        }

        // 4. Set final amounts & Calculate Discount
        BigDecimal subtotal = context.getSubtotal();
        savedOrder.setSubtotal(subtotal);

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (savedOrder.getVoucherIds() != null && !savedOrder.getVoucherIds().isEmpty()) {
            try {
                CalculateDiscountReq req = new CalculateDiscountReq(
                        savedOrder.getVoucherIds(),
                        subtotal,
                        savedOrder.getRestaurantId(),
                        savedOrder.getCustomerId(),
                        context.getDeliveryFee());
                discountAmount = paymentServiceClient.calculateVoucherDiscount(req);
            } catch (Exception e) {
                // Ignore error, just keep discount 0
            }
        }
        savedOrder.setDiscountAmount(discountAmount);
        savedOrder.setTotalAmount(subtotal.add(context.getDeliveryFee()).subtract(discountAmount));

        // Update with final prices and items
        savedOrder = orderRepository.save(savedOrder);

        // 5. Publish Kafka Events (Observer)
        orderEventProducer.publishOrderCreated(new OrderCreatedEvent(
                savedOrder.getId(), savedOrder.getCustomerId(), savedOrder.getRestaurantId(),
                savedOrder.getTotalAmount(), savedOrder.getOrderStatus()));

        orderEventProducer.publishTrackPlaceOrder(savedOrder.getCustomerId(), savedOrder.getRestaurantId());

        // 6. Process Payment via Adapter
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
                if (paymentResult.containsKey("success") && !(Boolean) paymentResult.get("success")) {
                    throw new IdInvalidException(paymentResult.get("message") != null ? 
                            (String) paymentResult.get("message") : "Wallet payment failed");
                }
            }
        } catch (IdInvalidException e) {
            throw e; 
        } catch (Exception e) {
            log.error("Failed to initiate payment: {}", e.getMessage());
            throw new IdInvalidException("Failed to initiate payment: " + e.getMessage());
        }

        return orderDTO;
    }
}
