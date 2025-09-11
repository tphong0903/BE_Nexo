package org.nexo.postservice.dto;

import lombok.*;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryRequestDto {
    private Long storyId;
    private Long userId;
    private Boolean isClosedFriend;
    private Boolean isArchive;
    private String mediaUrl;
}
