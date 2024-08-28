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
package com.ericsson.oss.adc.sftp.filetrans.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.adc.sftp.filetrans.model.ConnectionPropertiesModel;
import com.ericsson.oss.adc.sftp.filetrans.model.SubsystemModel;
import com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
/**
 * The Class SFTPFileTransferServiceBasicTest.
 */
class SFTPFileTransferServiceBasicTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SFTPFileTransferServiceBasicTest.class);
    private static Gson gson = new Gson();
    private static final int PORT = 1234;
    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private final ConnectedSystemsRetriever connectedSystemsRetrieverMock = mock(ConnectedSystemsRetriever.class);

    private final SFTPProcessingMetricsUtil metrics = new SFTPProcessingMetricsUtil(new SimpleMeterRegistry());

    /**
     * Verify disconnect channel sftp throw exception.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @DisplayName("Verify DisconnectChannelSftp fails when JSchException thrown")
    public void VerifyDisconnectChannelSftpThrowException() throws Exception {
        final SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService();
        final ChannelSftp channelSftp = mock(ChannelSftp.class);
        doThrow(new JSchException("Error occurred")).when(channelSftp).getSession();
        channelSftp.connect();
        assertFalse(sftpFileTransferService.disconnectChannelSftp(channelSftp),
                "Expected disconnectChannelSftp to fail with 'JSchException' exception thrown");
    }

    /**
     * Verify disconnect channel sftp.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @DisplayName("Verify DisconnectChannelSftp succeeds when sftp channel connected with session")
    public void VerifyDisconnectChannelSftp() throws Exception {
        final SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService();
        final Session session = mock(Session.class);
        final ChannelSftp channelSftp = mock(ChannelSftp.class);
        Mockito.when(channelSftp.isConnected()).thenReturn(true);
        Mockito.when(channelSftp.getSession()).thenReturn(session);
        channelSftp.connect();
        assertTrue(sftpFileTransferService.disconnectChannelSftp(channelSftp),
                "Expected disconnectChannelSftp to pass when sftp channel connected with session");
    }

    /**
     * Verify disconnect channel sftp with no session.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @DisplayName("Verify DisconnectChannelSftp succeeds when sftp channel connected with NO session")
    public void VerifyDisconnectChannelSftpWithNoSession() throws Exception {
        final SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService();
        final ChannelSftp channelSftp = mock(ChannelSftp.class);
        Mockito.when(channelSftp.isConnected()).thenReturn(true);
        Mockito.when(channelSftp.getSession()).thenReturn(null);
        channelSftp.connect();
        assertTrue(sftpFileTransferService.disconnectChannelSftp(channelSftp),
                "Expected disconnectChannelSftp to pass when sftp channel connected with NO session");
    }

    @Test
    @DisplayName("Verify successive calls to STFP Download will have non empty list of hosts to try to connect to.")
    public void VerifyDownload() throws Exception {
        final int numberRetries = 3;
        final int timeoutInMillis = 100;
        final String remoteFilePath = "some/remote/path/to/file";
        final ENMScriptingVMLoadBalancer lb = new ENMScriptingVMLoadBalancer();
        final SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService();
        this.setupSftpFileTransferService(sftpFileTransferService, lb, numberRetries, timeoutInMillis);
        // the first test will pass as getAllOnlineScriptingVMs() will return non empty list when called.
        testSftpDownload(numberRetries, remoteFilePath, lb, sftpFileTransferService);
        // The second test should pass too as getAllOnlineScriptingVMs() should return non empty list
        // the 'AllOnlineScriptingVM' List should be re-populated after failed connections attempts as
        // STFP Server may come online later.
        testSftpDownload(numberRetries, remoteFilePath, lb, sftpFileTransferService);

    }

    @Test
    @DisplayName("Verify successive calls to STFP createSFTPChannel will have non empty list of hosts to try to connect to.")
    public void VerifyCreateSftpChannel() throws Exception {
        final int numberRetries = 3;
        final int timeoutInMillis = 100;
        final ENMScriptingVMLoadBalancer lb = new ENMScriptingVMLoadBalancer();
        final SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService();
        this.setupSftpFileTransferService(sftpFileTransferService, lb, numberRetries, timeoutInMillis);

        final JSch jsch = mock(JSch.class);
        sftpFileTransferService.setJsch(jsch);
        Mockito.when(jsch.getSession(anyString(), anyString(), Mockito.anyInt())).thenThrow(new RuntimeException("From Test 1"))
        .thenThrow(new RuntimeException("From Test 2")).thenThrow(new RuntimeException("From Test 3"));

        sftpFileTransferService.setMetrics(metrics);

        // The first test will pass as getAllOnlineScriptingVMs() will return non empty list
        this.testCreateSftpChannel(numberRetries, lb, sftpFileTransferService);
        // The second test should pass too as getAllOnlineScriptingVMs() should return non empty list
        // the 'AllOnlineScriptingVM' List should be re-populated after failed connections attempts as
        // STFP Server may come online later.
        this.testCreateSftpChannel(numberRetries, lb, sftpFileTransferService);

    }

    @Test
    @DisplayName("Verify is valid bytes read from files returns false for zero bytes read")
    public void VerifyIsValidBytesReadFromFilesNegativeTest() throws Exception {
        final SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService();
        final String remoteFilePath = "some/remote/path/to/file";
        final boolean result = sftpFileTransferService.isValidBytesReadFromFiles(remoteFilePath, 0);
        assertFalse(result, "Expected false when 'numberBytesRead' = 0");
    }

    @Test
    @DisplayName("Verify is valid bytes read from files returns true for non zero bytes read")
    public void VerifyIsValidBytesReadFromFilesTest() throws Exception {
        final SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService();
        final String remoteFilePath = "some/remote/path/to/file";
        final boolean result = sftpFileTransferService.isValidBytesReadFromFiles(remoteFilePath, 100);
        assertTrue(result, "Expected true when 'numberBytesRead' = 100");
    }

    @Test
    @DisplayName("Verify is Valid Host returns false for null host")
    public void VerifyIsValidHostNullHostTest() throws Exception {
        final SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService();
        final boolean result = sftpFileTransferService.isValidHost(null);
        assertFalse(result, "Expected false when 'host' = null");
    }

    @Test
    @DisplayName("Verify is Valid Host returns false for blank host")
    public void VerifyIsValidHostBlankHostTest() throws Exception {
        final SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService();
        final boolean result = sftpFileTransferService.isValidHost("");
        assertFalse(result, "Expected false when 'host' = '' ");
    }

    @Test
    @DisplayName("Verify is Valid Host returns true for valid host")
    public void VerifyIsValidHostTest() throws Exception {
        final String host = "some-host";
        final SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService();
        final boolean result = sftpFileTransferService.isValidHost(host);
        assertTrue(result, "Expected true when 'host' = " + host);
    }

    @Test
    @DisplayName("Verify  call to STFP Download with invalid list of hosts will not connect.")
    public void VerifyDownloadInvalidHosts() throws Exception {
        final int numberRetries = 1;
        final int timeoutInMillis = 100;
        final String remoteFilePath = "some/remote/path/to/file";
        final ENMScriptingVMLoadBalancer lb = new ENMScriptingVMLoadBalancer();
        final SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService();
        this.setupSftpFileTransferServiceNoHosts(sftpFileTransferService, lb, numberRetries, timeoutInMillis);
        // the first test will pass as getAllOnlineScriptingVMs() will return non empty list when called.
        testSftpDownload(0, remoteFilePath, lb, sftpFileTransferService);
    }

    @Test
    @DisplayName("Verify ENMScriptingVMLoadBalancer will remove null and empty hosts.")
    public void VerifyENMScriptingVMLoadBalancerRemoveInvalidHosts() {
        final ENMScriptingVMLoadBalancer lb = new ENMScriptingVMLoadBalancer();
        final List<String> onlineHosts = Arrays.asList(new String[] { null, null, "" });
        lb.setScriptingVMs(onlineHosts);
        lb.removeVMFromOnlineScriptingVMs(null);
        lb.removeVMFromOnlineScriptingVMs(null);
        lb.removeVMFromOnlineScriptingVMs("");
        final boolean result = lb.getAllOnlineScriptingVMs().isEmpty();
        assertTrue(result, "Expected AllOnlineScriptingVMs to be empty");
        assertEquals(0, lb.getAllOnlineScriptingVMs().size(), "Expect ALL hosts to be removed from list");
    }

    @Test
    @DisplayName("Verify ENMScriptingVMLoadBalancer will not attempt to remove no existant null host.")
    public void VerifyENMScriptingVMLoadBalancerNotRemoveNonExistantNullHosts() {
        final ENMScriptingVMLoadBalancer lb = new ENMScriptingVMLoadBalancer();
        final List<String> onlineHosts = Arrays.asList(new String[] { "host1", "host2", "host3" });
        lb.setScriptingVMs(onlineHosts);
        lb.removeVMFromOnlineScriptingVMs(null);
        final boolean result = lb.getAllOnlineScriptingVMs().isEmpty();
        assertFalse(result, "Expected AllOnlineScriptingVMs NOT to be empty");
        assertEquals(onlineHosts.size(), lb.getAllOnlineScriptingVMs().size(), "Expect no hosts to be removed from list");
    }


    private void testCreateSftpChannel(final int numberRetries, final ENMScriptingVMLoadBalancer lb,
                                       final SFTPFileTransferService sftpFileTransferService) {
        final boolean channelCreated = sftpFileTransferService.createSFTPChannel(USER, PORT, PASSWORD);
        assertFalse(channelCreated, "Expected no channel to be created");
        assertNumberAttempts(lb, numberRetries, sftpFileTransferService.getNumberCreateChannelConnectionAttempts(),
                sftpFileTransferService.getNumberCreateChannelConnectionRetriesAttemps());
        sftpFileTransferService.disconnectChannel();
    }

    private void testSftpDownload(final int numberRetries, final String remoteFilePath, final ENMScriptingVMLoadBalancer lb,
                                  final SFTPFileTransferService sftpFileTransferService) {
        sftpFileTransferService.downloadFile(remoteFilePath);
        assertNumberAttempts(lb, numberRetries, sftpFileTransferService.getNumberDownloadConnectionAttempts(),
                sftpFileTransferService.getNumberDownloadConnectionRetriesAttemps());
    }

    private void assertNumberAttempts(final ENMScriptingVMLoadBalancer lb, final int expectedNumberRetries, final int actualNumberConnectionAttempts,
                                      final int actualNumberRetries) {
        LOGGER.info("Online Hosts = {}, Available Hosts = {}, Number hosts connection attempts = {}, Total number retries = {} ",
                lb.getAllOnlineScriptingVMs(), lb.getAllAvailableScriptingVMs(), actualNumberConnectionAttempts, actualNumberRetries);
        assertEquals(lb.getAllOnlineScriptingVMs().size(), lb.getAllAvailableScriptingVMs().size(),
                "Expected to have SAME number of ONLINE to AVAILABLE hosts after SftpDownload or createSftpChannel ( methods should reset AVAILABE hosts).");
        assertEquals(lb.getAllAvailableScriptingVMs().size(), actualNumberConnectionAttempts,
                "Expected to have attempted to connect to all available hosts");
        assertEquals(lb.getAllAvailableScriptingVMs().size() * expectedNumberRetries, actualNumberRetries,
                "Expected to have attempted to retry multiple times to all available hosts");
    }

    private ChannelSftp setupMockCreateSFTPChannel(final SFTPFileTransferService sftpFileTransferService) throws JSchException, SftpException {
        final ChannelSftp channelSftp = mock(ChannelSftp.class);
        final JSch jsch = mock(JSch.class);
        final Session session = mock(Session.class);

        sftpFileTransferService.setJsch(jsch);
        Mockito.when(jsch.getSession(anyString(), anyString(), Mockito.anyInt())).thenReturn(session);
        Mockito.when(session.openChannel(anyString())).thenReturn(channelSftp);
        Mockito.when(channelSftp.getSession()).thenReturn(session);
        Mockito.when(channelSftp.get(anyString())).thenThrow(new SftpException(1, "From Test"));
        channelSftp.connect();
        return channelSftp;
    }

    private ConnectionPropertiesModel setupMockConnectedSystems() throws FileNotFoundException {
        final Map<String, SubsystemModel> subsystemsByNameMap =  new HashMap<>();
        final List<SubsystemModel> subsystemList = Arrays.asList(gson.fromJson(new JsonReader(new FileReader(getClass().getClassLoader().getResource("GetSFTPSubsystemsResponse.json").getFile())), SubsystemModel[].class));
        for (final SubsystemModel subsystem: subsystemList) {
            subsystemsByNameMap.put(subsystem.getName(), subsystem);
        }
        final ConnectionPropertiesModel connectionPropertiesModel = getConnectedSystemPropertiesDto(subsystemsByNameMap);

        Mockito.when(connectedSystemsRetrieverMock.getConnectionPropertiesBySubsystemsName()).thenReturn(connectionPropertiesModel);
        return connectionPropertiesModel;
    }

    private ConnectionPropertiesModel getConnectedSystemPropertiesDto(final Map<String, SubsystemModel> subsystemsByNameMap) {
        final SubsystemModel subsystemModel = subsystemsByNameMap.get("enm1");
        final List<ConnectionPropertiesModel> connectionPropertiesModelList = subsystemModel.getConnectionProperties();
        final ConnectionPropertiesModel connectionPropertiesModel = connectionPropertiesModelList.get(0);
        return connectionPropertiesModel;
    }

    private void setupSftpFileTransferService(final SFTPFileTransferService sftpFileTransferService, final ENMScriptingVMLoadBalancer lb,
                                              final int numberRetries, final int timeoutInMillis)
                                                      throws JSchException, SftpException, FileNotFoundException {
        final ConnectionPropertiesModel connectionPropertiesModel = setupMockConnectedSystems();
        final List<String> onlineHosts = connectionPropertiesModel.getScriptingVMs();
        LOGGER.info("onlineHosts = {}", onlineHosts);

        setupSftpFileTransferServiceBase(sftpFileTransferService, lb, numberRetries, timeoutInMillis, onlineHosts);
    }

    private void setupSftpFileTransferServiceNoHosts(final SFTPFileTransferService sftpFileTransferService, final ENMScriptingVMLoadBalancer lb,
                                                     final int numberRetries, final int timeoutInMillis)
                                                             throws JSchException, SftpException, FileNotFoundException {
        final List<String> onlineHosts = Arrays.asList(new String[] { null, null, "" });
        LOGGER.info("'invalid' onlineHosts = {}", onlineHosts);

        setupSftpFileTransferServiceBase(sftpFileTransferService, lb, numberRetries, timeoutInMillis, onlineHosts);
    }

    private void setupSftpFileTransferServiceBase(final SFTPFileTransferService sftpFileTransferService, final ENMScriptingVMLoadBalancer lb,
                                                  final int numberRetries, final int timeoutInMillis, final List<String> onlineHosts)
                                                          throws JSchException, SftpException {
        lb.setScriptingVMs(onlineHosts);
        lb.getRandomOnlineScriptingVMs();
        sftpFileTransferService.setLoadBalancer(lb);
        sftpFileTransferService.setFileDownloadIntervalMillis(timeoutInMillis);
        sftpFileTransferService.setFileDownloadRetries(numberRetries);
        sftpFileTransferService.setInitialChannelSetupRetries(numberRetries);
        sftpFileTransferService.setBackoffPeriodInMillis(timeoutInMillis);

        final ChannelSftp channelSftp = setupMockCreateSFTPChannel(sftpFileTransferService);

        sftpFileTransferService.setChannelSftp(channelSftp);
        sftpFileTransferService.setRetriever(connectedSystemsRetrieverMock);
        sftpFileTransferService.setMetrics(metrics);
    }

}
