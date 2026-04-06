package org.nexo.userservice.service.Impl;

import java.time.LocalDateTime;
import java.util.List;

import org.nexo.userservice.dto.RecommendationFollowExportDTO;
import org.nexo.userservice.dto.RecommendationUserExportDTO;
import org.nexo.userservice.repository.FollowRepository;
import org.nexo.userservice.repository.UserRepository;
import org.nexo.userservice.service.RecommendationDataExportService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationDataExportServiceImpl implements RecommendationDataExportService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Override
    public List<RecommendationUserExportDTO> exportUsers() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime activitySince = now.minusDays(30);
        LocalDateTime postSince = now.minusDays(7);
        return userRepository.findAllForRecommendationExport(activitySince, postSince);
    }

    @Override
    public List<RecommendationFollowExportDTO> exportFollows() {
        return followRepository.findAllForRecommendationExport();
    }
}
