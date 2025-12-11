package org.nexo.interactionservice.mapper;

import org.nexo.grpc.user.UserServiceProto;
import org.nexo.interactionservice.dto.response.FolloweeDTO;

import java.util.List;
import java.util.stream.Collectors;

public class FolloweeMapper {

    public static FolloweeDTO toFolloweeDTO(UserServiceProto.UserDTOResponse3 userDto) {
        Boolean isFollowing = null;
        Boolean hasRequestedFollow = null;

        switch (userDto.getIsFollow()) {
            case 1 -> {
                isFollowing = true;
                hasRequestedFollow = false;
            }
            case 2 -> {
                isFollowing = false;
                hasRequestedFollow = true;
            }
            default -> {
                isFollowing = false;
                hasRequestedFollow = false;
            }
        }

        return FolloweeDTO.builder()
                .userId(userDto.getId())
                .userName(userDto.getUsername())
                .avatar(userDto.getAvatar())
                .fullName(null)
                .isFollowing(isFollowing)
                .hasRequestedFollow(hasRequestedFollow)
                .build();
    }

    public static FolloweeDTO toFolloweeDTO2(UserServiceProto.UserDTOResponse userDto) {
        return FolloweeDTO.builder()
                .userId(userDto.getId())
                .userName(userDto.getUsername())
                .avatar(userDto.getAvatar())
                .fullName(null)
                .isFollowing(null)
                .hasRequestedFollow(null)
                .build();
    }

    public static List<FolloweeDTO> toFolloweeDTOList(List<UserServiceProto.UserDTOResponse3> userDtos) {
        return userDtos.stream()
                .map(FolloweeMapper::toFolloweeDTO)
                .collect(Collectors.toList());
    }
}
