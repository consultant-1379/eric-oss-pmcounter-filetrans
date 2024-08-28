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
package com.ericsson.oss.adc.sftp.filetrans.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * The Class KafkaOutputConsumerTestIntegration.
 * Kafka consumer that reads back the Kafka Message that we produce on the Kafka to ensure that they are correct.
 *
 * End to End Test Flow:
 * Test producer -> INPUT Topic -> Consumer -> Input Listener -> download from enm - upload to BDR -> Producer -> OUTPUT Topic -> Test Consumer
 *
 * Used to get Messages from the output topic, to test the end to end flow..
 */
@Component
public class KafkaOutputConsumerTestIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaOutputConsumerTestIntegration.class);
    private CountDownLatch latch = new CountDownLatch(1);
    private final List<ConsumerRecord<String, String>> consumerRecords = new ArrayList<>();
    private final AtomicInteger numberRecordsReceived = new AtomicInteger(0);

    /**
     * Receive.
     *
     * @param consumerRecord
     *            the consumer record
     */
    @KafkaListener(containerFactory = "consumerKafkaListenerOutputContainerFactory", topics = "ran-pm-counter-sftp-file-transfer")
    public void receive(final ConsumerRecord<String, String> consumerRecord) {
        setConsumerRecord(consumerRecord);
        numberRecordsReceived.getAndIncrement();
        latch.countDown();
    }
    public int getNumberRecordsReceived() {
        return numberRecordsReceived.get();
    }
    public CountDownLatch getLatch() {
        return latch;
    }
    public List<ConsumerRecord<String, String>> getConsumerRecords() {
        return consumerRecords;
    }
    public void setCountDownLatch(final int count) {
        latch = new CountDownLatch(count);
    }

    /**
     * Reset.
     */
    public void reset() {
        consumerRecords.clear();
        numberRecordsReceived.getAndSet(0);
    }
    private void setConsumerRecord(final ConsumerRecord<String, String> consumerRecord) {
        this.consumerRecords.add(consumerRecord);
    }
}