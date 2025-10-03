package org.nexo.postservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryResponse {
    private Long userId;
    private String userName;
    private String avatarUrl;
    private List<Story> storyList;

    @Data
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Story {
        private Long storyId;
        private String mediaUrl;
        private String mediaType;
        private Boolean isLike;
        private LocalDateTime creatAt;
        private Boolean isActive;
        private Boolean isCloseFriend;
        private Boolean isSeen;
    }
}
