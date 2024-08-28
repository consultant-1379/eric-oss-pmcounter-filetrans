/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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

import lombok.Builder;
import lombok.Data;
import org.springframework.http.ResponseEntity;
/**
 * Holds generic Response Entity to be used by RestExtractor
 */
@Data
@Builder
public class ResponseEntityWrapper<T> {

    private ResponseEntity<T> responseEntity;

}