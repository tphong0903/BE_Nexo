package org.nexo.messagingservice.repository;

import org.nexo.messagingservice.model.MessageMediaModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageMediaRepository extends JpaRepository<MessageMediaModel, Long> {

    List<MessageMediaModel> findByMessageId(Long messageId);
}