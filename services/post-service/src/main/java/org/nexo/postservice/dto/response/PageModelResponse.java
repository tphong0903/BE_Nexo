package org.nexo.postservice.dto.response;

import lombok.*;

import java.util.List;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageModelResponse {
    private int pageNo;
    private int pageSize;
    private List<PostResponseDTO> postResponseDTOList;
}
