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

import static com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer;

import org.junit.jupiter.api.*;

/**
 * The Class SFTPFileTransferIntegrationTest.
 * End to End Test Flow:
 * Test producer -> INPUT Topic -> Consumer -> Input Listener -> download from enm (FakeSftpServer)- upload to BDR (Mock)
 * -> Producer -> OUTPUT Topic -> Test Consumer
 */
public class IntegrationWithLimitedRollbacksTest extends SFTPFileTransferIntegration {
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
    }


    /**
     * Verify kafka transactions will rollback a limited number of times,
     * then allow normal processing of subsequent transactions.
     *
     * One Batch of 5/5 is received and processed
     * Cumulative Metrics ... Received: 5.0, Replayed: 0.0, Sent: 5.0
     *
     * Two batches of 5/5 trigger a rollback, when processed the third time 4/5/ files are processed and sent to the output topic
     * Cumulative Metrics ... Received: 20.0, Replayed: 10.0, Sent: 9.0
     *
     * Final Batch of 5/5/ is received and processed
     * Cumulative Metrics ... Received: 25.0, Replayed: 10.0, Sent: 14.0
     *
     * @throws Exception the exception
     */
    @Test
    @DisplayName("Verify a failed Kafka Transaction with Limited Rollback")
    public void kafkaTransactionsWithLimitedRollbacks() throws Exception {
        kafkaOutputConsumerTestIntegration.reset();
        withSftpServer(server -> {
            setupForTesting(server);
            final long expectedOffset = 5;
            final int unlimitedTransactionsRollbacks = Integer.MAX_VALUE;

            endToEndRollbacksTest(server, NUM_BATCHES, expectedOffset, unlimitedTransactionsRollbacks);
        });
        withSftpServer(server -> {
            setupForTesting(server);
            final int numTransactionRollBacks = 2;
            kafkaOutputConsumerTestIntegration.reset();

            mockAddToKafkaMessagesResponseToTriggerRollbacks();

            endToEndWithFailureLimitedRollbackTest(server, NUM_BATCHES, 0, 4, numTransactionRollBacks);
        });
        withSftpServer(server -> {
            setupForTesting(server);
            final long expectedOffset = 15;
            kafkaOutputConsumerTestIntegration.reset();
            // expect two rollbacks from above.
            final int expectedNumTransactionRollBacks = 2;

            setOutputTopicControllerMocksBackToCallingRealMethods();

            endToEndRollbacksTest(server, NUM_BATCHES, expectedOffset, 0, expectedNumTransactionRollBacks, Integer.MAX_VALUE);
        });
    }
}