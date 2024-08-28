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

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.messaging.Message;

import com.ericsson.oss.adc.sftp.filetrans.service.DataCatalogService;

import io.micrometer.core.instrument.MeterRegistry;

@SpringBootTest
@EmbeddedKafka(brokerProperties = { "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1" })
//Set active profile to unique id to ensure full method runs in tests
class KafkaConfigurationTest {

    @Autowired
    private KafkaConfig kafkaConfiguration;

    @Autowired
    private MeterRegistry meterRegistry;

    @MockBean
    private DataCatalogService dataCatalogService;


    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServerConfig;

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



    @Test
    @DisplayName("Should create a consumer factory with Consumer configs ")
    void test_getKafkaConsumerFactorySuccess() {
        final Map<String, Object> expectedConfiguration = consumerConfigs();
        final ConsumerFactory<String, String> cf = kafkaConfiguration.consumerFactory();
        assertTrue("Consumer configuration does not contain expected values", expectedConfiguration.equals(cf.getConfigurationProperties()));
    }

    @Test
    @DisplayName("Should create a producer factory with Producer configs ")
    void test_getKafkaProducerFactorySuccess() {
        final Map<String, Object> expectedConfiguration = producerConfig();
        final Map<String, Object> actualConfiguration = kafkaConfiguration.producerFactory().getConfigurationProperties();
        assertEquals(expectedConfiguration.size(), actualConfiguration.size());
        assertTrue("Producer configuration does not contain expected values", expectedConfiguration.equals(actualConfiguration));
    }

    @Test
    @DisplayName("Should create a KafkaTransactionManager ")
    void test_getKafkaTransactionManagerSuccess() {
        final KafkaTransactionManager<String, Message<byte[]>> ktm = kafkaConfiguration.kafkaTransactionManager();
        assertFalse("KafkaTransactionManager should not be null ", ktm == null);
    }

    @Test
    @DisplayName("Should create a kafkaAdmin ")
    void test_getkafkaAdminSuccess() {
        final KafkaAdmin kafkaAdmin = kafkaConfiguration.kafkaAdmin();
        assertFalse("kafkaAdmin should not be null ", kafkaAdmin == null);
    }

    @Test
    @DisplayName("Should create a kafkaOutputTemplate ")
    void test_getkafkaOutputTemplateSuccess() {
        final KafkaTemplate<String, Message<byte[]>> kafkaOutputTemplate = kafkaConfiguration.kafkaOutputTemplate();
        assertFalse("kafkaOutputTemplate should not be null ", kafkaOutputTemplate == null);
    }

    private Map<String, Object> producerConfig() {
        final Map<String, Object> config = new LinkedHashMap<>(14);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, producerReconnectBackoffMaxMs);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, producerRequestTimeoutMs);
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, kafkaConfiguration.getBatchSizeConfig());
        config.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, producerReconnectBackoffMs);
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerConfig);
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, producerRetryBackoffMs);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, kafkaConfiguration.getBufferMemoryConfig());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.RETRIES_CONFIG, 2147483647);  // default to 0 (in kafka non transactions).
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put("internal.auto.downgrade.txn.commit", true);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        config.put(ProducerConfig.LINGER_MS_CONFIG, kafkaConfiguration.getLingerConfig());
        return config;
    }

    private Map<String, Object> consumerConfigs() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerConfig);
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

        props.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, consumerRetryBackoffMs);
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, consumerReconnectBackoffMs);
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, consumerReconnectBackoffMaxMs);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, consumerRequestTimeoutMs);
        props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);  //  setting auto create topics to false to prevent auto creation
        return props;
    }
}
