package org.nexo.uploadfileservice.dto;

import lombok.*;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostMediaDTO2 {
    private Long postId;
    private String mediaUrl;
    private String mediaType;
    private Integer mediaOrder;
}
