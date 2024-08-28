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

import static com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel.FileRepoType.S3;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_FAILED_BDR_UPLOADS_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_FAILED_FILE_TRANSFER_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_INPUT_KAFKA_MESSAGES_RECEIVED_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_INPUT_KAFKA_MESSAGES_REPLAYED_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_OUTPUT_KAFKA_MESSAGES_FAILED_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_SUCCESSFUL_BDR_UPLOADS_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_TRANSACTIONS_ROLLEDBACK_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.PROCESSED_BDR_DATA_VOLUME_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.PROCESSED_COUNTER_FILE_DATA_VOLUME_TOTAL;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.ericsson.oss.adc.sftp.filetrans.Startup;
import com.ericsson.oss.adc.sftp.filetrans.configuration.KafkaConfig;
import com.ericsson.oss.adc.sftp.filetrans.configuration.properties.DataCatalogProperties;
import com.ericsson.oss.adc.sftp.filetrans.configuration.properties.DataServiceProperties;
import com.ericsson.oss.adc.sftp.filetrans.model.InputMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.adc.sftp.filetrans.controller.InputTopicListener;
import com.ericsson.oss.adc.sftp.filetrans.controller.OutputTopicController;
import com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel;
import com.ericsson.oss.adc.sftp.filetrans.model.OutputMessage;
import com.ericsson.oss.adc.sftp.filetrans.service.BDRService;
import com.ericsson.oss.adc.sftp.filetrans.service.ConnectedSystemsRetriever;
import com.ericsson.oss.adc.sftp.filetrans.service.SFTPFileTransferService;
import com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil;
import com.ericsson.oss.adc.sftp.filetrans.util.StartupUtil;
import com.ericsson.oss.adc.sftp.filetrans.util.TestUtils;
import com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer;

import com.google.gson.Gson;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "subsystem.name=enm1",
        "dmm.data-catalog.base-url=http://localhost:",
        "dmm.data-catalog.base-port=9590",
        "dmm.data-catalog.notification-topic-uri=/catalog/v1/notification-topic/",
        "dmm.data-catalog.file-format-uri=/catalog/v1/file-format/",
        "dmm.data-catalog.file-format-uri-v2-uri=/catalog/v2/file-format/",
        "dmm.data-catalog.message-bus-uri=/catalog/v1/message-bus/",
        "dmm.data-catalog.bulk-data-repository-uri=/catalog/v1/bulk-data-repository/",
        "dmm.data-catalog.data-provider-type-5G=5G",
        "dmm.data-catalog.data-provider-type-4G=4G",
        "dmm.data-catalog.data-space=enm",
        "dmm.data-catalog.data-category=PM_COUNTERS",
        "connected.systems.uri=subsystem-manager/v1/subsystems/"})
@EmbeddedKafka(partitions = 1, topics = { "file-notification-service--sftp-filetrans--enm1", "sftp-filetrans--enm1" }, brokerProperties = {
        "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1", "replica.fetch.min.bytes=102400",
        "replica.fetch.wait.max.ms=300000", "replica.socket.timeout.ms=300000", "replica.lag.time.max.ms=300000" })
@DirtiesContext
@ActiveProfiles("noAsyncTest")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SFTPFileTransferIntegration {

    protected static final Logger LOGGER = LoggerFactory.getLogger(SFTPFileTransferIntegration.class);
    protected static final int WAIT_CONSUMER_LOCK_SECONDS = 1;
    protected static final int WAIT_MS = 1000;
    protected static final int INPUT_TOPIC_LATCH_TIIMEOUT_MS = 1000;
    protected static final int PRODUCER_SEND_WAIT_MS = 1000;
    protected static final int PORT = 1234;
    protected static final String CONNECTED_SYSTEMS_BASE_URL = "http://eric-eo-subsystem-management/subsystem-manager/v1/subsystems/";
    protected static final String USER = "user";
    protected static final String PASSWORD = "password";
    protected static final String NODE_NAME = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR102gNodeBRadio00010,ManagedElement=NR102gNodeBRadio00010";
    protected static final String ENM_FILE_PATH = "/ericsson/pmic1/XML/SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR102gNodeBRadio00010,ManagedElement=NR102gNodeBRadio00010/";
    private static final String SUB_SYSTEM_TYPE = "pENM";

    private static final String ENM_BUCKET_NAME = "enm2";
    protected static final String NODE_TYPE = "RadioNode";
    protected static final String DATA_TYPE = "4G";
    protected static final String FILE_TYPE = "XML";

    protected static final String FILE_LOCATION_STRING = "fileLocation";

    @Value("${spring.kafka.topics.input.name}")
    private String inputTopicName;

    @Value("${spring.kafka.topics.enm_id}")
    protected String enmID;

    private String outputTopicName;

    @Value("${spring.kafka.consumer.max-poll-records}")
    protected int maxPollRecords;

    @Value("${spring.kafka.consumer.group-id}")
    protected String group;

    @Autowired
    private KafkaConfig kafkaConfig;

    @Autowired
    protected RestTemplate restTemplate;

    @Autowired
    protected InputTopicListener inputTopicListener;

    @MockBean
    private Startup startup;

    @Autowired
    protected ConnectedSystemsRetriever connectedSystemsRetriever;

    @Autowired
    protected KafkaOutputConsumerTestIntegration kafkaOutputConsumerTestIntegration;

    @Autowired
    protected EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    protected SFTPFileTransferService sftpFileTransferService;

    @SpyBean
    protected OutputTopicController outputTopicController;

    @Autowired
    protected DataCatalogProperties dataCatalogProperties;

    @Autowired
    protected DataServiceProperties dataServiceProperties;

    @MockBean
    protected BDRService bdrService;

    @Autowired
    protected StartupUtil startupUtil;

    @Autowired
    Gson gson;

    @Mock
    protected final MinioClient minioClientMock = mock(MinioClient.class);

    @Mock
    protected ObjectWriteResponse objectWriteResponseMock;

    @Autowired
    protected SFTPProcessingMetricsUtil metrics;

    protected MockRestServiceServer mockServer;
    protected String brokerAddresses = null;

    private Producer<String, String> producer;

    private final int partition = 0;

    /**
     * Initializes kafka and other items for test.
     *
     * @throws Exception
     *          the exception
     */
    protected void init() throws Exception {
        outputTopicName = dataServiceProperties.getDataServiceName();
        embeddedKafkaBroker.restart(0);
        brokerAddresses = embeddedKafkaBroker.getBrokersAsString();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        connectedSystemsRetriever.addAccessEndpoints(new ArrayList<>(Collections.singletonList("http://eric-eo-subsystem-management/")));
        createProducer();
    }

    protected void setupForTesting(final FakeSftpServer server) throws URISyntaxException, InvalidKeyException, ErrorResponseException,
        IllegalArgumentException, InsufficientDataException, InternalException, InvalidResponseException,
        NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
        server.setPort(PORT);
        server.addUser(USER, PASSWORD);

        mockBdr();

        mockConnectedSystems();
    }

    protected void createProducer() {
        final Map<String, Object> configs = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.RETRIES_CONFIG, 0);  // default to 0; set to 2147483647 in logs, due to transaction maybe?
        configs.put(ProducerConfig.BATCH_SIZE_CONFIG, kafkaConfig.getBatchSizeConfig());
        configs.put(ProducerConfig.LINGER_MS_CONFIG, kafkaConfig.getLingerConfig());
        configs.put(ProducerConfig.BUFFER_MEMORY_CONFIG, kafkaConfig.getBufferMemoryConfig());
        configs.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        final ProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<String, String>(configs);
        producer = pf.createProducer();
    }

    protected void endToEndRollbacksTest(final FakeSftpServer server, final int numBatches, final long expectedOffset, final int numOfTransactionRollbacks) throws Exception {
        endToEndRollbacksTest(server, numBatches, expectedOffset, 0, 0, numOfTransactionRollbacks);
    }

    protected void getOutputTopicControllerToFailWriteToKafka() {
        when(outputTopicController.sendKafkaMessages()).thenThrow(NullPointerException.class);
    }

    /**
     * This mocks the calls to addToKafkaMessages in the OutputTopicController
     * There is three batches of 5 messages mocked here.
     * Batch 1 and 2 -  Throws a NullPointerException on the forth call to the method to trigger the rollback
     * Batch 3 Calls the real method four times and on the fifth call does nothing.
     * doNothing() is called here because we do not want to add the message to messagesToSendToOutput
     */
    protected void mockAddToKafkaMessagesResponseToTriggerRollbacks() {
        Mockito.doNothing().doNothing().doNothing().doThrow(NullPointerException.class).doNothing() // Batch 1
                .doNothing().doNothing().doNothing().doThrow(NullPointerException.class).doNothing() // Batch 2
                .doCallRealMethod().doCallRealMethod().doCallRealMethod().doCallRealMethod().doNothing() // Batch 3
                .when(outputTopicController).addToKafkaMessages(any());

    }

    protected void setOutputTopicControllerMocksBackToCallingRealMethods() {
        Mockito.doCallRealMethod().when(outputTopicController).addToKafkaMessages(any());
    }

    protected void endToEndRollbacksTest(final FakeSftpServer server, final int numBatches, final long expectedOffset,
                                         final int halfBatchNo, final int expectedTransactionRollbacks, final int numOfTransactionRollbacks)
        throws Exception {
        final int numFilesTotal = (maxPollRecords * numBatches) - halfBatchNo;

        final MetricsHolder metricsHolderAtStart = getMetricsNow();
        LOGGER.info(
            "Performing end To end test (Success) with Unlimited Rollbacks; numBatches = {}, halfBatchNo = {}, numtransactionRollBacks = {}, expectedTransactionRollbacks = {}",
            numBatches,
            halfBatchNo, numOfTransactionRollbacks, expectedTransactionRollbacks);
        ReflectionTestUtils.setField(inputTopicListener, "transactionMaxRetryAttempts", numOfTransactionRollbacks);
        ReflectionTestUtils.setField(inputTopicListener, "transactionRetryIntervalMillis", 1000);
        ReflectionTestUtils.setField(startupUtil, "createdBucketName", ENM_BUCKET_NAME);

        kafkaOutputConsumerTestIntegration.setCountDownLatch(numFilesTotal); // # Consumer Records
        inputTopicListener.setCountDownLatch(numFilesTotal); // # Consumer Records
        inputTopicListener.resetNumberBatchesProcessed(); // # batches

        final List<String> listFilenames = createAndSendBatchOfMessages(server, 0, numFilesTotal);
        TimeUnit.MILLISECONDS.sleep(PRODUCER_SEND_WAIT_MS);
        inputTopicListener.getLatch().await(INPUT_TOPIC_LATCH_TIIMEOUT_MS, TimeUnit.MILLISECONDS);

        kafkaOutputConsumerTestIntegration.getLatch().await(numFilesTotal * WAIT_CONSUMER_LOCK_SECONDS, TimeUnit.SECONDS);
        TimeUnit.MILLISECONDS.sleep(WAIT_MS);
        LOGGER.info("File Notification Test: (testing for)/Received {} Consumers Records in {} Batch(s)/Attempts", numFilesTotal,
            inputTopicListener.getNumberBatchesProcessed());

        assertTrue(sftpFileTransferService.isConnectionOpen());

        LOGGER.info("Verify that Notification is made available on Output Kafka Topic. kafka consumer countdown = {}",
            kafkaOutputConsumerTestIntegration.getLatch().getCount());
        assertTrue(kafkaOutputConsumerTestIntegration.getLatch().getCount() == 0L);

        final MetricsHolder metricsHolderNow = getMetricsNow();
        checkMetrics(metricsHolderAtStart, metricsHolderNow, numFilesTotal, numFilesTotal, 0, 0, false);

        assertEquals(kafkaOutputConsumerTestIntegration.getNumberRecordsReceived(), numFilesTotal);
        assertEquals(kafkaOutputConsumerTestIntegration.getConsumerRecords().size(), numFilesTotal);

        final int numActualTransactionRollbacks = (int)(metrics.getCounterValueByName(NUM_TRANSACTIONS_ROLLEDBACK_TOTAL));
        assertEquals("Expected " + expectedTransactionRollbacks + " transaction rollbacks, got " + numActualTransactionRollbacks,
            expectedTransactionRollbacks, numActualTransactionRollbacks);

        testNumberOutputMessages(numFilesTotal, metricsHolderAtStart);

        final List<ConsumerRecord<String, String>> consumerRecordsReceived = kafkaOutputConsumerTestIntegration.getConsumerRecords();

        final List<Map<String, String>> expectedMapsList = getExpectedOutputNotificationContent(listFilenames);

        final OffsetAndMetadata kafkaOffset = KafkaTestUtils.getCurrentOffset(brokerAddresses, group, inputTopicName + enmID, partition);
        LOGGER.debug("kafkaUtils offset = {}, metaData = {} ", kafkaOffset.offset(), kafkaOffset.toString());

        assertEquals("Kafka Offset not correct", expectedOffset, kafkaOffset.offset());
        final long baseOffset = consumerRecordsReceived.get(0).offset();
        int index = 0;
        for (final ConsumerRecord<String, String> consumerRecordRec : consumerRecordsReceived) {
            final Map<String, String> expectedMap = expectedMapsList.get(index);

            LOGGER.info("Verify that key for consumer record {} received has correct node name", index + 1);
            assertTrue(consumerRecordRec.key().toString().contains(NODE_NAME));

            LOGGER.info("Verify that consumer record {} received has the correct meta information", index + 1);
            LOGGER.info("Consumer Record topic{}, partition = {}, leaderEpoch = {}, offset = {}, timestamp = {}", consumerRecordRec.topic(),
                consumerRecordRec.partition(), consumerRecordRec.leaderEpoch(), consumerRecordRec.offset(), consumerRecordRec.timestamp());
            assertEquals("Expect partition to be zero", 0, consumerRecordRec.partition());

            assertTrue("Expected offset in consumer record " + (index + 1) + "to be >/= offset of consumer record 0",
                consumerRecordRec.offset() >= baseOffset);

            LOGGER.info("Verify that consumer record {} received have correct header information", index + 1);
            LOGGER.debug("Headers for consumer record {} received : {}", index + 1, consumerRecordRec.headers().toArray());
            final List<Header> headerList = Arrays.asList(consumerRecordRec.headers().toArray());
            for (final Header header : headerList) {
                final String key = header.key();
                final String value = new String(header.value(), StandardCharsets.UTF_8);
                LOGGER.debug("CONSUMER RECORD RECEIVED HEADER : {}:{}", key, value);
                LOGGER.debug("EXPECTED                        : {}:{}", key, expectedMap.get(key));
                assertTrue(expectedMap.get(key).equals(value));
            }

            assertTrue(consumerRecordRec.value().contains(FILE_LOCATION_STRING));
            final OutputMessage outputMessage = gson.fromJson(consumerRecordRec.value(), OutputMessage.class);
            assertTrue(outputMessage.getFileLocation().contains(ENM_BUCKET_NAME + ENM_FILE_PATH));

            index++;
        }
    }

    protected void endToEndWithFailureLimitedRollbackTest(final FakeSftpServer server, final int numBatches, final int halfBatchNo,
                                                          final int badFileInBatchPosition, final int numTransactionRollBacks)
        throws Exception {

        Map<String, Map<Integer, Long>> topicDetailsAtStart = getTopicDetails();
        final MetricsHolder metricsHolderAtStart = getMetricsNow();

        LOGGER.info(
            "Performing end To end test (Failure) with limited Rollback; numBatches = {}, halfBatchNo = {}, badFileInBatch = {}, numTransactionRollBacks = {}",
            numBatches, halfBatchNo, badFileInBatchPosition, numTransactionRollBacks);

        ReflectionTestUtils.setField(inputTopicListener, "transactionMaxRetryAttempts", numTransactionRollBacks);
        ReflectionTestUtils.setField(inputTopicListener, "transactionRetryIntervalMillis", 1000);

        final int numFilesTotal = (maxPollRecords * numBatches) - halfBatchNo;
        //one bad file per batch
        final int numBadFilesTotal = (1 * numBatches);

        kafkaOutputConsumerTestIntegration.setCountDownLatch(numFilesTotal); // # Consumer Records
        inputTopicListener.setCountDownLatch(numFilesTotal); // # Consumer Records
        inputTopicListener.setCountDownLatch(numFilesTotal); // # batches
        outputTopicController.setCountDownBatchLatch(1); // # check for split batches

        createAndSendBatchOfMessages(server, 0, numFilesTotal, badFileInBatchPosition);

        TimeUnit.MILLISECONDS.sleep(PRODUCER_SEND_WAIT_MS);

        // workaround for test - givenMockedSFTPVerifyServerConnectionAndOneFullBatchFilesWithOneBadFileDownloadedSuccessfully
        // If files sent in 2 batches and the first batch is successful.
        if (numTransactionRollBacks == Integer.MAX_VALUE) {
            // might need a while loop here.
            outputTopicController.getBatchLatch().await(INPUT_TOPIC_LATCH_TIIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (outputTopicController.getBatchLatch().getCount() == 0 && inputTopicListener.getLatch().getCount() != 0) {
                LOGGER.info("Split batch detected, Resetting Topic details at start");
                topicDetailsAtStart = getTopicDetails();
            }
        }
        inputTopicListener.getLatch().await(INPUT_TOPIC_LATCH_TIIMEOUT_MS, TimeUnit.MILLISECONDS);
        LOGGER.info("File Notification Test: (testing for)/Received {} Consumers Records in {} Batch(s)/Attempts", numFilesTotal,
            inputTopicListener.getNumberBatchesProcessed());

        // this will wait the full wait time as no consumer records are downloaded.
        kafkaOutputConsumerTestIntegration.getLatch().await(numFilesTotal * WAIT_CONSUMER_LOCK_SECONDS, TimeUnit.SECONDS);
        TimeUnit.MILLISECONDS.sleep(WAIT_MS);

        final Map<String, Map<Integer, Long>> topicDetailsAtEnd = getTopicDetails();
        LOGGER.info("Topic Details At Start: {}", topicDetailsAtStart);
        LOGGER.info("Topic Details At End: {}", topicDetailsAtEnd);

        assertTrue(sftpFileTransferService.isConnectionOpen());

        final MetricsHolder metricsHolderNow = getMetricsNow();

        if (numTransactionRollBacks == Integer.MAX_VALUE) {
            assertEquals("Offsets for both input and output topics should equal (UnLimited Transaction Rollbacks)", topicDetailsAtStart,
                topicDetailsAtEnd);
            checkMetrics(metricsHolderAtStart, metricsHolderNow, numFilesTotal, 0, 0, numTransactionRollBacks, true);
            assertEquals(kafkaOutputConsumerTestIntegration.getNumberRecordsReceived(), 0);
            assertEquals(kafkaOutputConsumerTestIntegration.getConsumerRecords().size(), 0);
            testNumberOutputMessages(0, metricsHolderAtStart);
        } else if (maxPollRecords == 1 && numTransactionRollBacks <= 0) {
            assertEquals("Output consumer should have received no records",0, kafkaOutputConsumerTestIntegration.getNumberRecordsReceived());
            assertEquals("Output consumer should have size of 0",0, kafkaOutputConsumerTestIntegration.getConsumerRecords().size());
            testOffsetsWhenNoMessageSent(topicDetailsAtStart, numFilesTotal, topicDetailsAtEnd);
        } else {
            testOffsetsWithSuccessfulMessagesSent(topicDetailsAtStart, numFilesTotal, topicDetailsAtEnd);
            checkMetrics(metricsHolderAtStart, metricsHolderNow, numFilesTotal, numFilesTotal - numBadFilesTotal, numBadFilesTotal,
                    numTransactionRollBacks, true);
            assertEquals(kafkaOutputConsumerTestIntegration.getNumberRecordsReceived(), numFilesTotal - numBadFilesTotal);
            assertEquals(kafkaOutputConsumerTestIntegration.getConsumerRecords().size(), numFilesTotal - numBadFilesTotal);
            testNumberOutputMessages(numFilesTotal - numBadFilesTotal, metricsHolderAtStart);
        }
    }

    private void testNumberOutputMessages(final int numFilesPerBatch, final MetricsHolder metricsHolderAtStart) {
        final int numMessagesDownloaded = (int) metrics.getCounterValueByName(NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY)
            - (int) metricsHolderAtStart.getNumOutputKafkaMessagesProducedSuccessfullyTotal();
        assertEquals("Expected " + numFilesPerBatch + " output messages", numFilesPerBatch, numMessagesDownloaded);
    }

    private void testOffsetsWithSuccessfulMessagesSent(final Map<String, Map<Integer, Long>> topicDetailsAtStart, final int numFilesTotal,
                                                       final Map<String, Map<Integer, Long>> topicDetailsAtEnd) {
        for (final Map.Entry<String, Map<Integer, Long>> entry : topicDetailsAtStart.entrySet()) {

            final String topicName = entry.getKey();
            final Map<Integer, Long> partitionOffsetMapStart = entry.getValue();
            final Map<Integer, Long> partitionOffsetMapEnd = topicDetailsAtEnd.get(topicName);
            assertTrue("Partition info should exist for topic " + topicName, partitionOffsetMapEnd != null);

            for (final Map.Entry<Integer, Long> entry2 : partitionOffsetMapStart.entrySet()) {
                final int pNoStart = entry2.getKey();
                final long offsetStart = partitionOffsetMapStart.get(pNoStart);
                final long offsetEnd = partitionOffsetMapEnd.get(pNoStart);
                LOGGER.debug("Compare : pNoStart {}, offsetStart {}, offsetEnd {}, numFilesPerBatch {} ", pNoStart, offsetStart, offsetEnd,
                    numFilesTotal);
                assertEquals("Offsets for output topic should have increased by " + numFilesTotal, offsetEnd, offsetStart + numFilesTotal);
            }
        }
    }

    private void testOffsetsWhenNoMessageSent(final Map<String, Map<Integer, Long>> topicDetailsAtStart, final int numFilesTotal,
                                              final Map<String, Map<Integer, Long>> topicDetailsAtEnd){

        for (final Map.Entry<String, Map<Integer, Long>> entry : topicDetailsAtStart.entrySet()) {
            final String topicName = entry.getKey();
            final Map<Integer, Long> partitionOffsetMapStart = entry.getValue();
            LOGGER.debug("Topic name:{}", topicName);
            LOGGER.debug("partitionOffsetMapStart:{}", partitionOffsetMapStart);
            final Map<Integer, Long> partitionOffsetMapEnd = topicDetailsAtEnd.get(topicName);
            LOGGER.debug("partitionOffsetMapEnd :{}", partitionOffsetMapEnd );
            assertTrue("Partition info should exist for topic " + topicName, partitionOffsetMapEnd != null);

            for (final Map.Entry<Integer, Long> entry2 : partitionOffsetMapEnd.entrySet()) {
                final int pNoStart = entry2.getKey();
                long offsetStart = 0;
                if (partitionOffsetMapStart.get(pNoStart) != null){
                    offsetStart = partitionOffsetMapStart.get(pNoStart);
                }
                final long offsetEnd = partitionOffsetMapEnd.get(pNoStart);
                if(topicName.equals(inputTopicName + enmID)) {
                    LOGGER.debug("Input topic name: {}. Compare : pNoStart {}, offsetStart {}, offsetEnd {}, numFilesPerBatch {} ", inputTopicName, pNoStart, offsetStart, offsetEnd,
                        numFilesTotal);
                    assertEquals("Offsets input topic should have increased by " + numFilesTotal, offsetEnd, offsetStart + numFilesTotal);
                }
                else if (topicName.equals(outputTopicName)){
                    LOGGER.debug("Output topic name: {}. Compare : pNoStart {}, offsetStart {}, offsetEnd {}, numFilesPerBatch {} ", outputTopicName, pNoStart, offsetStart, offsetEnd,
                        numFilesTotal);
                    assertEquals("Offsets output topic should not have increased by " + numFilesTotal, offsetEnd, offsetStart);
                }
            }
        }
    }

    private void mockConnectedSystems() throws URISyntaxException {
        mockServer.reset();
        mockServer.expect(ExpectedCount.once(), requestTo(new URI(CONNECTED_SYSTEMS_BASE_URL))).andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                .body(new InputStreamResource(getClass().getClassLoader().getResourceAsStream("IntegrationGetSubsystemsResponse.json"))));
    }


    private void mockBdr() throws
        InvalidKeyException, ErrorResponseException, IllegalArgumentException, InsufficientDataException, InternalException,
        InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException, IOException
    {

        final BulkDataRepositoryModel[] bdrDTOs = new BulkDataRepositoryModel[1];
        final BulkDataRepositoryModel bulkDataRepositoryModel = BulkDataRepositoryModel.builder()
                .name("testBDR1")
                .clusterName("testCluster")
                .nameSpace("testNS")
                .accessEndpoints(new ArrayList<>(Collections.singletonList("http://eric-data-object-storage-mn:9000")))
                .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
                .fileRepoType(S3)
                .build();

        bdrDTOs[0] = bulkDataRepositoryModel;
        ReflectionTestUtils.setField(bdrService, "numConnectionAttemptsMax", 3);
        ReflectionTestUtils.setField(bdrService, "retryIntervalMs", 10000);
        ReflectionTestUtils.setField(bdrService, "minioClient", minioClientMock);
        when(bdrService.getMetrics()).thenReturn(metrics);
        when(minioClientMock.putObject(Mockito.any(PutObjectArgs.class))).thenReturn(objectWriteResponseMock);
        when(bdrService.uploadObject(anyString(), anyString(), Mockito.any(byte[].class))).thenCallRealMethod();
        when(bdrService.doUpload(anyString(), Mockito.any(byte[].class), Mockito.any(String.class),
            Mockito.any(ByteArrayInputStream.class)))
            .thenCallRealMethod();
        when(bdrService.getAccessEndPointsFromDataCatalog(Mockito.any())).thenReturn(bulkDataRepositoryModel);
    }

    private List<Map<String, String>> getExpectedOutputNotificationContent(final List<String> listFilenames){
        final List<Map<String, String>> expectedMapsList = new ArrayList<>();
        for (final String inputFile : listFilenames) {
            final Map<String, String> expectedMap = new HashMap<>();
            expectedMap.put("nodeType", NODE_TYPE);
            expectedMap.put("fileType", FILE_TYPE);
            expectedMap.put("subSystemType", SUB_SYSTEM_TYPE);
            expectedMap.put("nodeName", NODE_NAME);
            expectedMap.put("dataType", DATA_TYPE);
            expectedMap.put("spring_json_header_types",
                "{\"nodeName\":\"java.lang.String\",\"subSystemType\":\"java.lang.String\",\"dataType\":\"java.lang.String\",\"nodeType\":\"java.lang.String\",\"fileType\":\"java.lang.String\"}");
            expectedMap.put("__TypeId__", "java.lang.String");
            expectedMapsList.add(expectedMap);
        }
        return expectedMapsList;
    }

    private List<String> createAndSendBatchOfMessages(final FakeSftpServer server, final int startingIndex, final int count)
        throws IOException {
        return createAndSendBatchOfMessages(server, startingIndex, count, - 1);
    }

    private List<String> createAndSendBatchOfMessages(final FakeSftpServer server, final int startingIndex, final int count, final int badFileInBatch)
        throws IOException {
        final List<String> listFilenames = TestUtils.getFiles(ENM_FILE_PATH, count);
        TestUtils.putFiles(server, listFilenames);
        final List<String> inputJsonListFiles;
        if (badFileInBatch >= 0) {
            LOGGER.debug("Generating BAD file at file number {}/{}", badFileInBatch, count - 1);
            inputJsonListFiles = TestUtils.getInputJsonListFilesOneBad(listFilenames, badFileInBatch);
        } else {
            inputJsonListFiles = TestUtils.getInputJsonListFiles(listFilenames);
        }

        final List<ProducerRecord<String, String>> producerRecordList = new ArrayList<>();
        for (int i = startingIndex; i < count + startingIndex; i++) {
            final ProducerRecord<String, String> producerRecord = createProducerRecord(inputJsonListFiles.get(i));
            producerRecordList.add(producerRecord);
        }
        sendToInputTopic(producerRecordList);
        return listFilenames;
    }

    private ProducerRecord<String, String> createProducerRecord(final String jsonInputTopicPayload){
        return new ProducerRecord<>(inputTopicName + enmID, "", jsonInputTopicPayload);
    }

    private void sendToInputTopic(final List<ProducerRecord<String, String>> producerRecordList){
        for (final ProducerRecord<String, String> producerRecord : producerRecordList) {
            producer.send(producerRecord);
        }
    }

    private Map<String, Map<Integer, Long>> getTopicDetails() throws Exception {
        final Set<String> topics = embeddedKafkaBroker.getTopics();
        final int numberPartitions = embeddedKafkaBroker.getPartitionsPerTopic();
        final Map<String, Map<Integer, Long>> topicDetails = new HashMap<>();
        for (final String myTopicName : topics) {
            final Map<Integer, Long> partitionDetails = new HashMap<>();
            for (int pNo = 0; pNo < numberPartitions; pNo++) {
                final OffsetAndMetadata kakfaOffset = KafkaTestUtils.getCurrentOffset(brokerAddresses, group, myTopicName, pNo);
                if (kakfaOffset != null) {
                    partitionDetails.put(pNo, kakfaOffset.offset());
                    LOGGER.debug("kafakUtils topic {}, partition {}, offset = {}, metaData = {} ", myTopicName, pNo,
                        kakfaOffset.offset(), kakfaOffset.toString());
                }
            }
            topicDetails.put(myTopicName, partitionDetails);
        }
        return topicDetails;
    }

    private void checkMetrics(final MetricsHolder metricsHolderAtStart, final MetricsHolder metricsHolderNow,
                              final int totalIn,
                              final int numExpectedSuccess,
                              final int numExpectedFails, final int numTransactionRollbacks, final boolean isBadTest){

        LOGGER.info("Metrics At Start of Test:\n{}", metricsHolderAtStart.toString());
        LOGGER.info("Metrics At End of Test:\n{}", metricsHolderNow.toString());

        final String failMessageExpectTotalIncrease = "FAIL: Expected 'SUCCESSFUL' total in metrics to have increased";
        final String failMessageExpectTotalNumFiles = "FAIL: Expected 'SUCCESSFUL' total in metrics to be ";
        final String failMessageExpectNoFails = "FAIL: Expected total failures in 'NUM FAILED' metrics to be ";
        final int actualNumberTransactionRollbacks = (int) (metricsHolderNow.getNumTransactionsRolledbackTotal()
            - metricsHolderAtStart.getNumTransactionsRolledbackTotal());

        final int totalReplayed = totalIn * actualNumberTransactionRollbacks;
        final int numInNow = (int) (metricsHolderNow.getNumInputKafkaMessagesReceivedTotal()
            - metricsHolderNow.getNumInputKafkaMessagesReplayedTotal());
        final int numInStart = (int) (metricsHolderAtStart.getNumInputKafkaMessagesReceivedTotal()
            - metricsHolderAtStart.getNumInputKafkaMessagesReplayedTotal());

        // Input # files tests
        testMetricEqual(failMessageExpectTotalNumFiles + totalIn, numInNow, numInStart, totalIn);
        testMetricEqual(failMessageExpectTotalNumFiles + totalReplayed, metricsHolderNow.getNumInputKafkaMessagesReplayedTotal(),
            metricsHolderAtStart.getNumInputKafkaMessagesReplayedTotal(), totalReplayed);
        testMetricEqual(failMessageExpectTotalNumFiles + totalIn + totalReplayed, metricsHolderNow.getNumInputKafkaMessagesReceivedTotal(),
            metricsHolderAtStart.getNumInputKafkaMessagesReceivedTotal(), totalIn + totalReplayed);

        // SFTP and BDR # files success tests
        if (!isBadTest) {
            testMetricEqual(failMessageExpectTotalNumFiles + totalIn, metricsHolderNow.getNumSuccessfulFileTransferTotal(),
                metricsHolderAtStart.getNumSuccessfulFileTransferTotal(), totalIn);
            testMetricEqual(failMessageExpectTotalNumFiles + totalIn, metricsHolderNow.getNumSuccessfulBdrUploadsTotal(),
                metricsHolderAtStart.getNumSuccessfulBdrUploadsTotal(), totalIn);
            LOGGER.debug("Verify number transaction rollbacks to be same: expected {}, actual {}", numTransactionRollbacks,
                actualNumberTransactionRollbacks);
            assertTrue("Expected number transaction rollbacks to be same", numTransactionRollbacks == actualNumberTransactionRollbacks);
        } else {
            testMetricGreaterThan(failMessageExpectTotalNumFiles + " greater than " + totalIn,
                metricsHolderNow.getNumSuccessfulFileTransferTotal(),
                metricsHolderAtStart.getNumSuccessfulFileTransferTotal());
            testMetricGreaterThan(failMessageExpectTotalNumFiles + " greater than " + totalIn,
                metricsHolderNow.getNumSuccessfulBdrUploadsTotal(),
                metricsHolderAtStart.getNumSuccessfulBdrUploadsTotal());
        }

        // Output # files tests
        testMetricEqual(failMessageExpectTotalNumFiles + numExpectedSuccess, metricsHolderNow.getNumOutputKafkaMessagesProducedSuccessfullyTotal(),
            metricsHolderAtStart.getNumOutputKafkaMessagesProducedSuccessfullyTotal(), numExpectedSuccess);
        testMetricEqual(failMessageExpectTotalNumFiles + numExpectedFails, metricsHolderNow.getNumOutputKafkaMessagesFailedTotal(),
            metricsHolderAtStart.getNumOutputKafkaMessagesFailedTotal(), numExpectedFails);

        // SFTP and BDR volume totals tests
        testMetricGreaterThan(failMessageExpectTotalIncrease, metricsHolderNow.getProcessedCounterFileDataVolumeTotal(),
            metricsHolderAtStart.getProcessedCounterFileDataVolumeTotal());
        testMetricGreaterThan(failMessageExpectTotalIncrease, metricsHolderNow.getProcessedCounterFileDataVolumeTotal(),
            metricsHolderAtStart.getProcessedCounterFileDataVolumeTotal());

        // SFTP and BDR # file failure tests
        testMetricEqual(failMessageExpectNoFails + "0", metricsHolderNow.getNumFailedFileTransferTotal(),
            metricsHolderAtStart.getNumFailedFileTransferTotal(), 0);
        testMetricEqual(failMessageExpectNoFails + "0", metricsHolderNow.getNumFailedBdrUploadsTotal(),
            metricsHolderAtStart.getNumFailedBdrUploadsTotal(), 0);
    }

    private void testMetricEqual(final String failMsg, final double now, final double start, final double expected){
        final double diff = now - start;
        assertEquals(failMsg, expected, diff);
    }

    private void testMetricGreaterThan(final String failMsg, final double now, final double start){
        assertTrue(failMsg, now > start);
    }

    private MetricsHolder getMetricsNow() {
        final double numInputKafkaMessagesReceivedTotal = metrics.getCounterValueByName(NUM_INPUT_KAFKA_MESSAGES_RECEIVED_TOTAL);
        final double numSuccessfulFileTransferTotal = metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL);
        final double processedCounterFileDataVolumeTotal = metrics.getGaugeValueByName(PROCESSED_COUNTER_FILE_DATA_VOLUME_TOTAL);
        final double numFailedFileTransferTotal = metrics.getCounterValueByName(NUM_FAILED_FILE_TRANSFER_TOTAL);
        final double numSuccessfulBdrUploadsTotal = metrics.getCounterValueByName(NUM_SUCCESSFUL_BDR_UPLOADS_TOTAL);
        final double processedBdrFileDataVolumeTotal = metrics.getGaugeValueByName(PROCESSED_BDR_DATA_VOLUME_TOTAL);
        final double numFailedBdrUploadsTotal = metrics.getCounterValueByName(NUM_FAILED_BDR_UPLOADS_TOTAL);
        final double numOutputKafkaMessagesProducedSuccessfullyTotal = metrics.getCounterValueByName(NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY);
        final double numOutputKafkaMessagesFailedTotal = metrics.getCounterValueByName(NUM_OUTPUT_KAFKA_MESSAGES_FAILED_TOTAL);
        final double numInputKafkaMessagesReplayedTotal = metrics.getCounterValueByName(NUM_INPUT_KAFKA_MESSAGES_REPLAYED_TOTAL);
        final double numTransactionsRolledbackTotal = metrics.getCounterValueByName(NUM_TRANSACTIONS_ROLLEDBACK_TOTAL);

        final MetricsHolder metricsHolder = new MetricsHolder(numInputKafkaMessagesReceivedTotal, numSuccessfulFileTransferTotal,
            processedCounterFileDataVolumeTotal, numFailedFileTransferTotal, numSuccessfulBdrUploadsTotal, processedBdrFileDataVolumeTotal,
            numFailedBdrUploadsTotal, numOutputKafkaMessagesProducedSuccessfullyTotal, numOutputKafkaMessagesFailedTotal,
            numInputKafkaMessagesReplayedTotal, numTransactionsRolledbackTotal);
        return metricsHolder;
    }
}
