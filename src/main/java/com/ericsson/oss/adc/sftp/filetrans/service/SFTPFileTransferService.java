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
package com.ericsson.oss.adc.sftp.filetrans.service;

import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_FAILED_FILE_TRANSFER_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.PROCESSED_COUNTER_FILE_DATA_VOLUME_TOTAL;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ericsson.oss.adc.sftp.filetrans.model.ConnectionPropertiesModel;
import com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil;
import com.ericsson.oss.adc.sftp.filetrans.util.Utils;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

@Service
@Slf4j
public class SFTPFileTransferService {


    @Autowired
    ConnectedSystemsRetriever retriever;

    @Autowired
    ENMScriptingVMLoadBalancer loadBalancer;

    @Autowired
    SFTPProcessingMetricsUtil metrics;

    @Value("${sftp.retries}")
    private int initialChannelSetupRetries;
    @Value("${sftp.file-download-retries}")
    private int fileDownloadRetries;
    @Value("${sftp.file-download-interval-millis}")
    private int fileDownloadIntervalMillis;
    @Value("${sftp.session-timeout-in-millis}")
    private int sessionTimeout;
    @Value("${sftp.backoff-period-in-millis}")
    private int backoffPeriodInMillis;

    private ChannelSftp channelSftp;
    private JSch jSch;
    private int numberCreateChannelConnectionAttempts;
    private int numberCreateChannelConnectionRetriesAttemps;
    private int numberDownloadConnectionAttempts;
    private int numberDownloadConnectionRetriesAttemps;

    public boolean connectToSftpHost(final Optional<String> userName, final Optional<Integer> port, final Optional<String> password) {
        // channelSftp == null and channelSftp.isConnected() checked in disconnectChannelSftp
        this.disconnectChannelSftp(channelSftp);
        if (!getLoadBalancer().getAllOnlineScriptingVMs().isEmpty() && userName.isPresent() && port.isPresent() && password.isPresent()) {
            return createSFTPChannel(userName.get(), port.get(), password.get());
        }
        log.error("SFTP connection details are not valid, unable to make connection");
        return false;
    }

    public byte[] downloadFile(final String remoteFilePath) {
        InputStream inputStream = null;
        numberDownloadConnectionAttempts = 0;
        numberDownloadConnectionRetriesAttemps = 0;
        while (!getLoadBalancer().getAllOnlineScriptingVMs().isEmpty()) {
            final String host = getLoadBalancer().getCurrentConnectedScriptingVMs();
            numberDownloadConnectionAttempts++;
            if (!isValidHost(host)) {
                getLoadBalancer().removeVMFromOnlineScriptingVMs(host);
                if (!getLoadBalancer().getAllOnlineScriptingVMs().isEmpty()) {
                    getLoadBalancer().getRandomOnlineScriptingVMs();
                }
                continue;
            }
            for (int i = 1; i <= fileDownloadRetries; i++) {
                numberDownloadConnectionRetriesAttemps++;
                log.debug("Attempting to download file {}, {} of {} for host '{}' ", remoteFilePath, i, fileDownloadRetries, host);
                try {
                    inputStream = channelSftp.get(remoteFilePath);
                    log.debug("Successfully downloaded file {} ", remoteFilePath);
                    final byte[] inputBytes = IOUtils.toByteArray(inputStream);
                    final int numberBytesRead = inputBytes.length;
                    isValidBytesReadFromFiles(remoteFilePath, numberBytesRead);
                    getMetrics().incrementCounterByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL);
                    getMetrics().addToGaugeByName(PROCESSED_COUNTER_FILE_DATA_VOLUME_TOTAL, inputBytes.length);
                    getLoadBalancer().resetAllAvailableScriptingVMs();
                    return inputBytes;
                } catch (final SftpException | IOException exception) {
                    log.error("FAILED to download file '{}' ", remoteFilePath, exception);
                }
                log.warn("Download attempt {}/{} FAILED for file: '{}', waiting for {} milliseconds before retrying...", i, fileDownloadRetries,
                        remoteFilePath, fileDownloadIntervalMillis);
                Utils.waitRetryInterval(fileDownloadIntervalMillis);

            }
            log.info("Disconnecting from host '{}'", host);
            getLoadBalancer().removeVMFromOnlineScriptingVMs(host);
            disconnectChannel();
            if (!getLoadBalancer().getAllOnlineScriptingVMs().isEmpty()) {
                log.info("Will try connecting to another host from list '{}'", getLoadBalancer().getAllOnlineScriptingVMs());
                final ConnectionPropertiesModel connectionProperties = getConnectedSystemsRetriever().getConnectionPropertiesBySubsystemsName();
                connectToSftpHost(Optional.of(connectionProperties.getUsername()), Optional.of(Integer.valueOf(connectionProperties.getSftpPort())), Optional.of(connectionProperties.getPassword()));
            } else {
                log.error("Download FAILED (RETRIES EXHAUSTED) for file: {} ... file lost.", remoteFilePath);
            }
        }
        //Re-populate OnlineScriptingVMs now so that next try to SFTP-download will have a populated list to try.
        getLoadBalancer().resetAllAvailableScriptingVMs();
        getMetrics().incrementCounterByName(NUM_FAILED_FILE_TRANSFER_TOTAL);
        log.warn("FAILED to download file: '{}'. Returning an empty byte array (zero-length file)." , remoteFilePath);
        return new byte[] {};
    }

    boolean createSFTPChannel(final String username, final int port, final String password) {
        numberCreateChannelConnectionAttempts = 0;
        numberCreateChannelConnectionRetriesAttemps = 0;
        while (!getLoadBalancer().getAllOnlineScriptingVMs().isEmpty()) {
            final String host = getLoadBalancer().getRandomOnlineScriptingVMs();
            numberCreateChannelConnectionAttempts++;
            if (!isValidHost(host)) {
                break;
            }
            for (long i = 1; i <= initialChannelSetupRetries; i++) {
                numberCreateChannelConnectionRetriesAttemps++;
                log.debug("Attempt {}/{} to create SFTP channel for host {} ", i, initialChannelSetupRetries, host);
                try {
                    this.jSch = getJsch();
                    final Session session = this.jSch.getSession(username, host, port);
                    session.setConfig("StrictHostKeyChecking", "no");
                    session.setPassword(password);
                    session.connect(sessionTimeout);
                    session.setServerAliveInterval(sessionTimeout);

                    final Channel channel = session.openChannel("sftp");
                    channel.connect();
                    channelSftp = (ChannelSftp) channel;
                    return true;
                } catch (final Exception exception) {
                    log.error("FAILED to create SFTP channel with host '{}' ", host, exception);
                }
                log.error("FAILED to create SFTP Channel for host '{}' , Attempt {}/{}. Will attempt to recreate SFTP channel in {}", host, i,
                        initialChannelSetupRetries, backoffPeriodInMillis);
                Utils.waitRetryInterval(backoffPeriodInMillis);
            }
            log.info("Disconnecting from host '{}'.", host);
            getLoadBalancer().removeVMFromOnlineScriptingVMs(host);
            if (!getLoadBalancer().getAllOnlineScriptingVMs().isEmpty()) {
                log.info("Will try connecting to another host from list '{}'", getLoadBalancer().getAllOnlineScriptingVMs());
            }
        }
        log.error("FAILED to make SFTP connection");
        //Re-populate OnlineScriptingVMs now so that next try to createSFTPChannel will have a populated list to try.
        getLoadBalancer().resetAllAvailableScriptingVMs();
        return false;
    }

    public boolean isConnectionOpen() {
        return (this.channelSftp != null) && (this.channelSftp.isConnected());
    }

    public void disconnectChannel() {
        disconnectChannelSftp(channelSftp);
    }

    boolean disconnectChannelSftp(final ChannelSftp channelSftp) {
        if (channelSftp != null) {
            try {
                if (channelSftp.isConnected()) {
                    channelSftp.disconnect();
                }

                if (channelSftp.getSession() != null) {
                    channelSftp.getSession().disconnect();
                }
            } catch (final JSchException exception) {
                log.error("FAILED to disconnect SFTP channel correctly", exception);
                return false;
            }
        }
        return true;
    }

    boolean isValidHost(final String host) {
        if (host == null || host.length() == 0) {
            log.error("FAILED to create SFTP channel. Invalid host name for host {} ", host);
            return false;
        }
        return true;
    }

    boolean isValidBytesReadFromFiles(final String remoteFilePath, final int numberBytesRead) {
        if (numberBytesRead <= 0) {
            log.error(
                    "FAILED to read any bytes from file '{}'. Its either an empty file or there is a SFTP download problem. Number bytes read is '{}' ",
                    remoteFilePath, numberBytesRead);
            return false;
        }
        return true;
    }

    //Used in test from here onwards.
    public JSch getJsch(){
        if (this.jSch == null) {
            return new JSch();
        }
        return this.jSch;
    }

    public void setJsch(final JSch jSch) {
        this.jSch = jSch;
    }

    public ConnectedSystemsRetriever getConnectedSystemsRetriever() {
        return retriever;
    }

    public ENMScriptingVMLoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public SFTPProcessingMetricsUtil getMetrics() {
        return metrics;
    }

    public void setChannelSftp(final ChannelSftp channelSftp) {
        this.channelSftp = channelSftp;
    }

    public void setFileDownloadRetries(final int fileDownloadRetries) {
        this.fileDownloadRetries = fileDownloadRetries;
    }

    public void setFileDownloadIntervalMillis(final int fileDownloadIntervalMillis) {
        this.fileDownloadIntervalMillis = fileDownloadIntervalMillis;
    }

    public void setLoadBalancer(final ENMScriptingVMLoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public void setRetriever(final ConnectedSystemsRetriever retriever) {
        this.retriever = retriever;
    }

    public void setMetrics(final SFTPProcessingMetricsUtil metrics) {
        this.metrics = metrics;
    }

    public void setInitialChannelSetupRetries(final int initialChannelSetupRetries) {
        this.initialChannelSetupRetries = initialChannelSetupRetries;
    }

    public void setBackoffPeriodInMillis(final int backoffPeriodInMillis) {
        this.backoffPeriodInMillis = backoffPeriodInMillis;
    }

    public int getNumberCreateChannelConnectionAttempts() {
        return numberCreateChannelConnectionAttempts;
    }

    public int getNumberCreateChannelConnectionRetriesAttemps() {
        return numberCreateChannelConnectionRetriesAttemps;
    }

    public int getNumberDownloadConnectionAttempts() {
        return numberDownloadConnectionAttempts;
    }

    public int getNumberDownloadConnectionRetriesAttemps() {
        return numberDownloadConnectionRetriesAttemps;
    }

}