package org.nexo.interactionservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.grpc.post.PostServiceOuterClass;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.interactionservice.exception.CustomException;
import org.nexo.interactionservice.model.LikeCommentModel;
import org.nexo.interactionservice.model.LikeModel;
import org.nexo.interactionservice.repository.ICommentRepository;
import org.nexo.interactionservice.repository.ILikeCommentRepository;
import org.nexo.interactionservice.repository.ILikeRepository;
import org.nexo.interactionservice.service.ILikeService;
import org.nexo.interactionservice.util.Enum.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements ILikeService {
    private final ILikeCommentRepository likeCommentRepository;
    private final ICommentRepository commentRepository;
    private final ILikeRepository likeRepository;
    private final SecurityUtil securityUtil;
    private final UserGrpcClient userGrpcClient;
    private final PostGrpcClient postGrpcClient;

    @Override
    public String saveLikeComment(Long id) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto response = userGrpcClient.getUserByKeycloakId(keyloakId);
        LikeCommentModel likeCommentModel = likeCommentRepository.findByCommentModelIdAndAndUserId(id, response.getUserId());
        if (likeCommentModel != null) {
            likeCommentRepository.delete(likeCommentModel);
        } else {
            LikeCommentModel model = LikeCommentModel.builder()
                    .commentModel(commentRepository.findById(id).orElseThrow(() -> new CustomException("Comment is not exist", HttpStatus.BAD_REQUEST)))
                    .userId(response.getUserId())
                    .build();
            likeCommentRepository.save(model);
        }
        return "Success";
    }

    @Override
    public String saveLikePost(Long id) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto response = userGrpcClient.getUserByKeycloakId(keyloakId);
        LikeModel model = likeRepository.findByPostIdAndUserId(id, response.getUserId());
        if (model != null) {
            likeRepository.delete(model);
            postGrpcClient.addLikeQuantityById(PostServiceOuterClass.GetPostRequest2.newBuilder().setId(id).setIsPost(true).setIsIncrease(false).build());
        } else {
            model = LikeModel.builder()
                    .postId(id)
                    .userId(response.getUserId())
                    .build();
            likeRepository.save(model);
            postGrpcClient.addLikeQuantityById(PostServiceOuterClass.GetPostRequest2.newBuilder().setId(id).setIsPost(true).setIsIncrease(true).build());
        }
        return "Success";
    }

    @Override
    public String saveLikeReel(Long id) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto response = userGrpcClient.getUserByKeycloakId(keyloakId);
        LikeModel model = likeRepository.findByPostIdAndUserId(id, response.getUserId());
        if (model != null) {
            postGrpcClient.addLikeQuantityById(PostServiceOuterClass.GetPostRequest2.newBuilder().setId(id).setIsPost(false).setIsIncrease(false).build());
            likeRepository.delete(model);
        } else {
            model = LikeModel.builder()
                    .reelId(id)
                    .userId(response.getUserId())
                    .build();
            likeRepository.save(model);
            postGrpcClient.addLikeQuantityById(PostServiceOuterClass.GetPostRequest2.newBuilder().setId(id).setIsPost(false).setIsIncrease(true).build());
        }
        return "Success";
    }
}
