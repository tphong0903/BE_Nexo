package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.postservice.dto.StoryRequestDto;
import org.nexo.postservice.dto.response.StoryResponse;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.model.StoryModel;
import org.nexo.postservice.model.StoryViewModel;
import org.nexo.postservice.repository.IStoryRepository;
import org.nexo.postservice.repository.IStoryViewRepository;
import org.nexo.postservice.service.IStoryService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryServiceImpl implements IStoryService {
    private final AsyncFileService fileServiceClient;
    private final IStoryRepository storyRepository;
    private final IStoryViewRepository storyViewRepository;

    @Override
    public String saveStory(StoryRequestDto dto, List<MultipartFile> files) {
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
            fileServiceClient.saveStoryMedia(files, model.getId());
        }
        return "Success";
    }

    @Override
    public String deleteStory(Long id) {
        //TODO Check owner's story
        StoryModel model = storyRepository.findById(id).orElseThrow(() -> new CustomException("Story is not exist", HttpStatus.BAD_REQUEST));
        storyRepository.delete(model);
        return "Success";
    }

    @Override
    public String archiveStory(Long id) {
        //TODO Check owner's story
        StoryModel model = storyRepository.findById(id).orElseThrow(() -> new CustomException("Story is not exist", HttpStatus.BAD_REQUEST));
        model.setIsArchive(true);
        model.setIsActive(false);
        storyRepository.save(model);
        return "Success";
    }

    @Override
    public String viewStory(Long id) {
        //TODO Check who watch from jwt
        StoryViewModel model = StoryViewModel.builder()
                .isLike(false)
                .seenUserId(1L)
                .storyModel(storyRepository.findById(id).orElseThrow(() -> new CustomException("Story is not exist", HttpStatus.BAD_REQUEST)))
                .build();
        storyViewRepository.save(model);
        return "Success";
    }

    @Override
    public List<StoryResponse> getAllStoryOfFriend(Long id) {
        //TODO Goi GRPC qua UserService de lay list nguoi dang fl
        List<Object> listFriend = new ArrayList<>();
        List<StoryResponse> storyResponseList = new ArrayList<>();
        for (int i = 0; i < listFriend.size(); i++) {
            Long userId = 100L; // listFriend.get(i).getId()
            List<StoryResponse.Story> storyList = new ArrayList<>();
            List<StoryModel> listStory1 = storyRepository.findByUserIdAndIsActive(userId, true);
            for (StoryModel model : listStory1) {
                StoryResponse.Story story = StoryResponse.Story.builder()
                        .creatAt(model.getCreatedAt())
                        .storyId(model.getId())
                        .mediaUrl(model.getMediaURL())
                        .mediaType(String.valueOf(model.getMediaType()))
                        //TODO: .isLike check da xem va like chua
                        .isActive(model.getIsActive())
                        .isCloseFriend(model.getIsClosedFriend())
                        .build();
                storyList.add(story);
            }
            if (true) { //listFriend.get(i).getIsClosedFriend()
                List<StoryModel> listStory2 = storyRepository.findByUserIdAndIsActiveAndIsClosedFriend(userId, true, true);
                for (StoryModel model : listStory2) {
                    StoryResponse.Story story = StoryResponse.Story.builder()
                            .creatAt(model.getCreatedAt())
                            .storyId(model.getId())
                            .mediaUrl(model.getMediaURL())
                            .mediaType(String.valueOf(model.getMediaType()))
                            //TODO: .isLike check da xem va like chua
                            .isActive(model.getIsActive())
                            .isCloseFriend(model.getIsClosedFriend())
                            .build();
                    storyList.add(story);
                }
            }

            if (!storyList.isEmpty()) {
                StoryResponse storyResponse = StoryResponse.builder()
                        .userName("hehe") //listFriend.get(i).getName()
                        .avatarUrl("url") //listFriend.get(i).getAvatarUrl()
                        .userId(userId)
                        .storyList(storyList)
                        .build();
                storyResponseList.add(storyResponse);
            }

        }
        return storyResponseList;
    }
}
