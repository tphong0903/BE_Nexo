package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.postservice.dto.StoryRequestDto;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.model.StoryModel;
import org.nexo.postservice.repository.IStoryRepository;
import org.nexo.postservice.service.IStoryService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryServiceImpl implements IStoryService {
    private final AsyncFileService fileServiceClient;
    private final IStoryRepository storyRepository;

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
}
