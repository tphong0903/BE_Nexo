package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.postservice.dto.CollectionRequestDto;
import org.nexo.postservice.dto.StoryRequestDto;
import org.nexo.postservice.dto.response.*;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.model.*;
import org.nexo.postservice.repository.ICollectionItemRepository;
import org.nexo.postservice.repository.ICollectionRepository;
import org.nexo.postservice.repository.IStoryRepository;
import org.nexo.postservice.repository.IStoryViewRepository;
import org.nexo.postservice.service.GrpcServiceImpl.client.UserGrpcClient;
import org.nexo.postservice.service.IStoryService;
import org.nexo.postservice.util.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
    private final ICollectionItemRepository collectionItemRepository;

    @Override
    public String saveStory(StoryRequestDto dto, List<MultipartFile> files) {
        securityUtil.checkOwner(dto.getUserId());
        UserServiceProto.UserDTOResponse userDTOResponse = userGrpcClient.getUserDTOById(dto.getUserId());

        StoryModel model;
        if (dto.getStoryId() != 0) {
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
        
        if (files != null && !files.isEmpty() && !files.getFirst().isEmpty() && dto.getStoryId() == 0) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String token = ((JwtAuthenticationToken) auth).getToken().getTokenValue();
            fileServiceClient.saveStoryMedia(files, model.getId(), token);
        }

        String redisKey = "story:expire:" + model.getId();
        redisTemplate.opsForValue().set(redisKey, model.getId().toString(), 24, TimeUnit.HOURS);


        return "Success";
    }

    @Override
    public String deleteStory(Long id) {
        StoryModel model = storyRepository.findById(id).orElseThrow(() -> new CustomException("Story is not exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(model.getUserId());
        storyRepository.delete(model);
        return "Success";
    }

    @Override
    public String likeStory(Long id) {
        StoryModel model = storyRepository.findById(id).orElseThrow(() -> new CustomException("Story is not exist", HttpStatus.BAD_REQUEST));
        Long userId = securityUtil.getUserIdFromToken();
        boolean isAllow = false;
        if (model.getUserId().equals(userId)) {
            isAllow = false;
        } else {
            UserServiceProto.CheckFollowResponse followResponse =
                    userGrpcClient.checkFollow(userId, model.getUserId());
            if (!followResponse.getIsPrivate() || followResponse.getIsFollow()) {
                isAllow = true;
            }
        }
        if (!isAllow)
            throw new CustomException("Dont allow to like story", HttpStatus.BAD_REQUEST);

        StoryViewModel storyViewModel = storyViewRepository.findByStoryModel_IdAndSeenUserId(id, userId).orElse(null);
        if (storyViewModel == null)
            throw new CustomException("Story View is not exist", HttpStatus.BAD_REQUEST);

        storyViewModel.setIsLike(!storyViewModel.getIsLike());
        storyViewRepository.save(storyViewModel);
        return "Success";
    }

    @Override
    public String saveCollection(CollectionRequestDto dto) {
        Long userId = securityUtil.getUserIdFromToken();
        CollectionModel collectionModel;
        List<Long> newStoryIds = dto.getStoryList();

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

        if (newStoryIds != null && !newStoryIds.isEmpty()) {
            List<StoryModel> storiesToAdd = storyRepository.findAllById(newStoryIds);

            if (storiesToAdd.size() != newStoryIds.size()) {
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
    public PageModelResponse<CollectionSummaryResponse> getAllCollections(Long id, int pageNo, int pageSize) {
        Long userId = securityUtil.getUserIdFromToken();

        boolean isAllow = false;
        if (id.equals(userId)) {
            isAllow = true;
        } else {
            UserServiceProto.CheckFollowResponse followResponse =
                    userGrpcClient.checkFollow(userId, id);
            if (!followResponse.getIsPrivate() || followResponse.getIsFollow()) {
                isAllow = true;
            }
        }
        if (!isAllow)
            throw new CustomException("Dont allow to get Collection", HttpStatus.BAD_REQUEST);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<CollectionModel> page = collectionRepository.findByUserId(id, pageable);

        List<CollectionSummaryResponse> content = page.getContent().stream()
                .filter(collection -> !collection.getCollectionItemModelList().isEmpty())
                .map(collection -> {
                    String thumbnailUrl = collection.getCollectionItemModelList()
                            .getFirst()
                            .getStoryModel()
                            .getMediaURL();

                    return CollectionSummaryResponse.builder()
                            .id(collection.getId())
                            .collectionName(collection.getCollectionName())
                            .mediaUrl(thumbnailUrl)
                            .createdAt(collection.getCreatedAt())
                            .build();
                })
                .toList();
        return PageModelResponse.<CollectionSummaryResponse>builder()
                .content(content)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Override
    public CollectionDetailResponse getCollectionDetail(Long collectionId) {
        CollectionModel collectionModel = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new CustomException("Collection not found", HttpStatus.BAD_REQUEST));

        securityUtil.checkOwner(collectionModel.getUserId());
        Long currentUserId = collectionModel.getUserId();

        List<StoryResponse.Story> storyList = collectionModel.getCollectionItemModelList().stream()
                .map(CollectionItemModel::getStoryModel)
                .map(storyModel -> toStoryResponse(storyModel, currentUserId))
                .collect(Collectors.toList());

        return CollectionDetailResponse.builder()
                .id(collectionModel.getId())
                .collectionName(collectionModel.getCollectionName())
                .stories(storyList)
                .build();
    }

    @Override
    public CollectionDetailResponse getFriendCollectionDetail(Long collectionId) {
        Long viewerId = securityUtil.getUserIdFromToken();

        CollectionModel collectionModel = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new CustomException("Collection not found", HttpStatus.NOT_FOUND));

        Long ownerId = collectionModel.getUserId();

        if (viewerId.equals(ownerId)) {
            throw new CustomException("Please use the /collections/my/{id} endpoint for your own collections", HttpStatus.BAD_REQUEST);
        }

        UserServiceProto.CheckFollowResponse followStatus = userGrpcClient.checkFollow(viewerId, ownerId);

        if (followStatus.getIsPrivate() && !followStatus.getIsFollow()) {
            throw new CustomException("This account is private. Follow them to see their collections.", HttpStatus.FORBIDDEN);
        }

        boolean isCloseFriendWithOwner = followStatus.getIsCloseFriend();

        List<StoryResponse.Story> storyList = collectionModel.getCollectionItemModelList().stream()
                .map(CollectionItemModel::getStoryModel)
                .filter(story -> !story.getIsClosedFriend() || isCloseFriendWithOwner)
                .map(storyModel -> toStoryResponse(storyModel, viewerId))
                .collect(Collectors.toList());

        return CollectionDetailResponse.builder()
                .id(collectionModel.getId())
                .collectionName(collectionModel.getCollectionName())
                .stories(storyList)
                .build();
    }

    @Override
    public String deleteCollection(Long id) {
        CollectionModel collectionModel = collectionRepository.findById(id).orElseThrow(() -> new CustomException("Collection is not exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(collectionModel.getUserId());
        collectionRepository.delete(collectionModel);
        return "Success";
    }

    @Override
    public String archiveStory(Long id) {
        StoryModel model = storyRepository.findById(id).orElseThrow(() -> new CustomException("Story is not exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(model.getUserId());
        model.setIsArchive(true);
        model.setIsActive(false);
        storyRepository.save(model);
        return "Success";
    }

    @Override
    public String viewStory(Long id) {
        Long userId = securityUtil.getUserIdFromToken();
        StoryViewModel existingViews = storyViewRepository.findByStoryModel_IdAndSeenUserId(id, userId).orElse(null);

        if (existingViews == null) {
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
    public PageModelResponse<ViewDetailStoryResponse> viewDetailStory(Long id, int pageNo, int pageSize) {
        StoryModel story = storyRepository.findById(id)
                .orElseThrow(() -> new CustomException("Story does not exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(story.getUserId());

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.ASC, "createdAt"));

        Page<StoryViewModel> page = storyViewRepository.findByStoryModel_Id(id, pageable);

        List<ViewDetailStoryResponse> viewDetailStoryResponses = new ArrayList<>();
        for (StoryViewModel storyViewModel : page.getContent()) {
            UserServiceProto.UserDTOResponse userDTOResponse = userGrpcClient.getUserDTOById(storyViewModel.getSeenUserId());
            viewDetailStoryResponses.add(
                    ViewDetailStoryResponse.builder()
                            .avatarUrl(userDTOResponse.getAvatar())
                            .isLike(storyViewModel.getIsLike())
                            .userName(userDTOResponse.getUsername())
                            .createdAt(storyViewModel.getCreatedAt())
                            .build()
            );
        }

        return PageModelResponse.<ViewDetailStoryResponse>builder()
                .content(viewDetailStoryResponses)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }


    @Override
    public PageModelResponse<StoryResponse> getAllStoryOfFriend(Long id, int pageNo, int pageSize) {
        securityUtil.checkOwner(id);
        Long userId = securityUtil.getUserIdFromToken();
        UserServiceProto.GetUserFolloweesResponse response = userGrpcClient.getUserFollowees(userId);

        if (!response.getSuccess()) {
            throw new CustomException("Không lấy được danh sách followees: " + response.getMessage(), HttpStatus.BAD_REQUEST);
        }

        List<StoryResponse> storyResponseList = new ArrayList<>();
        for (UserServiceProto.FolloweeInfo followeeInfo : response.getFolloweesList()) {
            Long friendId = followeeInfo.getUserId();
            List<StoryResponse.Story> storyList = new ArrayList<>();
            List<StoryModel> listStory1 = storyRepository.findAllByUserIdAndIsActiveAndIsClosedFriend(friendId, true, false);
            listStory1.forEach(model -> storyList.add(toStoryResponse(model, userId)));

            if (followeeInfo.getIsCloseFriend()) {
                List<StoryModel> listStory2 = storyRepository.findAllByUserIdAndIsActiveAndIsClosedFriend(friendId, true, true);
                listStory2.forEach(model -> storyList.add(toStoryResponse(model, userId)));
            }

            if (!storyList.isEmpty()) {
                storyResponseList.add(StoryResponse.builder()
                        .userName(followeeInfo.getUserName())
                        .avatarUrl(followeeInfo.getAvatar())
                        .userId(friendId)
                        .storyList(storyList)
                        .build());
            }

        }
        int start = pageNo * pageSize;
        if (start >= storyResponseList.size()) {
            return PageModelResponse.<StoryResponse>builder()
                    .content(List.of())
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .totalElements(storyResponseList.size())
                    .totalPages((int) Math.ceil((double) storyResponseList.size() / pageSize))
                    .build();
        }

        int end = Math.min(start + pageSize, storyResponseList.size());
        List<StoryResponse> pagedList = storyResponseList.subList(start, end);

        return PageModelResponse.<StoryResponse>builder()
                .content(pagedList)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(storyResponseList.size())
                .totalPages((int) Math.ceil((double) storyResponseList.size() / pageSize))
                .build();
    }

    @Override
    public PageModelResponse<StoryResponse> getStoriesOfUser(Long ownerId, int pageNo, int pageSize) {
        Long viewerId = securityUtil.getUserIdFromToken();

        boolean isAllow = false;
        if (ownerId.equals(viewerId)) {
            isAllow = true;
        } else {
            UserServiceProto.CheckFollowResponse response2 = userGrpcClient.checkFollow(viewerId, ownerId);
            if (!response2.getIsPrivate() || response2.getIsFollow()) {
                isAllow = true;
            }
        }

        if (!isAllow)
            throw new CustomException("Dont allow to get story", HttpStatus.BAD_REQUEST);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<StoryModel> storyPage = storyRepository.findByUserIdAndIsActive(ownerId, true, pageable);

        if (storyPage.isEmpty()) {
            return PageModelResponse.<StoryResponse>builder()
                    .content(Collections.emptyList())
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .totalElements(0)
                    .totalPages(0)
                    .build();
        }


        UserServiceProto.UserDTOResponse ownerInfo = userGrpcClient.getUserDTOById(ownerId);
        List<StoryResponse.Story> storyList = storyPage.getContent().stream()
                .map(model -> toStoryResponse(model, viewerId))
                .collect(Collectors.toList());

        StoryResponse storyResponse = StoryResponse.builder()
                .userName(ownerInfo.getUsername())
                .avatarUrl(ownerInfo.getAvatar())
                .userId(ownerId)
                .storyList(storyList)
                .build();

        return PageModelResponse.<StoryResponse>builder()
                .content(List.of(storyResponse))
                .pageNo(storyPage.getNumber())
                .pageSize(storyPage.getSize())
                .totalElements(storyPage.getTotalElements())
                .totalPages(storyPage.getTotalPages())
                .build();
    }


    @Override
    public PageModelResponse<StoryResponse> getAllStoriesOfUser(Long id, int pageNo, int pageSize) {
        securityUtil.checkOwner(id);
        UserServiceProto.UserDTOResponse user = userGrpcClient.getUserDTOById(id);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").ascending());

        Page<StoryModel> storyPage = storyRepository.findByUserId(id, pageable);

        List<StoryResponse.Story> storyList = storyPage.getContent().stream()
                .map(model -> toStoryResponse(model, id))
                .collect(Collectors.toList());

        StoryResponse storyResponse = StoryResponse.builder()
                .userId(id)
                .userName(user.getUsername())
                .avatarUrl(user.getAvatar())
                .storyList(storyList)
                .build();

        return PageModelResponse.<StoryResponse>builder()
                .content(List.of(storyResponse))
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(storyPage.getTotalElements())
                .totalPages(storyPage.getTotalPages())
                .build();
    }


    private StoryResponse.Story toStoryResponse(StoryModel model, Long currentUserId) {
        boolean isLike = false;
        boolean isSeen = false;
        if (!Objects.equals(model.getUserId(), currentUserId)) {
            StoryViewModel storyViewModel = storyViewRepository.findByStoryModel_IdAndSeenUserId(model.getId(), currentUserId).orElse(null);
            isLike = storyViewModel != null && storyViewModel.getIsLike();
            isSeen = storyViewModel != null;
        }

        return StoryResponse.Story.builder()
                .createdAt(model.getCreatedAt())
                .storyId(model.getId())
                .mediaUrl(model.getMediaURL())
//                .mediaType(model.getMediaType().name())
                .isLike(isLike)
                .isSeen(isSeen)
                .isActive(model.getIsActive())
                .isCloseFriend(model.getIsClosedFriend())
                .quantitySeen(Long.parseLong(String.valueOf(model.getViews().size())))
                .build();
    }

    @KafkaListener(topics = "story-deletion-topic", groupId = "story-deleter-group")
    public void consumeStoryDeletion(String storyId) {
        log.info("Received story ID from Kafka to delete: " + storyId);
        try {
            Long id = Long.parseLong(storyId.replaceAll("^\"|\"$", ""));
            StoryModel storyModel = storyRepository.findById(id).orElse(null);
            if (storyModel != null) {
                storyModel.setIsActive(false);
                storyRepository.save(storyModel);
                log.info("Successfully deleted story with ID: " + id);
            }
        } catch (NumberFormatException e) {
            log.error("Invalid story ID received: " + storyId);
        }
    }
}
