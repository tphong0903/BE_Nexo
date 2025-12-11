package org.nexo.postservice.service;

import org.nexo.postservice.model.AbstractPost;
import org.nexo.postservice.model.HashTagModel;

import java.util.List;

public interface IHashTagService {
    void findAndAddHashTagFromCaption(AbstractPost post);

    public List<HashTagModel> getTrendingHashtags(int topN);
}
