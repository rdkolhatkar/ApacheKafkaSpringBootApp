package com.ratnakar.kafka.config;

import com.ratnakar.kafka.model.ProductCreatedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    // Injects the value of "spring.kafka.producer.bootstrap-servers" from application.properties
    @Value("${spring.kafka.producer.bootstrap-servers}")
    public String bootstrapServers;

    // Injects key serializer class name from properties file
    @Value("${spring.kafka.producer.key-serializer}")
    public String keySerializer;

    // Injects value serializer class name from properties file
    @Value("${spring.kafka.producer.value-serializer}")
    public String valueSerializer;

    // Injects acknowledgment mode ("all", "1", "0") from properties
    @Value("${spring.kafka.producer.acks}")
    public String acks;

    // Injects delivery timeout setting in milliseconds
    @Value("${spring.kafka.producer.properties.delivery.timeout.ms}")
    public String deliveryTimeout;

    // Injects linger time setting (wait time before batching messages)
    @Value("${spring.kafka.producer.properties.linger.ms}")
    public String linger;

    // Injects request timeout setting in milliseconds
    @Value("${spring.kafka.producer.properties.request.timeout.ms}")
    public String requestTimeout;


    /**
     * Creates a map of Kafka Producer configurations.
     * This method reads all injected property values
     * and attaches them to Kafka's configuration keys.
     */
    public Map<String, Object> producerConfigs() {

        // Creating a HashMap that will store all producer configurations
        Map<String, Object> config = new HashMap<>();

        // ----------------------------------------------
        // Adding the Kafka Producer configurations
        // ----------------------------------------------

        // Kafka broker connection details (host:port)
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Serializer used to convert the key into byte[] before sending to Kafka
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);

        // Serializer used to convert the value (ProductCreatedEvent) into JSON/byte[]
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);

        // Acknowledgement level (all replicas must confirm)
        config.put(ProducerConfig.ACKS_CONFIG, acks);

        // Maximum time allowed for a message to be delivered
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeout);

        // Delay to wait before batching messages (0 means immediate send)
        config.put(ProducerConfig.LINGER_MS_CONFIG, linger);

        // Maximum time producer waits for broker response before timing out
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeout);

        // Returns the complete producer configuration map
        return config;
    }


    /**
     * Creates a ProducerFactory bean.
     * ProducerFactory is responsible for creating Kafka Producers.
     * It uses the configuration map returned by producerConfigs().
     */
    @Bean
    ProducerFactory<String, ProductCreatedEvent> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }


    /**
     * Creates a KafkaTemplate bean.
     * KafkaTemplate is the main class used to send messages to Kafka topics.
     * It uses the producerFactory() to create producers internally.
     */
    @Bean
    KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate() {
        return new KafkaTemplate<String, ProductCreatedEvent>(producerFactory());
    }

    /**
     * This method creates a Kafka topic named "product-created-events-topic"
     * with:
     * - 2 partitions
     * - 1 replica
     * - Custom config: min.insync.replicas = 1
     * It returns a NewTopic object which Spring Boot will use to
     * auto-create the topic at application startup (if auto-creation is enabled).
     */
    @Bean
    public NewTopic createTopic() {

        // TopicBuilder is a utility class provided by Spring Kafka
        // to build a Kafka topic definition in a fluent, readable way.
        return TopicBuilder
                .name("product-created-events-topic")   // The name of the Kafka topic to create
                .partitions(2)                         // How many partitions the topic will have
                .replicas(1)                           // How many replicas each partition will have
                .configs(Map.of("min.insync.replicas", "1")) // Extra topic config
                .build();                               // Builds and returns a NewTopic instance
    }


}
