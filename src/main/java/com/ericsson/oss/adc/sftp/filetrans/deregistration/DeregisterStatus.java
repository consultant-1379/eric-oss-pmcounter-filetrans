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

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Represents the Helm Hook script exit status.
 * See the inline comments for more details.
 */
public enum DeregisterStatus {

    SUCCESS(0), // Signals a successful delete of Data Service Instance from Data Catalog
    FAILURE(1), // Signals an unsuccessful delete operation
    EXCEPTION(2); // Signals a runtime error during the delete operation

    @Getter
    @JsonValue
    private final int exitStatus;

    DeregisterStatus(int exitStatus) {
        this.exitStatus = exitStatus;
    }

}