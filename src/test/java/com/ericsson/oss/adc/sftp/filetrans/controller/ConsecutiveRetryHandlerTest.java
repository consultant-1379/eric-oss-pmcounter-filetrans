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

import com.ericsson.oss.adc.sftp.filetrans.availability.DependentServiceAvailabilityUtil;
import com.ericsson.oss.adc.sftp.filetrans.controller.health.HealthCheck;
import com.ericsson.oss.adc.sftp.filetrans.model.InputMessage;
import com.ericsson.oss.adc.sftp.filetrans.util.ConsecutiveRetryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.test.context.EmbeddedKafka;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest( properties = { "retryhandler.max_num_of_retries=3"})
@EmbeddedKafka
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConsecutiveRetryHandlerTest {


    @Autowired
    HealthCheck healthCheck;

    @SpyBean
    DependentServiceAvailabilityUtil dependentServiceAvailabilityUtil;

    @SpyBean
    ConsecutiveRetryHandler consecutiveRetryHandlerSpy;

    @Mock
    SFTPHandler sftpHandlerMock;

    @Mock
    OutputTopicController outputTopicControllerMock;

    @InjectMocks
    InputTopicListener inputTopicListener;


    final InputMessage inputMessage = InputMessage.builder()
            .nodeName("TEST_NAME")
            .fileLocation("TEST_LOCATION")
            .nodeType("TEST_NODE")
            .dataType("TEST_DATA")
            .fileType("TEST_FILE")
            .build();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);;
    }

    @Test
    @Order(1)
    @DisplayName("When max num of retries reached - health status down")
    public void test_max_num_of_retries_reached_health_status_down() {
        Mockito.doNothing().when(consecutiveRetryHandlerSpy).initializeBackOff();
        consecutiveRetryHandlerSpy.incrementNumOfFails();
        consecutiveRetryHandlerSpy.incrementNumOfFails();
        consecutiveRetryHandlerSpy.incrementNumOfFails();
        assertEquals(Status.DOWN, healthCheck.health().getStatus());
        consecutiveRetryHandlerSpy.reset();
    }

    @Test
    @Order(2)
    @DisplayName("When max num of retries reached and back off completed - health status up")
    public void test_max_num_of_retries_reached_and_back_off_completed_health_status_up() {
        Mockito.doReturn(true).when(dependentServiceAvailabilityUtil).areAllDependentServicesAvailable();
        consecutiveRetryHandlerSpy.incrementNumOfFails();
        consecutiveRetryHandlerSpy.incrementNumOfFails();
        consecutiveRetryHandlerSpy.incrementNumOfFails();
        assertEquals(Status.UP, healthCheck.health().getStatus());
        assertEquals(1, consecutiveRetryHandlerSpy.getNumOfBackOffs());

    }
    @Test
    @Order(3)
    @DisplayName("When Sftp process fails 3 times back off is initialized")
    public void test_sftp_process_fails_3_times_back_off_initialized() {
        Mockito.when(sftpHandlerMock.process(inputMessage)).thenReturn(false).thenReturn(false).thenReturn(false);
        Mockito.doReturn(true).when(dependentServiceAvailabilityUtil).areAllDependentServicesAvailable();
        for (int i = 0; i < 3; i++){
            inputTopicListener.processNotification(inputMessage);
        }
        // Number of back offs is cumulative
        assertEquals(2, consecutiveRetryHandlerSpy.getNumOfBackOffs());
    }

    @Test
    @Order(4)
    @DisplayName("When kafka upload fails 3 times back off is initialized")
    public void test_kafka_upload_fails_3_times_back_off_initialized() {
        Mockito.when(sftpHandlerMock.process(inputMessage)).thenReturn(true).thenReturn(true).thenReturn(true);
        Mockito.doThrow(new KafkaException("Test")).when(outputTopicControllerMock).addToKafkaMessages(inputMessage);
        Mockito.doReturn(true).when(dependentServiceAvailabilityUtil).areAllDependentServicesAvailable();
        for (int i = 0; i < 3; i++){
            inputTopicListener.processNotification(inputMessage);
        }
        assertEquals(3, consecutiveRetryHandlerSpy.getNumOfBackOffs());
    }

    @Test
    @Order(5)
    @DisplayName("When 3 consecutive fails of sftp process or kafka upload check back off is initialized")
    public void test_consecutive_fails_3_times_back_off_initialized() {
        // fails sftp process twice and kafka upload once
        Mockito.when(sftpHandlerMock.process(inputMessage)).thenReturn(false).thenReturn(false).thenReturn(true);
        Mockito.doThrow(new KafkaException("Test")).when(outputTopicControllerMock).addToKafkaMessages(inputMessage);
        Mockito.doReturn(true).when(dependentServiceAvailabilityUtil).areAllDependentServicesAvailable();
        for (int i = 0; i < 3; i++){
            inputTopicListener.processNotification(inputMessage);
        }
        assertEquals(4, consecutiveRetryHandlerSpy.getNumOfBackOffs());
    }

    @Test
    @Order(6)
    @DisplayName("Check retry handler reset after one successful end to end")
    public void test_retry_handler_reset_after_successful_e2e() {
        Mockito.when(sftpHandlerMock.process(inputMessage)).thenReturn(false).thenReturn(true);
        Mockito.doNothing().when(outputTopicControllerMock).addToKafkaMessages(inputMessage);

        inputTopicListener.processNotification(inputMessage);
        assertEquals(1, consecutiveRetryHandlerSpy.getNumOfRetries());
        inputTopicListener.processNotification(inputMessage);
        assertEquals(0, consecutiveRetryHandlerSpy.getNumOfRetries());
    }
}
