package org.nexo.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncUserResponse {
    private String username;
    private String email;
    private String status;
    private String newKeycloakId;
}