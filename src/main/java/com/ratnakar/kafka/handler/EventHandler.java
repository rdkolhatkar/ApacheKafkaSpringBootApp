package com.ratnakar.kafka.handler;

import com.ratnakar.kafka.exception.NotRetryableException;
import com.ratnakar.kafka.exception.RetryableException;
import com.ratnakar.kafka.model.ProductCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
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

    public EventHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @KafkaHandler
    public void handle(ProductCreatedEvent productCreatedEvent){
        // To test the Not Retryable Exception uncomment the below line and run the application
        // if(true) throw new NotRetryableException("An Error took place. No need to consume the message again.");
        log.info("Received a new event: "+productCreatedEvent.getTitle() + " With product id as "+productCreatedEvent.getProductId());
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
    }
}
