package org.nexo.postservice.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.nexo.postservice.util.Enum.EVisibilityPost;

import java.util.List;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostRequestDTO {
    private Long postID;
    private Long userId;
    private String caption;
    private String visibility;
    private String tag;
    private List<String> mediaUrl;
}
