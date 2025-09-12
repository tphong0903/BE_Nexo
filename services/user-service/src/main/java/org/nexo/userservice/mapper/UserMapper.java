package org.nexo.userservice.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.nexo.userservice.dto.UpdateUserRequest;
import org.nexo.userservice.dto.UserDTOResponse;
import org.nexo.userservice.model.UserModel;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTOResponse toUserDTOResponse(UserModel userModel);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    UserModel updateUserModelFromDTO(UpdateUserRequest updateUserRequest, @MappingTarget UserModel userModel);

}
