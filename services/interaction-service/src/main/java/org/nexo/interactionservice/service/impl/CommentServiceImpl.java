package org.nexo.interactionservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.grpc.post.PostServiceOuterClass;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.interactionservice.dto.request.CommentDto;
import org.nexo.interactionservice.exception.CustomException;
import org.nexo.interactionservice.model.CommentModel;
import org.nexo.interactionservice.repository.ICommentRepository;
import org.nexo.interactionservice.service.ICommentMentionService;
import org.nexo.interactionservice.service.ICommentService;
import org.nexo.interactionservice.util.Enum.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements ICommentService {
    private final ICommentRepository commentRepository;
    private final ICommentMentionService commentMentionService;
    private final SecurityUtil securityUtil;
    private final UserGrpcClient userGrpcClient;
    private final PostGrpcClient postGrpcClient;

    @Override
    public String saveComment(CommentDto a) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto response = userGrpcClient.getUserByKeycloakId(keyloakId);
        if (response.getUserId() != a.getUserId())
            throw new CustomException("Dont allow", HttpStatus.BAD_REQUEST);
        CommentModel model;
        if (a.getId() != 0) {
            model = commentRepository.findById(a.getId()).orElseThrow(() -> new CustomException("Comment is not exist", HttpStatus.BAD_REQUEST));
        } else {
            model = CommentModel.builder()
                    .content(a.getContent())
                    .userId(a.getUserId())
                    .build();
        }

        if (a.getPostId() != 0) {
            model.setPostId(a.getPostId());
        } else {
            model.setReelId(a.getReelId());
        }

        if (a.getParentId() != null) {
            model.setParentComment(commentRepository.findById(a.getParentId()).orElseThrow(() -> new CustomException("Comment is not exist", HttpStatus.BAD_REQUEST)));
        }
        commentRepository.save(model);
        a.getListMentionUserId().forEach(i -> commentMentionService.addMentionComment(i, model));
        if (model.getPostId() != 0) {
            postGrpcClient.addCommentQuantityById(PostServiceOuterClass.GetPostRequest2.newBuilder().setId(model.getPostId()).setIsPost(true).setIsIncrease(true).build());
        } else {
            postGrpcClient.addCommentQuantityById(PostServiceOuterClass.GetPostRequest2.newBuilder().setId(model.getReelId()).setIsPost(false).setIsIncrease(true).build());
        }
        return "Success";
    }


    @Override
    public String deleteComment(Long id) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto response = userGrpcClient.getUserByKeycloakId(keyloakId);
        CommentModel model = commentRepository.findById(id).orElseThrow(() -> new CustomException("Comment is not exist", HttpStatus.BAD_REQUEST));
        if (response.getUserId() != model.getUserId())
            throw new CustomException("Dont allow", HttpStatus.BAD_REQUEST);
        commentRepository.delete(model);
        if (model.getPostId() != 0) {
            postGrpcClient.addCommentQuantityById(PostServiceOuterClass.GetPostRequest2.newBuilder().setId(model.getPostId()).setIsPost(true).setIsIncrease(false).build());
        } else {
            postGrpcClient.addCommentQuantityById(PostServiceOuterClass.GetPostRequest2.newBuilder().setId(model.getReelId()).setIsPost(false).setIsIncrease(false).build());
        }
        return "Success";
    }


}
