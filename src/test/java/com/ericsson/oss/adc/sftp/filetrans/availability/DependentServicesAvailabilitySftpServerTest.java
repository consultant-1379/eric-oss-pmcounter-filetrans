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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.ericsson.oss.adc.sftp.filetrans.util.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;

import com.ericsson.oss.adc.sftp.filetrans.controller.SFTPHandler;
import com.ericsson.oss.adc.sftp.filetrans.model.ConnectionPropertiesModel;
import com.ericsson.oss.adc.sftp.filetrans.service.ENMScriptingVMLoadBalancer;
import com.ericsson.oss.adc.sftp.filetrans.service.SFTPFileTransferService;
import com.ericsson.oss.adc.sftp.filetrans.util.TestUtils;

/**
 * The Class DependentServicesAvailabilitySftpServerTest.
 * Used to provide extra coverage for test, without the need for Spring.
 */
@ExtendWith(MockitoExtension.class)
class DependentServicesAvailabilitySftpServerTest {
    private static final int PORT = 1234;
    private static final String USER = "user";
    private static final String PASSWORD = "password";

    @InjectMocks
    DependentServiceAvailabilitySftpServer dependentServiceAvailabilitySftpServer = new DependentServiceAvailabilitySftpServer();

    @Mock
    SFTPHandler sftpHandler;

    @Mock
    SFTPFileTransferService sftpFileTransferService;

    /**
     * Test SFTP Server deployed returns true.
     */
    @Test
    void test_sftpServerDeployedReturnsTrue() {
        try {
            Mockito.when(sftpHandler.isConnectionOpen()).thenReturn(false);
            final ConnectionPropertiesModel connectionPropertiesModel = TestUtils.getConnectedSystemsDto();

            Mockito.when(sftpHandler.getConnectionDetailsToSFTPServerUsingConnectedSystemsResponse()).thenReturn(connectionPropertiesModel);
            Mockito.when(sftpHandler.connectToEnm(USER, PORT, PASSWORD)).thenReturn(true);

            final ENMScriptingVMLoadBalancer lb = new ENMScriptingVMLoadBalancer();
            lb.setScriptingVMs(connectionPropertiesModel.getScriptingVMs());
            assertTrue(dependentServiceAvailabilitySftpServer.isSftpServerDeployed(), "Expected check on Sftp Server deployed, " +
                    "with Sftp Server Deployed and Connected Systems Deployed to pass");
            assertEquals(1, dependentServiceAvailabilitySftpServer.getAttemptNo(), "Expect only one attempt for Happy Case");
        } catch (final UnsatisfiedExternalDependencyException exception) {
            fail("Did not expect exception to be thrown for happy case test");
        }
    }

    /**
     * Test SFTP Server deployed connection open returns true.
     */
    @Test
    void test_sftpServerDeployedConnectionOpenReturnsTrue() {
        try {
            Mockito.when(sftpHandler.isConnectionOpen()).thenReturn(true);
            final boolean result = dependentServiceAvailabilitySftpServer.isSftpServerDeployed();
            assertTrue(result, "Expected check on Sftp Server deployed, With Sftp Server connection already open to pass");
            assertEquals(1, dependentServiceAvailabilitySftpServer.getAttemptNo(), "Expect only one attempt for 'connection already open' case");
        } catch (final UnsatisfiedExternalDependencyException exception) {
            fail("Did not expect exception to be thrown for 'connection already open' case test");
        }
    }

    /**
     * Test SFTP Server deployed in valid connection properties returns false.
     */
    @Test
    void test_sftpServerDeployedInValidConnectionPropertiesReturnsFalse() {
        try {
            Mockito.when(sftpHandler.isConnectionOpen()).thenReturn(false);
            final ConnectionPropertiesModel connectionPropertiesModel = TestUtils.getConnectedSystemsDto();
            connectionPropertiesModel.setSftpPort(null);
            Mockito.when(sftpHandler.getConnectionDetailsToSFTPServerUsingConnectedSystemsResponse()).thenReturn(connectionPropertiesModel);

            final ENMScriptingVMLoadBalancer lb = new ENMScriptingVMLoadBalancer();
            lb.setScriptingVMs(connectionPropertiesModel.getScriptingVMs());
            dependentServiceAvailabilitySftpServer.isSftpServerDeployed();
            fail("Expected exception to be thrown for 'InValid Connection Properties' case test");
        } catch (final UnsatisfiedExternalDependencyException exception) {
            assertEquals(1, dependentServiceAvailabilitySftpServer.getAttemptNo(),
                    "Expect one attempt (no RetryTemplate used in test) for 'InValid Connection Properties' case");
        }
    }

    /**
     * Test SFTP Server deployed no SFTP server connection returns false.
     */
    @Test
    void test_sftpServerDeployedNoSftpServerConnectionReturnsFalse() {
        try {
            Mockito.when(sftpHandler.isConnectionOpen()).thenReturn(false);
            final ConnectionPropertiesModel connectionPropertiesModel = TestUtils.getConnectedSystemsDto();
            connectionPropertiesModel.setSftpPort(null);
            Mockito.when(sftpHandler.getConnectionDetailsToSFTPServerUsingConnectedSystemsResponse()).thenReturn(connectionPropertiesModel);

            final ENMScriptingVMLoadBalancer lb = new ENMScriptingVMLoadBalancer();
            lb.setScriptingVMs(connectionPropertiesModel.getScriptingVMs());
            dependentServiceAvailabilitySftpServer.isSftpServerDeployed();
            fail("Expected exception to be thrown for 'No Sftp Server Connection' case test");
        } catch (final UnsatisfiedExternalDependencyException exception) {
            assertEquals(1, dependentServiceAvailabilitySftpServer.getAttemptNo(), "Expect one attempt (no RetryTemplate used in test) for 'No Sftp Server Connection' case");
        }
    }

    /**
     * Test SFTP Server retry forever.
     */
    @Test
    void test_sftpServerDeployedRetryForever() {
        final int availabilityRetryCountMax = 1;
        final int availabilityBackoffTimeInMs = 1000;
        final RetryTemplate template = RetryTemplate.builder().maxAttempts(availabilityRetryCountMax).fixedBackoff(availabilityBackoffTimeInMs)
                .retryOn(UnsatisfiedExternalDependencyException.class)
                .withListener(Utils.getRetryListener()).build();

        dependentServiceAvailabilitySftpServer.setRetryForever(true);
        dependentServiceAvailabilitySftpServer.setAvailabilityRetryCountMax(availabilityRetryCountMax);
        dependentServiceAvailabilitySftpServer.setAvailabilityBackoffTimeInMs(availabilityBackoffTimeInMs);
        Mockito.when(sftpHandler.isConnectionOpen()).thenReturn(false).thenReturn(true);

        assertTrue(dependentServiceAvailabilitySftpServer.retrySftpServerDeployed(template), "Expected check on Sftp Server deployed, with retry forever to pass");
        assertEquals(1, dependentServiceAvailabilitySftpServer.getAttemptNo(), "Expect only one attempt for 'retry forever' case");
        assertEquals(2, dependentServiceAvailabilitySftpServer.getRetryForeverAttemptNo(), "Expect two 'retryForever' attempts for 'retry forever' case");
    }

    /**
     * Test retry log message returns true.
     */
    @Test
    void test_logRetryMessageDisplaysMessage() {
        assertTrue(dependentServiceAvailabilitySftpServer.logRetryMessage(1, 2), "Expected log message to be displayed");
    }

    /**
     * Test retry log message returns false.
     */
    @Test
    void test_logRetryMessageDoesNotDisplayMessage() {
        assertFalse(dependentServiceAvailabilitySftpServer.logRetryMessage(2, 1), "Expected log message NOT to be displayed");
    }
}
