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
package com.ericsson.oss.adc.sftp.filetrans;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class StartKafka {

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    private MessageListenerContainer messageListenerContainer;

    public StartKafka() {
        // No Args Constructor
    }

    public StartKafka(final KafkaListenerEndpointRegistry registry, final MessageListenerContainer messageListenerContainer) {
        super();
        this.registry = registry;
        this.messageListenerContainer = messageListenerContainer;
    }

    public boolean startKafkaListener(final String listenerId) {
        messageListenerContainer = registry.getListenerContainer(listenerId);
        return startKafkaListener(messageListenerContainer, listenerId);
    }

    public boolean startKafkaListener(final MessageListenerContainer messageListenerContainer, final String listenerId) {
        log.info("Starting Kafka Listener with Id: {}...", listenerId);
        if (!messageListenerContainer.isAutoStartup() && !messageListenerContainer.isRunning()) {
            messageListenerContainer.start();
        }
        if (messageListenerContainer.isRunning()) {
            log.info("Input Topic kafka Listener (Id: {}) started.", listenerId);
            return true;
        } else {
            log.error("FAILED to start Input Topic kafka Listener.");
        }
        return false;
    }
}
