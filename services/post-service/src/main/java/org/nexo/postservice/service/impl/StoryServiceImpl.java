package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.postservice.dto.StoryRequestDto;
import org.nexo.postservice.dto.response.PageModelResponse;
import org.nexo.postservice.dto.response.StoryResponse;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.model.StoryModel;
import org.nexo.postservice.model.StoryViewModel;
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

    @Override
    public String saveStory(StoryRequestDto dto, List<MultipartFile> files) {
        securityUtil.checkOwner(dto.getUserId());
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
        StoryViewModel model = StoryViewModel.builder()
                .isLike(false)
                .seenUserId(userId)
                .storyModel(storyRepository.findById(id).orElseThrow(() -> new CustomException("Story is not exist", HttpStatus.BAD_REQUEST)))
                .build();
        storyViewRepository.save(model);
        return "Success";
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
            List<StoryModel> listStory1 = storyRepository.findByUserIdAndIsActive(friendId, true);
            listStory1.forEach(model -> storyList.add(toStoryResponse(model, userId)));

            if (followeeInfo.getIsCloseFriend()) {
                List<StoryModel> listStory2 = storyRepository.findByUserIdAndIsActiveAndIsClosedFriend(friendId, true, true);
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
    public PageModelResponse<StoryResponse> getStoriesOfUser(Long id, int pageNo, int pageSize) {
        String klId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto response = userGrpcClient.getUserByKeycloakId(klId);
        UserServiceProto.UserDTOResponse response4 = userGrpcClient.getUserDTOById(response.getUserId());

        boolean isAllow = false;
        if (id.equals(response.getUserId())) {
            isAllow = true;
        } else {
            UserServiceProto.CheckFollowResponse response2 = userGrpcClient.checkFollow(response.getUserId(), id);
            if (!response2.getIsPrivate() || response2.getIsFollow()) {
                isAllow = true;
            }
        }

        if (!isAllow)
            throw new CustomException("Dont allow to get story", HttpStatus.BAD_REQUEST);

        List<StoryModel> allStories = storyRepository.findByUserIdAndIsActive(id, true);

        if (allStories.isEmpty()) {
            return PageModelResponse.<StoryResponse>builder()
                    .content(Collections.emptyList())
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .totalElements(0)
                    .totalPages(0)
                    .build();
        }

        int totalStories = allStories.size();
        int totalPages = (int) Math.ceil((double) totalStories / pageSize);

        if (pageNo < 0) pageNo = 0;
        if (pageNo >= totalPages) pageNo = totalPages - 1;

        int start = pageNo * pageSize;
        int end = Math.min(start + pageSize, totalStories);

        List<StoryModel> pagedStories = allStories.subList(start, end);

        List<StoryResponse.Story> storyList = new ArrayList<>();
        pagedStories.forEach(model -> storyList.add(toStoryResponse(model, id)));

        StoryResponse storyResponse = StoryResponse.builder()
                .userName(response4.getUsername())
                .avatarUrl(response4.getAvatar())
                .userId(id)
                .storyList(storyList)
                .build();

        return PageModelResponse.<StoryResponse>builder()
                .content(List.of(storyResponse))
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(totalStories)
                .totalPages(totalPages)
                .build();
    }


    @Override
    public PageModelResponse<StoryResponse> getAllStoriesOfUser(Long id, int pageNo, int pageSize) {
        securityUtil.checkOwner(id);
        UserServiceProto.UserDTOResponse user = userGrpcClient.getUserDTOById(id);
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());

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
