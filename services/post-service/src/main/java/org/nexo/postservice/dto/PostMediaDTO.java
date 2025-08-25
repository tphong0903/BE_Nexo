package org.nexo.postservice.dto;

import lombok.*;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostMediaDTO {
    private Long postId;
    private String mediaUrl;
    private String mediaType;
    private Integer mediaOrder;
}
