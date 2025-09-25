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
import org.nexo.postservice.util.Enum.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
    private final SecurityUtil securityUtil;

    @Override
    public String savePost(PostRequestDTO postRequestDTO, List<MultipartFile> files) {
        securityUtil.checkOwner(postRequestDTO.getUserId());

        PostModel model;
        if (postRequestDTO.getPostId() != 0) {
            List<PostMediaModel> postMediaModelList = postMediaRepository.findAllByPostModel_Id(postRequestDTO.getPostId());
            for (PostMediaModel postMediaModel : postMediaModelList) {
                if (!postRequestDTO.getMediaUrl().contains(postMediaModel.getMediaUrl()))
                    postMediaRepository.delete(postMediaModel);
            }
            model = postRepository.findById(postRequestDTO.getPostId())
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
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String token = ((JwtAuthenticationToken) auth).getToken().getTokenValue();
            fileServiceClient.savePostMedia(files, model.getId(), token);
        }
        hashTagService.findAndAddHashTagFromCaption(model);
        return "Success";
    }

    @Override
    public String saveReel(PostRequestDTO postRequestDTO, List<MultipartFile> files) {
        securityUtil.checkOwner(postRequestDTO.getUserId());

        ReelModel model;
        if (postRequestDTO.getPostId() != 0) {
            model = reelRepository.findById(postRequestDTO.getPostId())
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
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String token = ((JwtAuthenticationToken) auth).getToken().getTokenValue();
            fileServiceClient.saveReelMedia(files, model.getId(), token);
        }
        hashTagService.findAndAddHashTagFromCaption(model);
        return "Success";
    }

    @Override
    public String inactivePost(Long id) {

        PostModel model = postRepository.findById(id).orElseThrow(() -> new CustomException("Post is not  exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(model.getUserId());

        model.setIsActive(!model.getIsActive());
        postRepository.save(model);
        return "Success";
    }

    @Override
    public String inactiveReel(Long id) {
        ReelModel model = reelRepository.findById(id).orElseThrow(() -> new CustomException("Reel is not  exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(model.getUserId());
        model.setIsActive(!model.getIsActive());
        reelRepository.save(model);
        return "Success";
    }

    @Override
    public String deletePost(Long id) {
        PostModel model = postRepository.findById(id).orElseThrow(() -> new CustomException("Post is not  exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(model.getUserId());
        postRepository.delete(model);
        return "Success";
    }


}
