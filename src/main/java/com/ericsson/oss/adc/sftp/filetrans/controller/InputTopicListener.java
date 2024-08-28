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
package com.ericsson.oss.adc.sftp.filetrans.controller;

import com.ericsson.oss.adc.sftp.filetrans.model.InputMessage;
import com.ericsson.oss.adc.sftp.filetrans.util.ConsecutiveRetryHandler;
import com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil;
import com.ericsson.oss.adc.sftp.filetrans.util.Utils;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ericsson.oss.adc.sftp.filetrans.util.Constants.LISTENER_ID;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.*;

/**
 * The listener interface for receiving inputTopic events.
 * The class that is interested in processing a inputTopic
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addInputTopicListener<code> method. When
 * the inputTopic event occurs, that object's appropriate
 * method is invoked.
 */
@Component
@Slf4j
public class InputTopicListener {

    @Autowired
    SFTPHandler sftpHandler;

    @Autowired
    OutputTopicController outputTopicController;

    @Autowired
    SFTPProcessingMetricsUtil metrics;

    @Autowired
    ConsecutiveRetryHandler consecutiveRetryHandler;

    @Value("${spring.kafka.topics.input.name}")
    private String inputTopicName;

    @Value("${spring.kafka.topics.enm_id}")
    private String enmID;

    @Value("${spring.kafka.transaction.retry-interval-ms}")
    private int transactionRetryIntervalMillis;

    @Value("${spring.kafka.transaction.max-retry-attempts}")
    private int transactionMaxRetryAttempts;

    private CountDownLatch fileLatch = new CountDownLatch(1);
    private final Gson gson = new Gson();

    private final AtomicInteger retryCount = new AtomicInteger();

    // Just for testing.
    private final AtomicInteger numberBatchesProcessed = new AtomicInteger();

    @KafkaListener(id = LISTENER_ID,
            idIsGroup = false,
            containerFactory = "concurrentKafkaListenerContainerFactory",
            topics = "${spring.kafka.topics.input.name}${spring.kafka.topics.enm_id}",
            concurrency = "1",
            autoStartup = "${spring.kafka.auto.start:false}")
    public void listen(final List<ConsumerRecord<String, String>> consumerRecords) {
        long startTime = System.currentTimeMillis();
        boolean timerRunning = true;

        final String fullTopicName = inputTopicName + "-" + enmID;
        log.info("Received {} records from topic: {}", consumerRecords.size(), fullTopicName);
        if (log.isDebugEnabled()) {
            metrics.printAllCounterValues("(" + fullTopicName + ") Records Received");
        }
        log.debug("Received records: {}", consumerRecords);

        outputTopicController.setNumberConsumerRecordsReceivedAtInput(consumerRecords.size());
        updateMetrics(consumerRecords.size());
        numberBatchesProcessed.getAndIncrement();

        log.debug("Starting to process the consumed records ...");
        int recordNumber = 0;
        final List<String> processedFiles = new ArrayList<>(consumerRecords.size());
        for (final ConsumerRecord<String, String> consumerRecord : consumerRecords) {
            recordNumber++;
            if (!timerRunning) {
                startTime = System.currentTimeMillis();
            }
            log.debug("Consuming record: {} ...", consumerRecord);
            final InputMessage inputMessageObj = gson.fromJson(consumerRecord.value(), InputMessage.class);
            processedFiles.add(inputMessageObj.getFileLocation());
            processNotification(inputMessageObj);

            if (recordNumber < consumerRecords.size()) {
                metrics.recordTimer(PROCESSED_COUNTER_FILES_TIME_TOTAL, startTime);
                timerRunning = false;
            }
        }
        String sftpBatchDownloadTime = String.valueOf(metrics.getGaugeValueByName(BATCH_SFTP_DOWNLOAD_TIME_TOTAL));
        String bdrBatchUploadTime = String.valueOf(metrics.getGaugeValueByName(BATCH_BDR_UPLOAD_TIME_TOTAL));

        log.info("Processed '{}'. SFTP batch download time: {}ms. BDR batch upload time: {}ms. Files: {}", processedFiles.size(),
                sftpBatchDownloadTime, bdrBatchUploadTime, processedFiles);

        metrics.resetGaugeByName(BATCH_SFTP_DOWNLOAD_TIME_TOTAL);
        metrics.resetGaugeByName(BATCH_BDR_UPLOAD_TIME_TOTAL);

        log.debug("All batch messages consumed");
        log.debug("----------------------------------------------------------------------------------");

        // testing shows that this point is only reached, if all stages of processNotification are successful
        sendBatchToOutput();
        metrics.recordTimer(PROCESSED_COUNTER_FILES_TIME_TOTAL, startTime);
        if (log.isDebugEnabled()) {
            metrics.printAllCounterValues("File Notification: Sent");
        }
        updateFileLatch(consumerRecords.size());
    }

    @Scheduled(cron = "0 */${metrics.log.print-interval-mins} * * * *")
    public void scheduledSendMetrics() {
        final String fullTopicName = inputTopicName + "-" + enmID;
        metrics.printAllCounterValues("(" + fullTopicName + ") Periodic Report");
    }

    public CountDownLatch getLatch() {
        return fileLatch;
    }

    public void setCountDownLatch(final int count) {
        fileLatch = new CountDownLatch(count);
    }

    public int getNumberBatchesProcessed() {
        return numberBatchesProcessed.get();
    }

    public void resetNumberBatchesProcessed() {
        numberBatchesProcessed.getAndSet(0);
    }

    private void updateMetrics(final int numConsumerRecordsReceived) {
        metrics.incrementCounterByValue(NUM_INPUT_KAFKA_MESSAGES_RECEIVED_TOTAL, numConsumerRecordsReceived);
    }

    /**
     * Decrement count-down latch by number files processed.
     * Even though max.poll.records = X, listener can choose to process a given list of say X records, in multiple batches.
     * Decrement latch on number of consumer record processed, not number batches processed.
     */
    private void updateFileLatch(final int numConsumerRecordsReceived) {
        for (int i = 0; i < numConsumerRecordsReceived; i++) {
            fileLatch.countDown();
        }
    }

    void processNotification(final InputMessage inputMessageObj) {
        try {
            if (sftpHandler.process(inputMessageObj)) {
                outputTopicController.addToKafkaMessages(inputMessageObj);
                consecutiveRetryHandler.reset();
            } else {
                // have already backoff & retried in SFTP download and BDR Upload, so end transaction now.
                consecutiveRetryHandler.incrementNumOfFails();
                log.error("FAILED to process (download/upload): '{}'.", inputMessageObj);
            }
        } catch (final Exception exception) {
            log.error("FAILED to process: '{}', error: {}", inputMessageObj, exception.getMessage());
            // need to catch Exception and not just kafkaException so that exceptions like null pointer exception are caught and re-tried.
            consecutiveRetryHandler.incrementNumOfFails();
            if (retryCount.get() < transactionMaxRetryAttempts) {
                log.error("RETRYING to process: '{}' (retry {}/{})", inputMessageObj, retryCount.get(), transactionMaxRetryAttempts);
                outputTopicController.resetOutput();
                retryCount.getAndIncrement();
                Utils.waitRetryInterval(transactionRetryIntervalMillis);
                // throw exception so that MessageListener will catch it and trigger a ListenerExecutionFailedException and so cause a transaction rollback
                throw exception;
            } else {
                // catching the exceptions here will not cause a transaction roll back unless it is thrown again.
                log.error("FAILED to process: '{}' (retry {}/{}).", inputMessageObj, retryCount.get(), transactionMaxRetryAttempts, exception);
                retryCount.getAndSet(0);
            }
        }
    }

    /**
     * While the kafka transaction ensures that the BATCH on the input topic is not committed unless ALL messages in the batch
     * complete the consumer-process-producer loop, tests revealed that the output topic STILL registered the successful messages
     * from a 'bad batch' on the output topic if the CONSUMER of the messages from the output topic is not setup with
     * 'concurrentKafkaListenerContainerFactory'
     * <p>
     * So if a batch size = 5 and messages 0,1,2 are successful and 3 fails.... messages 0,1,2 are put on output topic (and offset
     * is incremented), but input topic will restart processing from message 0 (and input offset is not incremented, unless all
     * messages are successful).
     * <p>
     * Note:
     * IF the consumer of the messages from the output topic IS setup with 'concurrentKafkaListenerContainerFactory', then either
     * ALL the messages from the BATCH is committed to output topic or none (expected behavior).
     * <p>
     * So the order of operations is updated to not 'send' messages to the output topic unless all stages are good,
     * .. Read BATCH from kafka Input Topic
     * .... Download
     * .... Upload
     * .... Prepare the Kafka Message to send to output topic (Read kafka header)
     * .... Add Message to list of messages to send.
     * <p>
     * .. All messages in batch processed successfully
     * .. Send BATCH to producer. (and commit BATCH to INPUT TOPIC)
     */
    private void sendBatchToOutput() {
        outputTopicController.sendKafkaMessages();
        retryCount.getAndSet(0);
    }
}
