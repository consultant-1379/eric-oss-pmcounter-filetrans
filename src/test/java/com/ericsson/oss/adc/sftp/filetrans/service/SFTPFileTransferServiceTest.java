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

import static com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.FileReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.util.ReflectionTestUtils;

import com.ericsson.oss.adc.sftp.filetrans.model.SubsystemModel;
import com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

@SpringBootTest
@EmbeddedKafka
public class SFTPFileTransferServiceTest {

    private static final int PORT = 1234;
    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String ENM_FILE_PATH = "/ericsson/pmic1/XML/SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR102gNodeBRadio00010,ManagedElement=NR102gNodeBRadio00010/";
    private static final String FILE_NAME = "A20200721.1000+0100-1015+0100_SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR102gNodeBRadio00010,ManagedElement=NR102gNodeBRadio00010_statsfile.xml.gz";

    @Mock
    private JSch jsch;

    @Mock
    private Session session;

    @Mock
    private ChannelSftp channel;

    @Mock
    private ConnectedSystemsRetriever connectedSystemsRetrieverMock;

    private static Gson gson = new Gson();

    @Mock
    private SFTPFileTransferService sftpFileTransferService;

    @Mock
    ENMScriptingVMLoadBalancer loadBalancerMock;

    @Inject
    private SFTPProcessingMetricsUtil metrics;

    @Test
    @Order(1)
    @DisplayName("Verify that a host is tried up to 3 times when connection failure")
    public void VerifyThatAHostIsRetriedUpToThreeTimesWhenFailedConnection() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            server.putFile(ENM_FILE_PATH + FILE_NAME, "Content of file", UTF_8);

            ReflectionTestUtils.setField(sftpFileTransferService, "initialChannelSetupRetries", 3);
            Mockito.when(sftpFileTransferService.getConnectedSystemsRetriever()).thenReturn(connectedSystemsRetrieverMock);
            Mockito.when(sftpFileTransferService.getLoadBalancer()).thenReturn(loadBalancerMock);
            Mockito.when(sftpFileTransferService.getJsch()).thenReturn(jsch);
            Mockito.when(jsch.getSession(anyString(), anyString(), Mockito.anyInt())).thenReturn(session);
            Mockito.when(session.openChannel(anyString())).thenReturn(channel);
            Mockito.when(channel.getSession()).thenReturn(session);
            Mockito.doThrow(JSchException.class)
            .when(channel)
            .connect();
            Mockito.when(loadBalancerMock.getAllOnlineScriptingVMs()).thenReturn(Arrays.asList("localhost")).thenReturn(Arrays.asList("localhost")).thenReturn(Collections.EMPTY_LIST);
            Mockito.when(loadBalancerMock.getCurrentConnectedScriptingVMs()).thenReturn("localhost");
            Mockito.when(loadBalancerMock.getRandomOnlineScriptingVMs()).thenReturn("localhost").thenReturn(null);
            Mockito.when(sftpFileTransferService.connectToSftpHost(Optional.of(USER), Optional.of(PORT), Optional.of(PASSWORD))).thenCallRealMethod();
            Mockito.when(sftpFileTransferService.createSFTPChannel(USER, PORT, PASSWORD)).thenCallRealMethod();
            Mockito.when(sftpFileTransferService.isValidBytesReadFromFiles(anyString(), Mockito.anyInt())).thenCallRealMethod();
            Mockito.when(sftpFileTransferService.isValidHost(anyString())).thenCallRealMethod();

            assertFalse(sftpFileTransferService.connectToSftpHost(Optional.of(USER), Optional.of(PORT), Optional.of(PASSWORD)));
            Mockito.verify(channel, Mockito.atLeast(3)).connect();
        });
    }

    @Test
    @Order(2)
    @DisplayName("Verify that all hosts are tried up to 3 times when connection failure")
    public void VerifyThatAllHostsAreRetriedUpToThreeTimesWhenFailedConnection() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            server.putFile(ENM_FILE_PATH + FILE_NAME, "Content of file", UTF_8);

            ReflectionTestUtils.setField(sftpFileTransferService, "initialChannelSetupRetries", 3);
            Mockito.when(sftpFileTransferService.getConnectedSystemsRetriever()).thenReturn(connectedSystemsRetrieverMock);
            Mockito.when(sftpFileTransferService.getLoadBalancer()).thenReturn(loadBalancerMock);
            Mockito.when(sftpFileTransferService.getJsch()).thenReturn(jsch);
            Mockito.when(jsch.getSession(anyString(), anyString(), Mockito.anyInt())).thenReturn(session);
            Mockito.when(session.openChannel(anyString())).thenReturn(channel);
            Mockito.when(channel.getSession()).thenReturn(session);
            Mockito.doThrow(JSchException.class)
            .when(channel)
            .connect();
            /*Mockito.when(loadBalancerMock.getAllOnlineScriptingVMs()).thenReturn(Arrays.asList("localhost", "localhost"))
            .thenReturn(Arrays.asList("localhost", "localhost")).thenReturn(Arrays.asList("localhost"))
            .thenReturn(Arrays.asList("localhost")).thenReturn(Collections.EMPTY_LIST);*/
            mockGetAllOnlineScriptingVms();
            Mockito.when(loadBalancerMock.getCurrentConnectedScriptingVMs()).thenReturn("localhost").thenReturn("localhost");
            Mockito.when(loadBalancerMock.getRandomOnlineScriptingVMs()).thenReturn("localhost").thenReturn("localhost").thenReturn(null);
            Mockito.when(sftpFileTransferService.connectToSftpHost(Optional.of(USER), Optional.of(PORT), Optional.of(PASSWORD))).thenCallRealMethod();
            Mockito.when(sftpFileTransferService.createSFTPChannel(USER, PORT, PASSWORD)).thenCallRealMethod();
            Mockito.when(sftpFileTransferService.isValidBytesReadFromFiles(anyString(), Mockito.anyInt())).thenCallRealMethod();
            Mockito.when(sftpFileTransferService.isValidHost(anyString())).thenCallRealMethod();

            assertFalse(sftpFileTransferService.connectToSftpHost(Optional.of(USER), Optional.of(PORT), Optional.of(PASSWORD)));
            Mockito.verify(channel, Mockito.atLeast(6)).connect();
        });
    }

    @Test
    @Order(3)
    @DisplayName("Verify that all hosts are tried up to 3 times until successful connection")
    public void VerifyThatAllHostsAreRetriedUpToThreeTimesUntilSuccessfulConnectionMade() throws Exception {
        withSftpServer(server -> {
            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            server.putFile(ENM_FILE_PATH + FILE_NAME, "Content of file", UTF_8);

            ReflectionTestUtils.setField(sftpFileTransferService, "initialChannelSetupRetries", 3);
            Mockito.when(sftpFileTransferService.getConnectedSystemsRetriever()).thenReturn(connectedSystemsRetrieverMock);
            Mockito.when(sftpFileTransferService.getLoadBalancer()).thenReturn(loadBalancerMock);
            Mockito.when(sftpFileTransferService.getJsch()).thenReturn(jsch);
            Mockito.when(jsch.getSession(anyString(), anyString(), Mockito.anyInt())).thenReturn(session);
            Mockito.when(session.openChannel( anyString())).thenReturn(channel);
            Mockito.when(channel.getSession()).thenReturn(session);
            Mockito.doThrow(JSchException.class, JSchException.class, JSchException.class, JSchException.class, JSchException.class, JSchException.class).doCallRealMethod()
            .when(channel)
            .connect();
            mockGetAllOnlineScriptingVms();
            Mockito.when(loadBalancerMock.getCurrentConnectedScriptingVMs()).thenReturn("localhost").thenReturn("localhost").thenReturn("localhost");
            Mockito.when(loadBalancerMock.getRandomOnlineScriptingVMs()).thenReturn("localhost").thenReturn("localhost").thenReturn("localhost").thenReturn(null);
            Mockito.when(sftpFileTransferService.connectToSftpHost(Optional.of(USER), Optional.of(PORT), Optional.of(PASSWORD))).thenCallRealMethod();
            Mockito.when(sftpFileTransferService.createSFTPChannel(USER, PORT, PASSWORD)).thenCallRealMethod();
            Mockito.when(sftpFileTransferService.isValidBytesReadFromFiles(anyString(), Mockito.anyInt())).thenCallRealMethod();
            Mockito.when(sftpFileTransferService.isValidHost(anyString())).thenCallRealMethod();

            assertTrue(sftpFileTransferService.connectToSftpHost(Optional.of(USER), Optional.of(PORT), Optional.of(PASSWORD)));
            Mockito.verify(channel, Mockito.atLeast(7)).connect();
        });
    }

    @Test
    @Order(4)
    @DisplayName("Verify that a host is tried up to 3 times when file download fails")
    public void VerifyThatAHostIsRetriedUpToThreeTimesWhenFileDownloadFails() throws Exception {
        withSftpServer(server -> {
            final Map<String, SubsystemModel> subsystemsByNameMap =  new HashMap<>();
            final List<SubsystemModel> subsystemList = Arrays.asList(gson.fromJson(new JsonReader(new FileReader(getClass().getClassLoader().getResource("GetSFTPSubsystemsResponse.json").getFile())), SubsystemModel[].class));
            for (final SubsystemModel subsystem: subsystemList) {
                subsystemsByNameMap.put(subsystem.getName(), subsystem);
            }
            Mockito.when(connectedSystemsRetrieverMock.getConnectionPropertiesBySubsystemsName()).thenReturn(subsystemsByNameMap.get("enm1").getConnectionProperties().get(0));

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            server.putFile(ENM_FILE_PATH + FILE_NAME, "Content of file", UTF_8);

            ReflectionTestUtils.setField(sftpFileTransferService, "initialChannelSetupRetries", 3);
            ReflectionTestUtils.setField(sftpFileTransferService, "fileDownloadRetries", 3);
            Mockito.when(sftpFileTransferService.getConnectedSystemsRetriever()).thenReturn(connectedSystemsRetrieverMock);
            Mockito.when(sftpFileTransferService.getMetrics()).thenReturn(metrics);
            Mockito.when(sftpFileTransferService.getLoadBalancer()).thenReturn(loadBalancerMock);
            Mockito.when(sftpFileTransferService.getJsch()).thenReturn(jsch);
            Mockito.when(jsch.getSession(anyString(), anyString(), Mockito.anyInt())).thenReturn(session);
            Mockito.when(session.openChannel(anyString())).thenReturn(channel);
            Mockito.when(channel.getSession()).thenReturn(session);
            Mockito.doThrow(SftpException.class)
            .when(channel)
            .get(anyString());
            Mockito.when(loadBalancerMock.getAllOnlineScriptingVMs()).thenReturn(Arrays.asList("localhost")).thenReturn(Arrays.asList("localhost")).thenReturn(Arrays.asList("localhost")).thenReturn(Collections.EMPTY_LIST);
            Mockito.when(loadBalancerMock.getCurrentConnectedScriptingVMs()).thenReturn("localhost").thenReturn("localhost");
            Mockito.when(loadBalancerMock.getRandomOnlineScriptingVMs()).thenReturn("localhost").thenReturn(null);
            Mockito.when(sftpFileTransferService.connectToSftpHost(Optional.of(USER), Optional.of(PORT), Optional.of(PASSWORD))).thenCallRealMethod();
            Mockito.when(sftpFileTransferService.createSFTPChannel(USER, PORT, PASSWORD)).thenCallRealMethod();
            Mockito.when(sftpFileTransferService.downloadFile(ENM_FILE_PATH + FILE_NAME)).thenCallRealMethod();
            Mockito.when(sftpFileTransferService.isValidBytesReadFromFiles(anyString(), Mockito.anyInt())).thenCallRealMethod();
            Mockito.when(sftpFileTransferService.isValidHost(anyString())).thenCallRealMethod();

            sftpFileTransferService.connectToSftpHost(Optional.of(USER), Optional.of(PORT), Optional.of(PASSWORD));
            sftpFileTransferService.downloadFile(ENM_FILE_PATH + FILE_NAME);
            Mockito.verify(channel, Mockito.atLeast(3)).get(anyString());
        });
    }

    @Test
    @Order(5)
    @DisplayName("Verify that all hosts are tried up to 3 times when file download fails")
    public void VerifyThatAllHostsAreRetriedUpToThreeTimesWhenFileDownloadFails() throws Exception {
        withSftpServer(server -> {
            final Map<String, SubsystemModel> subsystemsByNameMap =  new HashMap<>();
            final List<SubsystemModel> subsystemList = Arrays.asList(gson.fromJson(new JsonReader(new FileReader(getClass().getClassLoader().getResource("GetSFTPSubsystemsResponse.json").getFile())), SubsystemModel[].class));
            for (final SubsystemModel subsystem: subsystemList) {
                subsystemsByNameMap.put(subsystem.getName(), subsystem);
            }
            Mockito.when(connectedSystemsRetrieverMock.getConnectionPropertiesBySubsystemsName()).thenReturn(subsystemsByNameMap.get("enm1").getConnectionProperties().get(0));

            server.deleteAllFilesAndDirectories();
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);
            server.putFile(ENM_FILE_PATH + FILE_NAME, "Content of file", UTF_8);

            ReflectionTestUtils.setField(sftpFileTransferService, "initialChannelSetupRetries", 3);
            ReflectionTestUtils.setField(sftpFileTransferService, "fileDownloadRetries", 3);
            Mockito.when(sftpFileTransferService.getConnectedSystemsRetriever()).thenReturn(connectedSystemsRetrieverMock);
            Mockito.when(sftpFileTransferService.getMetrics()).thenReturn(metrics);
            Mockito.when(sftpFileTransferService.getLoadBalancer()).thenReturn(loadBalancerMock);
            Mockito.when(sftpFileTransferService.getJsch()).thenReturn(jsch);
            Mockito.when(jsch.getSession(anyString(), anyString(), Mockito.anyInt())).thenReturn(session);
            Mockito.when(session.openChannel(anyString())).thenReturn(channel);
            Mockito.when(channel.getSession()).thenReturn(session);
            Mockito.doThrow(SftpException.class)
            .when(channel)
            .get(anyString());
            Mockito.when(loadBalancerMock.getAllOnlineScriptingVMs()).thenReturn(Arrays.asList("localhost", "localhost"))
            .thenReturn(Arrays.asList("localhost", "localhost")).thenReturn(Arrays.asList("localhost"))
            .thenReturn(Arrays.asList("localhost")).thenReturn(Collections.EMPTY_LIST);
            Mockito.when(loadBalancerMock.getCurrentConnectedScriptingVMs()).thenReturn("localhost").thenReturn("localhost").thenReturn("localhost").thenReturn("localhost");
            Mockito.when(loadBalancerMock.getRandomOnlineScriptingVMs()).thenReturn("localhost").thenReturn("localhost").thenReturn(null);
            Mockito.when(sftpFileTransferService.connectToSftpHost(Optional.of(USER), Optional.of(PORT), Optional.of(PASSWORD))).thenCallRealMethod();
            Mockito.when(sftpFileTransferService.createSFTPChannel(USER, PORT, PASSWORD)).thenCallRealMethod();
            Mockito.when(sftpFileTransferService.downloadFile(ENM_FILE_PATH + FILE_NAME)).thenCallRealMethod();
            Mockito.when(sftpFileTransferService.isValidBytesReadFromFiles(anyString(), Mockito.anyInt())).thenCallRealMethod();
            Mockito.when(sftpFileTransferService.isValidHost(anyString())).thenCallRealMethod();

            sftpFileTransferService.disconnectChannel();
            assertFalse(sftpFileTransferService.isConnectionOpen());

            assertTrue(sftpFileTransferService.connectToSftpHost(Optional.of(USER), Optional.of(PORT), Optional.of(PASSWORD)));
            sftpFileTransferService.downloadFile(ENM_FILE_PATH + FILE_NAME);
            Mockito.verify(channel, Mockito.atLeast(3)).get(anyString());

            assertFalse(sftpFileTransferService.connectToSftpHost(null, null, null));
            assertFalse(sftpFileTransferService.isConnectionOpen());
        });
    }


    private void mockGetAllOnlineScriptingVms() {
        Mockito.when(loadBalancerMock.getAllOnlineScriptingVMs())
        .thenReturn(Arrays.asList("localhost", "localhost", "localhost"))
        .thenReturn(Arrays.asList("localhost", "localhost", "localhost"))
        .thenReturn(Arrays.asList("localhost", "localhost", "localhost"))
        .thenReturn(Arrays.asList("localhost", "localhost"))
        .thenReturn(Arrays.asList("localhost", "localhost"))
        .thenReturn(Arrays.asList("localhost", "localhost"))
        .thenReturn(Arrays.asList("localhost"))
        .thenReturn(Arrays.asList("localhost"))
        .thenReturn(Arrays.asList("localhost"))
        .thenReturn(Collections.EMPTY_LIST);
    }
}