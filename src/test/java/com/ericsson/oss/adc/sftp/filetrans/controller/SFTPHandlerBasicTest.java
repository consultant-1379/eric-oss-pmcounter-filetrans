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
package com.ericsson.oss.adc.sftp.filetrans.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

import com.ericsson.oss.adc.sftp.filetrans.model.InputMessage;
import com.ericsson.oss.adc.sftp.filetrans.service.SFTPFileTransferService;
import com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil;

import com.ericsson.oss.adc.sftp.filetrans.util.StartupUtil;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * The Class SFTPHandlerBasicTest.
 * This class is used for testing unit logic in the SFTPHandler class that does not need the spring framework.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SFTPHandlerBasicTest {
    private static final String TEST_STRING_50 = "TEST_TEST_TEST_TEST_TEST_TEST_TEST_TEST_TEST_TEST_";
    private static final String NODE_NAME = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR102gNodeBRadio00010,ManagedElement=NR102gNodeBRadio00010";
    private static final String ENM_FILE_PATH = "/ericsson/pmic1/XML/SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR102gNodeBRadio00010,ManagedElement=NR102gNodeBRadio00010/";
    private static final String FILE_NAME = "A20200721.1000+0100-1015+0100_SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR102gNodeBRadio00010,ManagedElement=NR102gNodeBRadio00010_statsfile.1.xml.gz";
    private static final String NODE_TYPE = "RadioNode";
    private static final String DATA_TYPE = "4G";
    private static final String FILE_TYPE = "XML";
    private static final String SUBSYSTEM_NAME = "EnM4";
    private static final String VALIDATED_BUCKET_NAME = "enm4";

    private final SFTPProcessingMetricsUtil metrics = new SFTPProcessingMetricsUtil(new SimpleMeterRegistry());
    private final SFTPFileTransferService sftpFileTransferService = mock(SFTPFileTransferService.class);
    private final BDRComponent bdrComponent = mock(BDRComponent.class);
    private final StartupUtil startupUtil = mock(StartupUtil.class);

    /**
     * Verify SFTP handler download file and persist to BD R null bytes.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @Order(1)
    @DisplayName("Verify SFTP handler downloadFileAndPersistToBDR fails when input byte array is null.")
    public void verifySFTPhandlerDownloadFileAndPersistToBDR_nullBytes() throws Exception {
        when(sftpFileTransferService.downloadFile(anyString())).thenReturn(null);
        final boolean isBDRUploadSuccessful = doDownloadAndUploadToBdr();
        assertFalse(isBDRUploadSuccessful);
    }

    /**
     * Verify SFTP handler download file and persist to BD R zero bytes.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @Order(2)
    @DisplayName("Verify SFTP handler downloadFileAndPersistToBDR fails when input byte array is empty.")
    public void verifySFTPhandlerDownloadFileAndPersistToBDR_ZeroBytes() throws Exception {
        when(sftpFileTransferService.downloadFile(anyString())).thenReturn(new byte[] {});
        final boolean isBDRUploadSuccessful = doDownloadAndUploadToBdr();
        assertFalse(isBDRUploadSuccessful);
    }

    /**
     * Verify SFTP handler download file and persist to BD R exception thrown.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @Order(3)
    @DisplayName("Verify SFTP handler downloadFileAndPersistToBDR fails when sftpFileTransferService.downloadFile throws exception.")
    public void verifySFTPhandlerDownloadFileAndPersistToBDR_exceptionThrown() throws Exception {
        when(sftpFileTransferService.downloadFile(anyString()))
        .thenThrow(new RuntimeException("TEST-VerifySFTPhandlerDownloadFileAndPersistToBDR_exceptionThrown"));
        final boolean isBDRUploadSuccessful = doDownloadAndUploadToBdr();
        assertFalse(isBDRUploadSuccessful);
    }


    /**
     * Verify SFTP handler download file and persist to BD R good download bad upload.
     *
     * @throws Exception
     *             the exception
     */
    @Test
    @Order(4)
    @DisplayName("Verify SFTP handler downloadFileAndPersistToBDR fails with successful download but failed to upload")
    public void verifySFTPhandlerDownloadFileAndPersistToBDR_goodDownloadBadUpload() throws Exception {
        when(sftpFileTransferService.downloadFile(anyString())).thenReturn(TEST_STRING_50.getBytes());
        when(bdrComponent.uploadObject(anyString(), anyString(), Mockito.any(byte[].class))).thenReturn(false);
        final boolean isBDRUploadSuccessful = doDownloadAndUploadToBdr();
        assertFalse(isBDRUploadSuccessful);
    }

    /**
     * Verify SFTP handler download file and persist to BDR happy case.
     *
     */
    @Test
    @Order(5)
    @DisplayName("Verify SFTP handler downloadFileAndPersistToBDR. 'Happy Case'")
    public void verifySFTPHandlerDownloadFileAndPersistToBDR_happyCase() {
        when(sftpFileTransferService.downloadFile(anyString())).thenReturn(TEST_STRING_50.getBytes());
        when(bdrComponent.uploadObject(anyString(), anyString(), Mockito.any(byte[].class))).thenReturn(true);
        final boolean isBDRUploadSuccessful = doDownloadAndUploadToBdr();
        assertTrue(isBDRUploadSuccessful);
        verify(startupUtil, times(1)).getCreatedBucketName();
    }

    private boolean doDownloadAndUploadToBdr() {
        when(startupUtil.getCreatedBucketName()).thenReturn(VALIDATED_BUCKET_NAME);
        final InputMessage InputMessage = com.ericsson.oss.adc.sftp.filetrans.model.InputMessage.builder()
                .nodeName(NODE_NAME)
                .fileLocation(ENM_FILE_PATH + FILE_NAME)
                .nodeType(NODE_TYPE)
                .dataType(DATA_TYPE)
                .fileType(FILE_TYPE)
                .build();
        final SFTPHandler sftpHandler = new SFTPHandler(sftpFileTransferService, bdrComponent, metrics, SUBSYSTEM_NAME, startupUtil);
        final boolean isBDRUploadSuccessful = sftpHandler.downloadFileAndPersistToBDR(InputMessage);
        return isBDRUploadSuccessful;
    }
}
