package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.postservice.model.*;
import org.nexo.postservice.repository.IHashTagRepository;
import org.nexo.postservice.repository.IPostHashTagRepository;
import org.nexo.postservice.service.IHashTagService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class HashTagServiceImpl implements IHashTagService {

    // Compile pattern 1 lần tĩnh để tối ưu RAM thay vì compile lại mỗi lần gọi hàm
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("(#\\w+)");
    private final IPostHashTagRepository postHashTagRepository;
    private final IHashTagRepository hashTagRepository;

    @Override
    public void findAndAddHashTagFromCaption(AbstractPost post) {
        String caption = post.getCaption();
        if (caption == null || caption.trim().isEmpty()) {
            return;
        }

        Set<String> uniqueHashtags = extractUniqueHashtags(caption);

        postHashTagRepository.deleteByPostId(post.getId());

        List<HashTagModel> tagsToSave = new ArrayList<>();

        for (String name : uniqueHashtags) {
            HashTagModel hashTagModel = hashTagRepository.findByName(name);

            if (hashTagModel == null) {
                hashTagModel = HashTagModel.builder()
                        .name(name)
                        .isActive(true)
                        .usageCount(1L)
                        .postHashTagModel(new ArrayList<>())
                        .build();
            } else {
                hashTagModel.setUsageCount(hashTagModel.getUsageCount() + 1);
            }

            PostHashTagModel postHashTagModel = PostHashTagModel.builder()
                    .hashTagModel(hashTagModel)
                    .build();

            if (post instanceof PostModel postModel) {
                postHashTagModel.setPostModel(postModel);
            } else if (post instanceof ReelModel reelModel) {
                postHashTagModel.setReelModel(reelModel);
            }

            hashTagModel.getPostHashTagModel().add(postHashTagModel);
            tagsToSave.add(hashTagModel);
        }

        if (!tagsToSave.isEmpty()) {
            hashTagRepository.saveAll(tagsToSave);
        }
    }

    public List<HashTagModel> getTrendingHashtags(int topN) {
        Pageable pageable = PageRequest.of(0, topN);
        return hashTagRepository.findTopTrendingHashtags(pageable);
    }

    private Set<String> extractUniqueHashtags(String caption) {
        Set<String> tags = new HashSet<>();
        Matcher matcher = HASHTAG_PATTERN.matcher(caption);
        while (matcher.find()) {
            tags.add(matcher.group().toLowerCase());
        }
        return tags;
    }
}