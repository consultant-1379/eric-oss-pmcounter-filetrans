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
package com.ericsson.oss.adc.sftp.filetrans;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.ericsson.oss.adc.sftp.filetrans.availability.*;
import com.ericsson.oss.adc.sftp.filetrans.kafka.KafkaBootstrapSupplier;
import com.ericsson.oss.adc.sftp.filetrans.model.MessageBusModel;
import org.junit.Before;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import com.ericsson.oss.adc.sftp.filetrans.controller.OutputTopicController;
import com.ericsson.oss.adc.sftp.filetrans.service.ConnectedSystemsRetriever;
import com.ericsson.oss.adc.sftp.filetrans.util.StartupUtil;

import java.util.ArrayList;
import java.util.Collections;

/**
 * The Class StartupTest.
 *
 * The purpose of this test is to test the logic of the 'Startup' class rather than check the individual components.
 * THere are other tests for the relevant components, and there are integration test that covers all the logic.
 */
@ExtendWith(MockitoExtension.class)
class StartupTest {

    @InjectMocks
    Startup startup = new Startup();

    @Mock
    ConnectedSystemsRetriever connectedSystemsRetriever;

    @Mock
    OutputTopicController outputTopicController;

    @Mock
    StartupUtil startupUtil;

    @Mock
    StartKafka startKafka;

    @Mock
    DependentServiceAvailabilitySftpServer dependentServiceAvailabilitySftpServer;

    @Mock
    DependentServiceAvailabilityDataCatalog dependentServiceAvailabilityDataCatalog;

    @Mock
    DependentServiceAvailabilityBDR dependentServiceAvailabilityBDR;

    @Mock
    DependentServiceAvailabilityConnectedSystems dependentServiceAvailabilityConnectedSystems;

    @Mock
    DependentServiceAvailabilityKafka dependentServiceAvailabilityKafka;

    @Mock
    KafkaBootstrapSupplier kafkaBootstrapSupplier;

    @Mock
    Environment environment;

    MessageBusModel messageBus = MessageBusModel.builder()
            .id(1L)
            .name("name")
            .clusterName("clusterName")
            .nameSpace("nameSpace")
            .accessEndpoints(new ArrayList<>(Collections.singletonList("http://endpoint1:1234/")))
            .notificationTopicIds(new ArrayList<>(Collections.singletonList(1L)))
            .messageStatusTopicIds(new ArrayList<>(Collections.singletonList(1L)))
            .messageDataTopicIds(new ArrayList<>(Collections.singletonList(1L)))
            .build();

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("TEST: Verify Startup returns false Data Catalog Health Check:false")
    void test_startupWithDmmEnabledAndDataCatalogHealthCheckFails() throws UnsatisfiedExternalDependencyException {
        when(dependentServiceAvailabilityDataCatalog.checkHealth()).thenReturn(false);
        assertFalse("Expected Startup to return false when Data Catalog Health Check:false", startup.startAndConfigureExternalComponents());
        Mockito.verify(startKafka, times(0)).startKafkaListener(Mockito.anyString());
    }

    @Test
    @DisplayName("TEST: Verify Startup returns false when Data Catalog Health Check:true and BDR Health Check False")
    void test_startupWithDmmEnabledDataCatalogHealthCheckTrueBDRHealthCheckFails() throws UnsatisfiedExternalDependencyException {
        when(dependentServiceAvailabilityDataCatalog.checkHealth()).thenReturn(true);
        when(dependentServiceAvailabilityBDR.checkHealth()).thenReturn(false);
        assertFalse("Expected Startup to return false when Data Catalog Health Check:true, BDR Health Check false", startup.startAndConfigureExternalComponents());
        Mockito.verify(startupUtil, times(0)).setupBDRBucketUsingDataCatalog();
        Mockito.verify(startKafka, times(0)).startKafkaListener(Mockito.anyString());
    }

    @Test
    @DisplayName("TEST: Verify Startup returns false when Data Catalog Health Check:true and BDR Health Check True, BDR Bucket Creation Fails")
    void test_startupWithDmmEnabledDataCatalogHealthCheckTrueBDRHealthCheckTrueBDRBucketFails() throws UnsatisfiedExternalDependencyException {
        when(dependentServiceAvailabilityDataCatalog.checkHealth()).thenReturn(true);
        when(dependentServiceAvailabilityBDR.checkHealth()).thenReturn(true);
        when(startupUtil.setupBDRBucketUsingDataCatalog()).thenReturn(false);
        assertFalse("Expected Startup to return false when Data Catalog Health Check:true, BDR Health Check:true, BDR Bucket Creation Fails", startup.startAndConfigureExternalComponents());
        Mockito.verify(dependentServiceAvailabilityConnectedSystems, times(0)).checkHealth();
        Mockito.verify(startKafka, times(0)).startKafkaListener(Mockito.anyString());
    }

    @Test
    @DisplayName("TEST: Verify Startup returns false when Data Catalog Health Check:true and BDR Health Check True, BDR Bucket Creation Fails, Connected Systems Health Check called")
    void test_startupWithDmmEnabledDataCatalogHealthCheckTrueBDRHealthCheckTrueBDRBucketCreated() throws UnsatisfiedExternalDependencyException {
        when(dependentServiceAvailabilityDataCatalog.checkHealth()).thenReturn(true);
        when(dependentServiceAvailabilityBDR.checkHealth()).thenReturn(true);
        when(startupUtil.setupBDRBucketUsingDataCatalog()).thenReturn(true);
        assertFalse("Expected Startup to return false when Data Catalog Health Check:true, BDR Health Check:true, BDR Bucket Creation Fails, Connected Systems Health Check called", startup.startAndConfigureExternalComponents());
        Mockito.verify(dependentServiceAvailabilityConnectedSystems, times(1)).checkHealth();
        Mockito.verify(startKafka, times(0)).startKafkaListener(Mockito.anyString());
    }

    @Test
    @DisplayName("TEST: Verify Startup returns false when Data Catalog Health Check:true and BDR Health Check True, BDR Bucket Creation True, Connected System Health Check Fails")
    void test_startupWithDmmEnabledDataCatalogHealthCheckTrueBDRHealthCheckTrueBDRBucketCreatedConnectedSystemHealthFalse() throws UnsatisfiedExternalDependencyException {
        when(dependentServiceAvailabilityDataCatalog.checkHealth()).thenReturn(true);
        when(dependentServiceAvailabilityBDR.checkHealth()).thenReturn(true);
        when(startupUtil.setupBDRBucketUsingDataCatalog()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkHealth()).thenReturn(false);
        assertFalse("Expected Startup to return false when Data Catalog Health Check:true, BDR Health Check:true, BDR Bucket Creation True, Connected System Health Check Fails", startup.startAndConfigureExternalComponents());
        Mockito.verify(connectedSystemsRetriever, times(0)).checkSubsystemDetailsAvailable();
        Mockito.verify(dependentServiceAvailabilitySftpServer, times(0)).checkHealth();
        Mockito.verify(startKafka, times(0)).startKafkaListener(Mockito.anyString());
    }

    @Test
    @DisplayName("TEST: Verify Startup returns false when Data Catalog Health Check:true and BDR Health Check True, BDR Bucket Creation True, Connected System Health Check True, subsystem details False")
    void test_startupWithDmmEnabledDataCatalogHealthCheckTrueBDRHealthCheckTrueBDRBucketCreatedConnectedSystemHealthTrueSubsystemDetailsAvailableFalse() throws UnsatisfiedExternalDependencyException {
        when(dependentServiceAvailabilityDataCatalog.checkHealth()).thenReturn(true);
        when(dependentServiceAvailabilityBDR.checkHealth()).thenReturn(true);
        when(startupUtil.setupBDRBucketUsingDataCatalog()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkHealth()).thenReturn(true);
        when(connectedSystemsRetriever.checkSubsystemDetailsAvailable()).thenReturn(false);
        assertFalse("Expected Startup to return false when Data Catalog Health Check:true, BDR Health Check:true, BDR Bucket Creation True, Connected System Health Check True, subsystem details False", startup.startAndConfigureExternalComponents());
        Mockito.verify(connectedSystemsRetriever, times(1)).checkSubsystemDetailsAvailable();
        Mockito.verify(dependentServiceAvailabilitySftpServer, times(0)).checkHealth();
        Mockito.verify(startKafka, times(0)).startKafkaListener(Mockito.anyString());
    }

    @Test
    @DisplayName("TEST: Verify Startup returns false when Data Catalog Health Check:true and BDR Health Check True, BDR Bucket Creation True, Connected System Health Check true, subsystem details true")
    void test_startupWithDmmEnabledDataCatalogHealthCheckTrueBDRHealthCheckTrueBDRBucketCreatedConnectedSystemHealthTrueSubsystemDetailsAvailableTrue() throws UnsatisfiedExternalDependencyException {
        when(dependentServiceAvailabilityDataCatalog.checkHealth()).thenReturn(true);
        when(dependentServiceAvailabilityBDR.checkHealth()).thenReturn(true);
        when(startupUtil.setupBDRBucketUsingDataCatalog()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkHealth()).thenReturn(true);
        when(connectedSystemsRetriever.checkSubsystemDetailsAvailable()).thenReturn(true);
        assertFalse("Expected Startup to return false when Data Catalog Health Check:true, BDR Health Check:true, BDR Bucket Creation True, Connected System Health Check true, subsystem details true", startup.startAndConfigureExternalComponents());
        Mockito.verify(dependentServiceAvailabilitySftpServer, times(1)).checkHealth();
        Mockito.verify(startKafka, times(0)).startKafkaListener(Mockito.anyString());
    }

    @Test
    @DisplayName("TEST: Verify Startup returns false when Data Catalog Health Check:true and BDR Health Check True, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, SFTP Server False")
    void test_startupWithDmmEnabledDataCatalogHealthCheckTrueBDRHealthCheckTrueBDRBucketCreatedConnectedSystemHealthTrueSubsystemDetailsAvailableTrueSftpServerFalse() throws UnsatisfiedExternalDependencyException {
        when(dependentServiceAvailabilityDataCatalog.checkHealth()).thenReturn(true);
        when(dependentServiceAvailabilityBDR.checkHealth()).thenReturn(true);
        when(startupUtil.setupBDRBucketUsingDataCatalog()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkHealth()).thenReturn(true);
        when(connectedSystemsRetriever.checkSubsystemDetailsAvailable()).thenReturn(true);
        when(dependentServiceAvailabilitySftpServer.checkHealth()).thenReturn(false);
        assertFalse("Expected Startup to return false when Data Catalog Health Check:true, BDR Health Check:true, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, " +
                "SFTP Server False", startup.startAndConfigureExternalComponents());
        Mockito.verify(outputTopicController, times(0)).buildAndCreateTopic();
        Mockito.verify(startKafka, times(0)).startKafkaListener(Mockito.anyString());
    }

    @Test
    @DisplayName("TEST: Verify Startup returns false when Data Catalog Health Check:true and BDR Health Check True, BDR Bucket Creation True, Connected System Health Check true, " +
            "subsystem details true, SFTP Server True")
    void test_startupWithDmmEnabledDataCatalogHealthCheckTrueBDRHealthCheckTrueBDRBucketCreatedConnectedSystemHealthTrueSubsystemDetailsAvailableTrueSftpServerTrue() throws UnsatisfiedExternalDependencyException {
        when(dependentServiceAvailabilityDataCatalog.checkHealth()).thenReturn(true, true);
        when(dependentServiceAvailabilityBDR.checkHealth()).thenReturn(true);
        when(startupUtil.setupBDRBucketUsingDataCatalog()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkHealth()).thenReturn(true);
        when(connectedSystemsRetriever.checkSubsystemDetailsAvailable()).thenReturn(true);
        when(dependentServiceAvailabilitySftpServer.checkHealth()).thenReturn(true);
        when(startupUtil.setupFileFormatInDataCatalog()).thenReturn(true);
        assertFalse("Expected Startup to return false when Data Catalog Health Check:true, BDR Health Check:true, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, " +
                "SFTP Server True", startup.startAndConfigureExternalComponents());
        Mockito.verify(dependentServiceAvailabilityKafka, times(1)).checkHealth();
        Mockito.verify(outputTopicController, times(0)).buildAndCreateTopic();
        Mockito.verify(startKafka, times(0)).startKafkaListener(Mockito.anyString());
    }

    @Test
    @DisplayName("TEST: Verify Startup returns false when Data Catalog Health Check:true and BDR Health Check True, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, " +
            "SFTP Server True, Kafka Health false")
    void test_startupWithDmmEnabledDataCatalogHealthCheckTrueBDRHealthCheckTrueBDRBucketCreatedConnectedSystemHealthTrueSubsystemDetailsAvailableTrueSftpServerTrueKafkaFalse() throws UnsatisfiedExternalDependencyException {
        when(dependentServiceAvailabilityDataCatalog.checkHealth()).thenReturn(true, true);
        when(dependentServiceAvailabilityBDR.checkHealth()).thenReturn(true);
        when(startupUtil.setupBDRBucketUsingDataCatalog()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkHealth()).thenReturn(true);
        when(connectedSystemsRetriever.checkSubsystemDetailsAvailable()).thenReturn(true);
        when(dependentServiceAvailabilitySftpServer.checkHealth()).thenReturn(true);
        when(startupUtil.setupFileFormatInDataCatalog()).thenReturn(true);
        when(dependentServiceAvailabilityKafka.checkHealth()).thenReturn(false);
        assertFalse("Expected Startup to return false when Data Catalog Health Check:true, BDR Health Check:true, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, " +
                "SFTP Server True, Kafka Health false", startup.startAndConfigureExternalComponents());
        Mockito.verify(outputTopicController, times(0)).buildAndCreateTopic();
        Mockito.verify(startKafka, times(0)).startKafkaListener(Mockito.anyString());
    }

    @Test
    @DisplayName("TEST: Verify Startup returns false when Data Catalog Health Check:true and BDR Health Check True, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, " +
            "SFTP Server True, Kafka Health true")
    void test_startupWithDmmEnabledDataCatalogHealthCheckTrueBDRHealthCheckTrueBDRBucketCreatedConnectedSystemHealthTrueSubsystemDetailsAvailableTrueSftpServerTrueKafkaTrue() throws UnsatisfiedExternalDependencyException {
        when(dependentServiceAvailabilityDataCatalog.checkHealth()).thenReturn(true, true);
        when(dependentServiceAvailabilityBDR.checkHealth()).thenReturn(true);
        when(startupUtil.setupBDRBucketUsingDataCatalog()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkHealth()).thenReturn(true);
        when(connectedSystemsRetriever.checkSubsystemDetailsAvailable()).thenReturn(true);
        when(dependentServiceAvailabilitySftpServer.checkHealth()).thenReturn(true);
        when(startupUtil.setupFileFormatInDataCatalog()).thenReturn(true);
        when(dependentServiceAvailabilityKafka.checkHealth()).thenReturn(true);
        assertFalse("Expected Startup to return false when Data Catalog Health Check:true, BDR Health Check:true, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, " +
                "SFTP Server True, Kafka Health true", startup.startAndConfigureExternalComponents());
        Mockito.verify(outputTopicController, times(1)).buildAndCreateTopic();
        Mockito.verify(startKafka, times(0)).startKafkaListener(Mockito.anyString());
    }

    @Test
    @DisplayName("TEST: Verify Startup returns false when Data Catalog Health Check:true and BDR Health Check True, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, " +
            "SFTP Server True, Kafka Health true, Setup Topic False")
    void test_startupWithDmmEnabledDataCatalogHealthCheckTrueBDRHealthCheckTrueBDRBucketCreatedConnectedSystemHealthTrueSubsystemDetailsAvailableTrueSftpServerTrueKafkaTrueSetupTopicFalse() throws UnsatisfiedExternalDependencyException {
        when(dependentServiceAvailabilityDataCatalog.checkHealth()).thenReturn(true, true);
        when(dependentServiceAvailabilityBDR.checkHealth()).thenReturn(true);
        when(startupUtil.setupBDRBucketUsingDataCatalog()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkHealth()).thenReturn(true);
        when(connectedSystemsRetriever.checkSubsystemDetailsAvailable()).thenReturn(true);
        when(dependentServiceAvailabilitySftpServer.checkHealth()).thenReturn(true);
        when(dependentServiceAvailabilityKafka.checkHealth()).thenReturn(true);
        when(startupUtil.setupFileFormatInDataCatalog()).thenReturn(true);
        when(outputTopicController.buildAndCreateTopic()).thenReturn(false);
        assertFalse("Expected Startup to return false when Data Catalog Health Check:true, BDR Health Check:true, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, " +
                "SFTP Server True, Kafka Health true, Setup Topic False", startup.startAndConfigureExternalComponents());
        Mockito.verify(startKafka, times(0)).startKafkaListener(Mockito.anyString());
    }

    @Test
    @DisplayName("TEST: Verify Startup returns false when Data Catalog Health Check:true and BDR Health Check True, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, " +
            "SFTP Server True, Kafka Health true, Setup Topic true")
    void test_startupWithDmmEnabledDataCatalogHealthCheckTrueBDRHealthCheckTrueBDRBucketCreatedConnectedSystemHealthTrueSubsystemDetailsAvailableTrueSftpServerTrueKafkaTrueSetupTopicTrue() throws UnsatisfiedExternalDependencyException {
        when(dependentServiceAvailabilityDataCatalog.checkHealth()).thenReturn(true, false);
        when(dependentServiceAvailabilityBDR.checkHealth()).thenReturn(true);
        when(startupUtil.setupBDRBucketUsingDataCatalog()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkHealth()).thenReturn(true);
        when(connectedSystemsRetriever.checkSubsystemDetailsAvailable()).thenReturn(true);
        when(dependentServiceAvailabilitySftpServer.checkHealth()).thenReturn(true);
        assertFalse("Expected Startup to return false when Data Catalog Health Check:true, BDR Health Check:true, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, " +
                "SFTP Server True, Kafka Health true, Setup Topic true", startup.startAndConfigureExternalComponents());
        Mockito.verify(startupUtil, times(0)).setupFileFormatInDataCatalog();
        Mockito.verify(startKafka, times(0)).startKafkaListener(Mockito.anyString());
    }

    @Test
    @DisplayName("TEST: Verify Startup returns false when Data Catalog Health Check:true and BDR Health Check True, BDR Bucket Creation True, Connected System Health Check true, subsystem details true " +
            "SFTP Server True, Kafka Health true, Setup Topic true, Second DC Check True")
    void test_startupWithDmmEnabledDataCatalogHealthCheckTrueBDRHealthCheckTrueBDRBucketCreatedConnectedSystemHealthTrueSubsystemDetailsAvailableTrueSftpServerTrueKafkaTrueSetupTopicTrueSecondDCCheckTrue() throws UnsatisfiedExternalDependencyException {
        when(dependentServiceAvailabilityDataCatalog.checkHealth()).thenReturn(true, true);
        when(dependentServiceAvailabilityBDR.checkHealth()).thenReturn(true);
        when(startupUtil.setupBDRBucketUsingDataCatalog()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkHealth()).thenReturn(true);
        when(connectedSystemsRetriever.checkSubsystemDetailsAvailable()).thenReturn(true);
        when(dependentServiceAvailabilitySftpServer.checkHealth()).thenReturn(true);
        assertFalse("Expected Startup to return false when Data Catalog Health Check:true, BDR Health Check:true, BDR Bucket Creation True, Connected System Health Check true, subsystem details true" +
                "SFTP Server True, Kafka Health true, Setup Topic true, Second DC Check True", startup.startAndConfigureExternalComponents());
        Mockito.verify(startupUtil, times(1)).setupFileFormatInDataCatalog();
        Mockito.verify(startKafka, times(0)).startKafkaListener(Mockito.anyString());
    }

    @Test
    @DisplayName("TEST: Verify Startup returns false when Data Catalog Health Check:true and BDR Health Check True, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, " +
            "SFTP Server True, Kafka Health true, Setup Topic true, Second DC Check True, Register in DC")
    void test_startupWithDmmEnabledDataCatalogHealthCheckTrueBDRHealthCheckTrueBDRBucketCreatedConnectedSystemHealthTrueSubsystemDetailsAvailableTrueSftpServerTrueKafkaTrueSetupTopicTrueSecondDCCheckTrueRegisterInDC() throws UnsatisfiedExternalDependencyException {
        
        when(dependentServiceAvailabilityDataCatalog.checkHealth()).thenReturn(true, true);
        when(dependentServiceAvailabilityBDR.checkHealth()).thenReturn(true);
        when(startupUtil.setupBDRBucketUsingDataCatalog()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkHealth()).thenReturn(true);
        when(connectedSystemsRetriever.checkSubsystemDetailsAvailable()).thenReturn(true);
        when(dependentServiceAvailabilitySftpServer.checkHealth()).thenReturn(true);
        when(startupUtil.setupFileFormatInDataCatalog()).thenReturn(false);
        assertFalse("Expected Startup to return false when Data Catalog Health Check:true, BDR Health Check:true, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, " +
                "SFTP Server True, Kafka Health true, Setup Topic true, Second DC Check True, Register in DC, Listener in not called", startup.startAndConfigureExternalComponents());
        Mockito.verify(startKafka, times(0)).startKafkaListener(Mockito.anyString());
    }

    @Test
    @DisplayName("TEST: Verify Startup returns false when Data Catalog Health Check:true and BDR Health Check True, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, " +
            "SFTP Server True, Kafka Health true, Setup Topic true, Second DC Check True, Register in DC")
    void test_startupWithDmmEnabledDataCatalogHealthCheckTrueBDRHealthCheckTrueBDRBucketCreatedConnectedSystemHealthTrueSubsystemDetailsAvailableTrueSftpServerTrueKafkaTrueSetupTopicTrueSecondDCCheckTrueRegisterInDC2() throws UnsatisfiedExternalDependencyException {
        
        when(dependentServiceAvailabilityDataCatalog.checkHealth()).thenReturn(true, true);
        when(dependentServiceAvailabilityBDR.checkHealth()).thenReturn(true);
        when(startupUtil.setupBDRBucketUsingDataCatalog()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkHealth()).thenReturn(true);
        when(connectedSystemsRetriever.checkSubsystemDetailsAvailable()).thenReturn(true);
        when(dependentServiceAvailabilitySftpServer.checkHealth()).thenReturn(true);
        when(dependentServiceAvailabilityKafka.checkHealth()).thenReturn(true);
        when(outputTopicController.buildAndCreateTopic()).thenReturn(true);
        when(startupUtil.setupFileFormatInDataCatalog()).thenReturn(true);
        assertFalse("Expected Startup to return false when Data Catalog Health Check:true, BDR Health Check:true, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, " +
                "SFTP Server True, Kafka Health true, Setup Topic true, Second DC Check True, Register in DC, Listener ", startup.startAndConfigureExternalComponents());
        Mockito.verify(startKafka, times(1)).startKafkaListener(Mockito.anyString());
    }

    @Test
    @DisplayName("TEST: Verify Startup returns false when Data Catalog Health Check:true and BDR Health Check True, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, " +
            "SFTP Server True, Kafka Health true, Setup Topic true, Second DC Check True, Register in DC, Start listener False")
    void test_startupWithDmmEnabledDataCatalogHealthCheckTrueBDRHealthCheckTrueBDRBucketCreatedConnectedSystemHealthTrueSubsystemDetailsAvailableTrueSftpServerTrueKafkaTrueSetupTopicTrueSecondDCCheckTrueRegisterInDC3() throws UnsatisfiedExternalDependencyException {
        
        when(dependentServiceAvailabilityDataCatalog.checkHealth()).thenReturn(true, true);
        when(dependentServiceAvailabilityBDR.checkHealth()).thenReturn(true);
        when(startupUtil.setupBDRBucketUsingDataCatalog()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkHealth()).thenReturn(true);
        when(connectedSystemsRetriever.checkSubsystemDetailsAvailable()).thenReturn(true);
        when(dependentServiceAvailabilitySftpServer.checkHealth()).thenReturn(true);
        when(dependentServiceAvailabilityKafka.checkHealth()).thenReturn(true);
        when(outputTopicController.buildAndCreateTopic()).thenReturn(true);
        when(startupUtil.setupFileFormatInDataCatalog()).thenReturn(true);
        when(startKafka.startKafkaListener(Mockito.anyString())).thenReturn(false);
        assertFalse("Expected Startup to return false when Data Catalog Health Check:true, BDR Health Check:true, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, " +
                "SFTP Server True, Kafka Health true, Setup Topic true, Second DC Check True, Register in DC, Start listener False", startup.startAndConfigureExternalComponents());
        Mockito.verify(startKafka, times(1)).startKafkaListener(Mockito.anyString());
    }

    @Test
    @DisplayName("TEST: Verify Startup returns false when Data Catalog Health Check:true and BDR Health Check True, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, " +
            "SFTP Server True, Kafka Health true, Setup Topic true, Second DC Check True, Register in DC, listener is started Successfully")
    void test_startupWithDmmEnabledDataCatalogHealthCheckTrueBDRHealthCheckTrueBDRBucketCreatedConnectedSystemHealthTrueSubsystemDetailsAvailableTrueSftpServerTrueKafkaTrueSetupTopicTrueSecondDCCheckTrueRegisterInDC4() throws UnsatisfiedExternalDependencyException {
        
        when(dependentServiceAvailabilityDataCatalog.checkHealth()).thenReturn(true, true);
        when(dependentServiceAvailabilityBDR.checkHealth()).thenReturn(true);
        when(startupUtil.setupBDRBucketUsingDataCatalog()).thenReturn(true);
        when(dependentServiceAvailabilityConnectedSystems.checkHealth()).thenReturn(true);
        when(connectedSystemsRetriever.checkSubsystemDetailsAvailable()).thenReturn(true);
        when(dependentServiceAvailabilitySftpServer.checkHealth()).thenReturn(true);
        when(dependentServiceAvailabilityKafka.checkHealth()).thenReturn(true);
        when(outputTopicController.buildAndCreateTopic()).thenReturn(true);
        when(startupUtil.setupFileFormatInDataCatalog()).thenReturn(true);
        when(startKafka.startKafkaListener(Mockito.anyString())).thenReturn(true);
        assertTrue("Expected Startup to return false when Data Catalog Health Check:true, BDR Health Check:true, BDR Bucket Creation True, Connected System Health Check true, subsystem details true, " +
                "SFTP Server True, Kafka Health true, Setup Topic true, Second DC Check True, Register in DC, listener is started Successfully", startup.startAndConfigureExternalComponents());
        Mockito.verify(startKafka, times(1)).startKafkaListener(Mockito.anyString());
    }
}
