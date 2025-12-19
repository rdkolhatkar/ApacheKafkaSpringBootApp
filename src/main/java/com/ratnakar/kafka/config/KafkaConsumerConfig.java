package com.ratnakar.kafka.config;

// ========================= IMPORTS EXPLANATION =========================

import com.ratnakar.kafka.exception.NotRetryableException;
import com.ratnakar.kafka.exception.RetryableException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
// Provides Kafka consumer configuration keys like bootstrap servers, group id, deserializers, etc.

import org.apache.kafka.clients.producer.ProducerConfig;
// Provides Kafka producer configuration keys like serializers, retries, acks, etc.

import org.apache.kafka.common.serialization.StringDeserializer;
// Deserializes Kafka message KEY from byte[] into Java String

import org.apache.kafka.common.serialization.StringSerializer;
// Serializes Java String into byte[] before sending message KEY to Kafka

import org.springframework.beans.factory.annotation.Autowired;
// Enables dependency injection of Spring-managed beans

import org.springframework.context.annotation.Bean;
// Marks a method as a Spring Bean definition

import org.springframework.context.annotation.Configuration;
// Marks this class as a configuration class for Spring IoC container

import org.springframework.core.env.Environment;
// Used to read values from application.properties or application.yml

import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
// Factory that creates Kafka listener containers with support for concurrency

import org.springframework.kafka.core.*;
// Contains core Kafka components like ConsumerFactory, ProducerFactory, KafkaTemplate

import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
// Publishes failed messages to a Dead Letter Topic (DLT)

import org.springframework.kafka.listener.DefaultErrorHandler;
// Central error handling mechanism for Kafka consumers

import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
// Wraps real deserializers to gracefully handle deserialization errors

import org.springframework.kafka.support.serializer.JsonDeserializer;
// Converts JSON byte[] messages into Java POJOs

import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.web.client.HttpServerErrorException;
// Converts Java objects into JSON before publishing to Kafka

import java.util.HashMap;
import java.util.Map;
// Used to store Kafka configuration key-value pairs

/**
 * @Configuration ----------------
 * This class tells Spring that it contains bean definitions.
 * Spring will scan this class and register the Kafka Consumer beans in its context.
 */
@Configuration
public class KafkaConsumerConfig {

    /**
     * Injects Spring's Environment object.
     * <p>
     * This allows reading configuration values dynamically from:
     * - application.properties
     * - application.yml
     * - environment variables
     * <p>
     * This avoids hardcoding Kafka configuration values in Java code.
     */
    @Autowired
    Environment environment;

    /**
     * consumerFactory()
     * ------------------
     * Creates and configures a ConsumerFactory that Spring Kafka will use
     * to create Kafka Consumer instances.
     * <p>
     * This method reads Kafka configuration from application.properties,
     * sets deserializers, group-id, bootstrap servers, etc.
     *
     * @Bean: Spring creates this object and manages it as a singleton.
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {

        // Stores Kafka consumer configuration as key-value pairs
        Map<String, Object> config = new HashMap<>();

        /**
         * Registers Kafka broker addresses.
         *
         * Without this, the consumer would not know where Kafka is running.
         */
        config.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                environment.getProperty("spring.kafka.consumer.bootstrap-servers")
        );

        /**
         * Configures deserialization of Kafka message keys.
         *
         * Kafka sends keys as byte[], so StringDeserializer converts them to String.
         */
        config.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        /**
         * VALUE_DESERIALIZER_CLASS_CONFIG
         * --------------------------------
         * Instead of using JsonDeserializer directly, we use
         * ErrorHandlingDeserializer as a wrapper.
         *
         * WHY?
         * - Prevents consumer crashes due to malformed JSON
         * - Captures deserialization exceptions
         * - Allows failed messages to be sent to DLT
         */
        config.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class
        );

        /**
         * Specifies the actual deserializer to be used internally
         * by ErrorHandlingDeserializer.
         *
         * JsonDeserializer converts JSON payload into Java objects.
         */
        config.put(
                ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                JsonDeserializer.class
        );

        /**
         * Defines which Java packages are allowed for deserialization.
         *
         * SECURITY REASON:
         * - Prevents deserialization attacks
         * - Only trusted packages are allowed to be converted into objects
         */
        config.put(
                JsonDeserializer.TRUSTED_PACKAGES,
                environment.getProperty("spring.kafka.consumer.properties.spring.json.trusted.packages")
        );

        /**
         * Defines Kafka consumer group ID.
         *
         * Use case:
         * - Enables load balancing across multiple consumer instances
         * - Ensures message is processed by only one consumer in the group
         */
        config.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                environment.getProperty("spring.kafka.consumer.group-id")
        );

        /**
         * Creates a Kafka ConsumerFactory using the above configuration.
         *
         * Spring Kafka uses this factory internally to create consumers
         * whenever a @KafkaListener starts.
         */
        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * kafkaListenerContainerFactory()
     * --------------------------------
     * Creates listener containers that power @KafkaListener.
     * <p>
     * This factory:
     * - Manages consumer threads
     * - Handles retries and failures
     * - Integrates error handling and DLT support
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
    kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplateConfig) {

        /**
         * Error handler that:
         * - Catches consumer exceptions
         * - Publishes failed messages to Dead Letter Topic (DLT)
         *
         * DeadLetterPublishingRecoverer uses KafkaTemplate
         * to send messages to a predefined DLT.
         */
        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(
                        new DeadLetterPublishingRecoverer(kafkaTemplateConfig),
                        new FixedBackOff(5000, 3)
                        //FixedBackOff is a retry policy used by Spring Kafka’s error handling mechanism to control how many times and how often a failed Kafka message should be retried before giving up.
                );
        /**
         * ------------------------- EXCEPTION CLASSIFICATION -------------------------
         *
         * Spring Kafka allows us to explicitly classify exceptions as:
         * 1) NOT RETRYABLE  → message should NOT be retried and must be sent directly to DLT
         * 2) RETRYABLE      → message should be retried before sending to DLT
         *
         * This gives us fine-grained control over Kafka consumer behavior
         * instead of retrying blindly for all failures.
         */

        /**
         * addNotRetryableExceptions(...)
         * ---------------------------------------------------------------------------
         * These exceptions indicate PERMANENT failures where retrying will NOT help.
         *
         * Behavior:
         * - Kafka will NOT attempt any retries
         * - Message is IMMEDIATELY published to the Dead Letter Topic (DLT)
         * - Consumer continues processing next records
         *
         * Use cases:
         * - Validation failures
         * - Bad request / malformed data
         * - Business rule violations
         * - Irrecoverable downstream errors
         */
        errorHandler.addNotRetryableExceptions(
                NotRetryableException.class,      // Custom exception for permanent business failures
                HttpServerErrorException.class    // Indicates non-recoverable HTTP server errors
        );
        /**
         * addRetryableExceptions(...)
         * ---------------------------------------------------------------------------
         * These exceptions indicate TEMPORARY failures where retrying MAY succeed.
         *
         * Behavior:
         * - Kafka will retry message consumption as per configured BackOff policy
         * - In this case: 3 retries with 5 seconds delay (FixedBackOff)
         * - If retries are exhausted, message is sent to Dead Letter Topic (DLT)
         *
         * Use cases:
         * - Temporary database outage
         * - Network or timeout issues
         * - Downstream service unavailability
         */
        errorHandler.addRetryableExceptions(
                RetryableException.class          // Custom exception for transient / recoverable failures
        );

        /**
         * Factory responsible for creating Kafka listener containers.
         *
         * Supports:
         * - Concurrent message processing
         * - Partition-based parallelism
         * - Custom error handling
         */
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        /**
         * Associates this listener factory with the consumer factory.
         *
         * This tells Spring how to create actual Kafka consumers.
         */
        factory.setConsumerFactory(consumerFactory);

        /**
         * Registers the common error handler.
         *
         * Any exception thrown during message consumption
         * will be handled here.
         */
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    /**
     * KafkaTemplate Bean
     * ------------------
     * KafkaTemplate is used to publish messages to Kafka topics.
     * <p>
     * In this configuration:
     * - It is mainly used to publish messages to Dead Letter Topics (DLT)
     * - Can also be used for normal message production
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplateConfig(
            ProducerFactory<String, Object> producerFactoryConfig) {

        return new KafkaTemplate<>(producerFactoryConfig);
    }

    /**
     * ProducerFactory Bean
     * --------------------
     * Creates Kafka producer instances.
     * <p>
     * Used by KafkaTemplate to:
     * - Serialize messages
     * - Connect to Kafka brokers
     * - Publish messages reliably
     */
    @Bean
    public ProducerFactory<String, Object> producerFactoryConfig() {

        // Holds Kafka producer configuration
        Map<String, Object> config = new HashMap<>();

        /**
         * Kafka broker address for producer.
         *
         * Same broker can be used for both producer and consumer.
         */
        config.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                environment.getProperty("spring.kafka.consumer.bootstrap-servers")
        );

        /**
         * Serializes message VALUE (Java Object → JSON byte[])
         */
        config.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class
        );

        /**
         * Serializes message KEY (String → byte[])
         */
        config.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        /**
         * Creates Kafka ProducerFactory using above configuration.
         */
        return new DefaultKafkaProducerFactory<>(config);
    }
}
