package org.nexo.uploadfileservice.dto;

import lombok.*;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostMediaDTO {
    private String postId;
    private String mediaUrl;
    private String mediaType;
    private Integer order;
}
