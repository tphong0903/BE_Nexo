package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.grpc.interaction.InteractionServiceOuterClass;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.postservice.dto.response.*;
import org.nexo.postservice.repository.*;
import org.nexo.postservice.service.GrpcServiceImpl.client.InteractionGrpcClient;
import org.nexo.postservice.service.GrpcServiceImpl.client.UserGrpcClient;
import org.nexo.postservice.service.IDashboardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final IPostRepository postRepository;
    private final IReelRepository reelRepository;
    private final IReportReelRepository reportReelRepository;
    private final IReportPostRepository reportPostRepository;
    private final UserGrpcClient userGrpcClient;
    private final InteractionGrpcClient interactionGrpcClient;
    private final AdminContentRepository adminContentRepository;

    @Override
    public DashboardResponseDto getDashboardData() {
        Long totalUser = userGrpcClient.getTotalUsers();
        Long totalPost = postRepository.count() + reelRepository.count();
        Long totalInteract = interactionGrpcClient.getTotalInteractions();
        Long quantityReport = reportReelRepository.count() + reportPostRepository.count();

        Double percentUser = userGrpcClient.getPercentUsersInThisMonth();
        Double percentPost = getPercentPostInMonth();
        Double percentInteract = interactionGrpcClient.getPercentInteractionsInThisMonth();

        return DashboardResponseDto.builder()
                .totalUser(totalUser)
                .totalPost(totalPost)
                .totalInteract(totalInteract)
                .quantityReport(quantityReport)
                .percentUser(percentUser)
                .percentPost(percentPost)
                .percentInteract(percentInteract)
                .build();
    }

    public Double getPercentPostInMonth() {
        LocalDateTime startOfThisMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfLastMonth = startOfThisMonth.minusMonths(1);

        long thisMonth = countTotalContentBetween(startOfThisMonth, startOfThisMonth.plusMonths(1));
        long lastMonth = countTotalContentBetween(startOfLastMonth, startOfThisMonth);

        if (lastMonth == 0) return 0.0;
        return ((double) (thisMonth - lastMonth) / lastMonth) * 100;
    }

    @Override
    public ChartDataDto getPostsByTime(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        Map<LocalDate, Long> reportMap = new TreeMap<>();
        mergeQueryResultToMap(postRepository.countPostsByDate(startDateTime, endDateTime), reportMap);

        return buildChartDataFromMap(reportMap);
    }

    @Override
    public ChartDataDto getUsersByTime(LocalDate startDate, LocalDate endDate) {
        List<UserServiceProto.UserCountByDate> userCounts = userGrpcClient.getUsersByTime(startDate, endDate);

        List<String> dates = userCounts.stream().map(UserServiceProto.UserCountByDate::getDate).toList();
        List<Long> counts = userCounts.stream().map(UserServiceProto.UserCountByDate::getCount).toList();

        return ChartDataDto.builder().time(dates).data(counts).build();
    }

    @Override
    public ChartDataDto getInteractionsByTime(LocalDate startDate, LocalDate endDate) {
        List<InteractionServiceOuterClass.UserCountByDate> userCounts = interactionGrpcClient.getInteractionsByTime(startDate, endDate);

        List<String> dates = userCounts.stream().map(InteractionServiceOuterClass.UserCountByDate::getDate).toList();
        List<Long> counts = userCounts.stream().map(InteractionServiceOuterClass.UserCountByDate::getCount).toList();

        return ChartDataDto.builder().time(dates).data(counts).build();
    }

    @Override
    public ChartDataDto getReportsByTime(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        Map<LocalDate, Long> reportMap = new TreeMap<>();
        mergeQueryResultToMap(reportPostRepository.countReportsByDate(startDateTime, endDateTime), reportMap);
        mergeQueryResultToMap(reportReelRepository.countReportsByDate(startDateTime, endDateTime), reportMap);

        return buildChartDataFromMap(reportMap);
    }

    @Override
    public PageModelResponse getAllPost(String search, int page, int limit, String type) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by("created_at").descending());
        Page<ContentProjection> pageResult = adminContentRepository.findAllContent(search, type, pageable);

        return PageModelResponse.<ContentProjection>builder()
                .pageNo(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .content(pageResult.getContent())
                .build();
    }

    @Override
    public PostManagementInfo getPostManagementInfo() {
        Long quantityPost = postRepository.count();
        Long quantityReel = reelRepository.count();
        return PostManagementInfo.builder()
                .totalPost(quantityPost + quantityReel)
                .quantityPost(quantityPost)
                .quantityReel(quantityReel)
                .build();
    }


    private long countTotalContentBetween(LocalDateTime start, LocalDateTime end) {
        return postRepository.countByCreatedAtBetween(start, end)
                + reelRepository.countByCreatedAtBetween(start, end);
    }

    private void mergeQueryResultToMap(List<Object[]> queryResult, Map<LocalDate, Long> map) {
        for (Object[] row : queryResult) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = ((Number) row[1]).longValue();
            map.merge(date, count, Long::sum);
        }
    }

    private ChartDataDto buildChartDataFromMap(Map<LocalDate, Long> map) {
        List<String> time = new ArrayList<>();
        List<Long> data = new ArrayList<>();

        for (Map.Entry<LocalDate, Long> entry : map.entrySet()) {
            time.add(entry.getKey().format(DATE_FORMATTER));
            data.add(entry.getValue());
        }

        return ChartDataDto.builder()
                .time(time)
                .data(data)
                .build();
    }
}