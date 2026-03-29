package org.nexo.uploadfileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadResult {
    private String url;
    private String publicId;
    private String resourceType;
}