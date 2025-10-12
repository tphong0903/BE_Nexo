package org.nexo.postservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CollectionDetailResponse {
    private Long id;
    private String collectionName;
    private List<StoryResponse.Story> stories;
    private LocalDateTime createdAt;
}