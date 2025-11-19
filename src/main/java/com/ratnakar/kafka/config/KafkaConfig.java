package com.ratnakar.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Map;

@Configuration
public class KafkaConfig {
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
