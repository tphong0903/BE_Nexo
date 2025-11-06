package org.nexo.messagingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nexo.messagingservice.enums.EMessageType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageMediaDTO {
    private Long id;
    private String mediaUrl;
    private EMessageType mediaType;
    private String thumbnailUrl;
    private String fileName;
    private Long fileSize;
}