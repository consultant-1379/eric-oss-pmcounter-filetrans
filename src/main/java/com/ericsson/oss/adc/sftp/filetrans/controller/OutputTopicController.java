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

import com.ericsson.oss.adc.sftp.filetrans.configuration.properties.DataCatalogProperties;
import com.ericsson.oss.adc.sftp.filetrans.configuration.properties.DataServiceProperties;
import com.ericsson.oss.adc.sftp.filetrans.model.InputMessage;
import com.ericsson.oss.adc.sftp.filetrans.model.OutputMessage;
import com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil;
import com.ericsson.oss.adc.sftp.filetrans.util.StartupUtil;
import com.ericsson.oss.adc.sftp.filetrans.util.Utils;
import com.google.gson.Gson;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_INPUT_KAFKA_MESSAGES_RECEIVED_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_INPUT_KAFKA_MESSAGES_REPLAYED_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_OUTPUT_KAFKA_MESSAGES_FAILED_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.PROCESSED_COUNTER_FILES_TIME_TOTAL;


@Component
@Slf4j
@Data
public class OutputTopicController {

    private static final String NODE_NAME = "nodeName";
    private static final String SUB_SYSTEM_TYPE = "subSystemType";
    private static final String NODE_TYPE = "nodeType";
    private static final String DATA_TYPE = "dataType";
    private static final String FILE_TYPE = "fileType";
    private static final int BACKOFF_PERIOD_IN_MILLIS = 60000;
    private static final String MIN_IN_SYNC_REPLICAS = "min.insync.replicas";
    private static final int BROKER_NUMBER = 0;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Autowired
    private KafkaTemplate kafkaOutputTemplate;

    @Autowired
    private DataCatalogProperties dataCatalogProperties;

    @Autowired
    private DataServiceProperties dataServiceProperties;

    @Autowired
    private SFTPProcessingMetricsUtil metrics;

    @Autowired
    private StartupUtil startupUtil;

    @Autowired
    private Gson gson;

    @Value("${spring.kafka.topics.output.partitions}")
    private int partitions;

    @Value("${spring.kafka.topics.output.replicas}")
    private short replicas;

    @Value("${spring.kafka.topics.output.compression}")
    private String compression;

    @Value("${spring.kafka.topics.output.retention}")
    private String retention;

    private String outputTopicName;

    @Value("${spring.kafka.topics.enm_id}")
    private String enmID;

    @Value("${spring.kafka.transaction.max-retry-attempts}")
    private int transactionMaxRetryAttempts;

    private final List<Message<String>> messagesToSendToOutput = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger numberConsumerRecordsReceivedAtInput = new AtomicInteger();
    private CountDownLatch splitBatchLatch = new CountDownLatch(1);
    private static final String SUBSYSTEMTYPE = "pENM";

    @PostConstruct
    private void init(){
        outputTopicName = dataServiceProperties.getDataServiceName();
    }



    @Retryable(value = KafkaException.class,
            maxAttemptsExpression = "${retryable.kafka-topic.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${retryable.kafka-topic.delay:3000}"))
    public boolean buildAndCreateTopic() {
        final short minInSyncReplicas = getMinIsrReplicas(BROKER_NUMBER);
        if (replicas < minInSyncReplicas) {
            log.info("Number of Output Replicas ({}) is less than Minimum In-Sync Replicas ({}). Setting Output replicas to {}.",
                    replicas, minInSyncReplicas, minInSyncReplicas);
            replicas = minInSyncReplicas;
        }

        final NewTopic outputTopic = TopicBuilder.name(outputTopicName).partitions(partitions).replicas(replicas)
                .config(TopicConfig.RETENTION_MS_CONFIG, retention).config(TopicConfig.COMPRESSION_TYPE_CONFIG, compression).build();

        log.info("Attempting to create Kafka Output Topic (name: {}, replicas: {}, partitions: {}, retention: {}, compression: {})",
                outputTopicName, replicas, partitions, retention, compression);
        return createTopic(outputTopic);
    }

    public short getMinIsrReplicas(final int brokerId) {
        final AdminClient client = getAdminClient(kafkaAdmin.getConfigurationProperties().get("bootstrap.servers"));
        if (brokerId >= 0) {
            final ConfigResource brokenConfigResource = new ConfigResource(ConfigResource.Type.BROKER, String.valueOf(brokerId));
            try {
                final KafkaFuture<Config> kfConfigResult = client.describeConfigs(Collections.singletonList(brokenConfigResource)).values()
                        .get(brokenConfigResource);
                final Config configResult = (Config) getFuture(kfConfigResult);
                if (configResult != null) {
                    final ConfigEntry configEntry = configResult.get(MIN_IN_SYNC_REPLICAS);
                    final String configEntryValue = configEntry.value();
                    log.debug("Broker with ID '{}' has 'min.insync.replicas' set to: '{}'.", brokerId, configEntryValue);
                    client.close();
                    return Short.parseShort(configEntry.value());
                }
            } finally {
                client.close();
            }
        }
        return 0;
    }

    protected boolean createTopic(final NewTopic outputTopic) {
        try {
            kafkaAdmin.createOrModifyTopics(outputTopic);
            return true;
        } catch (final KafkaException kafkaException) {
            log.error("FAILED to create '{}' Output Topic, error: {}", outputTopic.name(), kafkaException.getMessage());
            log.warn("Waiting {} ms before attempting to create '{}' Output Topic again ...", outputTopicName, BACKOFF_PERIOD_IN_MILLIS);
            Utils.waitRetryInterval(BACKOFF_PERIOD_IN_MILLIS);
            throw kafkaException;
        }
    }

    @Recover
    protected boolean buildAndCreateTopicRecover(KafkaException exception) {
        return false;
    }

    Object getFuture(final KafkaFuture<?> kf) {
        try {
            return kf.get();
        } catch (final ExecutionException | InterruptedException exception) {
            log.error("ERROR while checking the result of a Kafka Future: {}", exception.getMessage(), exception);
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private AdminClient getAdminClient(final Object bootstrapServers) {
        final Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return AdminClient.create(properties);
    }

    public void addToKafkaMessages(final InputMessage inputMessageObj) {
        final OutputMessage outputMessagePayload = generateOutputMessagePayload(inputMessageObj);
        final String nodeName = inputMessageObj.getNodeName();

        final Message<String> message = MessageBuilder.withPayload(gson.toJson(outputMessagePayload))
                .setHeader(KafkaHeaders.KEY, nodeName)
                .setHeader(KafkaHeaders.TOPIC, outputTopicName)
                .setHeader(NODE_NAME, inputMessageObj.getNodeName())
                .setHeader(SUB_SYSTEM_TYPE, SUBSYSTEMTYPE)
                .setHeader(NODE_TYPE, inputMessageObj.getNodeType())
                .setHeader(DATA_TYPE, inputMessageObj.getDataType())
                .setHeader(FILE_TYPE, inputMessageObj.getFileType()).build();
        log.debug("Adding message: '{}' for nodeName'{}' to Output Topic: {}", message, nodeName, outputTopicName);
        messagesToSendToOutput.add(message);
    }

    @SuppressWarnings("unchecked")
    @Transactional(rollbackFor = Exception.class)
    public boolean sendKafkaMessages() {
        int count = 0;
        for (final Message<String> message : messagesToSendToOutput) {
            try {
                final CompletableFuture<SendResult<?, ?>> lf =  kafkaOutputTemplate.send(message);
                if (successfulSend(lf, message)) {
                    log.debug("SENT message: '{}' to '{}' Output Topic",
                            message, outputTopicName);
                    count++;
                }

            } catch (final Exception exception) {
                log.error("FAILED to send message: '{}' to '{}' Output Topic",
                        message, outputTopicName);
                if (transactionMaxRetryAttempts > 0) {
                    throw exception;
                }
            }
        }

        log.info("SENT {}/{} messages to Output Topic: '{}' (received {} records from the Input Topic)", count, messagesToSendToOutput.size(),
                outputTopicName, numberConsumerRecordsReceivedAtInput.get());
        log.info("Cumulative Metrics ... Received: {}, Replayed: {}, Sent: {}, Time: {}",
                metrics.getCounterValueByName(NUM_INPUT_KAFKA_MESSAGES_RECEIVED_TOTAL),
                metrics.getCounterValueByName(NUM_INPUT_KAFKA_MESSAGES_REPLAYED_TOTAL),
                metrics.getCounterValueByName(NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY),
                metrics.getTimer(PROCESSED_COUNTER_FILES_TIME_TOTAL).totalTime(TimeUnit.MILLISECONDS));

        checkAndSetFailedMetrics();
        resetOutput();
        splitBatchLatch.countDown();
        return true;
    }

    public boolean successfulSend(final CompletableFuture<SendResult<?, ?>> lf, Message<String> message) {
        if (!lf.isCancelled()) {
            metrics.incrementCounterByName(NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY);
            return true;
        } else {
            log.error("FAILED to send message: '{}'. Operation was CANCELLED before completion.", message);
            metrics.incrementCounterByName(NUM_OUTPUT_KAFKA_MESSAGES_FAILED_TOTAL);
        }
        return false;
    }


    public void resetOutput() {
        if (messagesToSendToOutput != null) {
            messagesToSendToOutput.clear();
        }
        numberConsumerRecordsReceivedAtInput.getAndSet(0);
    }

    public void setNumberConsumerRecordsReceivedAtInput(final int numberConsumerRecords) {
        numberConsumerRecordsReceivedAtInput.getAndAdd(numberConsumerRecords);
    }

    public CountDownLatch getBatchLatch() {
        return splitBatchLatch;
    }

    public void setCountDownBatchLatch(final int count) {
        splitBatchLatch = new CountDownLatch(count);
    }

    private void checkAndSetFailedMetrics() {
        if (messagesToSendToOutput != null) {
            final int delta = numberConsumerRecordsReceivedAtInput.get() - messagesToSendToOutput.size();
            if (delta > 0) {
                for (int i = 0; i < delta; i++) {
                    metrics.incrementCounterByName(NUM_OUTPUT_KAFKA_MESSAGES_FAILED_TOTAL);
                }
            }
        }
    }

    private OutputMessage generateOutputMessagePayload(final InputMessage inputMessage) {
        final OutputMessage outputMessage = new OutputMessage();
        outputMessage.setFileLocation(startupUtil.getCreatedBucketName() + inputMessage.getFileLocation());
        return outputMessage;
    }
}
