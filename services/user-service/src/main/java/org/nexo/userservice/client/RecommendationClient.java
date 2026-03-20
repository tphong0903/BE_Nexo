package org.nexo.userservice.client;

import org.nexo.userservice.dto.RecommendationResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "recommendation-service", url = "${recommendation.service.url:http://localhost:8000}")
public interface RecommendationClient {

    @GetMapping("/recommend/{userId}")
    RecommendationResponseDTO recommendFriends(@PathVariable("userId") Long userId);
}
