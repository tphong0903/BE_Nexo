package org.nexo.postservice.service;

import org.nexo.postservice.dto.PostMediaDTO;
import org.nexo.postservice.dto.PostRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IPostMediaService {
    String savePostMedia(List<PostMediaDTO> postMediaDTO);
}
