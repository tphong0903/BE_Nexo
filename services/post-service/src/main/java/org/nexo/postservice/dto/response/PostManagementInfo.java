package org.nexo.postservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostManagementInfo {
    Long totalPost;
    Long quantityPost;
    Long quantityReel;
}
