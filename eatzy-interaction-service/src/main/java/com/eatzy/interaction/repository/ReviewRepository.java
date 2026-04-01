package com.eatzy.interaction.repository;

import com.eatzy.interaction.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {
    List<Review> findByCustomerId(Long customerId);

    List<Review> findByOrderId(Long orderId);

    List<Review> findByOrderIdAndReviewTarget(Long orderId, String reviewTarget);

    List<Review> findByReviewTargetAndTargetName(String reviewTarget, String targetName);

    // Report queries
    List<Review> findByReviewTargetAndTargetNameOrderByCreatedAtDesc(String reviewTarget, String targetName);

    Long countByReviewTargetAndTargetNameAndRating(String reviewTarget, String targetName, Integer rating);

    Long countByReviewTargetAndTargetNameAndReplyIsNotNull(String reviewTarget, String targetName);
}
