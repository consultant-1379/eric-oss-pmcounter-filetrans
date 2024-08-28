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

import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_OUTPUT_KAFKA_MESSAGES_FAILED_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.PROCESSED_COUNTER_FILES_TIME_TOTAL;
import static com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.*;
import com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer;

import java.util.concurrent.TimeUnit;

/**
 * The Class SFTPFileTransferIntegrationTest.
 * End to End Test Flow:
 * Test producer -> INPUT Topic -> Consumer -> Input Listener -> download from enm (FakeSftpServer)- upload to BDR (Mock)
 * -> Producer -> OUTPUT Topic -> Test Consumer
 */

public class IntegrationWithUnlimitedRollbackTest extends SFTPFileTransferIntegration {


    final int UNLIMITED_TRANSACTIONS_ROLLBACKS = Integer.MAX_VALUE;

    /**
     * Init for test.
     *
     * @throws Exception the exception
     */
    @Override
    @BeforeEach
    public void init() throws Exception {
        super.init();
    }

    /**
     * Given mocked SFTP verify server connection and one batch files downloaded successfully.
     *
     * @throws Exception the exception
     */
    @Test
    @Order(1)
    @DisplayName("Verify One batch of files is processed when a notification is received on the input topic")
    public void givenMockedSFTPVerifyServerConnectionAndOneBatchFilesDownloadedSuccessfully() throws Exception {
        final long expectedOffset = 5;

        withSftpServer(server -> {
            assertEquals(0, metrics.getTimerValueByName(PROCESSED_COUNTER_FILES_TIME_TOTAL));
            setupForTesting(server);
            final int numBatches = 1;
            endToEndRollbacksTest(server, numBatches, expectedOffset, UNLIMITED_TRANSACTIONS_ROLLBACKS);
        });
        assertEquals(expectedOffset, metrics.getTimerValueByName(PROCESSED_COUNTER_FILES_TIME_TOTAL));
    }

    /**
     * Given mocked SFTP verify server connection and multiple full batch files downloaded successfully.
     *
     * @throws Exception the exception
     */
    @Test
    @Order(2)
    @DisplayName("Verify multiple FULL batches of files are processed when a notification is received on the input topic")
    public void givenMockedSFTPVerifyServerConnectionAndMultipleFullBatchFilesDownloadedSuccessfully() throws Exception {
        final long expectedOffset = 30;

        withSftpServer(server -> {
            setupForTesting(server);
            final int numBatches = 5;
            kafkaOutputConsumerTestIntegration.reset();
            endToEndRollbacksTest(server, numBatches, expectedOffset, UNLIMITED_TRANSACTIONS_ROLLBACKS);
        });
        assertEquals(expectedOffset, metrics.getTimerValueByName(PROCESSED_COUNTER_FILES_TIME_TOTAL));
    }

    /**
     * Given mocked SFTP verify server connection and multiple not full batch files downloaded successfully.
     *
     * @throws Exception the exception
     */
    @Test
    @Order(3)
    @DisplayName("Verify multiple batches of files (last one 1/2 full) are processed when a notification is received on the input topic")
    public void givenMockedSFTPVerifyServerConnectionAndMultipleNotFullBatchFilesDownloadedSuccessfully() throws Exception {
        final long expectedOffset = 38;

        withSftpServer(server -> {
            setupForTesting(server);
            final int numBatches = 2;
            kafkaOutputConsumerTestIntegration.reset();
            final int halfBatchNo = 2;
            final int expectedNumTransactionRollBacks = 0;
            endToEndRollbacksTest(server, numBatches, expectedOffset, halfBatchNo, expectedNumTransactionRollBacks, UNLIMITED_TRANSACTIONS_ROLLBACKS);
        });
        assertEquals(expectedOffset, metrics.getTimerValueByName(PROCESSED_COUNTER_FILES_TIME_TOTAL));
    }

    /**
     * Given mocked SFTP verify server connection and one full batch files with one bad file downloaded successfully.
     *
     * @throws Exception the exception
     */
    @Test
    @Order(4)
    @DisplayName("Verify One FULL batch of files (with one BAD file) is processed when a notification is received on the input topic")
    public void givenMockedSFTPVerifyServerConnectionAndOneFullBatchFilesWithOneBadFileDownloadedSuccessfully() throws Exception {
        kafkaOutputConsumerTestIntegration.reset();
        final long expectedOffset = 43;

        withSftpServer(server -> {
            setupForTesting(server);
            final int numBatches = 1;
            endToEndRollbacksTest(server, numBatches, expectedOffset, UNLIMITED_TRANSACTIONS_ROLLBACKS);
        });
        assertEquals(expectedOffset, metrics.getTimerValueByName(PROCESSED_COUNTER_FILES_TIME_TOTAL));
        withSftpServer(server -> {
            setupForTesting(server);
            final int numBatches = 1;
            kafkaOutputConsumerTestIntegration.reset();

            getOutputTopicControllerToFailWriteToKafka();

            endToEndWithFailureLimitedRollbackTest(server, numBatches, 0, 4, UNLIMITED_TRANSACTIONS_ROLLBACKS);
            updateNumberFailedMetrics(server, numBatches);
        });
        // Makes sure counter files are processed
        assertThat(metrics.getTimer(PROCESSED_COUNTER_FILES_TIME_TOTAL).totalTime(TimeUnit.MILLISECONDS)).isBetween(1.0, 15000.0);
    }

    private void updateNumberFailedMetrics(final FakeSftpServer server, final int numBatches) {
        // update metrics to account for the 5 files that never made it to the output; Invalid metrics affects other tests
        final int numFilesTotal = (maxPollRecords * numBatches);
        for (int i = 0; i < numFilesTotal; i++) {
            metrics.incrementCounterByName(NUM_OUTPUT_KAFKA_MESSAGES_FAILED_TOTAL);
        }
    }
}