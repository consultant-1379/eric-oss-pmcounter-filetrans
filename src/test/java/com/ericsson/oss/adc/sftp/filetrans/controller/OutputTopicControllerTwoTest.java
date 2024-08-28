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
package com.ericsson.oss.adc.sftp.filetrans.controller;

import com.ericsson.oss.adc.sftp.filetrans.model.InputMessage;
import org.junit.Before;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.Assert.*;
/**
 * The Class OutputTopicControllerTest.
 */
@SpringBootTest
@EmbeddedKafka(partitions = 3, brokerProperties = { "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1" })
class OutputTopicControllerTwoTest {
    @Autowired
    OutputTopicController outputTopicController;
    @MockBean
    KafkaTemplate kafkaOutputTemplate;
    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
    }
    @Test
    @DisplayName("Expect OutputTopicController sendMessages to throw exception when rollbacks allowed")
    void test_sendMessagesExceptionThrownWithRollbacks() {
        ReflectionTestUtils.setField(outputTopicController, "transactionMaxRetryAttempts",2);
        ReflectionTestUtils.setField(outputTopicController, "partitions",2);
        Mockito.when(kafkaOutputTemplate.send(Mockito.any(Message.class))).thenThrow(ClassCastException.class);
        final InputMessage inputMessage = InputMessage.builder()
                .nodeName("TEST_NAME_1")
                .fileLocation("TEST_LOCATION")
                .nodeType("TEST_NODE")
                .dataType("TEST_DATA")
                .fileType("TEST_FILE")
                .build();
        outputTopicController.addToKafkaMessages(inputMessage);
        assertThrows(ClassCastException.class, () -> {
            outputTopicController.sendKafkaMessages();
        });
    }

    @Test
    @DisplayName("Expect OutputTopicController sendMessages to NOT throw exception when NO rollbacks allowed")
    void test_sendMessagesExceptionNotThrownWithNoRollbacks() {
        ReflectionTestUtils.setField(outputTopicController, "transactionMaxRetryAttempts",-1);
        Mockito.when(kafkaOutputTemplate.send(Mockito.any(Message.class))).thenThrow(ClassCastException.class);
        final InputMessage inputMessage = InputMessage.builder()
                .nodeName("TEST_NAME_1")
                .fileLocation("TEST_LOCATION")
                .nodeType("TEST_NODE")
                .dataType("TEST_DATA")
                .fileType("TEST_FILE")
                .build();
        outputTopicController.addToKafkaMessages(inputMessage);
        assertTrue(outputTopicController.sendKafkaMessages());
    }

}
