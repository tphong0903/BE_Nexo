package org.nexo.interactionservice.service;

import org.nexo.interactionservice.dto.response.SavedPostStatusCheckResponseDTO;
import org.nexo.interactionservice.dto.response.PageModelResponse;
import org.nexo.interactionservice.dto.response.SavedPostResponseDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ISavedPostService {

    SavedPostResponseDTO savePost(Long postId);

    PageModelResponse<SavedPostResponseDTO> getSavedPosts(Pageable pageable);

    SavedPostStatusCheckResponseDTO checkSavedPosts(List<Long> postIds);
}
