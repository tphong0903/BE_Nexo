package org.nexo.postservice.dto;

import lombok.*;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTagDTO {
    private Long userId;
    private String userName;
}
