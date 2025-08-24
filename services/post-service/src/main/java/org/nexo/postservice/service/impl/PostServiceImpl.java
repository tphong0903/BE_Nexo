package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.postservice.dto.PostRequestDTO;
import org.nexo.postservice.model.PostModel;
import org.nexo.postservice.repository.IPostRepository;
import org.nexo.postservice.service.IPostService;
import org.nexo.postservice.util.Enum.EVisibilityPost;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements IPostService {
    private final IPostRepository postRepository;
    private final AsyncFileService fileServiceClient;
    @Override
    public String savePost(PostRequestDTO postRequestDTO, List<MultipartFile> files) {
        PostModel model;
        if (postRequestDTO.getPostID() != 0) {
            model = postRepository.findById(postRequestDTO.getPostID())
                    .orElseThrow(() -> new RuntimeException("Post not found"));
            model.setCaption(postRequestDTO.getCaption());
            model.setTag(postRequestDTO.getTag());
            model.setVisibility(EVisibilityPost.valueOf(postRequestDTO.getVisibility()));
        } else {
            model = PostModel.builder()
                    .userId(postRequestDTO.getUserId())
                    .caption(postRequestDTO.getCaption())
                    .tag(postRequestDTO.getTag())
                    .visibility(EVisibilityPost.valueOf(postRequestDTO.getVisibility()))
                    .build();
        }
        postRepository.save(model);
        if (files != null && !files.isEmpty()) {
            fileServiceClient.savePostMedia(files, String.valueOf(model.getId()));
        }

        return "Success";
    }
}
