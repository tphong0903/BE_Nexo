package org.nexo.userservice.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class RecommendationResponseDTO {
    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("suggested_friend_ids")
    private List<Long> suggestedFriendIds;
}
