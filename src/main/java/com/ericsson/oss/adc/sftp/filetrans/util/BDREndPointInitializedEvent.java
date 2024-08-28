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
package com.ericsson.oss.adc.sftp.filetrans.util;

import org.springframework.context.ApplicationEvent;

public class BDREndPointInitializedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1;
    String minIOUrl;
    public BDREndPointInitializedEvent(final Object source, final String minIOUrl) {
        super(source);
        this.minIOUrl = minIOUrl;
    }

    public String getMinIOUrl() {
        return this.minIOUrl;
    }

}
