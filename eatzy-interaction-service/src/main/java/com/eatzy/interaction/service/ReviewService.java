package com.eatzy.interaction.service;

import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.common.util.SecurityUtils;
import com.eatzy.interaction.designpattern.adapter.AuthServiceClient;
import com.eatzy.interaction.designpattern.adapter.OrderServiceClient;
import com.eatzy.interaction.designpattern.adapter.RestaurantServiceClient;
import com.eatzy.interaction.designpattern.event.KafkaProducerService;
import com.eatzy.interaction.domain.Review;
import com.eatzy.interaction.dto.request.ReqReviewCreateDTO;
import com.eatzy.interaction.dto.request.ScoringEventDTO;
import com.eatzy.interaction.dto.response.ResReviewDTO;
import com.eatzy.interaction.repository.ReviewRepository;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final AuthServiceClient authServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;
    private final UserScoringService userScoringService;
    private final KafkaProducerService kafkaProducerService;

    public ReviewService(ReviewRepository reviewRepository,
            AuthServiceClient authServiceClient,
            OrderServiceClient orderServiceClient,
            RestaurantServiceClient restaurantServiceClient,
            UserScoringService userScoringService,
            KafkaProducerService kafkaProducerService) {
        this.reviewRepository = reviewRepository;
        this.authServiceClient = authServiceClient;
        this.orderServiceClient = orderServiceClient;
        this.restaurantServiceClient = restaurantServiceClient;
        this.userScoringService = userScoringService;
        this.kafkaProducerService = kafkaProducerService;
    }

    private Map<String, Object> extractData(Map<String, Object> response) {
        if (response == null)
            return null;
        if (response.containsKey("result") && response.get("result") instanceof Map) {
            return (Map<String, Object>) response.get("result");
        } else if (response.containsKey("data") && response.get("data") instanceof Map) {
            return (Map<String, Object>) response.get("data");
        }
        return response;
    }

    public ResReviewDTO convertToDTO(Review review) {
        if (review == null) {
            return null;
        }
        ResReviewDTO dto = new ResReviewDTO();
        dto.setId(review.getId());
        dto.setReviewTarget(review.getReviewTarget());
        dto.setTargetName(review.getTargetName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setReply(review.getReply());
        dto.setCreatedAt(review.getCreatedAt());

        if (review.getOrderId() != null) {
            ResReviewDTO.Order orderDTO = new ResReviewDTO.Order();
            orderDTO.setId(review.getOrderId());
            dto.setOrder(orderDTO);
        }

        if (review.getCustomerId() != null) {
            try {
                Map<String, Object> userRes = extractData(authServiceClient.getUserById(review.getCustomerId()));
                if (userRes != null) {
                    ResReviewDTO.User customerDTO = new ResReviewDTO.User();
                    customerDTO.setId(review.getCustomerId());
                    customerDTO.setName((String) userRes.get("name"));
                    dto.setCustomer(customerDTO);
                }
            } catch (FeignException e) {
                // Ignore
            }
        }

        return dto;
    }

    public ResReviewDTO getReviewById(Long id) {
        Optional<Review> reviewOpt = this.reviewRepository.findById(id);
        return convertToDTO(reviewOpt.orElse(null));
    }

    public List<ResReviewDTO> getReviewsByCustomerId(Long customerId) {
        return this.reviewRepository.findByCustomerId(customerId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    public List<ResReviewDTO> getReviewsByOrderId(Long orderId) {
        return this.reviewRepository.findByOrderId(orderId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    public List<ResReviewDTO> getReviewsByTarget(String reviewTarget, String targetName) {
        return this.reviewRepository.findByReviewTargetAndTargetName(reviewTarget, targetName).stream()
                .map(this::convertToDTO)
                .toList();
    }

    public ResultPaginationDTO getReviewsByMyRestaurant(Specification<Review> spec, Pageable pageable)
            throws IdInvalidException {
        // Get current user's restaurant name from restaurant service
        String restaurantName = null;
        try {
            Map<String, Object> result = extractData(restaurantServiceClient.getMyRestaurant());
            if (result != null && result.containsKey("name")) {
                restaurantName = (String) result.get("name");
            }
        } catch (FeignException e) {
            // Ignore and proceed
        }

        if (restaurantName == null) {
            throw new IdInvalidException("User does not have a restaurant");
        }

        // Build specification to get reviews for this restaurant
        final String finalRestaurantName = restaurantName;
        Specification<Review> finalSpec = (root, query, cb) -> cb.and(
                cb.equal(root.get("reviewTarget"), "restaurant"),
                cb.equal(root.get("targetName"), finalRestaurantName));

        if (spec != null) {
            finalSpec = finalSpec.and(spec);
        }

        Page<Review> page = reviewRepository.findAll(finalSpec, pageable);

        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());
        result.setMeta(meta);
        List<ResReviewDTO> dtoList = page.getContent().stream()
                .map(this::convertToDTO)
                .toList();
        result.setResult(dtoList);
        return result;
    }

    @Transactional
    public ResReviewDTO createReview(ReqReviewCreateDTO req) throws IdInvalidException {
        Long customerId = SecurityUtils.getCurrentUserId();

        // 1. Validate Target
        String reviewTarget = req.getReviewTarget().toLowerCase();
        if (!reviewTarget.equals("restaurant") && !reviewTarget.equals("driver")) {
            throw new IdInvalidException("Review target must be either 'restaurant' or 'driver'");
        }

        // 2. Validate Order
        if (req.getOrder() == null || req.getOrder().getId() == null) {
            throw new IdInvalidException("Order id is required");
        }
        Long orderId = req.getOrder().getId();

        Map<String, Object> orderRes;
        try {
            orderRes = extractData(orderServiceClient.getOrderById(orderId));
            if (orderRes == null) {
                throw new IdInvalidException("Order not found with id: " + orderId);
            }
        } catch (FeignException.NotFound e) {
            throw new IdInvalidException("Order not found with id: " + orderId);
        }

        // 3. Verify customer owns order
        Map<String, Object> orderCustomer = (Map<String, Object>) orderRes.get("customer");
        if (orderCustomer == null || !customerId.equals(Long.valueOf(orderCustomer.get("id").toString()))) {
            throw new IdInvalidException("You are not allowed to review this order");
        }

        // 4. Check if order is DELIVERED
        if (!"DELIVERED".equals(orderRes.get("orderStatus"))) {
            throw new IdInvalidException("Only delivered orders can be reviewed");
        }

        // 5. Check if already reviewed
        List<Review> existingReviews = this.reviewRepository.findByOrderIdAndReviewTarget(orderId, reviewTarget);
        if (!existingReviews.isEmpty()) {
            throw new IdInvalidException("You have already reviewed this " + reviewTarget + " for this order");
        }

        // Extract Restaurant/Driver Info to set targetName and trigger Score Updates
        Long restaurantId = null;
        List<Long> restaurantTypeIds = new ArrayList<>();
        String targetNameObj = null;

        if (reviewTarget.equals("restaurant")) {
            Map<String, Object> rest = (Map<String, Object>) orderRes.get("restaurant");
            if (rest == null)
                throw new IdInvalidException("Order does not have a restaurant assigned");
            targetNameObj = (String) rest.get("name");
            restaurantId = Long.valueOf(rest.get("id").toString());
            List<Map<String, Object>> types = (List<Map<String, Object>>) rest.get("restaurantTypes");
            if (types != null) {
                for (Map<String, Object> t : types) {
                    restaurantTypeIds.add(Long.valueOf(t.get("id").toString()));
                }
            }
        } else if (reviewTarget.equals("driver")) {
            Map<String, Object> driver = (Map<String, Object>) orderRes.get("driver");
            if (driver == null)
                throw new IdInvalidException("Order does not have a driver assigned");
            targetNameObj = (String) driver.get("name");
        }

        Review review = new Review();
        review.setCustomerId(customerId);
        review.setOrderId(orderId);
        review.setReviewTarget(reviewTarget);
        review.setTargetName(targetNameObj);
        review.setRating(req.getRating());
        review.setComment(req.getComment());
        review.setCreatedAt(Instant.now());

        Review savedReview = reviewRepository.save(review);

        // 6. Update user score if restaurant
        if (reviewTarget.equals("restaurant")) {
            ScoringEventDTO scoreEvent = new ScoringEventDTO(customerId, restaurantId, restaurantTypeIds);
            userScoringService.trackRating(scoreEvent, review.getRating());
        }

        // 7. Publish review event to Kafka for Restaurant/Auth services to update
        // global ratings
        kafkaProducerService.publishReviewCreatedEvent(reviewTarget, review.getTargetName(), review.getRating());

        return convertToDTO(savedReview);
    }

    @Transactional
    public ResReviewDTO updateReview(Review inputReview) throws IdInvalidException {
        Review currentReview = this.reviewRepository.findById(inputReview.getId())
                .orElseThrow(() -> new IdInvalidException("Review not found with id: " + inputReview.getId()));

        boolean ratingChanged = false;

        if (inputReview.getRating() != null && !inputReview.getRating().equals(currentReview.getRating())) {
            if (inputReview.getRating() < 1 || inputReview.getRating() > 5) {
                throw new IdInvalidException("Rating must be between 1 and 5");
            }
            ratingChanged = true;
            currentReview.setRating(inputReview.getRating());
        }
        if (inputReview.getComment() != null) {
            currentReview.setComment(inputReview.getComment());
        }
        if (inputReview.getReply() != null) {
            currentReview.setReply(inputReview.getReply());
        }

        Review updatedReview = reviewRepository.save(currentReview);

        if (ratingChanged) {
            kafkaProducerService.publishReviewCreatedEvent(
                    updatedReview.getReviewTarget(),
                    updatedReview.getTargetName(),
                    updatedReview.getRating());
        }

        return convertToDTO(updatedReview);
    }

    @Transactional
    public void deleteReview(Long id) throws IdInvalidException {
        Review review = this.reviewRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Review not found with id: " + id));

        this.reviewRepository.deleteById(id);

        // Let external services recalculate rating if they want by publishing event
        // (optional)
    }

    public ResultPaginationDTO getAllReviews(Specification<Review> spec, Pageable pageable) {
        Page<Review> page = this.reviewRepository.findAll(spec, pageable);
        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());
        result.setMeta(meta);
        List<ResReviewDTO> dtoList = page.getContent().stream()
                .map(this::convertToDTO)
                .toList();
        result.setResult(dtoList);
        return result;
    }
}
