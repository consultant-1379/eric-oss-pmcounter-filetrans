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
package com.ericsson.oss.adc.sftp.filetrans.controller;

import com.ericsson.oss.adc.sftp.filetrans.model.ConnectionPropertiesModel;
import com.ericsson.oss.adc.sftp.filetrans.model.InputMessage;
import com.ericsson.oss.adc.sftp.filetrans.model.SubsystemModel;
import com.ericsson.oss.adc.sftp.filetrans.service.ConnectedSystemsRetriever;
import com.ericsson.oss.adc.sftp.filetrans.service.SFTPFileTransferService;
import com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil;
import com.ericsson.oss.adc.sftp.filetrans.util.StartupUtil;
import com.ericsson.oss.adc.sftp.filetrans.util.Utils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Component
@Getter
@Slf4j
public class SFTPHandler {

    @Autowired
    private SFTPFileTransferService fileTransferService;

    @Autowired
    BDRComponent bdrComponent;

    @Autowired
    ConnectedSystemsRetriever connectedSystemsRetriever;

    @Autowired
    SFTPProcessingMetricsUtil metrics;

    @Autowired
    StartupUtil startupUtil;

    @Value("${subsystem.name}")
    private String subsystemName;


    @Autowired
    public SFTPHandler() {
        //No Args Constructor for Spring.
    }

    /**
     * Constructor for Unit test only.
     */
    public SFTPHandler(final SFTPFileTransferService fileTransferService, final BDRComponent bdrComponent, final SFTPProcessingMetricsUtil metrics,
                       final String subsystemName, final StartupUtil startupUtil) {
        super();
        this.fileTransferService = fileTransferService;
        this.bdrComponent = bdrComponent;
        this.metrics = metrics;
        this.subsystemName = subsystemName;
        this.startupUtil = startupUtil;
    }

    public ConnectionPropertiesModel getConnectionDetailsToSFTPServerUsingConnectedSystemsResponse() {
        final Map<String, SubsystemModel> subsystemsDetailsMap = connectedSystemsRetriever.getSubsystemDetails();
        return getConnectionProperties(subsystemsDetailsMap);
    }

    ConnectionPropertiesModel getConnectionProperties(final Map<String, SubsystemModel> subsystemsDetailsMap) {
        if (isSubsystemsMapPopulated(subsystemsDetailsMap) && subsystemsDetailsMap.get(subsystemName) != null && isConnectionPropertiesValid(subsystemsDetailsMap)) {
            return subsystemsDetailsMap.get(subsystemName).getConnectionProperties().get(0);
        }
        log.warn("UNABLE to obtain valid Connection Properties.");
        return ConnectionPropertiesModel.builder().build();
    }

    private boolean isSubsystemsMapPopulated(final Map<String, SubsystemModel> subsystemsDetailsMap) {
        return subsystemsDetailsMap != null && !subsystemsDetailsMap.isEmpty();
    }


    private boolean isConnectionPropertiesValid(final Map<String, SubsystemModel> subsystemsDetailsMap) {
        return subsystemsDetailsMap.get(subsystemName).getConnectionProperties() != null && !subsystemsDetailsMap.get(subsystemName).getConnectionProperties().isEmpty();
    }

    public boolean setUpConnectionToSFTPServerUsingConnectedSystemsResponse() {
        final ConnectionPropertiesModel connectionProperties = getConnectionDetailsToSFTPServerUsingConnectedSystemsResponse();
        if (Utils.isValidConnectionProperties(connectionProperties)) {
            return connectToEnm(connectionProperties.getUsername(), Integer.valueOf(connectionProperties.getSftpPort()), connectionProperties.getPassword());
        }
        return false;
    }

    public boolean connectToEnm(final String username, final int port, final String password) {
        return fileTransferService.connectToSftpHost(Optional.of(username), Optional.of(port), Optional.of(password));
    }

    public boolean process(final InputMessage inputMessage) {
        if (!isConnectionOpen()) {
            if (setUpConnectionToSFTPServerUsingConnectedSystemsResponse()) {
                return downloadFileAndPersistToBDR(inputMessage);
            }
        } else {
            return downloadFileAndPersistToBDR(inputMessage);
        }
        return false;
    }

    public boolean downloadFileAndPersistToBDR(final InputMessage inputMessage) {
        boolean isBDRUploadSuccessful = false;
        final long downloadStartTime = System.currentTimeMillis();
        final List<String> remoteFilePathList = Arrays.asList(inputMessage.getFileLocation());
        int noSucccessfulFilesDownloaded = 0;
        for (final String remoteFilepath : remoteFilePathList) {
            try {
                long sftpDownloadStartTime = System.currentTimeMillis();
                final byte[] inputBytes = fileTransferService.downloadFile(remoteFilepath);
                long sftpDownloadEndTime = System.currentTimeMillis();

                metrics.addToGaugeByName(SFTPProcessingMetricsUtil.BATCH_SFTP_DOWNLOAD_TIME_TOTAL, sftpDownloadEndTime - sftpDownloadStartTime);

                if (inputBytes != null && inputBytes.length > 0) {
                    final String validatedBucketName = startupUtil.getCreatedBucketName();

                    long bdrUploadStartTime = System.currentTimeMillis();
                    isBDRUploadSuccessful = bdrComponent.uploadObject(validatedBucketName, remoteFilepath, inputBytes);
                    long bdrUploadEndTime = System.currentTimeMillis();
                    metrics.addToGaugeByName(SFTPProcessingMetricsUtil.BATCH_BDR_UPLOAD_TIME_TOTAL, bdrUploadEndTime - bdrUploadStartTime);
                    if (isBDRUploadSuccessful) {
                        noSucccessfulFilesDownloaded++;
                    }
                }
            } catch (final Exception exception) {
                log.error("ERROR while downloading/persisting file to BDR: '{}'.", remoteFilepath, exception);
            }
        }
        final long timeTakenForFileDownload = System.currentTimeMillis() - downloadStartTime;
        final String msg = String.format(
                "Downloaded and uploaded to BDR %s/%s files for node %s at time %s. Processing time for file: %s ms",
                noSucccessfulFilesDownloaded,
                remoteFilePathList.size(), inputMessage.getNodeName(), LocalDateTime.now(), timeTakenForFileDownload);
        log.debug(msg);
        return isBDRUploadSuccessful;
    }

    public boolean isConnectionOpen() {
        return fileTransferService.isConnectionOpen();
    }
}
