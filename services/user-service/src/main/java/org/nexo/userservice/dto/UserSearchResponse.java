package org.nexo.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchResponse {
    private List<UserSearchDocument> users;
    private Integer totalHits;
    private Integer limit;
    private Integer offset;
    private Long processingTimeMs;
    private String query;
}
