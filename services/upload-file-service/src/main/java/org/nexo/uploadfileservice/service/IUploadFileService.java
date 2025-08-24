package org.nexo.uploadfileservice.service;


import org.nexo.uploadfileservice.dto.PostMediaRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IUploadFileService {

    String upload(MultipartFile multipartFile);
    void savePostMedia(List<MultipartFile> files,String postId);
}
