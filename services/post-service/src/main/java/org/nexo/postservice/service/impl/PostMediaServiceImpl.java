package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.postservice.dto.PostMediaDTO;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.grpc.PostMediaServiceProto.PostMediaRequestDTO;
import org.nexo.postservice.model.PostMediaModel;
import org.nexo.postservice.model.PostModel;
import org.nexo.postservice.repository.IPostMediaRepository;
import org.nexo.postservice.repository.IPostRepository;
import org.nexo.postservice.service.IPostMediaService;
import org.nexo.postservice.util.Enum.EMediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostMediaServiceImpl implements IPostMediaService {

    private final IPostMediaRepository postMediaRepository;
    private final IPostRepository postRepository;

    public static PostMediaRequestDTO convertToDTO(PostMediaModel model) {
        return PostMediaRequestDTO.newBuilder()
                .setMediaOrder(model.getMediaOrder())
                .setMediaType(model.getMediaType().name())
                .setMediaUrl(model.getMediaUrl())
                .setPostMediaId(model.getId())
                .setPostID(model.getPostModel() != null ? model.getPostModel().getId() : 0L)
                .build();
    }

    @Override
    @Transactional
    public String savePostMedia(List<PostMediaDTO> postMediaDTOs) {
        if (postMediaDTOs == null || postMediaDTOs.isEmpty()) {
            return "No media to save";
        }

        Set<Long> postIds = postMediaDTOs.stream()
                .map(PostMediaDTO::getPostId)
                .collect(Collectors.toSet());

        List<PostModel> posts = postRepository.findAllById(postIds);
        Map<Long, PostModel> postMap = posts.stream()
                .collect(Collectors.toMap(PostModel::getId, post -> post));

        if (postMap.size() < postIds.size()) {
            throw new CustomException("One or more Posts do not exist", HttpStatus.BAD_REQUEST);
        }

        List<PostMediaModel> mediaModels = postMediaDTOs.stream().map(item ->
                PostMediaModel.builder()
                        .mediaOrder(item.getMediaOrder())
                        .mediaType(EMediaType.valueOf(item.getMediaType()))
                        .mediaUrl(item.getMediaUrl())
                        .postModel(postMap.get(item.getPostId()))
                        .build()
        ).toList();

        postMediaRepository.saveAll(mediaModels);

        return "Success";
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostMediaRequestDTO> findPostMediasOfPost(Long postId) {
        return postMediaRepository.findAllByPostModel_Id(postId).stream()
                .map(PostMediaServiceImpl::convertToDTO)
                .toList();
    }

    @Override
    @Transactional
    public void deletePostMedia(Long postMediaId) {
        PostMediaModel media = postMediaRepository.findById(postMediaId)
                .orElseThrow(() -> new CustomException("Post media does not exist", HttpStatus.BAD_REQUEST));
        postMediaRepository.delete(media);
    }
}