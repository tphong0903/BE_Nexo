package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.postservice.dto.PostRequestDTO;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.model.PostMediaModel;
import org.nexo.postservice.model.PostModel;
import org.nexo.postservice.model.ReelModel;
import org.nexo.postservice.repository.IPostMediaRepository;
import org.nexo.postservice.repository.IPostRepository;
import org.nexo.postservice.repository.IReelRepository;
import org.nexo.postservice.service.IHashTagService;
import org.nexo.postservice.service.IPostService;
import org.nexo.postservice.util.Enum.EVisibilityPost;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements IPostService {
    private final IPostRepository postRepository;
    private final IReelRepository reelRepository;
    private final IPostMediaRepository postMediaRepository;
    private final AsyncFileService fileServiceClient;
    private final IHashTagService hashTagService;

    @Override
    public String savePost(PostRequestDTO postRequestDTO, List<MultipartFile> files) {
        PostModel model;
        if (postRequestDTO.getPostID() != 0) {
            List<PostMediaModel> postMediaModelList = postMediaRepository.findAllByPostModel_Id(postRequestDTO.getPostID());
            for (PostMediaModel postMediaModel : postMediaModelList) {
                if (!postRequestDTO.getMediaUrl().contains(postMediaModel.getMediaUrl()))
                    postMediaRepository.delete(postMediaModel);
            }
            model = postRepository.findById(postRequestDTO.getPostID())
                    .orElseThrow(() -> new CustomException("Post not found", HttpStatus.BAD_REQUEST));
            model.setCaption(postRequestDTO.getCaption());
            model.setTag(postRequestDTO.getTag());
            model.setVisibility(EVisibilityPost.valueOf(postRequestDTO.getVisibility()));
            model.setIsActive(true);
        } else {
            model = PostModel.builder()
                    .userId(postRequestDTO.getUserId())
                    .caption(postRequestDTO.getCaption())
                    .tag(postRequestDTO.getTag())
                    .visibility(EVisibilityPost.valueOf(postRequestDTO.getVisibility()))
                    .isActive(true)
                    .build();
        }
        postRepository.save(model);
        if (files != null && !files.isEmpty() && !files.getFirst().isEmpty()) {
            fileServiceClient.savePostMedia(files, model.getId());
        }
        hashTagService.findAndAddHashTagFromCaption(model);
        return "Success";
    }

    @Override
    public String saveReel(PostRequestDTO postRequestDTO, List<MultipartFile> files) {
        ReelModel model;
        if (postRequestDTO.getPostID() != 0) {
            model = reelRepository.findById(postRequestDTO.getPostID())
                    .orElseThrow(() -> new CustomException("Post not found", HttpStatus.BAD_REQUEST));
            model.setCaption(postRequestDTO.getCaption());
            model.setVisibility(EVisibilityPost.valueOf(postRequestDTO.getVisibility()));
            model.setIsActive(true);
        } else {
            model = ReelModel.builder()
                    .userId(postRequestDTO.getUserId())
                    .caption(postRequestDTO.getCaption())
                    .visibility(EVisibilityPost.valueOf(postRequestDTO.getVisibility()))
                    .isActive(true)
                    .build();
        }
        reelRepository.save(model);
        if (files != null && !files.isEmpty() && !files.getFirst().isEmpty()) {
            fileServiceClient.saveReelMedia(files, model.getId());
        }
        hashTagService.findAndAddHashTagFromCaption(model);
        return "Success";
    }

    @Override
    public String inactivePost(Long id) {
        PostModel model = postRepository.findById(id).orElseThrow(() -> new CustomException("Post is not  exist", HttpStatus.BAD_REQUEST));
        model.setIsActive(!model.getIsActive());
        postRepository.save(model);
        return "Success";
    }

    @Override
    public String deletePost(Long id) {
        PostModel model = postRepository.findById(id).orElseThrow(() -> new CustomException("Post is not  exist", HttpStatus.BAD_REQUEST));
        postRepository.delete(model);
        return "Success";
    }


}
