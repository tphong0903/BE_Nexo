package org.nexo.interactionservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.grpc.post.PostServiceOuterClass;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.interactionservice.dto.MessageDTO;
import org.nexo.interactionservice.dto.request.CommentDto;
import org.nexo.interactionservice.dto.response.ListCommentResponse;
import org.nexo.interactionservice.exception.CustomException;
import org.nexo.interactionservice.mapper.CommentMapper;
import org.nexo.interactionservice.model.CommentModel;
import org.nexo.interactionservice.repository.ICommentRepository;
import org.nexo.interactionservice.service.ICommentMentionService;
import org.nexo.interactionservice.service.ICommentService;
import org.nexo.interactionservice.util.Enum.ENotificationType;
import org.nexo.interactionservice.util.Enum.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements ICommentService {
    private final ICommentRepository commentRepository;
    private final ICommentMentionService commentMentionService;
    private final SecurityUtil securityUtil;
    private final UserGrpcClient userGrpcClient;
    private final PostGrpcClient postGrpcClient;
    private final CommentMapper commentMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public String saveComment(CommentDto a) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto response = userGrpcClient.getUserByKeycloakId(keyloakId);
        if (response.getUserId() != a.getUserId())
            throw new CustomException("Dont allow", HttpStatus.BAD_REQUEST);
        CommentModel model;

        Boolean isAdd = false;
        if (a.getId() != 0) {
            model = commentRepository.findById(a.getId()).orElseThrow(() -> new CustomException("Comment is not exist", HttpStatus.BAD_REQUEST));
            model.setContent(a.getContent());
        } else {
            isAdd = true;
            model = CommentModel.builder()
                    .content(a.getContent())
                    .userId(a.getUserId())
                    .build();
        }

        if (a.getPostId() != null && a.getPostId() != 0) {
            model.setPostId(a.getPostId());
        } else {
            model.setReelId(a.getReelId());
        }

        if (a.getParentId() != 0) {
            model.setParentComment(commentRepository.findById(a.getParentId()).orElseThrow(() -> new CustomException("Comment is not exist", HttpStatus.BAD_REQUEST)));
        }
        commentRepository.save(model);

        if (isAdd) {
            a.getListMentionUserId().forEach(i -> commentMentionService.addMentionComment(i, model));
            String notificationType = "";
            Long id = 0L;
            String url = "";
            if (a.getPostId() != null && a.getPostId() != 0) {
                postGrpcClient.addCommentQuantityById(model.getPostId(), true, true);
                notificationType = String.valueOf(ENotificationType.COMMENT_POST);
                id = postGrpcClient.getPostById(model.getPostId()).getUserId();
                url = "/posts/" + model.getPostId();
            } else {
                postGrpcClient.addCommentQuantityById(model.getReelId(), false, true);
                notificationType = String.valueOf(ENotificationType.COMMENT_REEL);
                id = postGrpcClient.getReelById(model.getReelId()).getUserId();
                url = "/reels/" + model.getReelId();
            }
            MessageDTO messageDTO = MessageDTO.builder()
                    .actorId(response.getUserId())
                    .recipientId(id)
                    .notificationType(notificationType)
                    .targetUrl(url)
                    .build();
            kafkaTemplate.send("notification", messageDTO);
        }

        return "Success";
    }


    @Override
    public String deleteComment(Long id) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto response = userGrpcClient.getUserByKeycloakId(keyloakId);
        CommentModel model = commentRepository.findById(id).orElseThrow(() -> new CustomException("Comment is not exist", HttpStatus.BAD_REQUEST));
        Long ownerId = 0L;
        if (model.getReelId() != 0) {
            ownerId = postGrpcClient.getReelById(model.getReelId()).getUserId();
        } else {
            ownerId = postGrpcClient.getPostById(model.getPostId()).getUserId();
        }
        if (response.getUserId() != model.getUserId() || ownerId != response.getUserId())
            throw new CustomException("Dont allow", HttpStatus.BAD_REQUEST);
        commentRepository.delete(model);
        if (model.getPostId() != 0) {
            postGrpcClient.addCommentQuantityById(model.getPostId(), true, false);
        } else {
            postGrpcClient.addCommentQuantityById(model.getReelId(), false, false);
        }
        return "Success";
    }

    @Override
    public ListCommentResponse getCommentOfPost(Long postId, int pageNo, int pageSize) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto response = userGrpcClient.getUserByKeycloakId(keyloakId);
        PostServiceOuterClass.PostResponse model = postGrpcClient.getPostById(postId);

        boolean isAllow = false;
        if (model.getUserId() == response.getUserId()) {
            isAllow = true;
        } else {
            UserServiceProto.CheckFollowResponse response2 = userGrpcClient.checkFollow(response.getUserId(), model.getUserId());
            if (!response2.getIsPrivate() || response2.getIsFollow()) {
                isAllow = true;
            }
        }
        if (!isAllow)
            throw new CustomException("Dont allow to get story", HttpStatus.BAD_REQUEST);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());

        Page<CommentModel> commentsPage = commentRepository.findByPostIdAndParentComment(postId, pageable, null);
        return commentMapper.toListResponse(postId, commentsPage);

    }

    @Override
    public ListCommentResponse getCommentOfReel(Long reelId, int pageNo, int pageSize) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto response = userGrpcClient.getUserByKeycloakId(keyloakId);

        PostServiceOuterClass.ReelResponse model = postGrpcClient.getReelById(reelId);

        boolean isAllow = false;
        if (model.getUserId() == response.getUserId()) {
            isAllow = true;
        } else {
            UserServiceProto.CheckFollowResponse response2 = userGrpcClient.checkFollow(response.getUserId(), model.getUserId());
            if (!response2.getIsPrivate() || response2.getIsFollow()) {
                isAllow = true;
            }
        }
        if (!isAllow)
            throw new CustomException("Dont allow to get story", HttpStatus.BAD_REQUEST);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());

        Page<CommentModel> commentsPage = commentRepository.findByReelId(reelId, pageable);
        return commentMapper.toListResponse(reelId, commentsPage);
    }

    @Override
    public ListCommentResponse getReplies(Long commentId, int pageNo, int pageSize) {

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
        Page<CommentModel> repliesPage = commentRepository.findByParentCommentId(commentId, pageable);

        CommentModel model = commentRepository.findById(commentId).orElseThrow(() -> new CustomException("Comment is not exist", HttpStatus.BAD_REQUEST));

        Long id;
        if (model.getPostId() != null)
            id = model.getPostId();
        else
            id = model.getReelId();

        return commentMapper.toListResponse(id, repliesPage);
    }


}
