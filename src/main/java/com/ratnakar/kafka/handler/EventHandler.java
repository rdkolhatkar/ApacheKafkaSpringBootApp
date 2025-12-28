package com.ratnakar.kafka.handler;

import com.ratnakar.kafka.exception.NotRetryableException;
import com.ratnakar.kafka.exception.RetryableException;
import com.ratnakar.kafka.model.ProcessEventEntity;
import com.ratnakar.kafka.model.ProductCreatedEvent;
import com.ratnakar.kafka.repository.ProcessEventRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
//@KafkaListener(topics = "product-created-events-topic", groupId = "product-created-events")
@KafkaListener(topics = "product-created-events-topic")
public class EventHandler {
    private RestTemplate restTemplate;
    private ProcessEventRepository processEventRepository;

    public EventHandler(RestTemplate restTemplate, ProcessEventRepository processEventRepository) {
        this.restTemplate = restTemplate;
        this.processEventRepository = processEventRepository;
    }

    // @Payload is used to bind the message body (payload) of a request—commonly in SOAP or messaging-based Spring apps—to a method parameter.
    @KafkaHandler
    @Transactional
    public void handle(@Payload ProductCreatedEvent productCreatedEvent,
                       @Header("messageId") String messageId,
                       @Header(KafkaHeaders.RECEIVED_KEY) String messageKey){
        // To test the Not Retryable Exception uncomment the below line and run the application
        // if(true) throw new NotRetryableException("An Error took place. No need to consume the message again.");
        log.info("Received a new event: "+productCreatedEvent.getTitle() + " With product id as "+productCreatedEvent.getProductId());

        // Check if this event is already processed before or not
        ProcessEventEntity existingRecord = processEventRepository.findByMessageId(messageId);
        if(existingRecord != null){
            log.info("Found a Duplicate message id: {}", existingRecord);
            return;
        }

        String requestUrl = "http://localhost:8090/response/200";
        try{
            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, null, String.class);
            if(response.getStatusCode().value() == HttpStatus.OK.value()){
                log.info("Received response from a remote service: "+ response.getBody());
            }
        }catch (ResourceAccessException ex){
            log.error(ex.getMessage());
            throw new RetryableException(ex);
        }catch (HttpServerErrorException ex){
            log.error(ex.getMessage());
            throw new NotRetryableException(ex);
        }catch (Exception e){
            log.error(e.getMessage());
            throw new NotRetryableException(e);
            // if not retryable exception is thrown then our message will go to Dead letter topic
        }
        // To avoid the processing of same kafka message twice, I want to store the message in database
        // Below is the code which stores the unique message Id into the DB, if same message is sent again then DB will throw exception
        // With that exception we can Identify the duplicate message
        // Save unique message Id into database table
        try {
            processEventRepository.save(new ProcessEventEntity(messageId, productCreatedEvent.getProductId()));
        }catch (DataIntegrityViolationException Dx){
            throw new NotRetryableException(Dx);
        }
    }
}
