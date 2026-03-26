package org.nexo.interactionservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeServiceImpl implements ILikeService {

    private final ILikeCommentRepository likeCommentRepository;
    private final ICommentRepository commentRepository;
    private final ILikeRepository likeRepository;
    private final SecurityUtil securityUtil;
    private final UserGrpcClient userGrpcClient;
    private final PostGrpcClient postGrpcClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public String saveLikeComment(Long id) {
        Long currentUserId = securityUtil.getUserIdFromToken();
        LikeCommentModel likeCommentModel = likeCommentRepository.findByCommentModelIdAndAndUserId(id, currentUserId);

        if (likeCommentModel != null) {
            likeCommentRepository.delete(likeCommentModel);
            redisTemplate.opsForSet().remove("comment:" + id + ":likes", String.valueOf(currentUserId));
        } else {
            CommentModel commentModel = commentRepository.findById(id)
                    .orElseThrow(() -> new CustomException("Comment does not exist", HttpStatus.BAD_REQUEST));

            LikeCommentModel model = LikeCommentModel.builder()
                    .commentModel(commentModel)
                    .userId(currentUserId)
                    .build();
            likeCommentRepository.save(model);

            redisTemplate.opsForSet().add("comment:" + id + ":likes", String.valueOf(currentUserId));

            if (!currentUserId.equals(commentModel.getUserId())) {
                String targetUrl = commentModel.getPostId() != null ? "/posts/" + commentModel.getPostId() : "/reels/" + commentModel.getReelId();
                sendNotification(currentUserId, commentModel.getUserId(), ENotificationType.LIKE_COMMENT, targetUrl);
            }
        }
        return "Success";
    }

    @Override
    @Transactional
    public String saveLikePost(Long id) {
        Long currentUserId = securityUtil.getUserIdFromToken();
        LikeModel model = likeRepository.findByPostIdAndUserId(id, currentUserId);

        if (model != null) {
            likeRepository.delete(model);
            postGrpcClient.addLikeQuantityById(id, true, false);

            redisTemplate.opsForSet().remove("post:" + id + ":likes", String.valueOf(currentUserId));
            redisTemplate.opsForValue().decrement("global:likes:total");
            redisTemplate.opsForValue().decrement("user:" + currentUserId + ":likes:total");

        } else {
            model = LikeModel.builder().postId(id).userId(currentUserId).build();
            likeRepository.save(model);
            postGrpcClient.addLikeQuantityById(id, true, true);

            redisTemplate.opsForSet().add("post:" + id + ":likes", String.valueOf(currentUserId));
            redisTemplate.opsForValue().increment("global:likes:total");
            redisTemplate.opsForValue().increment("user:" + currentUserId + ":likes:total");

            PostServiceOuterClass.PostResponse postResponse = postGrpcClient.getPostById(id);

            if (!currentUserId.equals(postResponse.getUserId())) {
                sendNotification(currentUserId, postResponse.getUserId(), ENotificationType.LIKE_POST, "/posts/" + id);
            }
        }
        return "Success";
    }

    @Override
    @Transactional
    public String saveLikeReel(Long id) {
        Long currentUserId = securityUtil.getUserIdFromToken();
        LikeModel model = likeRepository.findByReelIdAndUserId(id, currentUserId);

        if (model != null) {
            likeRepository.delete(model);
            postGrpcClient.addLikeQuantityById(id, false, false);

            redisTemplate.opsForSet().remove("reel:" + id + ":likes", String.valueOf(currentUserId));
            redisTemplate.opsForValue().decrement("global:likes:total");
            redisTemplate.opsForValue().decrement("user:" + currentUserId + ":likes:total");

        } else {
            model = LikeModel.builder().reelId(id).userId(currentUserId).build();
            likeRepository.save(model);
            postGrpcClient.addLikeQuantityById(id, false, true);

            redisTemplate.opsForSet().add("reel:" + id + ":likes", String.valueOf(currentUserId));
            redisTemplate.opsForValue().increment("global:likes:total");
            redisTemplate.opsForValue().increment("user:" + currentUserId + ":likes:total");

            PostServiceOuterClass.ReelResponse reelResponse = postGrpcClient.getReelById(id);

            if (!currentUserId.equals(reelResponse.getUserId())) {
                sendNotification(currentUserId, reelResponse.getUserId(), ENotificationType.LIKE_REEL, "/reels/" + id);
            }
        }
        return "Success";
    }


    @Override
    public PageModelResponse<FolloweeDTO> getLikePostDetail(Long id, int pageNo, int pageSize) {
        Long currentUserId = securityUtil.getUserIdFromToken();
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());

        Page<LikeModel> likePage = likeRepository.findByPostId(id, pageable);
        List<Long> likedUserIds = likePage.getContent().stream().map(LikeModel::getUserId).toList();

        boolean hasLiked = likeRepository.existsByPostIdAndUserId(id, currentUserId);

        return buildLikeDetailResponse(likePage, likedUserIds, currentUserId, hasLiked);
    }

    @Override
    public PageModelResponse<FolloweeDTO> getLikeReelDetail(Long id, int pageNo, int pageSize) {
        Long currentUserId = securityUtil.getUserIdFromToken();
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());

        Page<LikeModel> likePage = likeRepository.findByReelId(id, pageable);
        List<Long> likedUserIds = likePage.getContent().stream().map(LikeModel::getUserId).toList();

        boolean hasLiked = likeRepository.existsByReelIdAndUserId(id, currentUserId);
        return buildLikeDetailResponse(likePage, likedUserIds, currentUserId, hasLiked);
    }

    @Override
    public PageModelResponse<FolloweeDTO> getLikeCommentDetail(Long id, int pageNo, int pageSize) {
        Long currentUserId = securityUtil.getUserIdFromToken();
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());

        Page<LikeCommentModel> likePage = likeCommentRepository.findByCommentModelId(id, pageable);
        List<Long> likedUserIds = likePage.getContent().stream().map(LikeCommentModel::getUserId).toList();

        boolean hasLiked = likeCommentRepository.existsByCommentModelIdAndUserId(id, currentUserId);
        return buildLikeDetailResponse(likePage, likedUserIds, currentUserId, hasLiked);
    }


    private void sendNotification(Long actorId, Long recipientId, ENotificationType type, String targetUrl) {
        MessageDTO messageDTO = MessageDTO.builder()
                .actorId(actorId)
                .recipientId(recipientId)
                .notificationType(type.name())
                .targetUrl(targetUrl)
                .build();
        kafkaTemplate.send("notification", messageDTO);
    }

    private PageModelResponse<FolloweeDTO> buildLikeDetailResponse(
            Page<?> pageData, List<Long> likedUserIds, Long currentUserId, boolean hasLikedCurrent) {

        if (likedUserIds.isEmpty()) {
            return buildEmptyPageResponse(pageData.getNumber(), pageData.getSize());
        }

        List<UserServiceProto.UserDTOResponse3> listUserData =
                userGrpcClient.getLikeUsersByIds(currentUserId, likedUserIds);

        List<FolloweeDTO> followeeDTOs = new ArrayList<>(FolloweeMapper.toFolloweeDTOList(listUserData));

        if (hasLikedCurrent) {
            UserServiceProto.UserDTOResponse currentUser = userGrpcClient.getUserDTOById(currentUserId);
            FolloweeDTO currentUserDTO = FolloweeMapper.toFolloweeDTO2(currentUser);

            followeeDTOs.removeIf(dto -> dto.getUserId().equals(currentUserDTO.getUserId()));
            followeeDTOs.add(0, currentUserDTO);
        }

        PageModelResponse<FolloweeDTO> pageResponse = new PageModelResponse<>();
        pageResponse.setContent(followeeDTOs);
        pageResponse.setPageNo(pageData.getNumber());
        pageResponse.setPageSize(pageData.getSize());
        pageResponse.setTotalElements(pageData.getTotalElements());
        pageResponse.setTotalPages(pageData.getTotalPages());
        pageResponse.setLast(pageData.isLast());

        return pageResponse;
    }

    private PageModelResponse<FolloweeDTO> buildEmptyPageResponse(int pageNo, int pageSize) {
        PageModelResponse<FolloweeDTO> response = new PageModelResponse<>();
        response.setContent(new ArrayList<>());
        response.setPageNo(pageNo);
        response.setPageSize(pageSize);
        response.setTotalElements(0L);
        response.setTotalPages(0);
        response.setLast(true);
        return response;
    }
}