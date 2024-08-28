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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeregisterResponse {

    private DeregisterStatus deregisterStatus;
    private String message;

    public static DeregisterResponse build(String dataServiceInstanceName, DeregisterStatus deregisterStatus, HttpStatus dataCatalogStatusCode) {
        String messageHeadline;
        switch (deregisterStatus) {
            case SUCCESS:
                messageHeadline = "Data Service Instance '" + dataServiceInstanceName + "' deleted";
                break;
            case FAILURE:
                messageHeadline = "FAILED to delete Data Service Instance: '" + dataServiceInstanceName + "'";
                break;
            default:
                messageHeadline = "'" + dataServiceInstanceName + "' <No Message>";
        }

        return builder().deregisterStatus(deregisterStatus)
                .message(messageHeadline + ", Data Catalog HTTP Status: " + dataCatalogStatusCode)
                .build();
    }

}