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
package com.ericsson.oss.adc.sftp.filetrans.controller.batch;

import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL;
import static com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer;
import static junit.framework.TestCase.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;

import com.ericsson.oss.adc.sftp.filetrans.util.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.adc.sftp.filetrans.controller.BDRComponent;
import com.ericsson.oss.adc.sftp.filetrans.controller.InputTopicListener;
import com.ericsson.oss.adc.sftp.filetrans.service.ConnectedSystemsRetriever;
import com.ericsson.oss.adc.sftp.filetrans.service.SFTPFileTransferService;
import com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil;
import com.ericsson.oss.adc.sftp.filetrans.util.TestUtils;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * The Class SFTPHandlerBatchTest.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"subsystem.name=enm1","retryhandler.max_num_of_retries=50" })
@EmbeddedKafka(partitions = 1, topics = { "file-notification-service--sftp-filetrans--enm1" }, brokerProperties = {"transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1" })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("NoAsync")
public class SFTPHandlerBatchTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SFTPHandlerBatchTest.class);
    private static final int PORT = 1234;
    private static final String BASE_URL = "http://eric-eo-subsystem-management/subsystem-manager/v1/subsystems/";
    private static final String USER = "user";
    private static final String PASSWORD = "password";

    @Value("${spring.kafka.consumer.max-poll-records}")
    private int maxPollRecords;

    @Autowired
    private InputTopicListener inputTopicListener;

    @Autowired
    SFTPFileTransferService sftpFileTransferService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    SFTPProcessingMetricsUtil metrics;

    private MockRestServiceServer mockServer;

    @Autowired
    private ConnectedSystemsRetriever connectedSystemsRetriever;

    @MockBean
    BDRComponent bdrComponent;

    @Mock
    private JSch jsch;

    @Mock
    private Session session;

    @Mock
    private Channel channel;

    @Mock
    private SFTPFileTransferService service;

    /**
     * Initializes parameters for testing.
     */
    @BeforeEach
    public void init() {
        Utils.waitRetryInterval(1000);
        System.setProperty("BDR_ACCESS_KEY", "accessKey");
        System.setProperty("BDR_SECRET_KEY", "secretKey");
        System.setProperty("BDR_HOST_URL", "http://testurl.com");
        mockServer = MockRestServiceServer.createServer(restTemplate);
        connectedSystemsRetriever.addAccessEndpoints(new ArrayList<>(Collections.singletonList("http://eric-eo-subsystem-management/")));
    }

    /**
     * Given mocked SFTP and connected systems response verify file downloaded successfully.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @Order(1)
    @DisplayName("Verify after receiving File Notification message that a file is downloaded successful")
    public void givenMockedSFTPAndConnectedSystemsResponseVerifyFileDownloadedSuccessfully() throws Exception {
        withSftpServer(server -> {
            mockServer.expect(ExpectedCount.once(), requestTo(new URI(BASE_URL))).andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                    .body(new InputStreamResource(getClass().getClassLoader().getResourceAsStream("GetSFTPSubsystemsResponse.json"))));

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            final double successfulFileCountBefore = metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL);

            TestUtils.putFilenamesOnInputTopic(server, inputTopicListener, 1);

            final double successfulFileCountAfter = metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL);
            final double downloadFileCount = successfulFileCountAfter - successfulFileCountBefore;
            LOGGER.info("Single File Mode : downloadFileCount = {}", downloadFileCount);
            assertEquals("Expected one file to be downloaded", 1, (int) downloadFileCount);
        });
    }

    /**
     * Given mocked SFTP and connected systems response verify one batch of files downloaded successfully.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @Order(2)
    @DisplayName("Verify after receiving File Notification message that One batch of files is downloaded successful")
    public void givenMockedSFTPAndConnectedSystemsResponseVerifyOneBatchOfFilesDownloadedSuccessfully() throws Exception {
        testBatchMode(maxPollRecords);
    }

    /**
     * Given mocked SFTP and connected systems response verify multiple batches of files downloaded successfully.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @Order(3)
    @DisplayName("Verify after receiving File Notification message that multiple batches files are downloaded successful")
    public void givenMockedSFTPAndConnectedSystemsResponseVerifyMultipleBatchesOfFilesDownloadedSuccessfully() throws Exception {
        // -2 to have an uneven batch size.
        final int numberBatchesForTesting = 5;
        final int numFilesToDownloadForTesting = (maxPollRecords * numberBatchesForTesting) - 2;
        testBatchMode(numFilesToDownloadForTesting);
    }

    private void testBatchMode(final int numFilesToDownload) throws Exception {
        withSftpServer(server -> {
            mockServer.expect(ExpectedCount.once(), requestTo(new URI(BASE_URL))).andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                    .body(new InputStreamResource(getClass().getClassLoader().getResourceAsStream("GetSFTPSubsystemsResponse.json"))));

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            final double successfulFileCountBefore = metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL);
            TestUtils.putFilenamesOnInputTopic(server, inputTopicListener, numFilesToDownload);
            final double successfulFileCountAfter = metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL);
            final double downloadFileCount = successfulFileCountAfter - successfulFileCountBefore;

            LOGGER.info("Batch File Mode : downloadFileCount = {}", (int) downloadFileCount);
            assertEquals("Expected " + numFilesToDownload + " files to be downloaded", numFilesToDownload, (int) downloadFileCount);
        });
    }
}
