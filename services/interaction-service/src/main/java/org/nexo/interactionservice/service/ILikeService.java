package org.nexo.interactionservice.service;

import org.nexo.interactionservice.dto.request.LikeCommentRequestDto;
import org.nexo.interactionservice.dto.request.LikePostRequestDto;


public interface ILikeService {
    String saveLikeComment(LikeCommentRequestDto dto);

    String saveLikePost(LikePostRequestDto dto);
}
