package org.nexo.postservice.dto;

import lombok.*;

import java.util.List;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionRequestDto {
    private Long id;
    private Long userId;
    private String collectionName;
    private List<Long> storyList;
}
