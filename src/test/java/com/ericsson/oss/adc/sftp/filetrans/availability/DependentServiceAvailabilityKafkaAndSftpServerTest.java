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
package com.ericsson.oss.adc.sftp.filetrans.availability;

import static com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.adc.sftp.filetrans.service.ConnectedSystemsRetriever;

/**
 * The Class DependentServiceAvailabilityKafkaAndSftpServerTest.
 *
 * Used to test Kafka and SFTP Server availability.
 * Using same class for both mean saving test time, as spring is initiated once.
 *
 */
@SpringBootTest( properties ={"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}",})
@EmbeddedKafka
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DependentServiceAvailabilityKafkaAndSftpServerTest {

    private static final String CONNECTED_SYSTEMS_BASE_URL = "http://eric-eo-subsystem-management/subsystem-manager/v1/subsystems/";
    private static final int PORT = 1234;
    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private final String inputTopicName = "test";
    protected MockRestServiceServer mockServer;

    @SpyBean
    KafkaAdmin kafkaAdmin;

    @Autowired
    private DependentServiceAvailabilityKafka dependentServiceAvailabilityKafka;

    @Autowired
    protected RestTemplate restTemplate;

    @Autowired
    protected ConnectedSystemsRetriever connectedSystemsRetriever;

    @Autowired
    private DependentServiceAvailabilitySftpServer dSASftpServer;

    @Mock
    AdminClient adminClient;

    @Mock Properties properties;

    @Value("${sftp.availability.retryCountMax}")
    private int availabilityRetryCountMax;

    @BeforeEach
    public void setup() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        connectedSystemsRetriever.addAccessEndpoints(new ArrayList<>(Collections.singletonList("http://eric-eo-subsystem-management/")));
        dSASftpServer.setRetryForever(false);
    }

    /**
     * Test input topic does not exist.
     */
    @Test
    @DisplayName("When input topic doesn't exist, expect check to fail")
    @Order(1)
    void test_input_topic_does_not_exist() {
        ReflectionTestUtils.setField(dependentServiceAvailabilityKafka, "inputTopicName", inputTopicName);
        ReflectionTestUtils.setField(dependentServiceAvailabilityKafka, "enmID", "");
        assertFalse(dependentServiceAvailabilityKafka.checkHealth());
        Mockito.verify(kafkaAdmin, atLeast(3)).getConfigurationProperties();
    }

    /**
     * Test input topic does exist.
     */
    @Test
    @DisplayName("When input topic exists, expect check to pass")
    void test_input_topic_does_exist() {
        ReflectionTestUtils.setField(dependentServiceAvailabilityKafka, "inputTopicName", inputTopicName);
        ReflectionTestUtils.setField(dependentServiceAvailabilityKafka, "enmID", "");
        buildAndCreateTopic();
        assertTrue(dependentServiceAvailabilityKafka.checkHealth());
        Mockito.verify(kafkaAdmin, times(1)).getConfigurationProperties();

    }

    /**
     * Test kafka not reachable max retries reached.
     */
    @Test
    @DisplayName("When a kafka error occurs, max retries should be reached and exhausted")
    void test_kafka_not_reachable_max_retries_reached() {
        final Map<String, Object> properties = new HashMap<>(1);
        properties.put("bootstrap.servers", "noserver");
        when(kafkaAdmin.getConfigurationProperties()).thenReturn(properties);
        assertFalse(dependentServiceAvailabilityKafka.checkHealth());
    }

    /**
     * Test SFTP Server deployed when no connected systems deployed.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @DisplayName("Check SFTP Server Available, With Sftp Server Deployed and Invalid Connected Systems Response")
    void test_sftpServerDeployedNoConnectedSystems() throws Exception {
        mockServer.reset();
        mockServer.expect(ExpectedCount.manyTimes(), requestTo(new URI(CONNECTED_SYSTEMS_BASE_URL))).andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                .body(""));
        withSftpServer(server -> {
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            assertFalse(dSASftpServer.checkHealth(), "Expected check on Sftp Server deployed, With Sftp Server Deployed and Invalid Connected Systems response to fail");
            assertEquals(availabilityRetryCountMax, dSASftpServer.getAttemptNo(),
                    "Expect " + availabilityRetryCountMax + " attempts for 'bad Connected Systems' case");
        });
    }

    /**
     * Test SFTP Server deployed when no SFTP server deployed.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @DisplayName("Check SFTP Server Available, With NO Sftp Server Deployed and Connected Systems Deployed")
    void test_sftpServerDeployedNoSftpServer() throws Exception {
        mockConnectedSystems();
        assertFalse(dSASftpServer.checkHealth(), "Expected check on Sftp Server deployed, With NO Sftp Server Deployed and Connected Systems deployed to fail");
        assertEquals(availabilityRetryCountMax, dSASftpServer.getAttemptNo(),
                "Expect " + availabilityRetryCountMax + " attempts for 'No SFTP Server' case");
    }

    /**
     * Test SFTP Server deployed.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @DisplayName("Check SFTP Server Available, With Sftp Server Deployed and Connected Systems Deployed")
    void test_sftpServerDeployed() throws Exception {
        mockConnectedSystems();
        withSftpServer(server -> {
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            assertTrue(dSASftpServer.checkHealth(), "Expected check on Sftp Server deployed, With Sftp Server Deployed and Connected Systems Deployed to pass");
            assertEquals(1, dSASftpServer.getAttemptNo(), "Expect only one attempt for Happy Case");
        });
    }

    /**
     * Test Bootstrap Server details not found.
     *
     *
     */
    @Test
    @DisplayName("Check Bootstrap Server Details Available, exception thrown when not found")
    void test_bootstrapServerDetailsNotFound() {
        ReflectionTestUtils.setField(dependentServiceAvailabilityKafka, "inputTopicName", inputTopicName);
        ReflectionTestUtils.setField(dependentServiceAvailabilityKafka, "enmID", "");
        buildAndCreateTopic();
        when(kafkaAdmin.getConfigurationProperties()).thenReturn(new HashMap<>());
        assertFalse(dependentServiceAvailabilityKafka.checkHealth());
    }

    private void buildAndCreateTopic() {
        final NewTopic outputTopic = TopicBuilder.name(inputTopicName)
                .partitions(1)
                .replicas(1)
                .build();

        kafkaAdmin.createOrModifyTopics(outputTopic);
    }

    private void mockConnectedSystems() throws URISyntaxException {
        mockServer.reset();
        mockServer.expect(ExpectedCount.manyTimes(), requestTo(new URI(CONNECTED_SYSTEMS_BASE_URL))).andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                .body(new InputStreamResource(getClass().getClassLoader().getResourceAsStream("IntegrationGetSubsystemsResponse.json"))));
    }
}
