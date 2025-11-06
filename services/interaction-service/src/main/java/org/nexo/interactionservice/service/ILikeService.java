package org.nexo.interactionservice.service;

import org.nexo.interactionservice.dto.response.FolloweeDTO;
import org.nexo.interactionservice.dto.response.PageModelResponse;

public interface ILikeService {
    String saveLikeComment(Long id);

    String saveLikePost(Long id);

    PageModelResponse<FolloweeDTO> getLikePostDetail(Long id, int pageNo, int pageSize);

    PageModelResponse<FolloweeDTO> getLikeReelDetail(Long id, int pageNo, int pageSize);

    String saveLikeReel(Long id);
}
