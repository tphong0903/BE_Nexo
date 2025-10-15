package org.nexo.postservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewDetailStoryResponse {
    private String userName;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private Boolean isLike;
}
