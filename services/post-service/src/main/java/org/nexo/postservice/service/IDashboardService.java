package org.nexo.postservice.service;

import org.nexo.postservice.dto.response.ChartDataDto;
import org.nexo.postservice.dto.response.DashboardResponseDto;
import org.nexo.postservice.dto.response.PageModelResponse;
import org.nexo.postservice.dto.response.PostManagementInfo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface IDashboardService {
    DashboardResponseDto getDashboardData();

    ChartDataDto getPostsByTime(LocalDate startDate, LocalDate endDate);

    ChartDataDto getUsersByTime(LocalDate startDate, LocalDate endDate);

    ChartDataDto getInteractionsByTime(LocalDate startDate, LocalDate endDate);

    ChartDataDto getReportsByTime(LocalDate startDate, LocalDate endDate);

    PageModelResponse getAllPost(String search, int page, int limit, String type,
                                 String hashtag, String content, String authorName,
                                 LocalDateTime startDate, LocalDateTime endDate);

    PostManagementInfo getPostManagementInfo();


}
