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


import com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel;
import com.ericsson.oss.adc.sftp.filetrans.service.BDRService;
import com.ericsson.oss.adc.sftp.filetrans.service.DataCatalogService;
import com.ericsson.oss.adc.sftp.filetrans.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DependentServiceAvailabilityBDR extends DependentServiceAvailability {

    @Autowired
    BDRService bdrService;
    @Autowired
    DataCatalogService dataCatalogService;

    @Value("${bdr.availability.retry_interval}")
    private int retryInterval;
    @Value("${bdr.availability.retry_attempts}")
    private int retryAttempts;

    /**
     * Determines the service's health based on whether BDR is reachable or not
     */
    public boolean checkHealth() {
        final RetryTemplate template = RetryTemplate.builder()
                .maxAttempts(retryAttempts)
                .fixedBackoff(retryInterval)
                .retryOn(UnsatisfiedExternalDependencyException.class)
                .withListener(Utils.getRetryListener())
                .build();
        try {
            return template.execute(retryContext -> checkBDRAccessEndpoint());
        } catch (final UnsatisfiedExternalDependencyException exception) {
            log.error("FAILED Health Check for 'BDR' service.", exception);
        }

        return false;
    }

    public boolean checkBDRAccessEndpoint() throws UnsatisfiedExternalDependencyException {
        final ResponseEntity<BulkDataRepositoryModel[]> bulkDataRepository = dataCatalogService.getAllBulkDataRepositories();

        if (bulkDataRepository != null) {
            final BulkDataRepositoryModel bulkDataRepositoryModel = bdrService.getAccessEndPointsFromDataCatalog(bulkDataRepository);
            if (bulkDataRepositoryModel != null && bulkDataRepositoryModel.getAccessEndpoints() != null && !bulkDataRepositoryModel.getAccessEndpoints().isEmpty()) {
                final String accessEndpoint = bulkDataRepositoryModel.getAccessEndpoints().get(0);
                if (accessEndpoint != null) {
                    return checkBDRHealth(accessEndpoint);
                }
            }
            throw new UnsatisfiedExternalDependencyException("UNABLE to obtain Access Endpoint for Bulk Data Repository.");
        }

        throw new UnsatisfiedExternalDependencyException("NO Bulk Data Repository found in Data Catalog.");
    }

    public boolean checkBDRHealth(final String accessEndpoint) {
        final String healthEndPoint = "/minio/health/live";
        return super.checkHealth(accessEndpoint + healthEndPoint, "BDR", retryAttempts, retryInterval);
    }

}
