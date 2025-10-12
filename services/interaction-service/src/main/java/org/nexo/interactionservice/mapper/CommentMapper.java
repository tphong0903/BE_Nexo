package org.nexo.interactionservice.mapper;


import lombok.RequiredArgsConstructor;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.interactionservice.dto.response.CommentResponse;
import org.nexo.interactionservice.dto.response.ListCommentResponse;
import org.nexo.interactionservice.model.CommentModel;
import org.nexo.interactionservice.repository.ICommentRepository;
import org.nexo.interactionservice.repository.ILikeCommentRepository;
import org.nexo.interactionservice.service.impl.UserGrpcClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CommentMapper {
    private final UserGrpcClient userGrpcClient;
    private final ICommentRepository commentRepository;
    private final ILikeCommentRepository likeCommentRepository;

    public CommentResponse toResponse(CommentModel model, Map<Long, UserServiceProto.UserDTOResponse2> userMap) {
        UserServiceProto.UserDTOResponse2 userDto = userMap.get(model.getUserId());
        Page<CommentModel> repliesPage = commentRepository.findByParentCommentId(
                model.getId(),
                PageRequest.of(0, 2, Sort.by("createdAt").descending())
        );

        List<CommentResponse> replyResponses = repliesPage.getContent().stream()
                .map(reply -> {
                    UserServiceProto.UserDTOResponse replyUser = userGrpcClient.getUserDTOById(reply.getUserId());
                    return CommentResponse.builder()
                            .id(reply.getId())
                            .userId(reply.getUserId())
                            .userName(replyUser != null ? replyUser.getUsername() : "Unknown")
                            .avatarUrl(replyUser != null ? replyUser.getAvatar() : null)
                            .content(reply.getContent())
                            .parentId(model.getId())
                            .creatAt(reply.getCreatedAt())
                            .hasMoreReplies(false)
                            .isLike(likeCommentRepository.findByCommentModelIdAndAndUserId(reply.getId(), model.getUserId()) != null)
                            .quantityLike(likeCommentRepository.countByCommentModel_Id(reply.getId()))
                            .build();
                })
                .toList();

        return CommentResponse.builder()
                .id(model.getId())
                .userId(model.getUserId())
                .userName(userDto.getUsername())
                .avatarUrl(userDto.getAvatar())
                .content(model.getContent())
                .quantityLike(likeCommentRepository.countByCommentModel_Id(model.getId()))
                .parentId(model.getParentComment() != null ? model.getParentComment().getPostId() : null)
                .responseChildList(replyResponses)
                .creatAt(model.getCreatedAt())
                .hasMoreReplies(repliesPage.getTotalElements() > 2)
                .isLike(likeCommentRepository.findByCommentModelIdAndAndUserId(model.getId(), model.getUserId()) != null)
                .build();
    }

    public ListCommentResponse toListResponse(Long postId, Page<CommentModel> commentsPage) {
        List<CommentModel> allComments = commentsPage.getContent();

        Set<Long> userIds = allComments.stream()
                .map(CommentModel::getUserId)
                .collect(Collectors.toSet());

        Map<Long, UserServiceProto.UserDTOResponse2> userMap = userGrpcClient.getUsersByIds(userIds);

        List<CommentResponse> responses = commentsPage.getContent()
                .stream()
                .map(comment -> toResponse(comment, userMap))
                .collect(Collectors.toList());

        return ListCommentResponse.builder()
                .postId(postId)
                .commentResponseList(responses)
                .pageNo(commentsPage.getNumber())
                .pageSize(commentsPage.getSize())
                .totalElements(commentsPage.getTotalElements())
                .totalPages(commentsPage.getTotalPages())
                .last(commentsPage.isLast())
                .build();
    }

}

