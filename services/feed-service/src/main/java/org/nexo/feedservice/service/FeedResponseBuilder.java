package org.nexo.feedservice.service;

import org.nexo.feedservice.dto.PageModelResponse;
import org.nexo.feedservice.dto.ResponseData;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class FeedResponseBuilder {
    public <T> Mono<ResponseData<?>> build(
            List<T> content,
            int page,
            long limit,
            Page<Long> pageResult,
            Comparator<T> comparator
    ) {
        List<T> sortedContent = new ArrayList<>(content);
        sortedContent.sort(comparator);
        return Mono.just(createResponseData(sortedContent, page, limit, pageResult));
    }

    public ResponseData<?> empty(int page, int limit) {
        PageModelResponse<Object> emptyPage = PageModelResponse.builder()
                .pageNo(page)
                .pageSize(limit)
                .totalElements(0L)
                .totalPages(0)
                .last(true)
                .content(new ArrayList<>())
                .build();
        return ResponseData.builder()
                .status(200)
                .message("No more feed available")
                .data(emptyPage)
                .build();
    }

    private <T> ResponseData<?> createResponseData(List<T> sortedContent, int page, long limit, Page<Long> pageResult) {
        long totalElements;
        int totalPages;
        boolean isLast;

        if (pageResult != null) {
            totalElements = pageResult.getTotalElements();
            totalPages = pageResult.getTotalPages();
            isLast = page + 1 >= totalPages;
        } else {
            totalElements = sortedContent.size();
            isLast = sortedContent.size() < (int) limit;
            totalPages = page + (isLast ? 1 : 2);
        }

        PageModelResponse<T> pageModelResponse = PageModelResponse.<T>builder()
                .pageNo(page)
                .pageSize((int) limit)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(isLast)
                .content(sortedContent)
                .build();

        return ResponseData.builder()
                .status(200)
                .message("Feed retrieved successfully")
                .data(pageModelResponse)
                .build();
    }
}
