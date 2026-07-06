package org.nexo.interactionservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.grpc.post.PostServiceOuterClass;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.interactionservice.dto.response.PageModelResponse;
import org.nexo.interactionservice.dto.response.SavedPostResponseDTO;
import org.nexo.interactionservice.dto.response.SavedPostStatusCheckResponseDTO;
import org.nexo.interactionservice.exception.CustomException;
import org.nexo.interactionservice.model.SavedPostModel;
import org.nexo.interactionservice.repository.ISavedPostRepository;
import org.nexo.interactionservice.service.ISavedPostService;
import org.nexo.interactionservice.util.Enum.EVisibilityPost;
import org.nexo.interactionservice.util.Enum.SecurityUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SavedPostServiceImpl implements ISavedPostService {

    private final ISavedPostRepository savedPostRepository;
    private final PostGrpcClient postGrpcClient;
    private final UserGrpcClient userGrpcClient;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional
    public SavedPostResponseDTO savePost(Long postId) {
        Long currentUserId = securityUtil.getUserIdFromToken();
        PostServiceOuterClass.PostResponse post = postGrpcClient.getPostById(postId);

        validateCanAccessPost(currentUserId, post);

        SavedPostModel existing = savedPostRepository.findByUserIdAndPostId(currentUserId, postId).orElse(null);
        if (existing != null) {
            savedPostRepository.delete(existing);
            return mapToResponse(existing, post);
        }

        SavedPostModel savedPost = SavedPostModel.builder()
                .userId(currentUserId)
                .postId(postId)
                .build();

        try {
            SavedPostModel persisted = savedPostRepository.save(savedPost);
            return mapToResponse(persisted, post);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate saved-post insert prevented for userId={} postId={}", currentUserId, postId);
            SavedPostModel persisted = savedPostRepository.findByUserIdAndPostId(currentUserId, postId)
                    .orElseThrow(() -> new CustomException("Post already saved", HttpStatus.CONFLICT));
            return mapToResponse(persisted, post);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageModelResponse<SavedPostResponseDTO> getSavedPosts(Pageable pageable) {
        Long currentUserId = securityUtil.getUserIdFromToken();
        Page<SavedPostModel> savedPostPage = savedPostRepository.findByUserId(currentUserId, pageable);

        if (savedPostPage.isEmpty()) {
            return buildPageResponse(savedPostPage, List.of());
        }

        List<Long> postIds = savedPostPage.getContent().stream()
                .map(SavedPostModel::getPostId)
                .toList();

        Map<Long, PostServiceOuterClass.PostResponse> postMap = postGrpcClient.getPostsByIds(postIds, currentUserId);
        Set<Long> followingIds = resolveFollowingIds(currentUserId);
        List<SavedPostResponseDTO> content = new ArrayList<>();

        for (SavedPostModel savedPost : savedPostPage.getContent()) {
            PostServiceOuterClass.PostResponse post = postMap.get(savedPost.getPostId());
            if (post == null || !post.getIsActive()) {
                continue;
            }

            if (!canAccessPost(currentUserId, post, followingIds)) {
                continue;
            }

            content.add(mapToResponse(savedPost, post));
        }

        return buildPageResponse(savedPostPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public SavedPostStatusCheckResponseDTO checkSavedPosts(List<Long> postIds) {
        Long currentUserId = securityUtil.getUserIdFromToken();
        List<Long> normalizedPostIds = postIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));

        if (normalizedPostIds.isEmpty()) {
            return SavedPostStatusCheckResponseDTO.builder().savedStatus(new LinkedHashMap<>()).build();
        }

        Map<Long, PostServiceOuterClass.PostResponse> postMap = postGrpcClient.getPostsByIds(normalizedPostIds, currentUserId);
        Set<Long> followingIds = resolveFollowingIds(currentUserId);

        List<Long> visiblePostIds = normalizedPostIds.stream()
                .filter(postId -> {
                    PostServiceOuterClass.PostResponse post = postMap.get(postId);
                    return post != null && post.getIsActive() && canAccessPost(currentUserId, post, followingIds);
                })
                .toList();

        if (visiblePostIds.isEmpty()) {
            return SavedPostStatusCheckResponseDTO.builder().savedStatus(new LinkedHashMap<>()).build();
        }

        Set<Long> savedPostIds = savedPostRepository.findByUserIdAndPostIdIn(currentUserId, visiblePostIds).stream()
                .map(SavedPostModel::getPostId)
                .collect(Collectors.toSet());

        Map<Long, Boolean> savedStatus = new LinkedHashMap<>();
        for (Long postId : visiblePostIds) {
            savedStatus.put(postId, savedPostIds.contains(postId));
        }

        return SavedPostStatusCheckResponseDTO.builder()
                .savedStatus(savedStatus)
                .build();
    }

    private void validateCanAccessPost(Long currentUserId, PostServiceOuterClass.PostResponse post) {
        if (post == null || post.getPostId() == 0L || !post.getIsActive()) {
            throw new CustomException("Post not found", HttpStatus.NOT_FOUND);
        }

        if (currentUserId.equals(post.getUserId())) {
            return;
        }

        if (isPublic(post)) {
            return;
        }

        UserServiceProto.CheckFollowResponse followCheck = userGrpcClient.checkFollow(currentUserId, post.getUserId());
        if (!followCheck.getIsFollow()) {
            throw new CustomException("You do not have permission to save this post", HttpStatus.FORBIDDEN);
        }
    }

    private Set<Long> resolveFollowingIds(Long currentUserId) {
        UserServiceProto.GetUserFollowingsResponse response = userGrpcClient.getUserFollowing(currentUserId);
        Set<Long> followingIds = new HashSet<>();
        response.getFollowingsList().forEach(following -> followingIds.add(following.getUserId()));
        return followingIds;
    }

    private boolean canAccessPost(Long currentUserId,
                                  PostServiceOuterClass.PostResponse post,
                                  Set<Long> followingIds) {
        if (currentUserId.equals(post.getUserId())) {
            return true;
        }

        if (isPublic(post)) {
            return true;
        }

        return followingIds.contains(post.getUserId());
    }

    private boolean isPublic(PostServiceOuterClass.PostResponse post) {
        if (post.getVisibility() == null || post.getVisibility().isBlank()) {
            return false;
        }

        return EVisibilityPost.PUBLIC.name().equalsIgnoreCase(post.getVisibility());
    }

    private SavedPostResponseDTO mapToResponse(SavedPostModel savedPost, PostServiceOuterClass.PostResponse post) {
        return SavedPostResponseDTO.builder()
                .savedPostId(savedPost.getId())
                .postId(post.getPostId())
                .ownerId(post.getUserId())
                .ownerUsername(post.getUserName())
                .ownerAvatarUrl(post.getAvatarUrl())
                .caption(post.getCaption())
                .visibility(post.getVisibility())
                .mediaUrls(post.getMediaUrlList())
                .quantityLike(post.getQuantityLike())
                .quantityComment(post.getQuantityComment())
                .isLike(post.getIsLike())
                .postCreatedAt(toLocalDateTime(post.getCreatedAt()))
                .savedAt(savedPost.getCreatedAt())
                .build();
    }

    private LocalDateTime toLocalDateTime(long epochMillis) {
        if (epochMillis <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
    }

    private PageModelResponse<SavedPostResponseDTO> buildPageResponse(
            Page<SavedPostModel> pageData,
            List<SavedPostResponseDTO> content) {
        return PageModelResponse.<SavedPostResponseDTO>builder()
                .pageNo(pageData.getNumber())
                .pageSize(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .last(pageData.isLast())
                .content(content)
                .build();
    }
}
