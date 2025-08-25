package org.nexo.postservice.service;

import org.nexo.postservice.dto.PostMediaDTO;
import org.nexo.postservice.dto.PostRequestDTO;
import org.nexo.postservice.grpc.PostMediaServiceProto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IPostMediaService {
    String savePostMedia(List<PostMediaDTO> postMediaDTO);
    List<PostMediaServiceProto.PostMediaRequestDTO> findPostMediasOfPost(Long id);

    void deletePostMedia(Long postMediaId);
}
