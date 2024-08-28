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
package com.ericsson.oss.adc.sftp.filetrans.util;

import lombok.extern.slf4j.Slf4j;
import com.ericsson.oss.adc.sftp.filetrans.model.ConnectionPropertiesModel;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

@Slf4j
public class Utils {

    private Utils() {
        // Private constructor to hide public constructor (sonar).
    }


    public static boolean waitRetryInterval(final int waitIntervalMillis) {
        try {
            log.warn("Will retry in {} ms", waitIntervalMillis);
            Thread.sleep(waitIntervalMillis);
            return true;
        } catch (final InterruptedException | IllegalArgumentException exception) {
            log.error("Unexpected interruption between retries: {} ", exception.getMessage());
            Thread.currentThread().interrupt();
        }
        return false;
    }

    public static boolean isValidConnectionProperties(final ConnectionPropertiesModel connectionProperties) {
        if (connectionProperties.getSftpPort() == null || connectionProperties.getUsername() == null || connectionProperties.getPassword() == null) {
            return false;
        }
        final int port = Integer.parseInt(connectionProperties.getSftpPort());
        final String username = connectionProperties.getUsername();
        final String pw = connectionProperties.getPassword();
        return isValidString(username) && isValidString(pw) && isValidPort(port);
    }

    public static boolean setRetryForever(final int availabilityRetryCountMax) {
        return availabilityRetryCountMax == Integer.MAX_VALUE;
    }

    private static boolean isValidString(final String string) {
        return string != null && (string.length() > 0);
    }

    private static boolean isValidPort(final int portValue) {
        return portValue > 0;
    }

    public static RetryListener getRetryListener() {
        return new RetryListener() {
            @Override
            public <T, E extends Throwable> boolean open(final RetryContext retryContext, final RetryCallback<T, E> retryCallback) {
                return true; // called before first retry
            }

            @Override
            public <T, E extends Throwable> void close(final RetryContext retryContext, final RetryCallback<T, E> retryCallback, final Throwable throwable) {
                log.error("Retry closed after {} attempts", retryContext.getRetryCount(), throwable);
            }

            @Override
            public <T, E extends Throwable> void onError(final RetryContext retryContext, final RetryCallback<T, E> retryCallback, final Throwable throwable) {
                if (checkRetryCountShouldDisplayError(retryContext)) {
                    log.error("Retry Triggered {}", retryContext.getRetryCount(), throwable);
                }
            }
            private boolean checkRetryCountShouldDisplayError(final RetryContext retryContext){
                return  retryContext.getRetryCount() % 10 == 0;
            }
        };
    }
}
