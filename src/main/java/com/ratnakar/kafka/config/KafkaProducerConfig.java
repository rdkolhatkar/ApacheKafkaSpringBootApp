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
import org.springframework.kafka.transaction.KafkaTransactionManager;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

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

    // Injects idempotence setting (true/false)
    // When enabled, ensures producer never sends duplicate messages and maintains message order.
    @Value("${spring.kafka.producer.properties.enable.idempotence}")
    public boolean idempotence;

    // Injects the max number of in-flight requests allowed per TCP connection.
    // Must be <= 5 when idempotence is enabled to avoid out-of-order message delivery.
    @Value("${spring.kafka.producer.properties.max.in.flight.requests.per.connection}")
    private Integer inflightRequests;

    @Value("${spring.kafka.producer.transaction-id-prefix}")
    private String transactionalIdPrefix;

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

        // Serializer used to convert the value (ProductCreatedEvent) into JSON/byte[].
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);

        // Acknowledgement level (all replicas must confirm)
        config.put(ProducerConfig.ACKS_CONFIG, acks);

        // Maximum time allowed for a message to be delivered
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeout);

        // Delay to wait before batching messages (0 means immediate send)
        config.put(ProducerConfig.LINGER_MS_CONFIG, linger);

        // Maximum time producer waits for broker response before timing out
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeout);

        // Enables idempotent producer to prevent duplicate messages
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, idempotence);

        // Maximum in-flight requests to allow at the same time
        // Must be <= 5 when idempotence is enabled to maintain ordering
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, inflightRequests);

        // config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);

        // ProducerConfig.TRANSACTIONAL_ID_CONFIG (set via spring.kafka.producer.transaction-id-prefix) assigns a transactional ID to the Kafka producer,
        // enabling exactly-once message delivery and transactional writes (commit or rollback messages atomically).
        config.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionalIdPrefix);

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
        return new KafkaTemplate<>(producerFactory());
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
    /**
     * ------------------------------------------------------------
     * KafkaTemplate<String, Object> Bean (Generic Kafka Template)
     * ------------------------------------------------------------
     *
     * WHY THIS BEAN EXISTS:
     * ---------------------
     * 1. KafkaTemplate is Spring Kafka's high-level abstraction used to
     *    send messages to Kafka topics.
     *
     * 2. This specific KafkaTemplate is defined with <String, Object>
     *    instead of <String, ProductCreatedEvent> to make it GENERIC.
     *
     * 3. A generic KafkaTemplate allows:
     *    - Sending different types of messages (multiple event models)
     *    - Using the same template inside Kafka transactions
     *    - Flexibility when working with multiple topics and payloads
     *
     * 4. Spring needs this bean so it can:
     *    - Be injected using @Autowired
     *    - Participate in Kafka transactions
     *    - Reuse producer instances efficiently
     *
     * WHEN THIS BEAN IS USED:
     * ----------------------
     * - Whenever KafkaTemplate<String, Object> is autowired
     * - When sending messages inside @Transactional methods
     * - When multiple event types are published from the same service
     *
     * HOW IT WORKS INTERNALLY:
     * ------------------------
     * - KafkaTemplate internally requests a Kafka Producer from
     *   the ProducerFactory
     * - The ProducerFactory uses producerConfigs()
     * - Serialization, retries, idempotence, and transactions
     *   are all handled automatically

    @Bean
    KafkaTemplate<String, Object> kafkaTemplate(
            ProducerFactory<String, Object> producerFactory) {

        // Creates a KafkaTemplate using the provided ProducerFactory
        // The ProducerFactory is responsible for creating and managing
        // KafkaProducer instances with the configured properties
        // (bootstrap servers, serializers, idempotence, transactions, etc.)
        return new KafkaTemplate<>(producerFactory);
    }
    */

    /**
     * ------------------------------------------------------------
     * KafkaTransactionManager<String, Object> Bean
     * ------------------------------------------------------------
     *
     * WHY THIS BEAN EXISTS:
     * ---------------------
     * 1. Kafka does NOT manage transactions automatically.
     *
     * 2. KafkaTransactionManager integrates Kafka transactions
     *    with Spring's transaction management system.
     *
     * 3. This bean is REQUIRED if you want to:
     *    - Use @Transactional with Kafka
     *    - Achieve exactly-once message delivery
     *    - Commit or rollback Kafka messages atomically
     *
     * WHAT PROBLEM IT SOLVES:
     * ----------------------
     * Without KafkaTransactionManager:
     * - Messages may be sent even if an exception occurs
     * - No rollback capability
     *
     * With KafkaTransactionManager:
     * - Messages are committed only if the transaction succeeds
     * - Messages are rolled back if any exception occurs
     *
     * WHEN THIS BEAN IS USED:
     * ----------------------
     * - Whenever a method annotated with @Transactional
     *   sends Kafka messages
     *
     * - Spring automatically detects this TransactionManager
     *   and uses it for Kafka operations
     *
     * HOW IT WORKS INTERNALLY:
     * ------------------------
     * 1. Spring detects @Transactional
     * 2. KafkaTransactionManager starts a Kafka transaction
     * 3. KafkaTemplate sends messages
     * 4. On success -> commitTransaction()
     * 5. On failure -> abortTransaction()
     *
     * IMPORTANT REQUIREMENT:
     * ----------------------
     * - spring.kafka.producer.transaction-id-prefix MUST be set
     * - This ensures each producer has a unique transactional.id
     * - Without this, Kafka transactions will NOT work

    @Bean
    KafkaTransactionManager<String, Object> kafkaTransactionManager(
            ProducerFactory<String, Object> producerFactory) {

        // Creates and returns a KafkaTransactionManager
        // The ProducerFactory supplies transactional KafkaProducer instances
        // This enables begin, commit, and rollback of Kafka transactions
        return new KafkaTransactionManager<>(producerFactory);
    }
    */


    @Bean
    public NewTopic createTopic() {
        return TopicBuilder
                .name("product-created-events-topic")
                .partitions(2)
                .replicas(1)
                .configs(Map.of("min.insync.replicas", "1"))
                .build();
    }

}
