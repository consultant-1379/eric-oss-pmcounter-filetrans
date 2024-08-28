/*******************************************************************************
 * COPYRIGHT Ericsson 2022
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
package com.ericsson.oss.adc.sftp.filetrans.integration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * The Class KafkaOutputConsumerTestIntegration.
 *
 * End to End Test Flow:
 * Test producer -> INPUT Topic -> Consumer -> Input Listener -> download from enm - upload to BDR -> Producer -> OUTPUT Topic -> Test Consumer
 *
 * Used to get Messages from the output topic, so as to test the end to end flow..
 */
@Component
public class KafkaOutputConsumerTestConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaOutputConsumerTestConfig.class);
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServerConfig;

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
    private ConsumerFactory<String, String> consumerConfigs() {
        final Map<String, Object> config = new HashMap<>(12);
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerConfig);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, group);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offset);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeout);
        config.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, reconnectTimeout);

        config.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, partitionAssignmentStrategy);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Consumer kafka listener output container factory.
     *
     * @return the concurrent kafka listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> consumerKafkaListenerOutputContainerFactory() {
        final ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerConfigs());
        return factory;
    }
}