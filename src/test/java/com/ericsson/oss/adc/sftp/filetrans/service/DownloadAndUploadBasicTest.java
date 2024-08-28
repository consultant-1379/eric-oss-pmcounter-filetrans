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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The Class SFTPFileTransferServiceBasicTest.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DownloadAndUploadBasicTest {
    private static final String TEST_STRING_50 = "TEST_TEST_TEST_TEST_TEST_TEST_TEST_TEST_TEST_TEST_";
    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadAndUploadBasicTest.class);
    private static Gson gson = new Gson();
    private final ConnectedSystemsRetriever connectedSystemsRetrieverMock = mock(ConnectedSystemsRetriever.class);
    private final SFTPProcessingMetricsUtil metrics = new SFTPProcessingMetricsUtil(new SimpleMeterRegistry());
    private final int numConnectionAttemptsMax = 1;
    private final int retryIntervalMs = 1000;

    /**
     * Verify download and upload.
     *
     * @throws Exception the exception
     */
    @Test
    @DisplayName("Verify SFTP Download and BDR Upload")
    public void VerifyDownloadAndUpload() throws Exception {

        final int numberRetries = 1;
        final int timeoutInMillis = 1;
        final String remoteFilePath = "some/remote/path/to/file";
        final ENMScriptingVMLoadBalancer lb = new ENMScriptingVMLoadBalancer();
        final SFTPFileTransferService sftpFileTransferService = new SFTPFileTransferService();
        final byte[] inputBytes = TEST_STRING_50.getBytes();
        this.setupSftpFileTransferService(sftpFileTransferService, lb, numberRetries, timeoutInMillis, inputBytes);
        final byte[] objectBytes = sftpFileTransferService.downloadFile(remoteFilePath);

        assertEquals(objectBytes.length, TEST_STRING_50.getBytes().length);
        assertArrayEquals(objectBytes, TEST_STRING_50.getBytes());

        final BDRService bdrService = new BDRService(metrics, numConnectionAttemptsMax, retryIntervalMs);
        final MinioClient minioClient = mock(MinioClient.class);
        final String bucketName = "enm1";
        final String objectName = "XML";
        final ObjectWriteResponse objectInfo = new ObjectWriteResponse(null, bucketName, "region", objectName, "etag", "versionId");
        when(minioClient.putObject(Mockito.any(PutObjectArgs.class))).thenReturn(objectInfo);
        ReflectionTestUtils.setField(bdrService, "minioClient", minioClient);
        final boolean isUploaded = bdrService.uploadObject(bucketName, objectName, objectBytes);
        assertTrue(isUploaded);
    }

    private ChannelSftp setupMockCreateSFTPChannel(final SFTPFileTransferService sftpFileTransferService, final byte[] inputBytes)
            throws JSchException, SftpException {
        final ChannelSftp channelSftp = mock(ChannelSftp.class);
        final JSch jsch = mock(JSch.class);
        final Session session = mock(Session.class);

        sftpFileTransferService.setJsch(jsch);
        Mockito.when(jsch.getSession(anyString(), anyString(), Mockito.anyInt())).thenReturn(session);
        Mockito.when(session.openChannel(anyString())).thenReturn(channelSftp);
        Mockito.when(channelSftp.getSession()).thenReturn(session);
        final InputStream inputStream = new ByteArrayInputStream(inputBytes);
        Mockito.when(channelSftp.get(anyString())).thenReturn(inputStream);
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
                                              final int numberRetries, final int timeoutInMillis, final byte[] inputBytes)
                                                      throws JSchException, SftpException, FileNotFoundException {
        final ConnectionPropertiesModel connectionPropertiesModel = setupMockConnectedSystems();
        final List<String> onlineHosts = connectionPropertiesModel.getScriptingVMs();
        LOGGER.info("onlineHosts = {}", onlineHosts);

        setupSftpFileTransferServiceBase(sftpFileTransferService, lb, numberRetries, timeoutInMillis, onlineHosts, inputBytes);
    }


    private void setupSftpFileTransferServiceBase(final SFTPFileTransferService sftpFileTransferService, final ENMScriptingVMLoadBalancer lb,
                                                  final int numberRetries, final int timeoutInMillis, final List<String> onlineHosts,
                                                  final byte[] inputBytes)
                                                          throws JSchException, SftpException {
        lb.setScriptingVMs(onlineHosts);
        lb.getRandomOnlineScriptingVMs();
        sftpFileTransferService.setLoadBalancer(lb);
        sftpFileTransferService.setFileDownloadIntervalMillis(timeoutInMillis);
        sftpFileTransferService.setFileDownloadRetries(numberRetries);
        sftpFileTransferService.setInitialChannelSetupRetries(numberRetries);
        sftpFileTransferService.setBackoffPeriodInMillis(timeoutInMillis);

        final ChannelSftp channelSftp = setupMockCreateSFTPChannel(sftpFileTransferService, inputBytes);

        sftpFileTransferService.setChannelSftp(channelSftp);
        sftpFileTransferService.setRetriever(connectedSystemsRetrieverMock);
        sftpFileTransferService.setMetrics(metrics);
    }
}
