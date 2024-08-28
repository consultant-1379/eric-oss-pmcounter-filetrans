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

import com.ericsson.oss.adc.sftp.filetrans.util.RestExecutor;
import com.ericsson.oss.adc.sftp.filetrans.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DependentServiceAvailability {
    @Autowired
    RestExecutor restExecutor;

    public boolean checkHealth(final String URL, final String dependentService, final int retryAttempts, final int retryInterval) {
        final RetryListener retryListener = Utils.getRetryListener();

        final RetryTemplate template = RetryTemplate.builder()
                .maxAttempts(retryAttempts)
                .fixedBackoff(retryInterval)
                .retryOn(UnsatisfiedExternalDependencyException.class)
                .withListener(retryListener)
                .build();

        try {
            return template.execute(retryContext -> isReachable(URL, dependentService));

        } catch (final UnsatisfiedExternalDependencyException exception) {
            log.error("FAILED Health Check for '{}' service. Exhausted all retry attempts ({}).", retryAttempts, exception);
        }

        return false;
    }

    public boolean isReachable(final String URL, final String dependentService) throws UnsatisfiedExternalDependencyException {
        try {
            final ResponseEntity<String> response = restExecutor.exchange(
                    URL,
                    "Checking if " + dependentService + " is available ...",
                    HttpMethod.GET,
                    String.class).getResponseEntity();
            return isOkResponseCode(response);

        } catch (final Exception exception) {
            throw new UnsatisfiedExternalDependencyException(dependentService + " is unreachable.", exception);
        }
    }

    private boolean isOkResponseCode(final ResponseEntity<String> response) throws UnsatisfiedExternalDependencyException {
        if (response.getStatusCode().equals(HttpStatus.OK)) {
            return true;
        }
        throw new UnsatisfiedExternalDependencyException("OK status code not found, found: " + response.getStatusCode().value());
    }

}
