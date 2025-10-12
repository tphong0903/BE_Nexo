package org.nexo.postservice.service;

import org.nexo.postservice.dto.StoryRequestDto;
import org.nexo.postservice.dto.response.PageModelResponse;
import org.nexo.postservice.dto.response.StoryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IStoryService {
    String saveStory(StoryRequestDto dto, List<MultipartFile> files);

    String deleteStory(Long id);

    String archiveStory(Long id);

    String viewStory(Long id);

    PageModelResponse<StoryResponse> getAllStoryOfFriend(Long id, int pageNo, int pageSize);

    PageModelResponse<StoryResponse> getStoriesOfUser(Long id, int pageNo, int pageSize);

    PageModelResponse<StoryResponse> getAllStoriesOfUser(Long id, int pageNo, int pageSize);
}
