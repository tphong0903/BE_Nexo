package org.nexo.userservice.dto;

import org.nexo.userservice.enums.EAccountStatus;
import org.nexo.userservice.enums.ERole;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResonspeAdmin {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("role")
    private ERole role;

    @JsonProperty("account_status")
    private EAccountStatus accountStatus;

    @JsonProperty("violation_count")
    private Integer violationCount;
}
