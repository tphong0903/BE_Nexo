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
    private static final String FEED_KEY_PREFIX = "feed:";
    private final IPostRepository postRepository;
    private final IReelRepository reelRepository;
    private final UserGrpcClient userGrpcClient;
    private final IPostMediaRepository postMediaRepository;
    private final AsyncFileService fileServiceClient;
    private final IHashTagService hashTagService;
    private final SecurityUtil securityUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AsyncFeedService asyncFeedService;

    @Override
    public String savePost(PostRequestDTO postRequestDTO, List<MultipartFile> files) {
        securityUtil.checkOwner(postRequestDTO.getUserId());
        UserServiceProto.GetUserFolloweesResponse response = userGrpcClient.getUserFollowees(postRequestDTO.getUserId());

        if (!response.getSuccess()) {
            throw new CustomException("Không lấy được danh sách followees: " + response.getMessage(), HttpStatus.BAD_REQUEST);
        }
        List<UserServiceProto.FolloweeInfo> listFriend = response.getFolloweesList();

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
            asyncFeedService.saveFeedAsync(model.getUserId(), listFriend, model.getId());
            String event = "{ \"postId\": " + model.getId()
                    + ", \"authorId\": " + postRequestDTO.getUserId()
                    + ", \"createdAt\": " + Instant.now().toEpochMilli() + " }";

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
        UserServiceProto.UserDto response = userGrpcClient.getUserById(id);
        List<PostModel> listPost;
        List<PostResponseDTO> postResponseList = new ArrayList<>();
        //TODO
//        UserServiceProto.CheckFollower response2 = userGrpcClient.checkFollwer(id, response.getUserId());
//
//        Boolean isAllow = false;
//        if (id == response.getUserId()) {
//            isAllow = true;
//            listPost = postRepository.findByUserId(id, true);
//        } else if (response2.getIsPublic || response2.getIsFollowed) {
//            isAllow = true;
//        }
//        if (!isAllow)
//            throw new CustomException("Dont allow to get story", HttpStatus.BAD_REQUEST);
        listPost = postRepository.findByUserIdAndIsActive(id, true);

        if (!listPost.isEmpty()) {
            for (PostModel model : listPost) {
                postResponseList.add(convertToPostResponseDTO(model, response));
            }

        }
        return postResponseList;
    }

    @Override
    public PostResponseDTO getPostById(Long id) {
        PostModel model = postRepository.findById(id).orElseThrow(() -> new CustomException("Post is not exist", HttpStatus.BAD_REQUEST));
        UserServiceProto.UserDto response = userGrpcClient.getUserById(id);
        return convertToPostResponseDTO(model, response);
    }

    @Override
    public String deleteReel(Long id) {
        ReelModel model = reelRepository.findById(id).orElseThrow(() -> new CustomException("Reel is not  exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(model.getUserId());
        reelRepository.delete(model);
        return "Success";
    }

    PostResponseDTO convertToPostResponseDTO(PostModel model, UserServiceProto.UserDto userDto) {
        List<UserTagDTO> userTagDTOList = new ArrayList<>();
        for (String id : model.getTag().split(",")) {
            UserServiceProto.UserDto response = userGrpcClient.getUserById(id);
            userTagDTOList.add(UserTagDTO.builder()
                    .userId(response.getUserId())
                    .userName(response.getUsername())
                    .build());
        }

        return PostResponseDTO.builder()
                .postId(model.getId())
                .userName(userDto.getUsername())
                .avatarUrl("Dang doi Quang")
                .visibility(model.getVisibility().toString())
                .tag(model.getTag())
                .listUserTag(userTagDTOList)
                .caption(model.getCaption())
                .createdAt(model.getCreatedAt())
                .isActive(model.getIsActive())
                .quantityLike(99999L)
                .quantityComment(99999L)
                .mediaUrl(model.getPostMediaModels().stream().map(PostMediaModel::getMediaUrl).toList())
                .build();
    }
}
