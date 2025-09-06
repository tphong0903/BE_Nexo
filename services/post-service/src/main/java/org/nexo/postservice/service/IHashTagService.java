package org.nexo.postservice.service;

import org.nexo.postservice.model.AbstractPost;

public interface IHashTagService {
    void findAndAddHashTagFromCaption(AbstractPost post);
}
