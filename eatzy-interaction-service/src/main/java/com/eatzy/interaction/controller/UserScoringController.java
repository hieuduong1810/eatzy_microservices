package com.eatzy.interaction.controller;

import com.eatzy.interaction.dto.request.BatchScoreRequestDTO;
import com.eatzy.interaction.dto.request.ScoringEventDTO;
import com.eatzy.interaction.dto.response.BatchScoreResponseDTO;
import com.eatzy.interaction.service.UserScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interaction")
@RequiredArgsConstructor
public class UserScoringController {

    private final UserScoringService userScoringService;

    @PostMapping("/batch-scores")
    public ResponseEntity<BatchScoreResponseDTO> getBatchScores(@RequestBody BatchScoreRequestDTO request) {
        return ResponseEntity.ok(userScoringService.getBatchScores(request));
    }
    
    @PostMapping("/track/search")
    public ResponseEntity<Void> trackSearch(@RequestBody ScoringEventDTO event) {
        userScoringService.trackSearchRestaurantByNameAndClick(event);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/track/view")
    public ResponseEntity<Void> trackView(@RequestBody ScoringEventDTO event) {
        userScoringService.trackViewRestaurantDetails(event);
        return ResponseEntity.ok().build();
    }
}
