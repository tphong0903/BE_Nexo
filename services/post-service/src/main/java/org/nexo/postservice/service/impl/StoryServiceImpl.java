package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.postservice.dto.StoryRequestDto;
import org.nexo.postservice.dto.response.StoryResponse;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.model.StoryModel;
import org.nexo.postservice.model.StoryViewModel;
import org.nexo.postservice.repository.IStoryRepository;
import org.nexo.postservice.repository.IStoryViewRepository;
import org.nexo.postservice.service.GrpcServiceImpl.client.UserGrpcClient;
import org.nexo.postservice.service.IStoryService;
import org.nexo.postservice.util.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StoryServiceImpl implements IStoryService {
    private final AsyncFileService fileServiceClient;
    private final IStoryRepository storyRepository;
    private final IStoryViewRepository storyViewRepository;
    private final SecurityUtil securityUtil;
    private final UserGrpcClient userGrpcClient;

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
    public List<StoryResponse> getAllStoryOfFriend(Long id) {
        securityUtil.checkOwner(id);
        Long userId = securityUtil.getUserIdFromToken();
        UserServiceProto.GetUserFolloweesResponse response = userGrpcClient.getUserFollowees(userId);

        if (!response.getSuccess()) {
            throw new CustomException("Không lấy được danh sách followees: " + response.getMessage(), HttpStatus.BAD_REQUEST);
        }

        List<UserServiceProto.FolloweeInfo> listFriend = response.getFolloweesList();
        List<StoryResponse> storyResponseList = new ArrayList<>();
        for (UserServiceProto.FolloweeInfo followeeInfo : listFriend) {
            Long friendId = followeeInfo.getUserId();
            List<StoryResponse.Story> storyList = new ArrayList<>();
            List<StoryModel> listStory1 = storyRepository.findByUserIdAndIsActive(friendId, true);
            listStory1.forEach(model -> storyList.add(toStoryResponse(model, userId)));

            if (followeeInfo.getIsCloseFriend()) {
                List<StoryModel> listStory2 = storyRepository.findByUserIdAndIsActiveAndIsClosedFriend(friendId, true, true);
                listStory2.forEach(model -> storyList.add(toStoryResponse(model, userId)));
            }

            if (!storyList.isEmpty()) {
                StoryResponse storyResponse = StoryResponse.builder()
                        .userName(followeeInfo.getUserName())
                        .avatarUrl(followeeInfo.getAvatar())
                        .userId(userId)
                        .storyList(storyList)
                        .build();
                storyResponseList.add(storyResponse);
            }

        }
        return storyResponseList;
    }

    @Override
    public List<StoryResponse> getStoriesOfUser(Long id) {
        String klId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto response = userGrpcClient.getUserByKeycloakId(klId);
        UserServiceProto.UserDTOResponse response4 = userGrpcClient.getUserDTOById(response.getUserId());


        Boolean isAllow = false;
        if (id == response.getUserId()) {
            isAllow = true;
        } else {
            UserServiceProto.CheckFollowResponse response2 = userGrpcClient.checkFollow(response.getUserId(), id);
            if (!response2.getIsPrivate() || response2.getIsFollow()) {
                isAllow = true;
            }
        }
        if (!isAllow)
            throw new CustomException("Dont allow to get story", HttpStatus.BAD_REQUEST);

        List<StoryResponse> storyResponseList = new ArrayList<>();
        List<StoryResponse.Story> storyList = new ArrayList<>();
        List<StoryModel> listStory1 = storyRepository.findByUserIdAndIsActive(id, true);
        listStory1.forEach(model -> storyList.add(toStoryResponse(model, id)));
        if (!storyList.isEmpty()) {
            StoryResponse storyResponse = StoryResponse.builder()
                    .userName(response.getUsername())
                    .avatarUrl(response4.getAvatar())
                    .userId(response.getUserId())
                    .storyList(storyList)
                    .build();
            storyResponseList.add(storyResponse);
        }
        return storyResponseList;
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
                .creatAt(model.getCreatedAt())
                .storyId(model.getId())
                .mediaUrl(model.getMediaURL())
                .mediaType(model.getMediaType().name())
                .isLike(isLike)
                .isSeen(isSeen)
                .isActive(model.getIsActive())
                .isCloseFriend(model.getIsClosedFriend())
                .build();
    }

}
