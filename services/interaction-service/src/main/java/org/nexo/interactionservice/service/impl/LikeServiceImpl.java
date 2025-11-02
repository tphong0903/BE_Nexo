package org.nexo.interactionservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.grpc.post.PostServiceOuterClass;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.interactionservice.dto.MessageDTO;
import org.nexo.interactionservice.dto.response.FolloweeDTO;
import org.nexo.interactionservice.dto.response.PageModelResponse;
import org.nexo.interactionservice.exception.CustomException;
import org.nexo.interactionservice.mapper.FolloweeMapper;
import org.nexo.interactionservice.model.CommentModel;
import org.nexo.interactionservice.model.LikeCommentModel;
import org.nexo.interactionservice.model.LikeModel;
import org.nexo.interactionservice.repository.ICommentRepository;
import org.nexo.interactionservice.repository.ILikeCommentRepository;
import org.nexo.interactionservice.repository.ILikeRepository;
import org.nexo.interactionservice.service.ILikeService;
import org.nexo.interactionservice.util.Enum.ENotificationType;
import org.nexo.interactionservice.util.Enum.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements ILikeService {
    private final ILikeCommentRepository likeCommentRepository;
    private final ICommentRepository commentRepository;
    private final ILikeRepository likeRepository;
    private final SecurityUtil securityUtil;
    private final UserGrpcClient userGrpcClient;
    private final PostGrpcClient postGrpcClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public String saveLikeComment(Long id) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto response = userGrpcClient.getUserByKeycloakId(keyloakId);
        LikeCommentModel likeCommentModel = likeCommentRepository.findByCommentModelIdAndAndUserId(id, response.getUserId());
        if (likeCommentModel != null) {
            likeCommentRepository.delete(likeCommentModel);
        } else {
            CommentModel commentModel = commentRepository.findById(id).orElseThrow(() -> new CustomException("Comment is not exist", HttpStatus.BAD_REQUEST));
            LikeCommentModel model = LikeCommentModel.builder()
                    .commentModel(commentModel)
                    .userId(response.getUserId())
                    .build();
            likeCommentRepository.save(model);

            String targetUrl = commentModel.getPostId() != null ? "/posts/" + commentModel.getPostId() : "/reels/" + commentModel.getReelId();
            MessageDTO messageDTO = MessageDTO.builder()
                    .actorId(response.getUserId())
                    .recipientId(commentModel.getUserId())
                    .notificationType(String.valueOf(ENotificationType.LIKE_COMMENT))
                    .targetUrl(targetUrl)
                    .build();
            kafkaTemplate.send("notification", messageDTO);
        }
        return "Success";
    }

    @Override
    public String saveLikePost(Long id) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto response = userGrpcClient.getUserByKeycloakId(keyloakId);
        LikeModel model = likeRepository.findByPostIdAndUserId(id, response.getUserId());
        try {
            if (model != null) {
                likeRepository.delete(model);
                postGrpcClient.addLikeQuantityById(id, true, false);
            } else {
                model = LikeModel.builder()
                        .postId(id)
                        .userId(response.getUserId())
                        .build();
                likeRepository.save(model);
                postGrpcClient.addLikeQuantityById(id, true, true);
                PostServiceOuterClass.PostResponse postResponse = postGrpcClient.getPostById(id);
                MessageDTO messageDTO = MessageDTO.builder()
                        .actorId(response.getUserId())
                        .recipientId(postResponse.getUserId())
                        .notificationType(String.valueOf(ENotificationType.LIKE_POST))
                        .targetUrl("/posts/" + id)
                        .build();
                kafkaTemplate.send("notification", messageDTO);
            }
        } catch (Exception e) {
            throw new CustomException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return "Success";
    }

    @Override
    public PageModelResponse<FolloweeDTO> getLikePostDetail(Long id, int pageNo, int pageSize) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto response = userGrpcClient.getUserByKeycloakId(keyloakId);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
        Page<LikeModel> likePage = likeRepository.findByPostId(id, pageable);
        List<Long> listLikeUsersId = likePage.getContent().stream()
                .map(LikeModel::getUserId)
                .toList();

        List<UserServiceProto.UserDTOResponse3> listUserData =
                userGrpcClient.getLikeUsersByIds(response.getUserId(), listLikeUsersId);

        List<FolloweeDTO> followeeDTOs = FolloweeMapper.toFolloweeDTOList(listUserData);


        boolean hasLiked = likeRepository.existsByPostIdAndUserId(id, response.getUserId());
        if (hasLiked) {
            UserServiceProto.UserDTOResponse currentUser =
                    userGrpcClient.getUserDTOById(response.getUserId());

            FolloweeDTO currentUserDTO = FolloweeMapper.toFolloweeDTO2(currentUser);

            followeeDTOs.removeIf(dto -> dto.getUserId().equals(currentUserDTO.getUserId()));

            followeeDTOs.add(0, currentUserDTO);
        }

        PageModelResponse<FolloweeDTO> pageResponse = new PageModelResponse<>();
        pageResponse.setContent(followeeDTOs);
        pageResponse.setPageNo(likePage.getNumber());
        pageResponse.setPageSize(likePage.getSize());
        pageResponse.setTotalElements(likePage.getTotalElements());
        pageResponse.setTotalPages(likePage.getTotalPages());
        pageResponse.setLast(likePage.isLast());

        return pageResponse;
    }

    @Override
    public PageModelResponse<FolloweeDTO> getLikeReelDetail(Long id, int pageNo, int pageSize) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto response = userGrpcClient.getUserByKeycloakId(keyloakId);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
        Page<LikeModel> likePage = likeRepository.findByReelId(id, pageable);
        List<Long> listLikeUsersId = likePage.getContent().stream()
                .map(LikeModel::getUserId)
                .toList();

        List<UserServiceProto.UserDTOResponse3> listUserData =
                userGrpcClient.getLikeUsersByIds(response.getUserId(), listLikeUsersId);

        List<FolloweeDTO> followeeDTOs = FolloweeMapper.toFolloweeDTOList(listUserData);


        boolean hasLiked = likeRepository.existsByPostIdAndUserId(id, response.getUserId());
        if (hasLiked) {
            UserServiceProto.UserDTOResponse currentUser =
                    userGrpcClient.getUserDTOById(response.getUserId());

            FolloweeDTO currentUserDTO = FolloweeMapper.toFolloweeDTO2(currentUser);

            followeeDTOs.removeIf(dto -> dto.getUserId().equals(currentUserDTO.getUserId()));

            followeeDTOs.add(0, currentUserDTO);
        }

        PageModelResponse<FolloweeDTO> pageResponse = new PageModelResponse<>();
        pageResponse.setContent(followeeDTOs);
        pageResponse.setPageNo(likePage.getNumber());
        pageResponse.setPageSize(likePage.getSize());
        pageResponse.setTotalElements(likePage.getTotalElements());
        pageResponse.setTotalPages(likePage.getTotalPages());
        pageResponse.setLast(likePage.isLast());

        return pageResponse;
    }

    @Override
    public String saveLikeReel(Long id) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto response = userGrpcClient.getUserByKeycloakId(keyloakId);
        LikeModel model = likeRepository.findByReelIdAndUserId(id, response.getUserId());
        try {
            if (model != null) {
                postGrpcClient.addLikeQuantityById(id, false, false);
                likeRepository.delete(model);
            } else {
                model = LikeModel.builder()
                        .reelId(id)
                        .userId(response.getUserId())
                        .build();
                likeRepository.save(model);
                postGrpcClient.addLikeQuantityById(id, false, true);
                PostServiceOuterClass.ReelResponse reelResponse = postGrpcClient.getReelById(id);
                MessageDTO messageDTO = MessageDTO.builder()
                        .actorId(response.getUserId())
                        .recipientId(reelResponse.getUserId())
                        .notificationType(String.valueOf(ENotificationType.LIKE_REEL))
                        .targetUrl("/reels/" + id)
                        .build();
                kafkaTemplate.send("notification", messageDTO);
            }
        } catch (Exception e) {
            throw new CustomException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return "Success";
    }
}
