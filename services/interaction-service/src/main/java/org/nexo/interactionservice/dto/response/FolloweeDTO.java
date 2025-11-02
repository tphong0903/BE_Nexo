package org.nexo.interactionservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolloweeDTO {
    private Long userId;
    private String userName;
    private String fullName;
    private String avatar;
    private Boolean isFollowing;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean hasRequestedFollow;
}