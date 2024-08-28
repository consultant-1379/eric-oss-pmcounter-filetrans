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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DependentServiceAvailabilityDataCatalog extends DependentServiceAvailability {

    @Value("${dmm.data-catalog.base-url}")
    private String dataCatalogUri;

    @Value("${dmm.data-catalog.base-port}")
    private String dataCatalogPort;

    @Value("${dmm.data-catalog.availability.retry_interval}")
    private int retryInterval;

    @Value("${dmm.data-catalog.availability.retry_attempts}")
    private int retryAttempts;

    public boolean checkHealth() {
        final String healthEndPoint = "actuator/health";
        return super.checkHealth(dataCatalogUri + addTrailingSlash(dataCatalogPort) + healthEndPoint, "Data Catalog", retryAttempts, retryInterval);
    }

    private String addTrailingSlash(String port) {
        if (!port.endsWith("/")) {
            port += "/";
        }
        return port;
    }
}
