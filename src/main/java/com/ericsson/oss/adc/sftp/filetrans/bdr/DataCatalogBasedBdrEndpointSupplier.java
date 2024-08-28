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
package com.ericsson.oss.adc.sftp.filetrans.bdr;

import com.ericsson.oss.adc.sftp.filetrans.util.BDREndPointInitializedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Component
@Slf4j
public class DataCatalogBasedBdrEndpointSupplier implements Supplier<String> {

    private String bdrEndpoint;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    //IDUN-63020 Remove the explicit null check using the Apache Commons isEmpty
    public void initBdrEndPointDetailsFromDataCatalog(List<String> accessEndpointList) {
        if (accessEndpointList != null && !accessEndpointList.isEmpty()) {
            bdrEndpoint = accessEndpointList.get(0);
            applicationEventPublisher.publishEvent(new BDREndPointInitializedEvent(this, bdrEndpoint));
            log.info("Event is published for initializing minioClient");
        }
    }

    @Override
    public String get() {
        return bdrEndpoint;
    }
}
