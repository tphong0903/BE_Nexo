package org.nexo.userservice.service;

import java.util.List;

import org.nexo.userservice.dto.RecommendationBlockExportDTO;
import org.nexo.userservice.dto.RecommendationFollowExportDTO;
import org.nexo.userservice.dto.RecommendationUserExportDTO;

public interface RecommendationDataExportService {
    List<RecommendationUserExportDTO> exportUsers();

    List<RecommendationFollowExportDTO> exportFollows();

    List<RecommendationBlockExportDTO> exportBlocks();
}
