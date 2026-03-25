package org.nexo.postservice.service.GrpcServiceImpl;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.nexo.grpc.post.PostServiceGrpc;
import org.nexo.grpc.post.PostServiceOuterClass;
import org.nexo.postservice.dto.response.PostResponseDTO;
import org.nexo.postservice.dto.response.ReelResponseDTO;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.model.PostModel;
import org.nexo.postservice.model.ReelModel;
import org.nexo.postservice.repository.IPostRepository;
import org.nexo.postservice.repository.IReelRepository;
import org.nexo.postservice.service.IPostService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class PostGrpcServiceImpl extends PostServiceGrpc.PostServiceImplBase {
    private final IPostService postService;
    private final IPostRepository postRepository;
    private final IReelRepository reelRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void getPostsByIds(PostServiceOuterClass.GetPostsByIdsRequest request,
                              StreamObserver<PostServiceOuterClass.GetPostsByIdsResponse> responseObserver) {
        try {
            List<PostResponseDTO> postDTOs = postService.getPostsByIds(request.getPostIdsList(), request.getUserId());

            List<PostServiceOuterClass.PostResponse> responses = postDTOs.stream()
                    .filter(Objects::nonNull)
                    .map(this::mapToPostResponse)
                    .toList();

            responseObserver.onNext(PostServiceOuterClass.GetPostsByIdsResponse.newBuilder().addAllPosts(responses).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            handleGrpcError("getPostsByIds", e, responseObserver);
        }
    }

    @Override
    public void getReelsByIds(PostServiceOuterClass.GetPostsByIdsRequest request,
                              StreamObserver<PostServiceOuterClass.GetReelsByIdsResponse> responseObserver) {
        try {
            List<ReelResponseDTO> reelDTOs = postService.getReelsByIds(request.getPostIdsList(), request.getUserId());

            List<PostServiceOuterClass.ReelResponse> responses = reelDTOs.stream()
                    .filter(Objects::nonNull)
                    .map(this::mapToReelResponse)
                    .toList();

            responseObserver.onNext(PostServiceOuterClass.GetReelsByIdsResponse.newBuilder().addAllReels(responses).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            handleGrpcError("getReelsByIds", e, responseObserver);
        }
    }

    @Override
    public void getPostById(PostServiceOuterClass.GetPostRequest request,
                            StreamObserver<PostServiceOuterClass.PostResponse> responseObserver) {
        try {
            PostResponseDTO dto = postService.getPostById(request.getId());
            responseObserver.onNext(mapToPostResponse(dto));
            responseObserver.onCompleted();
        } catch (Exception e) {
            handleGrpcError("getPostById", e, responseObserver);
        }
    }

    @Override
    public void getReelById(PostServiceOuterClass.GetPostRequest request,
                            StreamObserver<PostServiceOuterClass.ReelResponse> responseObserver) {
        try {
            ReelResponseDTO dto = postService.getReelById(request.getId());
            responseObserver.onNext(mapToReelResponse(dto));
            responseObserver.onCompleted();
        } catch (Exception e) {
            handleGrpcError("getReelById", e, responseObserver);
        }
    }

    @Override
    public void addLikeQuantityById(PostServiceOuterClass.GetPostRequest2 request,
                                    StreamObserver<PostServiceOuterClass.PostMessageResponse> responseObserver) {
        try {
            updateRedisCounter("likes", request.getIsPost(), request.getId(), request.getIsIncrease());

            int count = request.getIsIncrease() ? 1 : -1;
            if (request.getIsPost()) {
                postRepository.updateLikeQuantity(request.getId(), count);
            } else {
                reelRepository.updateLikeQuantity(request.getId(), count);
            }

            sendSuccessMessage(responseObserver);
        } catch (Exception e) {
            handleGrpcError("addLikeQuantityById", e, responseObserver);
        }
    }

    @Override
    public void addCommentQuantityById(PostServiceOuterClass.GetPostRequest2 request,
                                       StreamObserver<PostServiceOuterClass.PostMessageResponse> responseObserver) {
        try {
            updateRedisCounter("comments", request.getIsPost(), request.getId(), request.getIsIncrease());

            int count = request.getIsIncrease() ? 1 : -1;
            if (request.getIsPost()) {
                postRepository.updateCommentQuantity(request.getId(), count);
            } else {
                reelRepository.updateCommentQuantity(request.getId(), count);
            }

            sendSuccessMessage(responseObserver);
        } catch (Exception e) {
            handleGrpcError("addCommentQuantityById", e, responseObserver);
        }
    }

    @Override
    public void getUserPostsCount(PostServiceOuterClass.GetUserPostsCountRequest request,
                                  StreamObserver<PostServiceOuterClass.GetUserPostsCountResponse> responseObserver) {
        try {
            Long userId = request.getUserId();
            long totalCount = postRepository.countByUserIdAndIsActive(userId, true)
                    + reelRepository.countByUserIdAndIsActive(userId, true);

            responseObserver.onNext(PostServiceOuterClass.GetUserPostsCountResponse.newBuilder().setPostsCount(totalCount).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            handleGrpcError("getUserPostsCount", e, responseObserver);
        }
    }


    private PostServiceOuterClass.PostResponse mapToPostResponse(PostResponseDTO dto) {
        if (dto == null) {
            return PostServiceOuterClass.PostResponse.newBuilder().build();
        }

        PostServiceOuterClass.PostResponse.Builder builder = PostServiceOuterClass.PostResponse.newBuilder()
                .setPostId(dto.getPostId())
                .setUserId(dto.getUserId())
                .setUserName(dto.getUserName() != null ? dto.getUserName() : "")
                .setAvatarUrl(dto.getAvatarUrl() != null ? dto.getAvatarUrl() : "")
                .setCaption(dto.getCaption() != null ? dto.getCaption() : "")
                .setVisibility(dto.getVisibility() != null ? dto.getVisibility() : "")
                .setTag(dto.getTag() != null ? dto.getTag() : "")
                .addAllMediaUrl(dto.getMediaUrl() != null ? dto.getMediaUrl() : List.of())
                .setQuantityLike(dto.getQuantityLike() != null ? dto.getQuantityLike() : 0L)
                .setQuantityComment(dto.getQuantityComment() != null ? dto.getQuantityComment() : 0L)
                .setIsActive(dto.getIsActive() != null ? dto.getIsActive() : false)
                .setCreatedAt(toEpochMilli(dto.getCreatedAt()))
                .setUpdateAt(toEpochMilli(dto.getUpdatedAt()))
                .setIsLike(dto.getIsLike() != null ? dto.getIsLike() : false);

        if (dto.getListUserTag() != null && !dto.getListUserTag().isEmpty()) {
            builder.addAllListUserTag(dto.getListUserTag().stream()
                    .map(tag -> PostServiceOuterClass.UserTag.newBuilder()
                            .setUserId(tag.getUserId())
                            .setUserName(tag.getUserName())
                            .build())
                    .toList());
        }

        return builder.build();
    }

    private PostServiceOuterClass.ReelResponse mapToReelResponse(ReelResponseDTO dto) {
        if (dto == null) {
            return PostServiceOuterClass.ReelResponse.newBuilder().build();
        }

        return PostServiceOuterClass.ReelResponse.newBuilder()
                .setPostId(dto.getReelId())
                .setUserId(dto.getUserId())
                .setUserName(dto.getUserName() != null ? dto.getUserName() : "")
                .setAvatarUrl(dto.getAvatarUrl() != null ? dto.getAvatarUrl() : "")
                .setCaption(dto.getCaption() != null ? dto.getCaption() : "")
                .setVisibility(dto.getVisibility() != null ? dto.getVisibility() : "")
                .setMediaUrl(dto.getMediaUrl() != null ? dto.getMediaUrl() : "")
                .setQuantityLike(dto.getQuantityLike() != null ? dto.getQuantityLike() : 0L)
                .setQuantityComment(dto.getQuantityComment() != null ? dto.getQuantityComment() : 0L)
                .setIsActive(dto.getIsActive() != null ? dto.getIsActive() : false)
                .setCreatedAt(toEpochMilli(dto.getCreatedAt()))
                .setUpdateAt(toEpochMilli(dto.getUpdatedAt()))
                .setIsLike(dto.getIsLike() != null ? dto.getIsLike() : false)
                .build();
    }

    private void updateRedisCounter(String type, boolean isPost, Long id, boolean isIncrease) {
        String key = (isPost ? "post:" : "reel:") + type + ":" + id;
        if (isIncrease) {
            redisTemplate.opsForValue().increment(key);
        } else {
            redisTemplate.opsForValue().decrement(key);
        }
    }

    private PostModel getPostModel(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new CustomException("Post is not exist", HttpStatus.BAD_REQUEST));
    }

    private ReelModel getReelModel(Long id) {
        return reelRepository.findById(id)
                .orElseThrow(() -> new CustomException("Reel is not exist", HttpStatus.BAD_REQUEST));
    }

    private long toEpochMilli(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 0L;
    }

    private void sendSuccessMessage(StreamObserver<PostServiceOuterClass.PostMessageResponse> responseObserver) {
        responseObserver.onNext(PostServiceOuterClass.PostMessageResponse.newBuilder().setMessage("Success").build());
        responseObserver.onCompleted();
    }

    private void handleGrpcError(String methodName, Exception e, StreamObserver<?> responseObserver) {
        log.error("Error in {}", methodName, e);
        responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription(e.getMessage())
                .asRuntimeException());
    }
}