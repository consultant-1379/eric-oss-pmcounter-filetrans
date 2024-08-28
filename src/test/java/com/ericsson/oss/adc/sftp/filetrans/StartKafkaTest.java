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

import static com.ericsson.oss.adc.sftp.filetrans.util.Constants.LISTENER_ID;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

/**
 * The Class StartKafkaTest.
 *
 * The purpose of this test is the test the LOGIC of the 'StartKafka' Class, not to test iof the listener actually starts.
 * Integration tests will fail if the listener fails to start.
 */
class StartKafkaTest {
    @MockBean
    private KafkaListenerEndpointRegistry registry;

    @Mock
    private final MessageListenerContainer messageListenerContainer = mock(MessageListenerContainer.class);

    /**
     * Test start kafta success.
     */
    @Test
    @DisplayName("TEST: Verify  startKafkaListener starts if 'messageListenerContainer'starts")
    void test_startKaftaSuccess() {
        when(messageListenerContainer.isAutoStartup()).thenReturn(false);
        when(messageListenerContainer.isRunning()).thenReturn(false).thenReturn(true);
        Mockito.doNothing().when(messageListenerContainer).start();
        final StartKafka startKafta = new StartKafka(registry, messageListenerContainer);
        final boolean isStarted = startKafta.startKafkaListener(messageListenerContainer, LISTENER_ID);
        assertTrue("Expected StartKafta to return true if 'messageListenerContainer' starts", isStarted);
    }

    /**
     * Test start kafta fails if listener not started.
     */
    @Test
    @DisplayName("TEST: Verify  startKafkaListener does not start if 'messageListenerContainer' does not start")
    void test_startKaftaFailsIfListenerNotStarted() {
        when(messageListenerContainer.isAutoStartup()).thenReturn(false);
        when(messageListenerContainer.isRunning()).thenReturn(false).thenReturn(false);
        Mockito.doNothing().when(messageListenerContainer).start();
        final StartKafka startKafta = new StartKafka(registry, messageListenerContainer);
        final boolean isStarted = startKafta.startKafkaListener(messageListenerContainer, LISTENER_ID);
        assertFalse("Expected StartKafta to return false if message listener not started", isStarted);
    }

    /**
     * Test start kafta fails if listener auto startup enabled.
     */
    @Test
    @DisplayName("TEST: Verify  startKafkaListener does not start if Auto Startup Enabled")
    void test_startKaftaFailsIfListenerAutoStartupEnabled() {
        when(messageListenerContainer.isAutoStartup()).thenReturn(true);
        when(messageListenerContainer.isRunning()).thenReturn(false).thenReturn(false);
        Mockito.doNothing().when(messageListenerContainer).start();
        final StartKafka startKafta = new StartKafka(registry, messageListenerContainer);
        final boolean isStarted = startKafta.startKafkaListener(messageListenerContainer, LISTENER_ID);
        assertFalse("Expected StartKafta to return false if Auto Startup Enabled", isStarted);
    }

    /**
     * Test start kafta passes if listener already running.
     */
    @Test
    @DisplayName("TEST: Verify  startKafkaListener starts if listener already running")
    void test_startKaftaPassesIfListenerAlreadyRunning() {
        when(messageListenerContainer.isAutoStartup()).thenReturn(false);
        when(messageListenerContainer.isRunning()).thenReturn(true).thenReturn(true);
        Mockito.doNothing().when(messageListenerContainer).start();
        final StartKafka startKafta = new StartKafka(registry, messageListenerContainer);
        final boolean isStarted = startKafta.startKafkaListener(messageListenerContainer, LISTENER_ID);
        assertTrue("Expected StartKafta to return true if listener already running", isStarted);
    }

    /**
     * Test start kafta fails if listener auto startup enabled and listener already running.
     */
    @Test
    @DisplayName("TEST: Verify  startKafkaListener does not start if Auto Startup Enabled & listener Running")
    void test_startKaftaFailsIfListenerAutoStartupEnabledAndListenerAlreadyRunning() {
        when(messageListenerContainer.isAutoStartup()).thenReturn(true);
        when(messageListenerContainer.isRunning()).thenReturn(true).thenReturn(true);
        Mockito.doNothing().when(messageListenerContainer).start();
        final StartKafka startKafta = new StartKafka(registry, messageListenerContainer);
        final boolean isStarted = startKafta.startKafkaListener(messageListenerContainer, LISTENER_ID);
        assertTrue("Expected StartKafta to return true if Auto Startup Enabled & listener Running", isStarted);
    }
}
