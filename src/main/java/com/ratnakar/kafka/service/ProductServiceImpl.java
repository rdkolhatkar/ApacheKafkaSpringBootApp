package com.ratnakar.kafka.service;

import com.ratnakar.kafka.model.ProductCreatedEvent;
import com.ratnakar.kafka.model.ProductRestModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService{

    KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate; // KafkaTemplate is a class from "org.springframework.kafka.core"
    // KafkaTemplate simplifies sending messages to Kafka topics. It handles serialization, producer configuration, and provides easy methods like send() to publish data asynchronously and reliably.

    public ProductServiceImpl(KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public String createProduct(ProductRestModel productRestModel) throws Exception{
        String productId = UUID.randomUUID().toString(); // Java code for generating the random UUID for productId
        // TO DO: Persist Product into database table before publishing an event
        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(
                productId,
                productRestModel.getTitle(),
                productRestModel.getPrice(),
                productRestModel.getQuantity()
        );
        // To include unique id into our kafka message header we will use the below code
        ProducerRecord<String, ProductCreatedEvent> record = new ProducerRecord<>(
                "product-created-event-topic",
                productId,
                productCreatedEvent
        );
        // Now we can add new headers inside our message
        record.headers().add("messageId", UUID.randomUUID().toString().getBytes()); // This unique identifier is different from the productId
        // To test if messageId is getting stored in H2 db or not and to check if our DB is throwing an exception when it is receiving the same message again use below commented code
        // record.headers().add("messageId", "1234567890".getBytes()); // Here we are hard-coding the message ID as 1234567890
        /*
        CompletableFuture<SendResult<String, ProductCreatedEvent>> future =
                kafkaTemplate.send("product-created-events-topic", productId, productCreatedEvent);
        // CompletableFuture is a class from "java.util.concurrent" & It captures the asynchronous result of sending a Kafka message, allowing you to check success or failure later, attach callbacks, or handle errors without blocking the main thread.

        future.whenComplete((result, exception) -> {
            if(exception != null){
               log.error("Failed to send message to Kafka server, Exception: "+exception.getMessage());
            } else {
                log.info("Message Sent Successfully to Kafka Server, Details: "+result.getRecordMetadata());
            }
        });
        future.join(); // by adding future.join() this code we can make our service is synchronous and if we remove this code then our application service becomes asynchronous
        */

        // future.join() blocks the current thread until the asynchronous task completes and returns the result, throwing only unchecked exceptions, making it a simpler alternative to get() without try-catch.
        log.info("**** Before publishing the product created event ****");
        /*
        SendResult<String, ProductCreatedEvent> result =
                kafkaTemplate.send("product-created-events-topic", productId, productCreatedEvent).get();
         */
        // This code sends the Kafka message and waits for Kafka’s acknowledgment, making the operation synchronous and giving you metadata about the sent record.
        // From the result we can read the 1) Topic Name 2) Partition 3) Offset 4) Timestamp
        // 1) Topic Name : To verify message is stored in which topic
        // 2) Partition : To verify partitions for debugging and monitoring the message stream
        // 3) Offset : To trace the position of record in the partition or position tracking of record
        // 4) Timestamp: To measure the latency or throughput of the producer
        // Now we will pass the record object inside the send method
        SendResult<String, ProductCreatedEvent> result =
                kafkaTemplate.send(record).get();
        // Printing the Partition
        log.info("Partition : "+ result.getRecordMetadata().partition());
        // Printing the Topic
        log.info("Topic : "+ result.getRecordMetadata().topic());
        // Printing the Offset
        log.info("Offset : "+ result.getRecordMetadata().offset());
        // Printing the Timestamp
        log.info("Timestamp : "+ result.getRecordMetadata().timestamp());

        /*
        // Sending data to insync-topic
        SendResult<String, ProductCreatedEvent> insyncTopicResult =
                kafkaTemplate.send("insync-topic", productId, productCreatedEvent).get();

        // Printing the Partition
        log.info("Partition : "+ insyncTopicResult.getRecordMetadata().partition());
        // Printing the Topic
        log.info("Topic : "+ insyncTopicResult.getRecordMetadata().topic());
        // Printing the Offset
        log.info("Offset : "+ insyncTopicResult.getRecordMetadata().offset());
        // Printing the Timestamp
        log.info("Timestamp : "+ insyncTopicResult.getRecordMetadata().timestamp());
        */

        log.info("**** Returning product id ****");
        return productId;
    }
}
