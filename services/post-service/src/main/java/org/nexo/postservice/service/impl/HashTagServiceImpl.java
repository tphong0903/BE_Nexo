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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class HashTagServiceImpl implements IHashTagService {
    private final IPostHashTagRepository postHashTagRepository;
    private final IHashTagRepository hashTagRepository;


    @Override
    public void findAndAddHashTagFromCaption(AbstractPost post) {
        String caption = post.getCaption();
        Pattern pattern = Pattern.compile("(#\\w+)");
        Matcher matcher = pattern.matcher(caption);
        postHashTagRepository.deleteByPostId(post.getId());
        while (matcher.find()) {
            String name = matcher.group();
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
            if (post.getClass() == PostModel.class) {
                postHashTagModel.setPostModel((PostModel) post);

            } else {
                postHashTagModel.setReelModel((ReelModel) post);
            }

            hashTagModel.getPostHashTagModel().add(postHashTagModel);

            hashTagRepository.save(hashTagModel);
        }
    }

    public List<HashTagModel> getTrendingHashtags(int topN) {
        Pageable pageable = PageRequest.of(0, topN);
        return hashTagRepository.findTopTrendingHashtags(pageable);
    }
}
