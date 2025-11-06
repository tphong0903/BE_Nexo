package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.grpc.interaction.InteractionServiceOuterClass;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.postservice.dto.response.ChartDataDto;
import org.nexo.postservice.dto.response.DashboardResponseDto;
import org.nexo.postservice.repository.IPostRepository;
import org.nexo.postservice.repository.IReportPostRepository;
import org.nexo.postservice.repository.IReportReelRepository;
import org.nexo.postservice.service.GrpcServiceImpl.client.InteractionGrpcClient;
import org.nexo.postservice.service.GrpcServiceImpl.client.UserGrpcClient;
import org.nexo.postservice.service.IDashboardService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private final IPostRepository postRepository;
    private final IReportReelRepository reportReelRepository;
    private final IReportPostRepository reportPostRepository;
    private final UserGrpcClient userGrpcClient;
    private final InteractionGrpcClient interactionGrpcClient;

    @Override
    public DashboardResponseDto getDashboardData() {
        Long totalUser = userGrpcClient.getTotalUsers();
        Long totalPost = postRepository.count();
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
        LocalDateTime endOfLastMonth = startOfThisMonth;

        long thisMonth = postRepository.countByCreatedAtBetween(startOfThisMonth, startOfThisMonth.plusMonths(1));
        long lastMonth = postRepository.countByCreatedAtBetween(startOfLastMonth, endOfLastMonth);

        double percent = 0;
        if (lastMonth > 0) {
            percent = ((double) (thisMonth - lastMonth) / lastMonth) * 100;
        }
        return percent;
    }

    @Override
    public ChartDataDto getPostsByTime(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<Object[]> result = postRepository.countPostsByDate(startDateTime, endDateTime);

        List<String> time = new ArrayList<>();
        List<Long> data = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        for (Object[] row : result) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = ((Number) row[1]).longValue();
            time.add(date.format(formatter));
            data.add(count);
        }

        return ChartDataDto.builder()
                .time(time)
                .data(data)
                .build();
    }

    @Override
    public ChartDataDto getUsersByTime(LocalDate startDate, LocalDate endDate) {
        List<UserServiceProto.UserCountByDate> userCounts = userGrpcClient.getUsersByTime(startDate, endDate);

        List<String> dates = userCounts.stream()
                .map(UserServiceProto.UserCountByDate::getDate)
                .collect(Collectors.toList());

        List<Long> counts = userCounts.stream()
                .map(UserServiceProto.UserCountByDate::getCount)
                .collect(Collectors.toList());

        return ChartDataDto.builder()
                .time(dates)
                .data(counts)
                .build();
    }

    @Override
    public ChartDataDto getInteractionsByTime(LocalDate startDate, LocalDate endDate) {
        List<InteractionServiceOuterClass.UserCountByDate> userCounts = interactionGrpcClient.getInteractionsByTime(startDate, endDate);

        List<String> dates = userCounts.stream()
                .map(InteractionServiceOuterClass.UserCountByDate::getDate)
                .collect(Collectors.toList());

        List<Long> counts = userCounts.stream()
                .map(InteractionServiceOuterClass.UserCountByDate::getCount)
                .collect(Collectors.toList());

        return ChartDataDto.builder()
                .time(dates)
                .data(counts)
                .build();
    }

    @Override
    public ChartDataDto getReportsByTime(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<Object[]> postReports = reportPostRepository.countReportsByDate(startDateTime, endDateTime);
        List<Object[]> reelReports = reportReelRepository.countReportsByDate(startDateTime, endDateTime);

        Map<LocalDate, Long> reportMap = new TreeMap<>();

        for (Object[] row : postReports) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = ((Number) row[1]).longValue();
            reportMap.merge(date, count, Long::sum);
        }

        for (Object[] row : reelReports) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = ((Number) row[1]).longValue();
            reportMap.merge(date, count, Long::sum);
        }

        List<String> time = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        for (Map.Entry<LocalDate, Long> entry : reportMap.entrySet()) {
            time.add(entry.getKey().format(formatter));
            data.add(entry.getValue());
        }

        return ChartDataDto.builder()
                .time(time)
                .data(data)
                .build();
    }


}