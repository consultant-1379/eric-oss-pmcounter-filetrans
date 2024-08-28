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
package com.ericsson.oss.adc.sftp.filetrans.util;

import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.*;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
class SFTPProcessingMetricsUtilTest {

    private static final String BLAH = "Blah";
    private static final String BLOB = "Blob";
    private static final String NOT_A_VALID_METRIC_NAME = "Not a valid metric name";
    private final SFTPProcessingMetricsUtil metrics = new SFTPProcessingMetricsUtil(new SimpleMeterRegistry());

    @Test
    @DisplayName("TEST: Verify incrementCounterByName fails to increment unknown counter name")
    void test_incrementCounterByNameFailsUnknownCounter() {
        assertFalse("Expected increment on unknown counter to fail", metrics.incrementCounterByName(BLAH));
    }

    @Test
    @DisplayName("TEST: Verify incrementCounterByName successfully increments known counter name")
    void test_incrementCounterByNamePassesknownCounter() {
        assertTrue("Expected increment on known counter to pass", metrics.incrementCounterByName(NUM_INPUT_KAFKA_MESSAGES_RECEIVED_TOTAL));
    }

    @Test
    @DisplayName("TEST: Verify addToGaugeByName fails to increment unknown guage name")
    void test_addToGaugeByNameFailsUnknownCounter() {
        assertFalse("Expected add on unknown guage to fail", metrics.addToGaugeByName(BLAH, 0L));
    }

    @Test
    @DisplayName("TEST: Verify addToGaugeByName successfully increments known guage name")
    void test_addToGaugeByNameyNamePassesknownCounter() {
        assertTrue("Expected add on known guage to pass", metrics.addToGaugeByName(PROCESSED_BDR_DATA_VOLUME_TOTAL, 0L));
    }

    @Test
    @DisplayName("TEST: Verify addCounterByName fails to add already known counter name")
    void test_addCounterByNameFailsknownCounter() {
        assertFalse("Expected add known counter to fail", metrics.addCounterByName(NUM_INPUT_KAFKA_MESSAGES_RECEIVED_TOTAL));
    }

    @Test
    @DisplayName("TEST: Verify addCounterByName successfully adds unknown counter name")
    void test_addCounterByNamePassesUnknownCounter() {
        assertTrue("Expected add unknown counter to pass", metrics.addCounterByName(BLOB));
    }

    @Test
    @DisplayName("TEST: Verify getCounterValueByName fails to get value of unknown counter name")
    void test_getCounterValueByNameFailsUnknownCounter() {
        assertEquals("Expected get value of unknown counter to reurn zero", 0.0, metrics.getCounterValueByName(BLAH));
    }

    @Test
    @DisplayName("TEST: Verify getCounterValueByName successfully get value of known counter name")
    void test_getCounterValueByNamePassesknownCounter() {
        metrics.incrementCounterByName(NUM_INPUT_KAFKA_MESSAGES_RECEIVED_TOTAL);
        final Double result = metrics.getCounterValueByName(NUM_INPUT_KAFKA_MESSAGES_RECEIVED_TOTAL);
        assertTrue("Expected get value of known counter to return non zero, actual value = " + result, result > 0.0);
    }

    @Test
    @DisplayName("TEST: Verify getGaugeValueByName fails to get value of unknown guage name")
    void test_getGaugeValueByNameFailsUnknownGuage() {
        assertEquals("Expected get value of unknown guage to reurn zero", 0.0, metrics.getGaugeValueByName(BLAH));
    }

    @Test
    @DisplayName("TEST: Verify getGaugeValueByName successfully get value of known guage name")
    void test_getGaugeValueByNamePassesknownGuage() {
        metrics.addToGaugeByName(PROCESSED_BDR_DATA_VOLUME_TOTAL, 1L);
        final Double result = metrics.getGaugeValueByName(PROCESSED_BDR_DATA_VOLUME_TOTAL);
        assertTrue("Expected get value of known guage to return non zero, actual value = " + result, result > 0.0);
    }

    @Test
    @DisplayName("TEST: Verify getTimerByName fails to get value of unknown timer name")
    void test_getTimerValueByNameFailsUnknownTimer() {
        assertEquals("Expected get value of unknown timer to return zero", 0, metrics.getTimerValueByName(NOT_A_VALID_METRIC_NAME));
    }

    @Test
    @DisplayName("TEST: Verify recordTimer fails to get value of unknown timer name")
    void test_recordTimerFailsUnknownTimer() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        Logger appLogger = (Logger) LoggerFactory.getLogger(SFTPProcessingMetricsUtil.class);

        appender.start();
        appLogger.addAppender(appender);

        metrics.recordTimer(BLAH,0L);
        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .containsExactly("Timer Blah doesn't exist");
    }

    @Test
    @DisplayName("TEST: Verify resetGaugeByName fails to reset unknown gauge name")
    void test_resetGaugeByNameFailsUnknownName() {
        Assertions.assertFalse(metrics.resetGaugeByName(BLAH), "Expected reset unknown gauge to fail");
    }

    @Test
    @DisplayName("TEST: Verify resetGaugeByName successfully gauge name")
    void test_resetGaugeByNamePassesKnownName() {
        Assertions.assertTrue(metrics.resetGaugeByName(BATCH_BDR_UPLOAD_TIME_TOTAL),"Expected reset known gauge to pass");
    }


    @Test
    @DisplayName("TEST: Verify resetGaugeByName resets the value to 0")
    void test_resetGaugeByNameResetsValue() {
        metrics.addToGaugeByName(BATCH_SFTP_DOWNLOAD_TIME_TOTAL,5);
        metrics.addToGaugeByName(BATCH_BDR_UPLOAD_TIME_TOTAL,5);
        Assertions.assertEquals(5, metrics.getGaugeValueByName(BATCH_SFTP_DOWNLOAD_TIME_TOTAL),"Expected SFTP batch gauge value to be 5");
        Assertions.assertEquals(5, metrics.getGaugeValueByName(BATCH_BDR_UPLOAD_TIME_TOTAL),"Expected BDR batch gauge value to be 5");

        metrics.resetGaugeByName(BATCH_SFTP_DOWNLOAD_TIME_TOTAL);
        metrics.resetGaugeByName(BATCH_BDR_UPLOAD_TIME_TOTAL);

        Assertions.assertEquals(0, metrics.getGaugeValueByName(BATCH_SFTP_DOWNLOAD_TIME_TOTAL), "Expected SFTP batch gauge value to be 0");
        Assertions.assertEquals(0, metrics.getGaugeValueByName(BATCH_BDR_UPLOAD_TIME_TOTAL), "Expected BDR batch gauge value to be 0");
    }

}
