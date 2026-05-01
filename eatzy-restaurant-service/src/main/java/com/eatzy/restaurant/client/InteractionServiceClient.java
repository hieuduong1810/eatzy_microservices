package com.eatzy.restaurant.client;

import com.eatzy.restaurant.client.dto.BatchScoreRequestDTO;
import com.eatzy.restaurant.client.dto.BatchScoreResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "eatzy-interaction-service")
public interface InteractionServiceClient {

    @PostMapping("/api/v1/interaction/batch-scores")
    BatchScoreResponseDTO getBatchScores(@RequestBody BatchScoreRequestDTO request);

    @GetMapping("/api/v1/reviews/target")
    java.util.List<com.eatzy.restaurant.client.dto.ReviewClientDTO> getReviewsByTarget(
            @org.springframework.web.bind.annotation.RequestParam("reviewTarget") String reviewTarget,
            @org.springframework.web.bind.annotation.RequestParam("targetName") String targetName);
}
