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

import com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel;
import com.ericsson.oss.adc.sftp.filetrans.util.Utils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;


import static com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel.FileRepoType.S3;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@SpringBootTest
@EmbeddedKafka
public class DependentServiceAvailabilityBDRTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    DependentServiceAvailabilityBDR dependentServiceAvailabilityBDR;

    @Value("${bdr.hostname}")
    private String bdrHostname;

    @Value("${bdr.availability.retry_interval}")
    private int retryInterval;

    @Value("${bdr.availability.retry_attempts}")
    private int retryAttempts;

    private MockRestServiceServer mockServer;

    private static final String HEALTH_PATH = "/minio/health/live";
    private static final String DATA_CATALOG_BDR_URL = "http://localhost:9590/catalog/v1/bulk-data-repository/";

    private final BulkDataRepositoryModel[] bdrDTOs = new BulkDataRepositoryModel[1];
    private final BulkDataRepositoryModel bulkDataRepositoryModel = BulkDataRepositoryModel.builder()
            .name("testBDR1")
            .clusterName("testCluster")
            .nameSpace("testNS")
            .accessEndpoints(new ArrayList<>(Collections.singletonList("http://eric-data-object-storage-mn:9000")))
            .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
            .fileRepoType(S3)
            .build();

    @BeforeEach
    public void setup() {
        Utils.waitRetryInterval(1000);
        mockServer = MockRestServiceServer.createServer(restTemplate);
        bdrDTOs[0] = bulkDataRepositoryModel;
    }

    @Test
    @DisplayName("When response from health status OK, availability should be true")
    public void get_health_status_ok() throws Exception {

        final Gson gson = new Gson();
        mockServer.expect(ExpectedCount.once(),
            requestTo(new URI(DATA_CATALOG_BDR_URL)))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(gson.toJson(bdrDTOs)));

        mockServer.expect(ExpectedCount.once(),
            requestTo(new URI( bdrHostname + HEALTH_PATH)))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK));

        final boolean result = dependentServiceAvailabilityBDR.checkHealth();
        mockServer.verify();
        assertTrue(result);
    }

    @Test
    @DisplayName("When response from health status not OK, availability should be false")
    public void get_health_status_not_ok() throws Exception {

        final Gson gson = new Gson();

        mockServer.expect(ExpectedCount.once(),
            requestTo(new URI(DATA_CATALOG_BDR_URL)))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(gson.toJson(bdrDTOs)));

        mockServer.expect(ExpectedCount.times(retryAttempts),
            requestTo(new URI( bdrHostname + HEALTH_PATH)))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        final boolean result = dependentServiceAvailabilityBDR.checkHealth();
        mockServer.verify();
        assertFalse(result);
    }
}
