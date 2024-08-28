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

package com.ericsson.oss.adc.sftp.filetrans.availability;

import com.ericsson.oss.adc.sftp.filetrans.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class DependentServiceAvailabilityKafka {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServerConfig;

    @Value("${spring.kafka.availability.retry_interval}")
    private int retryInterval;

    @Value("${spring.kafka.availability.retry_attempts}")
    private int retryAttempts;

    @Value("${spring.kafka.topics.input.name}")
    private String inputTopicName;

    @Value("${spring.kafka.topics.enm_id}")
    private String enmID;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    /**
     * @return true once Kafka is reached and Input Topic Found, false if max retries exhausted
     */
    public boolean checkHealth() {
        final RetryTemplate template = RetryTemplate.builder()
            .maxAttempts(retryAttempts)
            .fixedBackoff(retryInterval)
            .retryOn(UnsatisfiedExternalDependencyException.class)
            .withListener(Utils.getRetryListener())
            .build();

        try {
            return template.execute(retryContext -> doesInputTopicExist(inputTopicName + enmID));
        } catch (final UnsatisfiedExternalDependencyException exception) {
            log.error("FAILED Health Check for Kafka service.", exception);
        }

        return false;
    }

    public boolean doesInputTopicExist(final String inputTopicName) throws UnsatisfiedExternalDependencyException {
        final Map<String, Object> configurationProperties = kafkaAdmin.getConfigurationProperties();
        if (!configurationProperties.isEmpty()) {
            final String bootstrapServerAddress = (String) configurationProperties.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG);
            if (bootstrapServerAddress == null || bootstrapServerAddress.equals("")) {
                throw new UnsatisfiedExternalDependencyException("Kafka Bootstrap Server is NOT configured.");
            }

            log.debug("Validating Input Topic: '{}' for Kafka Server: '{}' ...", inputTopicName, bootstrapServerAddress);
            try (final AdminClient client = AdminClient.create(configurationProperties)) {
                final Set<String> existingTopics = client.listTopics().names().get();
                if (existingTopics.contains(inputTopicName)) {
                    log.info("FOUND Input Topic: '{}' in the configuration among the existing Kafka Client topics.", inputTopicName);
                    return true;
                } else {
                    log.error("COULDN'T find Input Topic: '{}'.", inputTopicName);
                    throw new UnsatisfiedExternalDependencyException("Input Topic '" + inputTopicName + "' not found.");
                }
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new UnsatisfiedExternalDependencyException("InterruptedException during Kafka service Health Check.", exception);
            } catch (final Exception exception) {
                throw new UnsatisfiedExternalDependencyException("General Exception during Kafka service Health Check.", exception);
            }
        }
        throw new UnsatisfiedExternalDependencyException("Kafka configuration properties not found");
    }
}


