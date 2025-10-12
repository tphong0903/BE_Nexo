package org.nexo.postservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CollectionSummaryResponse {
    private Long id;
    private String collectionName;
    private String mediaUrl;
    private LocalDateTime createdAt;
}