package org.nexo.postservice.service;

import org.nexo.postservice.dto.StoryRequestDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IStoryService {
    String saveStory(StoryRequestDto dto, List<MultipartFile> files);
}
