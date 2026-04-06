package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.postservice.dto.CollectionRequestDto;
import org.nexo.postservice.dto.MessageDTO;
import org.nexo.postservice.dto.StoryDeletionEvent;
import org.nexo.postservice.dto.StoryRequestDto;
import org.nexo.postservice.dto.response.*;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.model.CollectionItemModel;
import org.nexo.postservice.model.CollectionModel;
import org.nexo.postservice.model.StoryModel;
import org.nexo.postservice.model.StoryViewModel;
import org.nexo.postservice.repository.ICollectionRepository;
import org.nexo.postservice.repository.IStoryRepository;
import org.nexo.postservice.repository.IStoryViewRepository;
import org.nexo.postservice.service.GrpcServiceImpl.client.UserGrpcClient;
import org.nexo.postservice.service.IStoryService;
import org.nexo.postservice.util.Enum.ENotificationType;
import org.nexo.postservice.util.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoryServiceImpl implements IStoryService {
    private final FileService fileServiceClient;
    private final IStoryRepository storyRepository;
    private final IStoryViewRepository storyViewRepository;
    private final SecurityUtil securityUtil;
    private final UserGrpcClient userGrpcClient;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ICollectionRepository collectionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    @Override
    @Transactional
    public String saveStory(StoryRequestDto dto, List<MultipartFile> files) {
        securityUtil.checkOwner(dto.getUserId());
        UserServiceProto.UserDTOResponse userDTOResponse = userGrpcClient.getUserDTOById(dto.getUserId());

        StoryModel model;
        boolean isNew = (dto.getStoryId() == 0);

        if (!isNew) {
            model = storyRepository.findById(dto.getStoryId())
                    .orElseThrow(() -> new CustomException("Story not found", HttpStatus.BAD_REQUEST));
            model.setIsClosedFriend(dto.getIsClosedFriend());
            model.setIsArchive(dto.getIsArchive());
        } else {
            model = StoryModel.builder()
                    .userId(dto.getUserId())
                    .isClosedFriend(dto.getIsClosedFriend())
                    .isArchive(false)
                    .isActive(true)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();
        }

        model.setAuthorName(userDTOResponse.getUsername());
        storyRepository.save(model);

        if (isNew && files != null && !files.isEmpty() && !files.getFirst().isEmpty()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String token = ((JwtAuthenticationToken) auth).getToken().getTokenValue();
            fileServiceClient.saveStoryMedia(files, model.getId(), token);
        }

        if (isNew) {
            String redisKey = "story:expire:" + model.getId();
            redisTemplate.opsForValue().set(redisKey, model.getId().toString(), 24, TimeUnit.HOURS);
        }

        return "Success";
    }

    @Override
    @Transactional
    public String deleteStory(Long id) {
        StoryModel model = storyRepository.findById(id)
                .orElseThrow(() -> new CustomException("Story not exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(model.getUserId());
        storyRepository.delete(model);
        return "Success";
    }

    @Override
    @Transactional
    public String archiveStory(Long id) {
        StoryModel model = storyRepository.findById(id)
                .orElseThrow(() -> new CustomException("Story not exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(model.getUserId());
        model.setIsArchive(true);
        model.setIsActive(false);
        storyRepository.save(model);
        return "Success";
    }

    @Override
    @Transactional
    public String viewStory(Long id) {
        Long userId = securityUtil.getUserIdFromToken();
        Optional<StoryViewModel> existingView = storyViewRepository.findByStoryModel_IdAndSeenUserId(id, userId);
        if (existingView.isEmpty()) {
            StoryModel story = storyRepository.findById(id)
                    .orElseThrow(() -> new CustomException("Story does not exist", HttpStatus.BAD_REQUEST));

            StoryViewModel model = StoryViewModel.builder()
                    .isLike(false)
                    .seenUserId(userId)
                    .storyModel(story)
                    .build();
            storyViewRepository.save(model);
        }
        return "Success";
    }

    @Override
    @Transactional
    public String likeStory(Long id) {
        Long userId = securityUtil.getUserIdFromToken();
        StoryModel model = storyRepository.findById(id)
                .orElseThrow(() -> new CustomException("Story not exist", HttpStatus.BAD_REQUEST));

        checkVisibilityAccess(model.getUserId(), userId);

        StoryViewModel storyViewModel = storyViewRepository.findByStoryModel_IdAndSeenUserId(id, userId)
                .orElseThrow(() -> new CustomException("Story View not exist. Please view first.", HttpStatus.BAD_REQUEST));

        boolean newLikeStatus = !storyViewModel.getIsLike();
        storyViewModel.setIsLike(newLikeStatus);
        storyViewRepository.save(storyViewModel);


        if (newLikeStatus && !userId.equals(model.getUserId())) {
            sendNotification(
                    userId,
                    model.getUserId(),
                    ENotificationType.LIKE_STORY,
                    "/stories/" + id
            );
        }


        return "Success";
    }

    @Override
    @Transactional
    public String saveCollection(CollectionRequestDto dto) {
        Long userId = securityUtil.getUserIdFromToken();
        CollectionModel collectionModel;

        if (dto.getId() != 0) {
            collectionModel = collectionRepository.findById(dto.getId())
                    .orElseThrow(() -> new CustomException("Collection not found", HttpStatus.NOT_FOUND));
            securityUtil.checkOwner(collectionModel.getUserId());
        } else {
            collectionModel = new CollectionModel();
            collectionModel.setUserId(userId);
        }

        collectionModel.setCollectionName(dto.getCollectionName());
        collectionModel.getCollectionItemModelList().clear();

        if (dto.getStoryList() != null && !dto.getStoryList().isEmpty()) {
            List<StoryModel> storiesToAdd = storyRepository.findAllById(dto.getStoryList());

            if (storiesToAdd.size() != dto.getStoryList().size()) {
                throw new CustomException("One or more stories could not be found.", HttpStatus.BAD_REQUEST);
            }

            for (StoryModel story : storiesToAdd) {
                CollectionItemModel newItem = new CollectionItemModel();
                newItem.setStoryModel(story);
                newItem.setCollectionModel(collectionModel);
                collectionModel.getCollectionItemModelList().add(newItem);
            }
        }
        collectionRepository.save(collectionModel);
        return "Success";
    }

    @Override
    @Transactional
    public String deleteCollection(Long id) {
        CollectionModel collectionModel = collectionRepository.findById(id)
                .orElseThrow(() -> new CustomException("Collection not exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(collectionModel.getUserId());
        collectionRepository.delete(collectionModel);
        return "Success";
    }


    @Override
    public PageModelResponse<ViewDetailStoryResponse> viewDetailStory(Long id, int pageNo, int pageSize) {
        StoryModel story = storyRepository.findById(id)
                .orElseThrow(() -> new CustomException("Story does not exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(story.getUserId());

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<StoryViewModel> page = storyViewRepository.findByStoryModel_Id(id, pageable);

        if (page.isEmpty()) return buildEmptyPageResponse(pageNo, pageSize);

        List<Long> seenUserIds = page.getContent().stream().map(StoryViewModel::getSeenUserId).distinct().toList();
        Map<Long, UserServiceProto.UserDTOResponse2> userMap = userGrpcClient.getUsersByIds(seenUserIds).stream()
                .collect(Collectors.toMap(UserServiceProto.UserDTOResponse2::getId, u -> u));

        List<ViewDetailStoryResponse> responses = page.getContent().stream().map(view -> {
            UserServiceProto.UserDTOResponse2 user = userMap.get(view.getSeenUserId());
            return ViewDetailStoryResponse.builder()
                    .avatarUrl(user != null ? user.getAvatar() : "")
                    .isLike(view.getIsLike())
                    .userName(user != null ? user.getUsername() : "Unknown")
                    .createdAt(view.getCreatedAt())
                    .build();
        }).toList();

        return buildPageResponse(page, responses);
    }

    @Override
    public PageModelResponse<CollectionSummaryResponse> getAllCollections(Long targetUserId, int pageNo, int pageSize) {
        Long viewerId = securityUtil.getUserIdFromToken();
        checkVisibilityAccess(targetUserId, viewerId);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CollectionModel> page = collectionRepository.findByUserId(targetUserId, pageable);

        List<CollectionSummaryResponse> content = page.getContent().stream()
                .filter(collection -> !collection.getCollectionItemModelList().isEmpty())
                .map(collection -> {
                    String thumbnailUrl = collection.getCollectionItemModelList().getFirst().getStoryModel().getMediaURL();
                    return CollectionSummaryResponse.builder()
                            .id(collection.getId())
                            .collectionName(collection.getCollectionName())
                            .mediaUrl(thumbnailUrl)
                            .createdAt(collection.getCreatedAt())
                            .build();
                }).toList();

        return buildPageResponse(page, content);
    }

    @Override
    public CollectionDetailResponse getCollectionDetail(Long collectionId) {
        CollectionModel collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new CustomException("Collection not found", HttpStatus.BAD_REQUEST));
        Long currentUserId = securityUtil.getUserIdFromToken();
        securityUtil.checkOwner(collection.getUserId());

        List<StoryModel> stories = collection.getCollectionItemModelList().stream().map(CollectionItemModel::getStoryModel).toList();
        Map<Long, StoryViewModel> viewMap = getBatchStoryViews(stories, currentUserId);

        List<StoryResponse.Story> storyResponses = stories.stream()
                .map(story -> toStoryResponse(story, viewMap.get(story.getId())))
                .toList();

        return CollectionDetailResponse.builder()
                .id(collection.getId())
                .collectionName(collection.getCollectionName())
                .stories(storyResponses)
                .build();
    }

    @Override
    public CollectionDetailResponse getFriendCollectionDetail(Long collectionId) {
        Long viewerId = securityUtil.getUserIdFromToken();
        CollectionModel collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new CustomException("Collection not found", HttpStatus.NOT_FOUND));

        Long ownerId = collection.getUserId();
        if (viewerId.equals(ownerId)) {
            throw new CustomException("Please use /collections/my/{id} for your own collections", HttpStatus.BAD_REQUEST);
        }

        UserServiceProto.CheckFollowResponse followStatus = userGrpcClient.checkFollow(viewerId, ownerId);
        if (followStatus.getIsPrivate() && !followStatus.getIsFollow()) {
            throw new CustomException("This account is private. Follow them to see collections.", HttpStatus.FORBIDDEN);
        }

        boolean isCloseFriend = followStatus.getIsCloseFriend();
        List<StoryModel> visibleStories = collection.getCollectionItemModelList().stream()
                .map(CollectionItemModel::getStoryModel)
                .filter(story -> !story.getIsClosedFriend() || isCloseFriend)
                .toList();

        Map<Long, StoryViewModel> viewMap = getBatchStoryViews(visibleStories, viewerId);

        List<StoryResponse.Story> storyResponses = visibleStories.stream()
                .map(story -> toStoryResponse(story, viewMap.get(story.getId())))
                .toList();

        return CollectionDetailResponse.builder()
                .id(collection.getId())
                .collectionName(collection.getCollectionName())
                .stories(storyResponses)
                .build();
    }

    @Override
    public PageModelResponse<StoryResponse> getAllStoryOfFriend(Long viewerId, int pageNo, int pageSize) {
        securityUtil.checkOwner(viewerId);
        UserServiceProto.GetUserFollowingsResponse followingsResponse = userGrpcClient.getUserFollowing(viewerId);

        if (!followingsResponse.getSuccess()) {
            throw new CustomException("Failed to get followings", HttpStatus.BAD_REQUEST);
        }

        // 1. Phân loại bạn bè (Bình thường vs Close Friend)
        Set<Long> closeFriendIds = new HashSet<>();
        Set<Long> allFriendIds = new HashSet<>();
        Map<Long, UserServiceProto.FolloweeInfo> friendInfoMap = new HashMap<>();

        for (UserServiceProto.FolloweeInfo info : followingsResponse.getFollowingsList()) {
            allFriendIds.add(info.getUserId());
            friendInfoMap.put(info.getUserId(), info);
            if (info.getIsCloseFriend()) closeFriendIds.add(info.getUserId());
        }

        if (allFriendIds.isEmpty()) return buildEmptyPageResponse(pageNo, pageSize);

        List<StoryModel> allActiveStories = storyRepository.findAllByUserIdInAndIsActive(allFriendIds, true);

        // 3. Batch views
        Map<Long, StoryViewModel> viewMap = getBatchStoryViews(allActiveStories, viewerId);

        // 4. Lọc quyền xem (Close friend) và gom nhóm theo User
        Map<Long, List<StoryResponse.Story>> storiesByUser = new HashMap<>();

        for (StoryModel story : allActiveStories) {
            if (story.getIsClosedFriend() && !closeFriendIds.contains(story.getUserId()))
                continue; // Bỏ qua nếu ko phải close friend

            storiesByUser.computeIfAbsent(story.getUserId(), k -> new ArrayList<>())
                    .add(toStoryResponse(story, viewMap.get(story.getId())));
        }

        // 5. Build danh sách kết quả
        List<StoryResponse> finalResponse = storiesByUser.entrySet().stream()
                .map(entry -> {
                    UserServiceProto.FolloweeInfo info = friendInfoMap.get(entry.getKey());
                    return StoryResponse.builder()
                            .userId(info.getUserId())
                            .userName(info.getUserName())
                            .avatarUrl(info.getAvatar())
                            .storyList(entry.getValue())
                            .build();
                })
                .toList();

        // 6. Pagination trên RAM (Giữ nguyên như code cũ vì Grouping)
        int start = pageNo * pageSize;
        if (start >= finalResponse.size()) return buildEmptyPageResponse(pageNo, pageSize);

        List<StoryResponse> pagedList = finalResponse.subList(start, Math.min(start + pageSize, finalResponse.size()));

        return PageModelResponse.<StoryResponse>builder()
                .content(pagedList)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(finalResponse.size())
                .totalPages((int) Math.ceil((double) finalResponse.size() / pageSize))
                .build();
    }

    @Override
    public PageModelResponse<StoryResponse> getStoriesOfUser(Long ownerId, int pageNo, int pageSize) {
        Long viewerId = securityUtil.getUserIdFromToken();
        checkVisibilityAccess(ownerId, viewerId);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<StoryModel> storyPage = storyRepository.findByUserIdAndIsActive(ownerId, true, pageable);

        if (storyPage.isEmpty()) return buildEmptyPageResponse(pageNo, pageSize);

        UserServiceProto.UserDTOResponse ownerInfo = userGrpcClient.getUserDTOById(ownerId);
        Map<Long, StoryViewModel> viewMap = getBatchStoryViews(storyPage.getContent(), viewerId);

        List<StoryResponse.Story> storyList = storyPage.getContent().stream()
                .map(model -> toStoryResponse(model, viewMap.get(model.getId())))
                .toList();

        StoryResponse storyResponse = StoryResponse.builder()
                .userName(ownerInfo.getUsername())
                .avatarUrl(ownerInfo.getAvatar())
                .userId(ownerId)
                .storyList(storyList)
                .build();

        return buildPageResponse(storyPage, List.of(storyResponse)); // Trả về list chứa 1 user
    }

    @Override
    public PageModelResponse<StoryResponse> getAllStoriesOfUser(Long id, int pageNo, int pageSize) {
        securityUtil.checkOwner(id);
        UserServiceProto.UserDTOResponse user = userGrpcClient.getUserDTOById(id);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").ascending());
        Page<StoryModel> storyPage = storyRepository.findByUserId(id, pageable);

        if (storyPage.isEmpty()) return buildEmptyPageResponse(pageNo, pageSize);

        // Chủ tài khoản tự xem thì lấy hết view, nên mặc định pass null (Tự map view ở một hàm khác nếu cần, ở đây tạm pass null)
        Map<Long, StoryViewModel> viewMap = getBatchStoryViews(storyPage.getContent(), id);

        List<StoryResponse.Story> storyList = storyPage.getContent().stream()
                .map(model -> toStoryResponse(model, viewMap.get(model.getId())))
                .toList();

        StoryResponse storyResponse = StoryResponse.builder()
                .userId(id)
                .userName(user.getUsername())
                .avatarUrl(user.getAvatar())
                .storyList(storyList)
                .build();

        return buildPageResponse(storyPage, List.of(storyResponse));
    }


    // =========================================================================
    // 3. KAFKA LISTENER VÀ CÁC HÀM HELPER
    // =========================================================================

    @KafkaListener(topics = "story-deletion-topic", groupId = "story-deleter-group")
    @Transactional
    public void consumeStoryDeletion(StoryDeletionEvent event) {
        Long storyId = event.getStoryId();
        try {
            storyRepository.findById(storyId).ifPresent(storyModel -> {
                storyModel.setIsActive(false);
                storyRepository.save(storyModel);
                log.info("Successfully deactivated story with ID: {}", storyId);
            });
        } catch (Exception e) {
            log.error("Failed to deactivate story ID: {}", storyId, e);
        }
    }

    private void checkVisibilityAccess(Long targetUserId, Long viewerId) {
        if (targetUserId.equals(viewerId)) return;

        UserServiceProto.CheckFollowResponse followCheck = userGrpcClient.checkFollow(viewerId, targetUserId);
        if (followCheck.getIsPrivate() && !followCheck.getIsFollow()) {
            throw new CustomException("This account is private. Follow to view content.", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * TỐI ƯU N+1: Lấy trạng thái View/Like của TẤT CẢ story ID trong 1 lần Query DB
     * Yêu cầu thêm method: findAllByStoryModel_IdInAndSeenUserId(List<Long> storyIds, Long seenUserId) trong IStoryViewRepository
     */
    private Map<Long, StoryViewModel> getBatchStoryViews(List<StoryModel> stories, Long viewerId) {
        if (stories.isEmpty()) return Collections.emptyMap();
        List<Long> storyIds = stories.stream().map(StoryModel::getId).toList();

        return storyViewRepository.findAllByStoryModel_IdInAndSeenUserId(storyIds, viewerId)
                .stream()
                .collect(Collectors.toMap(view -> view.getStoryModel().getId(), view -> view));
    }

    private StoryResponse.Story toStoryResponse(StoryModel model, StoryViewModel view) {
        return StoryResponse.Story.builder()
                .createdAt(model.getCreatedAt())
                .storyId(model.getId())
                .mediaUrl(model.getMediaURL())
                .isLike(view != null && view.getIsLike())
                .isSeen(view != null)
                .isActive(model.getIsActive())
                .isCloseFriend(model.getIsClosedFriend())
                .quantitySeen(model.getViews() != null ? (long) model.getViews().size() : 0L)
                .build();
    }

    private <T> PageModelResponse<T> buildPageResponse(Page<?> pageData, List<T> content) {
        return PageModelResponse.<T>builder()
                .pageNo(pageData.getNumber())
                .pageSize(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .last(pageData.isLast())
                .content(content)
                .build();
    }

    private <T> PageModelResponse<T> buildEmptyPageResponse(int pageNo, int pageSize) {
        return PageModelResponse.<T>builder()
                .content(Collections.emptyList())
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .build();
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
}