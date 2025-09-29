package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.postservice.dto.PostRequestDTO;
import org.nexo.postservice.dto.UserTagDTO;
import org.nexo.postservice.dto.response.PostResponseDTO;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.model.PostMediaModel;
import org.nexo.postservice.model.PostModel;
import org.nexo.postservice.model.ReelModel;
import org.nexo.postservice.repository.IPostMediaRepository;
import org.nexo.postservice.repository.IPostRepository;
import org.nexo.postservice.repository.IReelRepository;
import org.nexo.postservice.service.GrpcServiceImpl.client.UserGrpcClient;
import org.nexo.postservice.service.IHashTagService;
import org.nexo.postservice.service.IPostService;
import org.nexo.postservice.util.Enum.EVisibilityPost;
import org.nexo.postservice.util.Enum.SecurityUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements IPostService {

    private final IPostRepository postRepository;
    private final IReelRepository reelRepository;
    private final UserGrpcClient userGrpcClient;
    private final IPostMediaRepository postMediaRepository;
    private final AsyncFileService fileServiceClient;
    private final IHashTagService hashTagService;
    private final SecurityUtil securityUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public String savePost(PostRequestDTO postRequestDTO, List<MultipartFile> files) {
        securityUtil.checkOwner(postRequestDTO.getUserId());
        UserServiceProto.GetUserFolloweesResponse response = userGrpcClient.getUserFollowees(postRequestDTO.getUserId());

        if (!response.getSuccess()) {
            throw new CustomException("Không lấy được danh sách followees: " + response.getMessage(), HttpStatus.BAD_REQUEST);
        }

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
        if (postRequestDTO.getPostId() == 0) {
            String event = "{ \"postId\": " + model.getId() + ", \"authorId\": " + postRequestDTO.getUserId() + ", \"createdAt\": " + Instant.now().toEpochMilli() + " }";

            redisTemplate.convertAndSend("post-created", event);
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

    @Override
    public List<PostResponseDTO> getAllPostOfUser(Long id) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto response = userGrpcClient.getUserByKeycloakId(keyloakId);
        List<PostModel> listPost = new ArrayList<>();
        List<PostResponseDTO> postResponseList = new ArrayList<>();


        Boolean isAllow = false;
        if (id == response.getUserId()) {
            isAllow = true;
            listPost = postRepository.findByUserId(id);
        } else {
            UserServiceProto.CheckFollowResponse response2 = userGrpcClient.checkFollow(response.getUserId(), id);
            if (!response2.getIsPrivate() || response2.getIsFollow()) {
                listPost = postRepository.findByUserIdAndIsActive(id, true);
                isAllow = true;
            }
        }
        if (!isAllow)
            throw new CustomException("Dont allow to get story", HttpStatus.BAD_REQUEST);

        UserServiceProto.UserDTOResponse response3 = userGrpcClient.getUserDTOById(id);
        if (!listPost.isEmpty()) {
            for (PostModel model : listPost) {
                postResponseList.add(convertToPostResponseDTO(model, response3));
            }

        }
        return postResponseList;
    }

    @Override
    public PostResponseDTO getPostById(Long id) {
        PostModel model = postRepository.findById(id).orElseThrow(() -> new CustomException("Post is not exist", HttpStatus.BAD_REQUEST));
        UserServiceProto.UserDTOResponse response = userGrpcClient.getUserDTOById(model.getUserId());
        return convertToPostResponseDTO(model, response);
    }

    @Override
    public String deleteReel(Long id) {
        ReelModel model = reelRepository.findById(id).orElseThrow(() -> new CustomException("Reel is not  exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(model.getUserId());
        reelRepository.delete(model);
        return "Success";
    }

    PostResponseDTO convertToPostResponseDTO(PostModel model, UserServiceProto.UserDTOResponse userDto) {
        List<UserTagDTO> userTagDTOList = new ArrayList<>();
        if (!model.getTag().isEmpty()) {
            for (String id : model.getTag().split(",")) {
                UserServiceProto.UserDTOResponse response = userGrpcClient.getUserDTOById(Long.parseLong(id));
                userTagDTOList.add(UserTagDTO.builder()
                        .userId(response.getId())
                        .userName(response.getUsername())
                        .build());
            }
        }

        return PostResponseDTO.builder()
                .postId(model.getId())
                .userName(userDto.getUsername())
                .avatarUrl(userDto.getAvatar())
                .visibility(model.getVisibility().toString())
                .tag(model.getTag())
                .listUserTag(userTagDTOList)
                .caption(model.getCaption())
                .createdAt(model.getCreatedAt())
                .isActive(model.getIsActive())
                .quantityLike(99999L)
                .quantityComment(99999L)
                .userId(model.getUserId())
                .mediaUrl(model.getPostMediaModels().stream().map(PostMediaModel::getMediaUrl).toList())
                .build();
    }
}
