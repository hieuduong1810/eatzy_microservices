package com.eatzy.interaction.controller;

import com.eatzy.common.dto.ResultPaginationDTO;
import com.eatzy.common.exception.IdInvalidException;
import com.eatzy.common.util.SecurityUtils;
import com.eatzy.interaction.domain.Review;
import com.eatzy.interaction.dto.request.ReqReviewCreateDTO;
import com.eatzy.interaction.dto.response.ResReviewDTO;
import com.eatzy.interaction.service.ReviewService;
import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ResReviewDTO> createReview(@Valid @RequestBody ReqReviewCreateDTO req)
            throws IdInvalidException {
        return ResponseEntity.ok(reviewService.createReview(req));
    }

    @PutMapping
    public ResponseEntity<ResReviewDTO> updateReview(@RequestBody Review review)
            throws IdInvalidException {
        return ResponseEntity.ok(reviewService.updateReview(review));
    }

    @GetMapping
    public ResponseEntity<ResultPaginationDTO> getAllReviews(
            @Filter Specification<Review> spec, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getAllReviews(spec, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResReviewDTO> getReviewById(@PathVariable("id") Long id) throws IdInvalidException {
        ResReviewDTO review = reviewService.getReviewById(id);
        if (review == null) {
            throw new IdInvalidException("Review not found with id: " + id);
        }
        return ResponseEntity.ok(review);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<ResReviewDTO>> getReviewsByCustomerId(@PathVariable("customerId") Long customerId) {
        return ResponseEntity.ok(reviewService.getReviewsByCustomerId(customerId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<ResReviewDTO>> getReviewsByOrderId(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(reviewService.getReviewsByOrderId(orderId));
    }

    @GetMapping("/target")
    public ResponseEntity<List<ResReviewDTO>> getReviewsByTarget(
            @RequestParam("reviewTarget") String reviewTarget,
            @RequestParam("targetName") String targetName) {
        return ResponseEntity.ok(reviewService.getReviewsByTarget(reviewTarget, targetName));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable("id") Long id) throws IdInvalidException {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
