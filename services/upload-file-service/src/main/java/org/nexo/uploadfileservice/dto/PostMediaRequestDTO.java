package org.nexo.uploadfileservice.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostMediaRequestDTO {
    private String postId;
}
