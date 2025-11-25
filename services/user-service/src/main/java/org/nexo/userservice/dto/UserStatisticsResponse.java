package org.nexo.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsResponse {

    private Long userId;
    private String username;
    private Long postsCount;
    private Long interactionsCount;
    private Long newFollowersCount;
    private Long totalFollowersCount;
    private Long totalFollowingCount;
}
