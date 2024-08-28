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

package com.ericsson.oss.adc.sftp.filetrans.integration;

import com.ericsson.oss.adc.sftp.filetrans.util.Utils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;

import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.*;
import static com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer;
import static org.junit.Assert.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.max-poll-records=1",
        "spring.kafka.transaction.max-retry-attempts=-1",
})
public class IntegrationWithNoRollBackTest extends SFTPFileTransferIntegration {

    final int NUM_BATCHES = 1;

    /**
     * Inits for test.
     *
     * @throws Exception the exception
     */
    @Override
    @BeforeEach
    public void init() throws Exception {
        super.init();
        Utils.waitRetryInterval(1000);
    }

    @Test
    @DisplayName("Verify rollback not initiated with bad file when transaction rollbacks disabled")
    @Order(1)
    public void givenMockedSFTPVerifyRollBackNotInitiatedWithBadFile() throws Exception {
        withSftpServer(server -> {
            setupForTesting(server);

            kafkaOutputConsumerTestIntegration.reset();

            endToEndWithFailureLimitedRollbackTest(server, NUM_BATCHES, 0, 0, 0);
            endToEndWithFailureLimitedRollbackTest(server, NUM_BATCHES, 0, 0, 0);
        });
        assertEquals(0, (int) metrics.getCounterValueByName(NUM_TRANSACTIONS_ROLLEDBACK_TOTAL));
        assertEquals(2, (int) metrics.getCounterValueByName(NUM_OUTPUT_KAFKA_MESSAGES_FAILED_TOTAL));
    }

    @Test
    @DisplayName("Verify one file is processed when a notification is received on the input topic when transaction rollbacks disabled")
    @Order(2)
    public void givenMockedSFTPVerifyFileProcessedWithOneFilePerBatch() throws Exception {
        final long valueOfMetricBeforeTestRun = metrics.getTimerValueByName(PROCESSED_COUNTER_FILES_TIME_TOTAL);
        final long expectedOffset = valueOfMetricBeforeTestRun + 1;

        withSftpServer(server -> {
            setupForTesting(server);
            endToEndRollbacksTest(server, NUM_BATCHES, expectedOffset, 0);
        });
        assertEquals(expectedOffset, metrics.getTimerValueByName(PROCESSED_COUNTER_FILES_TIME_TOTAL));
    }
}