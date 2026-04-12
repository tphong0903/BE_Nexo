package org.nexo.authservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SeedUsersResponse {
    private int requested;
    private int succeeded;
    private int failed;
    private List<String> errors;
}
