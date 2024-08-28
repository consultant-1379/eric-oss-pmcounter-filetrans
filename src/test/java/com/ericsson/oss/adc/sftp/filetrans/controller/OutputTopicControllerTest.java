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

import com.ericsson.oss.adc.sftp.filetrans.CoreApplication;
import com.ericsson.oss.adc.sftp.filetrans.configuration.properties.DataCatalogProperties;
import com.ericsson.oss.adc.sftp.filetrans.model.InputMessage;
import com.ericsson.oss.adc.sftp.filetrans.util.StartupUtil;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class OutputTopicControllerTest.
 */
@SpringBootTest
@EmbeddedKafka(partitions = 3, brokerProperties = { "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1" })
class OutputTopicControllerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutputTopicController.class);
    private static final String NODE_NAME = "nodeName";
    private static final String SUB_SYSTEM_TYPE = "subSystemType";
    private static final String NODE_TYPE = "nodeType";
    private static final String DATA_TYPE = "dataType";
    private static final String FILE_TYPE = "fileType";

    private static final String OUTPUT_TOPIC = "ran-pm-counter-sftp-file-transfer";

    @Autowired
    OutputTopicController outputTopicController;

    @Autowired
    DataCatalogProperties dataCatalogProperties;

    @Autowired
    KafkaAdmin kafkaAdmin;

    @Autowired
    private KafkaTemplate kafkaOutputTemplate;

    @MockBean
    private BDRComponent bdrComponent;

    @Autowired
    @InjectMocks
    private StartupUtil startupUtil;

    /**
     * Test build and create topics.
     */
    @Test
    @DisplayName("Should successfully build and create the topics on the embedded kafka server")
    @SneakyThrows
    void test_buildAndCreateTopics() {

        final int expectedPartitions = 3;
        final int expectedReplicas = 1;

        // USE ONLY THIS SETTER INSIDE TEST CASES
        outputTopicController.setOutputTopicName(OUTPUT_TOPIC);
        // Adding a sleep to allow EmbeddedKafka to fully start up. Was seeing issues with leader_not_available.
        TimeUnit.SECONDS.sleep(10);
        assertTrue(outputTopicController.buildAndCreateTopic());
        final Map<String, TopicDescription> topics = kafkaAdmin.describeTopics(OUTPUT_TOPIC);

        assertNotNull(topics);
        assertEquals(expectedPartitions, topics.get(OUTPUT_TOPIC).partitions().size());
        assertEquals(expectedReplicas, topics.get(OUTPUT_TOPIC).partitions().get(0).replicas().size());
        assertEquals(OUTPUT_TOPIC, topics.get(OUTPUT_TOPIC).name());
    }

    /**
     * Test build and create topics duplicate topic.
     */
    @Test
    @DisplayName("Should only add a topic once and not decrease the partitions")
    void test_buildAndCreateTopics_duplicateTopic() {
        final String testTopic = "outputTopicENM2";
        final int expectedPartitions = 3;
        final int expectedReplicas = 1;

        final int duplicatePartitions = 2;
        final short duplicateReplicas = 1;
        final NewTopic duplicateTopic = new NewTopic(testTopic, duplicatePartitions, duplicateReplicas);

        // USE ONLY THIS SETTER INSIDE TEST CASES
        outputTopicController.setOutputTopicName(testTopic);
        outputTopicController.buildAndCreateTopic();
        kafkaAdmin.createOrModifyTopics(duplicateTopic);
        final Map<String, TopicDescription> topics = kafkaAdmin.describeTopics(testTopic);

        assertNotNull(topics);
        assertEquals(expectedPartitions, topics.get(testTopic).partitions().size());
        assertEquals(expectedReplicas, topics.get(testTopic).partitions().get(0).replicas().size());
        assertEquals(testTopic, topics.get(testTopic).name());
    }

    /**
     * Test successful send is successful.
     */
    @Test
    @DisplayName("Expected outputTopicController to successfully send the message.")
    void test_successfulSend_is_successful() {
        final boolean result = sendMessage(false);
        Assertions.assertTrue(result, "Expected outputTopicController to successfully sent the message.");
    }

    /**
     * Test successful send is not successful.
     */
    @Test
    @DisplayName("Expected outputTopicController to FAIL to send the message.")
    void test_successfulSend_is_not_successful() {
        final boolean result = sendMessage(true);
        Assertions.assertFalse(result, "Expected outputTopicController to Fail to send the message.");
    }

    /**
     *
     * Implemented this way as 'java.lang.AssertionError: Expected KafkaException to be thrown, but KafkaException was thrown'
     * was received when using assertThrows
     */
    @Test
    @DisplayName("Expected outputTopicController to FAIL to create an invalid topic.")
    void test_create_bad_topic_is_not_successful() {
        try {
            final NewTopic outputTopic = TopicBuilder.name("muck").partitions(-1).replicas(0).build();
            final boolean result = outputTopicController.createTopic(outputTopic);
            Assertions.assertFalse(result, "Expected outputTopicController to Fail to create an invalid topic.");
        } catch (final Exception e) {
            //Expected KafkaException to be thrown
        }
    }

    /**
     * Test get future not successful for execution exception.
     */
    @Test
    @DisplayName("Expected outputTopicController getFuture to FAIL for invalid Future (ExecutionException).")
    void test_getFutureNotSuccessfulForExecutionException() {
        final Set<String> noTopics = Collections.emptySet();
        final KafkaFuture<Set<String>> kf = KafkaFuture.completedFuture(noTopics).whenComplete(null);
        final Set<String> existingTopics = (Set<String>) outputTopicController.getFuture(kf);
        LOGGER.info("Get Future not Successful for Execution Exception,  existingTopics = {}", existingTopics);
        Assertions.assertTrue(existingTopics == null, "Expected existingTopics to be null as expected ExecutionException");
    }

    /**
     * Test get future not successful for interrupted exception.
     *
     * @throws InterruptedException
     *             the interrupted exception
     * @throws ExecutionException
     *             the execution exception
     */
    @Test
    @DisplayName("Expected outputTopicController getFuture to FAIL for invalid Future (InterruptedException)")
    void test_getFutureNotSuccessfulForInterruptedException() throws InterruptedException, ExecutionException {
        final KafkaFuture kafkaFuture = mock(KafkaFuture.class);
        doThrow(new InterruptedException("Error occurred")).when(kafkaFuture).get();
        final Set<String> existingTopics = (Set<String>) outputTopicController.getFuture(kafkaFuture);
        LOGGER.info("Get Future not Successful for InterruptedException,  existingTopics = {}", existingTopics);
    }

    @Test
    @DisplayName("Payload should contain the created Bucket Name from StartupUtil, and the File Location from Input Topic,")
    void test_messageForKafkaOutputHasValidPayload(){
        final InputMessage inputMessage = new InputMessage();
        inputMessage.setNodeName("testNodeName");
        inputMessage.setFileLocation("/testLocation");
        inputMessage.setNodeType("testNodeType");
        inputMessage.setDataType("testDataType");
        inputMessage.setFileType("testFileType");

        when(bdrComponent.createBucket("ran-pm-counter-sftp-file-transfer")).thenReturn(true);
        startupUtil.createBdrBucket(new ArrayList<>(Collections.singletonList("http://test/")));

        outputTopicController.addToKafkaMessages(inputMessage);
        final List<Message<String>> messages = (List<Message<String>>) ReflectionTestUtils.getField(outputTopicController, "messagesToSendToOutput");

        Assertions.assertEquals("{\"fileLocation\":\"ran-pm-counter-sftp-file-transfer/testLocation\"}", messages.get(0).getPayload());
    }


    @Transactional
    boolean sendMessage(final boolean shouldCancelTransaction) {
        final Message<String> message = getMessage();
        final Object res = kafkaOutputTemplate.executeInTransaction(t -> {
            final CompletableFuture<SendResult<?, ?>> lf =  kafkaOutputTemplate.send(message);
            if (shouldCancelTransaction) {
                lf.cancel(true);
            }
            final boolean result = outputTopicController.successfulSend(lf, message);
            return result;
        });
        return (boolean) res;
    }

    private Message<String> getMessage() {
        return MessageBuilder.withPayload("{test=test}")
                .setHeader(KafkaHeaders.KEY, NODE_NAME)
                .setHeader(KafkaHeaders.TOPIC, "outputTopicName")
                .setHeader(KafkaHeaders.PARTITION, 0)
                .setHeader(NODE_NAME, NODE_NAME)
                .setHeader(SUB_SYSTEM_TYPE, SUB_SYSTEM_TYPE)
                .setHeader(NODE_TYPE, NODE_TYPE)
                .setHeader(DATA_TYPE, DATA_TYPE)
                .setHeader(FILE_TYPE, FILE_TYPE)
                .build();
    }
}
