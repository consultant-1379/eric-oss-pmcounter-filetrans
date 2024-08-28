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


import com.ericsson.oss.adc.sftp.filetrans.service.ConnectedSystemsRetriever;
import com.ericsson.oss.adc.sftp.filetrans.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class DependentServiceAvailabilityConnectedSystems extends DependentServiceAvailability {

    @Value("${connected.systems.availability.retry_interval}")
    private int retryInterval;
    @Value("${connected.systems.availability.retry_attempts}")
    private int retryAttempts;

    @Autowired
    ConnectedSystemsRetriever connectedSystemsRetriever;


    /**
     * @return true once Connected Systems is reached and Access Endpoints Found, false if max retries exhausted
     */
    public boolean checkHealth() {
        final RetryTemplate template = RetryTemplate.builder()
                .maxAttempts(retryAttempts)
                .fixedBackoff(retryInterval)
                .retryOn(UnsatisfiedExternalDependencyException.class)
                .withListener(Utils.getRetryListener())
                .build();

        try {
            return template.execute(retryContext -> checkConnectedSystemsAccessEndpoints());
        } catch (final UnsatisfiedExternalDependencyException exception) {
            log.error("FAILED Health Check for Connected Systems service.", exception);
        }

        return false;
    }

    public boolean checkConnectedSystemsAccessEndpoints() throws UnsatisfiedExternalDependencyException {
        final List<String> connectedSystemsAccessPoints = connectedSystemsRetriever.getConnectedSystemsAccessPoints();
        if (connectedSystemsAccessPoints != null && !connectedSystemsAccessPoints.isEmpty()) {
            final String connectedSystemsUri = connectedSystemsAccessPoints.get(0);
            if (connectedSystemsUri != null) {
                return checkConnectedSystemsHealth(connectedSystemsUri);
            }

            throw new UnsatisfiedExternalDependencyException("INVALID Connected Systems URI for '" + connectedSystemsAccessPoints + "' Access Point.");
        }

        throw new UnsatisfiedExternalDependencyException("UNABLE to obtain Access Points for Connected Systems.");
    }

    public boolean checkConnectedSystemsHealth(final String connectedSystemsUri) {
        final String healthEndPoint = "/actuator/health";
        return super.checkHealth(connectedSystemsUri + healthEndPoint, "Connected Systems", retryAttempts, retryInterval);
    }
}
