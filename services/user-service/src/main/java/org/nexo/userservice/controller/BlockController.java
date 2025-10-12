package org.nexo.userservice.controller;

import org.nexo.userservice.dto.FolloweeDTO;
import org.nexo.userservice.dto.PageModelResponse;
import org.nexo.userservice.dto.ResponseData;
import org.nexo.userservice.dto.UserDTOResponse;
import org.nexo.userservice.service.BlockService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class BlockController {

    private final BlockService blockService;


    @PostMapping("/{username}/block")
    public ResponseData<?> blockUser(
            @PathVariable String username,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        blockService.blockUser(token, username);
        return ResponseData.builder()
                .status(200)
                .message("User blocked successfully")
                .data(null)
                .build();
    }
    
    @DeleteMapping("/{username}/block")
    public ResponseData<?> unblockUser(
            @PathVariable String username,
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        blockService.unblockUser(token, username);
        return ResponseData.builder()
                .status(200)
                .message("User unblocked successfully")
                .data(null)
                .build();
    }
    @GetMapping("/blocked-users")
    public ResponseData<?> getBlockedUsers(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @RequestParam(value = "search", required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        String accessToken = authHeader.replace("Bearer ", "").trim();
        PageModelResponse<UserDTOResponse> blockedUsers = blockService.getBlockedUsers(accessToken, pageable, search);
        return ResponseData.builder()
                .status(200)
                .message("Blocked users retrieved successfully")
                .data(blockedUsers)
                .build();
    }
}
