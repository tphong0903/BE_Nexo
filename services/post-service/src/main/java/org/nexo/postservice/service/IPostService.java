package org.nexo.postservice.service;

import org.nexo.postservice.dto.PostRequestDTO;
import org.nexo.postservice.dto.response.PageModelResponse;
import org.nexo.postservice.dto.response.PostResponseDTO;
import org.nexo.postservice.dto.response.ReelResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IPostService {
    String savePost(PostRequestDTO postRequestDTO, List<MultipartFile> files);

    String saveReel(PostRequestDTO postRequestDTO, List<MultipartFile> files);

    String inactivePost(Long id);

    String inactiveReel(Long id);

    String deletePost(Long id);

    PageModelResponse getAllPostOfUser(Long id, int page, int limit);

    PageModelResponse getAllReelOfUser(Long id, int page, int limit);

    PostResponseDTO getPostById(Long id);

    PostResponseDTO getPostById2(Long id);

    ReelResponseDTO getReelById(Long id);

    ReelResponseDTO getReelById2(Long id);

    String deleteReel(Long id);
}
