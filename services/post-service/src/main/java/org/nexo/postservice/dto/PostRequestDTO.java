package org.nexo.postservice.dto;

import lombok.*;

import java.util.List;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostRequestDTO {
    private Long postId;
    private Long userId;
    private String caption;
    private String visibility;
    private String tag;
    private List<String> mediaUrl;
}
