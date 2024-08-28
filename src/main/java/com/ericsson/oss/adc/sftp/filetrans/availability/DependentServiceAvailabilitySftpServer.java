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

import com.ericsson.oss.adc.sftp.filetrans.controller.SFTPHandler;
import com.ericsson.oss.adc.sftp.filetrans.model.ConnectionPropertiesModel;
import com.ericsson.oss.adc.sftp.filetrans.service.SFTPFileTransferService;
import com.ericsson.oss.adc.sftp.filetrans.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

/**
 * Checks If the SFTP Server ( or ENM scripting cluster) is available and checks if connection can be made.
 * Reads the Server details from Connected Systems.
 */
@Component
@Slf4j
public class DependentServiceAvailabilitySftpServer extends RetryForeverHandler {

    @Autowired
    private SFTPFileTransferService sftpFileTransferService;

    @Autowired
    private SFTPHandler sftpHandler;

    @Value("${sftp.availability.backoffTimeInMs}")
    private int availabilityBackoffTimeInMs;

    @Value("${sftp.availability.retryCountMax}")
    private int availabilityRetryCountMax;

    public boolean checkHealth() {
        retryForever = Utils.setRetryForever(availabilityRetryCountMax);
        final RetryTemplate template = RetryTemplate.builder().maxAttempts(availabilityRetryCountMax).fixedBackoff(availabilityBackoffTimeInMs)
                .retryOn(UnsatisfiedExternalDependencyException.class).withListener(Utils.getRetryListener()).build();
        return retrySftpServerDeployed(template);
    }

    boolean retrySftpServerDeployed(final RetryTemplate template) {
        retryForeverAttemptNo = 0;
        do {
            attemptNo = 0;
            retryForeverAttemptNo++;

            try {
                return template.execute(retryContext -> isSftpServerDeployed());
            } catch (final UnsatisfiedExternalDependencyException exception) {
                log.error("FAILED Health Check for SFTP Server service.", exception);
            }

            //Allow retryForever to be false for testing.
        } while (retryForever);
        return false;
    }

    public boolean isSftpServerDeployed() throws UnsatisfiedExternalDependencyException {
        try {
            attemptNo++;
            log.debug("Checking if SFTP Server (Subsystem: {}) is available (attempt {}/{}) ...",
                    sftpHandler.getSubsystemName(), attemptNo, availabilityRetryCountMax);
            if (sftpHandler.isConnectionOpen()) {
                log.info("SFTP Server (Subsystem: {}) Connection is already open.", sftpHandler.getSubsystemName());
                //Leave the connection open if already open.
                return true;
            }

            final ConnectionPropertiesModel connectionProperties = sftpHandler.getConnectionDetailsToSFTPServerUsingConnectedSystemsResponse();
            if (!Utils.isValidConnectionProperties(connectionProperties)) {
                throw new UnsatisfiedExternalDependencyException(
                        "SFTP Server (Subsystem: '" + sftpHandler.getSubsystemName() + "' has INVALID Connection Properties.");
            }

            final int port = Integer.parseInt(connectionProperties.getSftpPort());
            final String username = connectionProperties.getUsername();
            final String pw = connectionProperties.getPassword();

            if (sftpHandler.connectToEnm(username, port, pw)) {
                log.info("Successfully connected to the SFTP Server (Subsystem: {}).", sftpHandler.getSubsystemName());
                sftpFileTransferService.disconnectChannel();
                return true;
            }

            final String errorMessage = String.format("SFTP Server(s): %s are unreachable on port: %d",
                    sftpFileTransferService.getLoadBalancer().getAllOnlineScriptingVMs(), port);
            log.error(errorMessage);
            throw new UnsatisfiedExternalDependencyException(errorMessage);

        } catch (final Exception exception) {
            throw new UnsatisfiedExternalDependencyException("General Exception during SFTP Server service Health Check.", exception);
        }
    }

    /**
     * @deprecated Used only in testing. Tests should not depend on the production code.
     */
    @Deprecated
    boolean logRetryMessage(final int attemptNumber, final int availabilityRetryCountMaxNumber) {
        if (attemptNumber < availabilityRetryCountMaxNumber) {
            log.info("Check SFTP Server Available: Try again in {} mS ", availabilityBackoffTimeInMs);
            return true;
        }
        return false;
    }

    public void setAvailabilityRetryCountMax(final int availabilityRetryCountMax) {
        this.availabilityRetryCountMax = availabilityRetryCountMax;
    }

    public void setAvailabilityBackoffTimeInMs(final int availabilityBackoffTimeInMs) {
        this.availabilityBackoffTimeInMs = availabilityBackoffTimeInMs;
    }
}