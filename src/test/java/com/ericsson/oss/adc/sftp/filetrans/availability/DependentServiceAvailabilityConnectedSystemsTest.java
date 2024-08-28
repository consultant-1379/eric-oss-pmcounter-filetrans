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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;

import com.ericsson.oss.adc.sftp.filetrans.Startup;
import com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel;
import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel.FileRepoType.SFTP;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@SpringBootTest
@EmbeddedKafka
public class DependentServiceAvailabilityConnectedSystemsTest {

    private static final String HEALTH_PATH = "actuator/health";

    @Value("${connected.systems.availability.retry_attempts}")
    private int retryAttempts;

    @Autowired
    private RestTemplate restTemplate;

    @MockBean
    private Startup startup;

    @Autowired
    DependentServiceAvailabilityConnectedSystems dependentServiceAvailabilityConnectedSystems;

    private MockRestServiceServer mockServer;

    private static final String DATA_CATALOG_BDR_URL = "http://localhost:9590/catalog/v1/bulk-data-repository/";
    private static final String CONNECTED_SYSTEM_URL =  "http://eric-eo-subsystem-management/";

    private final BulkDataRepositoryModel[] bdrDTOs = new BulkDataRepositoryModel[1];

    private final BulkDataRepositoryModel[] bdrEmptyAccessEndpointsDTOs = new BulkDataRepositoryModel[1];

    private final BulkDataRepositoryModel bulkDataRepositoryModel = BulkDataRepositoryModel.builder()
            .name("testBDR1")
            .clusterName("testCluster")
            .nameSpace("testNS")
            .accessEndpoints(new ArrayList<>(Collections.singletonList("http://eric-eo-subsystem-management/")))
            .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
            .fileRepoType(SFTP)
            .build();

    private final BulkDataRepositoryModel bulkDataRepositoryEmptyAccessEndpointsDTO = BulkDataRepositoryModel.builder()
            .name("testBDR1")
            .clusterName("testCluster")
            .nameSpace("testNS")
            .accessEndpoints(new ArrayList<>())
            .fileFormatIds(new ArrayList<>())
            .fileRepoType(SFTP)
            .build();
    @BeforeEach
    public void setup() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        bdrDTOs[0] = bulkDataRepositoryModel;
        bdrEmptyAccessEndpointsDTOs[0] = bulkDataRepositoryEmptyAccessEndpointsDTO;
    }

    @Test
    @DisplayName("When health status UP/OK, availability should be true")
    public void get_health_status_ok() throws Exception {
        final Gson gson = new Gson();
        mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI(DATA_CATALOG_BDR_URL)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(gson.toJson(bdrDTOs)));

        mockServer.expect(ExpectedCount.once(),
            requestTo(new URI(CONNECTED_SYSTEM_URL + HEALTH_PATH)))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(getBody(true)));

        final boolean result = dependentServiceAvailabilityConnectedSystems.checkHealth();
        mockServer.verify();
        assertTrue(result);
    }

    @Test
    @DisplayName("When health status DOWN/Not Found, availability should be false")
    public void get_health_status_not_ok() throws Exception {
        final Gson gson = new Gson();
        mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI(DATA_CATALOG_BDR_URL)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(gson.toJson(bdrDTOs)));

        mockServer.expect(ExpectedCount.times(retryAttempts),
            requestTo(new URI(CONNECTED_SYSTEM_URL + HEALTH_PATH)))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(getBody(false)));

        final boolean result = dependentServiceAvailabilityConnectedSystems.checkHealth();
        mockServer.verify();
        assertFalse(result);
    }

    @Test
    @DisplayName("When health status UP/OK, but empty response returned, connected system health path not called")
    public void get_health_status_not_ok_access_Endpoint_Empty() throws Exception {
        final Gson gson = new Gson();
        mockServer.reset();

        mockServer.expect(ExpectedCount.manyTimes(),
                        requestTo(new URI(DATA_CATALOG_BDR_URL)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(gson.toJson(bdrEmptyAccessEndpointsDTOs)));

        mockServer.expect(ExpectedCount.never(),
                        requestTo(new URI(CONNECTED_SYSTEM_URL + HEALTH_PATH)));

        final boolean result = dependentServiceAvailabilityConnectedSystems.checkHealth();
        mockServer.verify();
        assertFalse(result);
    }

    @Test
    @DisplayName("When health status UP/OK, but null response returned, availability should be false")
    public void get_health_status_not_ok_data_catalog_response_null() throws Exception {
        final Gson gson = new Gson();
        mockServer.expect(ExpectedCount.manyTimes(),
                        requestTo(new URI(DATA_CATALOG_BDR_URL)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(gson.toJson(null)));

        mockServer.expect(ExpectedCount.never(),
                        requestTo(new URI(CONNECTED_SYSTEM_URL + HEALTH_PATH)));

        final boolean result = dependentServiceAvailabilityConnectedSystems.checkHealth();
        mockServer.verify();
        assertFalse(result);
    }

    private String getBody(final boolean up) {
        final String body = "{\"status\":\"%1$s\",\"groups\":[\"liveness\",\"readiness\"]}";
        if (up) {
            return String.format(body, "UP");
        }
        return String.format(body, "DOWN");
    }
}

