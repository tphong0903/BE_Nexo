package org.nexo.userservice.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchResponseAdmin {
    private List<UserResponseAdmin> users;
    private Integer totalHits;
    private Integer limit;
    private Integer offset;
    private Long processingTimeMs;
    private String query;
}
