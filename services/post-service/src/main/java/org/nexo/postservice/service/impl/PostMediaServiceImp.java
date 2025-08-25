package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.postservice.dto.PostMediaDTO;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.grpc.PostMediaServiceProto.PostMediaRequestDTO;
import org.nexo.postservice.model.PostMediaModel;
import org.nexo.postservice.repository.IPostMediaRepository;
import org.nexo.postservice.repository.IPostRepository;
import org.nexo.postservice.service.IPostMediaService;
import org.nexo.postservice.util.Enum.EMediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class PostMediaServiceImp implements IPostMediaService {
    private final IPostMediaRepository postMediaRepository;
    private final IPostRepository postRepository;
    @Override
    public String savePostMedia(List<PostMediaDTO> postMediaDTOs) {
        for(PostMediaDTO item : postMediaDTOs){
            PostMediaModel model = PostMediaModel.builder()
                    .mediaOrder(item.getMediaOrder())
                    .mediaType(EMediaType.valueOf(item.getMediaType()))
                    .mediaUrl(item.getMediaUrl())
                    .postModel(postRepository.findById(item.getPostId()).orElseThrow(() -> new CustomException("Post is not exist", HttpStatus.BAD_REQUEST)))
                    .build();

            postMediaRepository.save(model);
        }
        return "Success";
    }

    @Override
    public List<PostMediaRequestDTO> findPostMediasOfPost(Long postId) {
        return postMediaRepository.findAllByPostModel_Id(postId).stream().map(PostMediaServiceImp::convertToDTO).toList();
    }

    @Override
    public void deletePostMedia(Long postMediaId) {
        postMediaRepository.delete(postMediaRepository.findById(postMediaId).orElseThrow(()->new CustomException("Post media is not exist",HttpStatus.BAD_REQUEST)));
    }

    public static PostMediaRequestDTO convertToDTO(PostMediaModel model){
        return  PostMediaRequestDTO.newBuilder()
                .setMediaOrder(model.getMediaOrder())
                .setMediaType(model.getMediaType().toString())
                .setMediaUrl(model.getMediaUrl())
                .setPostMediaId(model.getId())
                .setPostID(model.getId())
                .build();
    }
}
