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

import com.ericsson.oss.adc.sftp.filetrans.util.RestExecutor;
import com.ericsson.oss.adc.sftp.filetrans.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
@SpringBootTest(classes = {DependentServiceAvailabilityDataCatalogTest.class, DependentServiceAvailabilityDataCatalog.class, RestExecutor.class})
@AutoConfigureWebClient(registerRestTemplate = true)
public class DependentServiceAvailabilityDataCatalogTest {

    private static final String HEALTH_PATH = "actuator/health";

    @Value("${dmm.data-catalog.base-url}")
    private String dataCatalogUri;

    @Value("${dmm.data-catalog.base-port}")
    private String dataCatalogPort;

    @Value("${dmm.data-catalog.availability.retry-attempts}")
    private int retryAttempts;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    DependentServiceAvailabilityDataCatalog dependentServiceAvailabilityDataCatalog;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setup() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    @DisplayName("When health status UP/OK, availability should be true")
    public void get_health_status_ok() throws Exception {
        mockServer.expect(ExpectedCount.once(),
            requestTo(new URI(dataCatalogUri + TestUtils.addTrailingSlash(dataCatalogPort) + HEALTH_PATH)))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(getBody(true)));
        final boolean result = dependentServiceAvailabilityDataCatalog.checkHealth();
        mockServer.verify();
        assertTrue(result);
    }

    @Test
    @DisplayName("When health status DOWN/Not Found, availability should be false")
    public void get_health_status_not_ok() throws Exception {

        mockServer.expect(ExpectedCount.times(retryAttempts),
            requestTo(new URI(dataCatalogUri + TestUtils.addTrailingSlash(dataCatalogPort) + HEALTH_PATH)))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(getBody(false)));

        final boolean result = dependentServiceAvailabilityDataCatalog.checkHealth();
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