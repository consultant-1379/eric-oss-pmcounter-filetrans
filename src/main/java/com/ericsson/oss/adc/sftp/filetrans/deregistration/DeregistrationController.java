/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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
package com.ericsson.oss.adc.sftp.filetrans.deregistration;

import com.ericsson.oss.adc.sftp.filetrans.configuration.properties.DataServiceProperties;
import com.ericsson.oss.adc.sftp.filetrans.service.DataCatalogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal REST Controller handling Deregistration tasks. Not to be exposed to other services.
 * Currently, the only clients of these endpoints are the relevant Helm Hook jobs.
 */
@RestController
@Slf4j
public class DeregistrationController {

    public static final String DEREGISTER_DATA_SERVICE_INSTANCE_URI = "/data-service-instance";

    @Autowired
    private DataCatalogService dataCatalogService;
    @Autowired
    private DataServiceProperties dataServiceProperties;

    @DeleteMapping(value = DEREGISTER_DATA_SERVICE_INSTANCE_URI, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DeregisterResponse> deleteDataServiceInstance() {
        String dataServiceInstanceName = dataServiceProperties.getDataServiceInstanceName();
        log.info("Received a request to DELETE (deregister) the Data Service Instance '{}'.", dataServiceInstanceName);
        ResponseEntity<Void> dataCatalogResponse =
                dataCatalogService.deleteDataServiceInstance(dataServiceProperties.getDataServiceName(), dataServiceInstanceName);

        DeregisterStatus deregisterStatus;
        if (HttpStatus.NO_CONTENT.equals(dataCatalogResponse.getStatusCode()) || HttpStatus.NOT_FOUND.equals(dataCatalogResponse.getStatusCode())) {
            deregisterStatus = DeregisterStatus.SUCCESS;
        } else {
            deregisterStatus = DeregisterStatus.FAILURE;
        }

        return ResponseEntity.status(deregisterStatus == DeregisterStatus.SUCCESS ? HttpStatus.OK : dataCatalogResponse.getStatusCode())
                .body(DeregisterResponse.build(dataServiceInstanceName, deregisterStatus, (HttpStatus) dataCatalogResponse.getStatusCode()));
    }

}