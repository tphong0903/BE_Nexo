package org.nexo.postservice.service;

import org.nexo.postservice.dto.PostRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IPostService {
    String savePost(PostRequestDTO postRequestDTO, List<MultipartFile> files);

    String saveReel(PostRequestDTO postRequestDTO, List<MultipartFile> files);

    String inactivePost(Long id);

    String inactiveReel(Long id);

    String deletePost(Long id);

    String deleteReel(Long id);
}
