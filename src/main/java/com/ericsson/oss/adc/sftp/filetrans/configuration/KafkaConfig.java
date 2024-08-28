/*******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/
package com.ericsson.oss.adc.sftp.filetrans.configuration;

import com.ericsson.oss.adc.sftp.filetrans.kafka.KafkaBootstrapSupplier;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.MicrometerConsumerListener;
import org.springframework.kafka.core.MicrometerProducerListener;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.AfterRollbackProcessor;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.messaging.Message;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
@Getter
@Slf4j
public class KafkaConfig {

    private static final String PROD_1 = UUID.randomUUID().toString();


    private static final String PRODUCER_MAX_AGE = "600000000";

    @Value("${spring.kafka.topics.input.name}")
    private String topic;

    @Value("${spring.kafka.topics.enm_id}")
    private String enmID;

    @Value("${spring.kafka.consumer.group-id}")
    private String group;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String offset;

    // REF : https://dev.to/confluentinc/5-things-every-apache-kafka-developer-should-know-4nb (tip #3)
    @Value("${spring.kafka.consumer.partition-assignment-strategy}")
    private String partitionAssignmentStrategy;

    @Value("${spring.kafka.consumer.max-poll-records}")
    private int maxPollRecords;

    @Value("${spring.kafka.consumer.session-timeout-ms}")
    private int sessionTimeout;

    @Value("${spring.kafka.consumer.max-poll-reconnect-timeout-ms}")
    private int reconnectTimeout;

    @Value("${spring.kafka.consumer.max-poll-interval-ms}")
    private int maxPollIntervalMs;

    @Value("${spring.kafka.transaction.retry-interval-ms}")
    private int transactionRetryIntervalMillis;

    @Value("${spring.kafka.transaction.max-retry-attempts}")
    private int transactionMaxRetryAttempts;

    @Value("${spring.kafka.producer.retry-backoff-ms}")
    private int producerRetryBackoffMs;

    @Value("${spring.kafka.producer.reconnect-backoff-ms}")
    private int producerReconnectBackoffMs;

    @Value("${spring.kafka.producer.reconnect-backoff-max-ms}")
    private int producerReconnectBackoffMaxMs;

    @Value("${spring.kafka.producer.request-timeout-ms}")
    private int producerRequestTimeoutMs;

    @Value("${spring.kafka.consumer.retry-backoff-ms}")
    private int consumerRetryBackoffMs;

    @Value("${spring.kafka.consumer.reconnect-backoff-ms}")
    private int consumerReconnectBackoffMs;

    @Value("${spring.kafka.consumer.reconnect-backoff-max-ms}")
    private int consumerReconnectBackoffMaxMs;

    @Value("${spring.kafka.consumer.request-timeout-ms}")
    private int consumerRequestTimeoutMs;

    @Value("${spring.kafka.bootstrap-servers}")
    private String defaultBootstrap;

    @Value("${spring.kafka.topics.output.batch-size}")
    private String batchSizeConfig;

    @Value("${spring.kafka.topics.output.linger}")
    private String lingerConfig;

    @Value("${spring.kafka.topics.output.buffer-memory}")
    private String bufferMemoryConfig;

    @Autowired
    private KafkaBootstrapSupplier kafkaBootstrapSupplier;

    @Autowired
    private Environment environment;

    @Autowired
    private MeterRegistry meterRegistry;

    @Value("${dmm.data-catalog.base-url}")
    private String dataCatalogUri;

    /**
     * @return the after rollback processor<? super string,? super string>
     */
    @Bean
    public AfterRollbackProcessor<String, String> getAfterRollbackProcessor() {
        logWithBanner("Initializing After Roll back Processor");
        return new DefaultAfterRollbackProcessorImpl<>();
    }

    @java.lang.SuppressWarnings("squid:S4449")
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> concurrentKafkaListenerContainerFactory() {

        logWithBanner("Initializing kafka Listener Container Factory");
        final ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        factory.getContainerProperties().setEosMode(ContainerProperties.EOSMode.V2);
        factory.getContainerProperties().setTransactionManager(kafkaTransactionManager()); //set the consumer factory to use same transaction manager as the producer factory
        factory.setBatchListener(true);

        // handling errors in listener.... Errors thrown by transactions in batching need to be handled by listener so that individual record can be skipped and not the entire batch.
        final CommonErrorHandler errorhandler = null;
        factory.setCommonErrorHandler(errorhandler);
        factory.setAfterRollbackProcessor(getAfterRollbackProcessor());

        return factory;
    }

    @Bean
    public KafkaTransactionManager<String, Message<byte[]>> kafkaTransactionManager() {
        logWithBanner("Initializing Kafka Transaction Manager");
        final KafkaTransactionManager<String, Message<byte[]>> transactionManager = new KafkaTransactionManager<>(producerFactory()); // provide transaction manager with producer factory
        transactionManager.setTransactionIdPrefix(PROD_1);
        return transactionManager;
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        logWithBanner("Initializing kafka Admin - 12");
        final Map<String, Object> adminConfig = new HashMap<>();
        KafkaAdmin kafkaAdmin = new KafkaAdmin(adminConfig);
        kafkaAdmin.setBootstrapServersSupplier(kafkaBootstrapSupplier);
        kafkaAdmin.setModifyTopicConfigs(true);
        return kafkaAdmin;
    }

    @Bean
    public KafkaTemplate<String, Message<byte[]>> kafkaOutputTemplate() {
        logWithBanner("Initializing Kafka Template");
        return new KafkaTemplate<>(producerFactory(), false);
    }

    /**
     * Creates a consumer factory for both 4G and 5G data space.
     */
    @Bean
    @Scope("singleton")
    public ConsumerFactory<String, String> consumerFactory() {
        logWithBanner("Initializing Consumer Factory");
        logWithBanner(this.toString());
        final DefaultKafkaConsumerFactory<String, String> factory = new DefaultKafkaConsumerFactory<>(consumerConfigs());
        factory.addListener(new MicrometerConsumerListener<>(meterRegistry));
        factory.setBootstrapServersSupplier(kafkaBootstrapSupplier);
        return factory;
    }

    private Map<String, Object> consumerConfigs() {
        final Map<String, Object> props = new HashMap<>(12);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, group);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeout);
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, reconnectTimeout);

        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, partitionAssignmentStrategy);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);  //  setting auto create topics to false to prevent auto creation

        addConsumerRobustnessValues(props);
        return props;
    }

    @Bean
    @Scope("singleton")
    public ProducerFactory<String, Message<byte[]>> producerFactory() {
        logWithBanner("Initializing Producer Factory");
        final Map<String, Object> config = new HashMap<>(4);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put("internal.auto.downgrade.txn.commit", true);
        // max in flight requests per connection set to one prevents pipelining (and message re-ordering),
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1); // transaction rollbacks will also cause message re-ordering too.
        // batch.size, linger.ms & buffer.memory can be configured to 'batch' messages together for sending.
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSizeConfig);
        config.put(ProducerConfig.LINGER_MS_CONFIG, lingerConfig);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemoryConfig);
        //retries must be non zero for transactions to work. Defaults to 2147483647 in logs when transactions enabled.
        config.put(ProducerConfig.RETRIES_CONFIG, 2147483647);  // default to 0 (in kafka non transactions).

        addProducerRobustnessValues(config);

        final DefaultKafkaProducerFactory<String, Message<byte[]>> factory = new DefaultKafkaProducerFactory<>(config);
        factory.transactionCapable(); // this is not needed..  returns boolean...
        factory.setTransactionIdPrefix(PROD_1);
        factory.setBootstrapServersSupplier(kafkaBootstrapSupplier);
        factory.addListener(new MicrometerProducerListener<>(meterRegistry));
        // smaller than transactional.id.expiration.ms on broker. Default: 604800000
        // to allow the producer to expire when the transactional id expires.
        // Else the producer tries to use an expired transaction id.
        factory.setMaxAge(Duration.ofMillis(Long.parseLong(PRODUCER_MAX_AGE)));

        return factory;
    }


    private void logWithBanner(final String msg) {
        log.info("-----------------------------------------------------------------------");
        log.info("KAFKA_CONFIG: {}", msg);
        log.info("-----------------------------------------------------------------------");
    }

    private void addProducerRobustnessValues(final Map<String, Object> config) {
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, producerRetryBackoffMs);
        config.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, producerReconnectBackoffMs);
        config.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, producerReconnectBackoffMaxMs);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, producerRequestTimeoutMs);
    }

    private void addConsumerRobustnessValues(final Map<String, Object> config) {
        config.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, consumerRetryBackoffMs);
        config.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, consumerReconnectBackoffMs);
        config.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, consumerReconnectBackoffMaxMs);
        config.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, consumerRequestTimeoutMs);
    }

    @Override
    public String toString() {
        return "User requested kafka configuration parameters [\n bootstrapServerConfig=" + defaultBootstrap + ",\n topic=" + topic
                + ",\n enmID=" + enmID + ",\n group=" + group + ",\n offset=" + offset + ",\n partitionAssignmentStrategy="
                + partitionAssignmentStrategy + ",\n maxPollRecords=" + maxPollRecords + ",\n sessionTimeout="
                + sessionTimeout + ",\n reconnectTimeout=" + reconnectTimeout + ",\n maxPollIntervalMs=" + maxPollIntervalMs
                + ",\n transactionRetryIntervalMillis=" + transactionRetryIntervalMillis + ",\n transactionMaxRetryAttempts="
                + transactionMaxRetryAttempts + ",\n producerRetryBackoffMs="
                + producerRetryBackoffMs + ",\n producerReconnectBackoffMs=" + producerReconnectBackoffMs + ",\n producerReconnectBackoffMaxMs="
                + producerReconnectBackoffMaxMs + ",\n producerRequestTimeoutMs=" + producerRequestTimeoutMs + ",\n consumerRetryBackoffMs="
                + consumerRetryBackoffMs + ",\n consumerReconnectBackoffMs=" + consumerReconnectBackoffMs + ",\n consumerReconnectBackoffMaxMs="
                + consumerReconnectBackoffMaxMs + ",\n consumerRequestTimeoutMs=" + consumerRequestTimeoutMs + "\n]";
    }

}
