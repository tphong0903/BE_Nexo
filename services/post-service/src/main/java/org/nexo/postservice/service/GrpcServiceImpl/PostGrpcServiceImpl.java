package org.nexo.postservice.service.GrpcServiceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.ZoneId;
import java.util.List;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class PostGrpcServiceImpl extends PostServiceGrpc.PostServiceImplBase {
    private final IPostService postService;
    private final IPostRepository postRepository;
    private final IReelRepository reelRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void getPostById(PostServiceOuterClass.GetPostRequest request, StreamObserver<PostServiceOuterClass.PostResponse> responseObserver) {
        try {
            Long id = request.getId();
            PostResponseDTO dto = postService.getPostById2(id);

            if (dto == null) {
                responseObserver.onNext(PostServiceOuterClass.PostResponse.newBuilder().build());
                responseObserver.onCompleted();
                return;
            }
            PostServiceOuterClass.PostResponse response = PostServiceOuterClass.PostResponse.newBuilder()
                    .setPostId(dto.getPostId())
                    .setUserId(dto.getUserId())
                    .setUserName(dto.getUserName() != null ? dto.getUserName() : "")
                    .setAvatarUrl(dto.getAvatarUrl() != null ? dto.getAvatarUrl() : "")
                    .setCaption(dto.getCaption() != null ? dto.getCaption() : "")
                    .setVisibility(dto.getVisibility() != null ? dto.getVisibility() : "")
                    .setTag(dto.getTag() != null ? dto.getTag() : "")
                    .addAllMediaUrl(dto.getMediaUrl() != null ? dto.getMediaUrl() : List.of())
                    .setQuantityLike(dto.getQuantityLike() != null ? dto.getQuantityLike() : 0)
                    .setQuantityComment(dto.getQuantityComment() != null ? dto.getQuantityComment() : 0)
                    .addAllListUserTag(
                            dto.getListUserTag() != null
                                    ? dto.getListUserTag().stream()
                                    .map(tag -> PostServiceOuterClass.UserTag.newBuilder()
                                            .setUserId(tag.getUserId())
                                            .setUserName(tag.getUserName())
                                            .build())
                                    .toList()
                                    : List.of()
                    )
                    .setIsActive(dto.getIsActive() != null ? dto.getIsActive() : false)
                    .setCreatedAt(dto.getCreatedAt() != null
                            ? dto.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            : 0L)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getPostById", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getReelById(PostServiceOuterClass.GetPostRequest request, StreamObserver<PostServiceOuterClass.ReelResponse> responseObserver) {
        try {
            Long id = request.getId();
            ReelResponseDTO dto = postService.getReelById2(id);

            if (dto == null) {
                responseObserver.onNext(PostServiceOuterClass.ReelResponse.newBuilder().build());
                responseObserver.onCompleted();
                return;
            }
            PostServiceOuterClass.ReelResponse response = PostServiceOuterClass.ReelResponse.newBuilder()
                    .setPostId(dto.getPostId())
                    .setUserId(dto.getUserId())
                    .setUserName(dto.getUserName() != null ? dto.getUserName() : "")
                    .setAvatarUrl(dto.getAvatarUrl() != null ? dto.getAvatarUrl() : "")
                    .setCaption(dto.getCaption() != null ? dto.getCaption() : "")
                    .setVisibility(dto.getVisibility() != null ? dto.getVisibility() : "")
                    .setTag(dto.getTag() != null ? dto.getTag() : "")
                    .setMediaUrl(dto.getMediaUrl() != null ? dto.getMediaUrl() : null)
                    .setQuantityLike(dto.getQuantityLike() != null ? dto.getQuantityLike() : 0)
                    .setQuantityComment(dto.getQuantityComment() != null ? dto.getQuantityComment() : 0)
                    .setIsActive(dto.getIsActive() != null ? dto.getIsActive() : false)
                    .setCreatedAt(dto.getCreatedAt() != null
                            ? dto.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            : 0L)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getPostById", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void addLikeQuantityById(org.nexo.grpc.post.PostServiceOuterClass.GetPostRequest2 request, StreamObserver<PostServiceOuterClass.PostMessageResponse> responseObserver) {
        try {
            Long id = request.getId();
            Boolean isPost = request.getIsPost();
            Boolean isIncrease = request.getIsIncrease();

            String key = isPost ? "post:likes:" + id : "reel:likes:" + id;

            if (isIncrease) {
                redisTemplate.opsForValue().increment(key);
            } else {
                redisTemplate.opsForValue().decrement(key);
            }

            int count = isIncrease ? 1 : -1;
            if (isPost) {
                PostModel model = postRepository.findById(id).orElseThrow(() -> new CustomException("PostModel is not exist", HttpStatus.BAD_REQUEST));
                model.setLikeQuantity(model.getLikeQuantity() + count);
                postRepository.save(model);
            } else {
                ReelModel model = reelRepository.findById(id).orElseThrow(() -> new CustomException("PostModel is not exist", HttpStatus.BAD_REQUEST));
                model.setLikeQuantity(model.getLikeQuantity() + count);
                reelRepository.save(model);
            }
            PostServiceOuterClass.PostMessageResponse response = PostServiceOuterClass.PostMessageResponse.newBuilder().setMessage("Success").build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getPostById", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void addCommentQuantityById(PostServiceOuterClass.GetPostRequest2 request, StreamObserver<PostServiceOuterClass.PostMessageResponse> responseObserver) {
        try {
            Long id = request.getId();
            Boolean isPost = request.getIsPost();
            Boolean isIncrease = request.getIsIncrease();

            String key = isPost ? "post:comments:" + id : "reel:comments:" + id;

            if (isIncrease) {
                redisTemplate.opsForValue().increment(key);
            } else {
                redisTemplate.opsForValue().decrement(key);
            }
            int count = isIncrease ? 1 : -1;
            if (isPost) {
                PostModel model = postRepository.findById(id).orElseThrow(() -> new CustomException("PostModel is not exist", HttpStatus.BAD_REQUEST));
                model.setCommentQuantity(model.getCommentQuantity() + count);
                postRepository.save(model);
            } else {
                ReelModel model = reelRepository.findById(id).orElseThrow(() -> new CustomException("PostModel is not exist", HttpStatus.BAD_REQUEST));
                model.setCommentQuantity(model.getCommentQuantity() + count);
                reelRepository.save(model);
            }
            PostServiceOuterClass.PostMessageResponse response = PostServiceOuterClass.PostMessageResponse.newBuilder().setMessage("Success").build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getPostById", e);
            responseObserver.onError(e);
        }
    }
}
