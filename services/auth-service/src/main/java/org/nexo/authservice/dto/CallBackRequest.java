package org.nexo.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data

public class CallBackRequest {
    @NotBlank
    private String email;

    @NotBlank
    private String keycloakId;
}
