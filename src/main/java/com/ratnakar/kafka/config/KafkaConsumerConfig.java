package com.ratnakar.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;                // Kafka consumer configuration keys
import org.apache.kafka.common.serialization.StringDeserializer;       // Deserializer to convert Kafka key bytes → String
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;                          // To read properties from application.properties
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;                   // Factory to create Kafka consumer instances
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;     // Default implementation for creating consumers
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;  // Converts JSON messages to Java Objects

import java.util.HashMap;
import java.util.Map;

/**
 * @Configuration
 * ----------------
 * This class tells Spring that it contains bean definitions.
 * Spring will scan this class and register the Kafka Consumer beans in its context.
 */
@Configuration
public class KafkaConsumerConfig {

    /**
     * @Autowired Environment
     * -----------------------
     * Environment allows us to read values from application.properties
     * Example:
     * spring.kafka.consumer.bootstrap-servers
     * spring.kafka.consumer.group-id
     * spring.kafka.consumer.properties.spring.json.trusted.packages
     */
    @Autowired
    Environment environment;

    /**
     * consumerFactory()
     * ------------------
     * Creates and configures a ConsumerFactory that Spring Kafka will use
     * to create Kafka Consumer instances.
     *
     * This method reads Kafka configuration from application.properties,
     * sets deserializers, group-id, bootstrap servers, etc.
     *
     * @Bean: Spring creates this object and manages it as a singleton.
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {

        // Map that stores Kafka consumer configuration properties
        Map<String, Object> config = new HashMap<>();

        /**
         * BOOTSTRAP_SERVERS_CONFIG
         * ------------------------
         * The Kafka broker addresses the consumer should connect to.
         * e.g. localhost:9092
         */
        config.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                environment.getProperty("spring.kafka.consumer.bootstrap-servers")
        );

        /**
         * KEY_DESERIALIZER_CLASS_CONFIG
         * -----------------------------
         * Deserializer to convert message KEY bytes → String.
         */
        config.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        /**
         * VALUE_DESERIALIZER_CLASS_CONFIG
         * --------------------------------
         * Deserializer to convert message VALUE bytes → Java Object.
         * JsonDeserializer converts JSON into Java POJO automatically.
         */
        /*
        config.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class
        );
        */

        /*
         This tells Kafka Consumer to use ErrorHandlingDeserializer
         as the PRIMARY deserializer for message values.
         Why this is important:
         ----------------------
         If deserialization of a Kafka message fails (for example:
          - invalid JSON
          - schema mismatch
          - corrupted message),
         the consumer will NOT crash.
         Instead, the error is captured and handled gracefully
         (e.g., sent to a Dead Letter Topic or logged).
         */

        config.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class
        );

        /*
         This tells ErrorHandlingDeserializer which ACTUAL deserializer
         it should delegate the work to.
         Here, we specify JsonDeserializer, meaning:
          - Kafka message value is expected to be JSON
          - JSON will be converted into a Java object
         Without this configuration:
         ----------------------------
         ErrorHandlingDeserializer would not know how to deserialize
         the message and would fail at runtime.
         */

        config.put(
                ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                JsonDeserializer.class
        );

        /**
         * TRUSTED_PACKAGES
         * ----------------
         * For security reasons, Kafka will only deserialize objects
         * from trusted packages.
         *
         * Example property:
         * spring.kafka.consumer.properties.spring.json.trusted.packages=com.ratnakar.kafka.model
         */
        config.put(
                JsonDeserializer.TRUSTED_PACKAGES,
                environment.getProperty("spring.kafka.consumer.properties.spring.json.trusted.packages")
        );

        /**
         * GROUP_ID_CONFIG
         * ----------------
         * Consumer group ID - all consumers with the same group ID
         * share the same partition messages.
         *
         * Essential for load balancing.
         */
        config.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                environment.getProperty("spring.kafka.consumer.group-id")
        );

        /**
         * DefaultKafkaConsumerFactory
         * ----------------------------
         * Creates Kafka Consumer instance using above configuration.
         */
        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * kafkaListenerContainerFactory()
     * --------------------------------
     * This factory creates the listener containers that power @KafkaListener.
     *
     * - It uses the ConsumerFactory defined above.
     * - Spring will automatically use this factory for all KafkaListeners.
     *
     * @Bean ensures Spring manages it.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
    kafkaListenerContainerFactory(ConsumerFactory<String, Object> consumerFactory) {

        /**
         * ConcurrentKafkaListenerContainerFactory
         * ---------------------------------------
         * Allows multi-threaded Kafka consumer processing.
         * Example:
         * factory.setConcurrency(3);
         * → 3 threads will process Kafka partitions in parallel.
         */
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // Inject the consumer factory created earlier
        factory.setConsumerFactory(consumerFactory);

        // Return the fully configured factory
        return factory;
    }
}
