package org.nexo.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
public class UserProfileDTOResponse {
    private Long id;
    private String username;
    private String fullName;
    private String avatar;
    private String bio;
    private Boolean isPrivate;
    private Boolean isFollowing;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean hasRequestedFollow;
    private Long followers;
    private Long following;

    public void setHasRequestedFollow(Boolean hasRequestedFollow) {
        if (Boolean.TRUE.equals(this.isPrivate)) {
            this.hasRequestedFollow = hasRequestedFollow;
        } else {
            this.hasRequestedFollow = null;
        }
    }
}
