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

import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_FAILED_FILE_TRANSFER_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL;
import static com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.net.URI;
import java.util.*;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.adc.sftp.filetrans.model.ConnectionPropertiesModel;
import com.ericsson.oss.adc.sftp.filetrans.model.InputMessage;
import com.ericsson.oss.adc.sftp.filetrans.model.SubsystemModel;
import com.ericsson.oss.adc.sftp.filetrans.service.ConnectedSystemsRetriever;
import com.ericsson.oss.adc.sftp.filetrans.service.ENMScriptingVMLoadBalancer;
import com.ericsson.oss.adc.sftp.filetrans.service.SFTPFileTransferService;
import com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil;
import com.ericsson.oss.adc.sftp.filetrans.util.TestUtils;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * The Class SFTPHandlerTest.
 */
@SpringBootTest(properties = {"subsystem.name=enm1", "retryhandler.max_num_of_retries=50"})
@EmbeddedKafka(partitions = 1, topics = {"file-notification-service--sftp-filetrans--enm1"}, brokerProperties = {"transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SFTPHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SFTPHandlerTest.class);
    private static final int PORT = 1234;
    private static final String BASE_URL = "http://eric-eo-subsystem-management/subsystem-manager/v1/subsystems/";
    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String NODE_NAME = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR102gNodeBRadio00010,ManagedElement=NR102gNodeBRadio00010";
    private static final String ENM_FILE_PATH = "/ericsson/pmic1/XML/SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR102gNodeBRadio00010,ManagedElement=NR102gNodeBRadio00010/";
    private static final String FILE_NAME = "A20200721.1000+0100-1015+0100_SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR102gNodeBRadio00010,ManagedElement=NR102gNodeBRadio00010_statsfile.1.xml.gz";
    private static final String FILE_NAME_2 = "A20200721.1000+0100-1015+0100_SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR102gNodeBRadio00010,ManagedElement=NR102gNodeBRadio00010_statsfile.2.xml.gz";
    private static final String NODE_TYPE = "RadioNode";
    private static final String DATA_TYPE = "4G";
    private static final String FILE_TYPE = "XML";

    @Autowired
    private InputTopicListener inputTopicListener;

    @MockBean
    private BDRComponent bdrComponent;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @Autowired
    private SFTPHandler sftpHandler;

    @Autowired
    private SFTPFileTransferService sftpFileTransferService;

    @Autowired
    private ConnectedSystemsRetriever connectedSystemsRetriever;

    @Autowired
    private SFTPProcessingMetricsUtil metrics;

    @Mock
    private JSch jsch;

    @Mock
    private Session session;

    @Mock
    private Channel channel;

    @Autowired
    private ENMScriptingVMLoadBalancer loadBalancer;

    /**
     * Inits the.
     */


    @BeforeEach
    public void newMockServer () {
        System.setProperty("BDR_ACCESS_KEY", "accessKey");
        System.setProperty("BDR_SECRET_KEY", "secretKey");
        System.setProperty("BDR_HOST_URL", "http://testurl.com");
        connectedSystemsRetriever.addAccessEndpoints(new ArrayList<>(Collections.singletonList("http://eric-eo-subsystem-management/")));
        mockServer =  MockRestServiceServer.createServer(restTemplate);
    }

    /**
     * Given mocked SFTP verify server connection and multiple files downloaded successfully.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @Order(1)
    @DisplayName("Verify multiple files is downloaded when successful connections is made")
    public void givenMockedSFTPVerifyServerConnectionAndMultipleFilesDownloadedSuccessfully() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            server.putFile(ENM_FILE_PATH + FILE_NAME, "Content of first file", UTF_8);
            server.putFile(ENM_FILE_PATH + FILE_NAME_2, "Content of second file", UTF_8);

            loadBalancer.setScriptingVMs(Arrays.asList("localhost"));
            assertTrue(sftpFileTransferService.connectToSftpHost(Optional.of(USER), Optional.of(PORT), Optional.of(PASSWORD)));
            assertTrue(sftpFileTransferService.isConnectionOpen());

            final InputMessage firstInputMessage = InputMessage.builder()
                    .nodeName(NODE_NAME)
                    .fileLocation(ENM_FILE_PATH + FILE_NAME)
                    .nodeType(NODE_TYPE)
                    .dataType(DATA_TYPE)
                    .fileType(FILE_TYPE)
                    .build();
            final InputMessage secondInputMessage = InputMessage.builder()
                    .nodeName(NODE_NAME)
                    .fileLocation(ENM_FILE_PATH + FILE_NAME_2)
                    .nodeType(NODE_TYPE)
                    .dataType(DATA_TYPE)
                    .fileType(FILE_TYPE)
                    .build();
            final double transferCountBefore = metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL);
            sftpHandler.downloadFileAndPersistToBDR(firstInputMessage);
            sftpHandler.downloadFileAndPersistToBDR(secondInputMessage);
            assertEquals(transferCountBefore + 2, metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL));

            //Additional SQ tests
            ConnectionPropertiesModel connectionProperties = sftpHandler.getConnectionProperties(null);
            assertTrue(connectionProperties.getUsername() == null);
            connectionProperties = sftpHandler.getConnectionProperties(new HashMap<>());
            assertTrue(connectionProperties.getUsername() == null);
        });
    }

    /**
     * Given mocked SFTP and connected systems response verify file downloaded successfully.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @Order(2)
    @DisplayName("Verify after receiving File Notification message that a file is downloaded successful")
    public void givenMockedSFTPAndConnectedSystemsResponseVerifyFileDownloadedSuccessfully() throws Exception {
        withSftpServer(server -> {
            mockServer.expect(ExpectedCount.once(),
                    requestTo(new URI(BASE_URL)))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new InputStreamResource(getClass().getClassLoader().getResourceAsStream("GetSFTPSubsystemsResponse.json"))));

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            server.putFile(ENM_FILE_PATH + FILE_NAME, "Content of file", UTF_8);

            final double successfulFileCountBefore = metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL);
            TestUtils.putFilenamesOnInputTopic(server, inputTopicListener, 1);
            assertEquals( (successfulFileCountBefore + 1),  metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL));
            final byte[] result = sftpFileTransferService.downloadFile(ENM_FILE_PATH + FILE_NAME);
            assertTrue(result != null && result.length > 0);
        });
    }

    /**
     * Given mocked SFTP and connected systems empty response file downloaded failure.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @DisplayName("Verify after receiving File Notification message and empty connected systems response that download method is never called")
    public void givenMockedSFTPAndConnectedSystemsEmptyResponseFileDownloadedFailure() throws Exception {
        withSftpServer(server -> {
            mockServer.expect(ExpectedCount.once(),
                    requestTo(new URI(BASE_URL)))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new InputStreamResource(getClass().getClassLoader().getResourceAsStream("EmptySubsystemResponse.json"))));

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            server.putFile(ENM_FILE_PATH + FILE_NAME, "Content of file", UTF_8);

            final double successfulFileCountBefore = metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL);
            final double failedFileCountBefore = metrics.getCounterValueByName(NUM_FAILED_FILE_TRANSFER_TOTAL);

            TestUtils.putFilenamesOnInputTopic(server, inputTopicListener, 1);

            assertEquals(successfulFileCountBefore,  metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL));
            assertEquals(failedFileCountBefore,  metrics.getCounterValueByName(NUM_FAILED_FILE_TRANSFER_TOTAL));
        });
    }

    /**
     * Given mocked SFTP and connected systems response failed SFTP server authentication invalid user.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @DisplayName("Verify after receiving File Notification and connected systems response contains incorrect credentials, download method is never called")
    public void givenMockedSFTPAndConnectedSystemsResponseFailedSFTPServerAuthenticationInvalidUser() throws Exception {
        withSftpServer(server -> {
            mockServer.expect(ExpectedCount.once(),
                    requestTo(new URI(BASE_URL)))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new InputStreamResource(getClass().getClassLoader().getResourceAsStream("GetSFTPSubsystemsResponse.json"))));

            LOGGER.info("Setting up Mock SFTP Server with different user than connected systems :{} ", BASE_URL);
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER + 1, PASSWORD);
            server.putFile(ENM_FILE_PATH + FILE_NAME, "Content of file", UTF_8);

            final double successfulFileCountBefore = metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL);
            final double failedFileCountBefore = metrics.getCounterValueByName(NUM_FAILED_FILE_TRANSFER_TOTAL);
            TestUtils.putFilenamesOnInputTopic(server, inputTopicListener, 1);

            assertEquals(successfulFileCountBefore,  metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL));
            assertEquals(failedFileCountBefore,  metrics.getCounterValueByName(NUM_FAILED_FILE_TRANSFER_TOTAL));
            assertFalse(sftpFileTransferService.isConnectionOpen());

            LOGGER.info("Setting up Mock SFTP Server with different password than connected systems :{} ", BASE_URL);
            server.addUser(USER, PASSWORD + 1);
            assertEquals(successfulFileCountBefore,  metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL));
            assertEquals(failedFileCountBefore,  metrics.getCounterValueByName(NUM_FAILED_FILE_TRANSFER_TOTAL));
            assertFalse(sftpFileTransferService.isConnectionOpen());
        });
    }

    /**
     * Given mocked SFTP and connected systems bad response failed SFTP connection.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @DisplayName("Verify after receiving File Notification and connected systems bad response, download method is never called")
    public void givenMockedSFTPAndConnectedSystemsBadResponseFailedSFTPConnection() throws Exception {
        withSftpServer(server -> {
            mockServer.expect(ExpectedCount.once(),
                    requestTo(new URI(BASE_URL)))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON));

            LOGGER.info("Setting up Mock SFTP Server with different credentials than connected systems :{} ", BASE_URL);
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            server.putFile(ENM_FILE_PATH + FILE_NAME, "Content of file", UTF_8);

            final double successfulFileCountBefore = metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL);
            final double failedFileCountBefore = metrics.getCounterValueByName(NUM_FAILED_FILE_TRANSFER_TOTAL);

            TestUtils.putFilenamesOnInputTopic(server, inputTopicListener, 1);

            assertEquals(successfulFileCountBefore,  metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL));
            assertEquals(failedFileCountBefore,  metrics.getCounterValueByName(NUM_FAILED_FILE_TRANSFER_TOTAL));
            assertFalse(sftpFileTransferService.isConnectionOpen());
        });
    }

    /**
     * Given mocked SFTP and connected systems response verify file download failed when file not found on SFTP server.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @DisplayName("Verify after receiving File Notification and connected systems response, when file not found on SFTP server, file is not downloaded")
    public void givenMockedSFTPAndConnectedSystemsResponseVerifyFileDownloadFailedWhenFileNotFoundOnSFTPServer() throws Exception {
        withSftpServer(server -> {
            LOGGER.info("Setting up Mock Server for Connected Systems:{} ", BASE_URL);
            mockServer.expect(ExpectedCount.once(),
                    requestTo(new URI(BASE_URL)))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new InputStreamResource(getClass().getClassLoader().getResourceAsStream("GetSingleSFTPSubsystemsResponse.json"))));

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            server.putFile(ENM_FILE_PATH + FILE_NAME + 1, "Content of file", UTF_8);

            final double successfulFileCountBefore = metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL);
            final double failedFileCountBefore = metrics.getCounterValueByName(NUM_FAILED_FILE_TRANSFER_TOTAL);

            TestUtils.putBadFilenamesOnServer(server, inputTopicListener, 1);

            assertEquals(successfulFileCountBefore,  metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL));
            assertEquals(failedFileCountBefore + 1,  metrics.getCounterValueByName(NUM_FAILED_FILE_TRANSFER_TOTAL));
        });
    }

    /**
     * Verify file download failed when file requested to download incorrect file name.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @DisplayName("Verify after receiving File Notification and connected systems response, when incorrect filename in message requested on SFTP server, file is not downloaded")
    public void verifyFileDownloadFailedWhenFileRequestedToDownloadIncorrectFileName() throws Exception {
        withSftpServer(server -> {
            LOGGER.info("Setting up Mock Server for Connected Systems:{} ", BASE_URL);
            mockServer.expect(ExpectedCount.once(),
                    requestTo(new URI(BASE_URL)))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new InputStreamResource(getClass().getClassLoader().getResourceAsStream("GetSingleSFTPSubsystemsResponse.json"))));

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            server.putFile(ENM_FILE_PATH + FILE_NAME, "Content of file", UTF_8);

            final double successfulFileCountBefore = metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL);
            TestUtils.putBadFilenamesOnInputTopic(server, inputTopicListener, 1);
            assertEquals(successfulFileCountBefore,  metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL));
        });
    }

    /**
     * Given mocked SFTP verify server connection and disconnection successfully.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @DisplayName("Verify connection and disconnection to SFTP server")
    public void givenMockedSFTPVerifyServerConnectionAndDisconnectionSuccessfully() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            server.putFile(ENM_FILE_PATH + FILE_NAME, "Content of file", UTF_8);

            loadBalancer.setScriptingVMs(Arrays.asList("localhost"));
            assertTrue(sftpFileTransferService.connectToSftpHost(Optional.of(USER), Optional.of(PORT), Optional.of(PASSWORD)));
            sftpFileTransferService.disconnectChannel();
            assertFalse(sftpFileTransferService.isConnectionOpen());
        });
    }

    /**
     * Given connection to SFTP server already open file is downloaded straight away.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @DisplayName("Verify connection and disconnection to SFTP server")
    public void givenConnectionToSFTPServerAlreadyOpenFileIsDownloadedStraightAway() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            server.putFile(ENM_FILE_PATH + FILE_NAME, "Content of file", UTF_8);

            loadBalancer.setScriptingVMs(Arrays.asList("localhost"));
            sftpFileTransferService.connectToSftpHost(Optional.of(USER), Optional.of(PORT), Optional.of(PASSWORD));
            assertTrue(sftpFileTransferService.isConnectionOpen());

            final double successfulFileCountBefore = metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL);
            sftpHandler.process(InputMessage.builder()
                            .nodeName(NODE_NAME)
                            .fileLocation(ENM_FILE_PATH + FILE_NAME)
                            .nodeType(NODE_TYPE)
                            .dataType(DATA_TYPE)
                            .fileType(FILE_TYPE)
                            .build());
            assertEquals((successfulFileCountBefore + 1),  metrics.getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL));
        });
    }

    /**
     * Given connection to SFTP server already open file is downloaded straight away.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @DisplayName("Verify that NullPointerException is not thrown when getting connection properties")
    public void getConnectionPropertiesNullPointerChecks() {
        final Map<String, SubsystemModel> subsystemsDetailsMapNullENM1Subsystem =  new HashMap<>();
        subsystemsDetailsMapNullENM1Subsystem.put("enm10", SubsystemModel.builder().build());
        assertDoesNotThrow(() -> sftpHandler.getConnectionProperties(subsystemsDetailsMapNullENM1Subsystem));

        subsystemsDetailsMapNullENM1Subsystem.put("enm1", SubsystemModel.builder().build());
        assertDoesNotThrow(() -> sftpHandler.getConnectionProperties(subsystemsDetailsMapNullENM1Subsystem));

        final List<ConnectionPropertiesModel> connectionPropertiesList = new ArrayList<>();
        final SubsystemModel subsystemModelEmptyConnectionProperties = SubsystemModel.builder().build();
        subsystemModelEmptyConnectionProperties.setConnectionProperties(connectionPropertiesList);
        subsystemsDetailsMapNullENM1Subsystem.put("enm1", subsystemModelEmptyConnectionProperties);
        assertDoesNotThrow(() -> sftpHandler.getConnectionProperties(subsystemsDetailsMapNullENM1Subsystem));
    }
}