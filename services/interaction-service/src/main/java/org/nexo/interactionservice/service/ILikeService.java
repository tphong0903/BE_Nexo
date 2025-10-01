package org.nexo.interactionservice.service;

public interface ILikeService {
    String saveLikeComment(Long id);

    String saveLikePost(Long id);

    String saveLikeReel(Long id);
}
