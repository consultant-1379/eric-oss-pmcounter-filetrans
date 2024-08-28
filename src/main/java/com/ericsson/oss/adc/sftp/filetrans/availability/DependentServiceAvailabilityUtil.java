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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DependentServiceAvailabilityUtil {

    @Autowired
    private DependentServiceAvailabilityDataCatalog dependentServiceAvailabilityDataCatalog;

    @Autowired
    private DependentServiceAvailabilityBDR dependentServiceAvailabilityBDR;

    @Autowired
    private DependentServiceAvailabilityConnectedSystems dependentServiceAvailabilityConnectedSystems;

    @Autowired
    private DependentServiceAvailabilityKafka dependentServiceAvailabilityKafka;

    public DependentServiceAvailabilityUtil() {
        // No Args Constructor for Spring.
    }
    public boolean areAllDependentServicesAvailable() {
        dependentServiceAvailabilityDataCatalog.checkHealth();
        dependentServiceAvailabilityBDR.checkHealth();
        dependentServiceAvailabilityKafka.checkHealth();
        dependentServiceAvailabilityConnectedSystems.checkHealth();
        return true;
    }
}
