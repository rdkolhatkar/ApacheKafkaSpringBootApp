package com.ratnakar.kafka.handler;

import com.ratnakar.kafka.model.ProductCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@KafkaListener(topics = "product-created-events-topic")
public class EventHandler {
    @KafkaHandler
    public void handle(ProductCreatedEvent productCreatedEvent){
        log.info("Received a new event: "+productCreatedEvent.getTitle());
    }
}
