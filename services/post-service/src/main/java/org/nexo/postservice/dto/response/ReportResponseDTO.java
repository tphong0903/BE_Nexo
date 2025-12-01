package org.nexo.postservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponseDTO {
    private Long id;
    private String reason;
    private Long userId;
    private Long postId;
    private String detail;
    private String reportStatus;
    private String reporterName;
    private String ownerPostName;
    private String reporterAvatarUrl;
    private String ownerPostAvatarUrl;
    private String content;
    private LocalDateTime createdAt;
    private List<String> mediaUrls;
    private String caption;
    private Boolean isActive;
    private String note;
}
