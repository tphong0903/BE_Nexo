package org.nexo.userservice.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.nexo.userservice.dto.UpdateUserRequest;
import org.nexo.userservice.dto.UserDTOResponse;
import org.nexo.userservice.dto.UserProfileDTOResponse;
import org.nexo.userservice.model.FollowModel;
import org.nexo.userservice.model.UserModel;

@Mapper(componentModel = "spring")
public interface UserMapper {

    default Long map(List<FollowModel> value) {
        return value == null ? 0L : (long) value.size();
    }

    UserDTOResponse toUserDTOResponse(UserModel userModel);

    UserProfileDTOResponse toUserProfileDTOResponse(UserModel userModel);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    UserModel updateUserModelFromDTO(UpdateUserRequest updateUserRequest, @MappingTarget UserModel userModel);

}
