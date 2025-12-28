package com.ratnakar.kafka.repository;

import com.ratnakar.kafka.model.ProcessEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessEventRepository extends JpaRepository<ProcessEventEntity, Long> {
    ProcessEventEntity findByMessageId(String messageId);
}
