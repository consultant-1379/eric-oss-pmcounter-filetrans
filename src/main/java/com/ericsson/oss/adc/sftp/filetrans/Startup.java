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


import static com.ericsson.oss.adc.sftp.filetrans.util.Constants.LISTENER_ID;

import com.ericsson.oss.adc.sftp.filetrans.availability.*;
import com.ericsson.oss.adc.sftp.filetrans.model.MessageBusModel;
import com.ericsson.oss.adc.sftp.filetrans.kafka.KafkaBootstrapSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.ericsson.oss.adc.sftp.filetrans.controller.OutputTopicController;
import com.ericsson.oss.adc.sftp.filetrans.service.ConnectedSystemsRetriever;
import com.ericsson.oss.adc.sftp.filetrans.util.StartupUtil;
import org.springframework.util.CollectionUtils;

/**
 * The Class Startup. Starts required services post Spring Startup
 */
@Component
@Slf4j
public class Startup {

    @Autowired
    OutputTopicController outputTopicController;

    @Autowired
    ConnectedSystemsRetriever connectedSystemsRetriever;

    @Autowired
    StartupUtil startupUtil;

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private Environment environment;

    @Autowired
    private StartKafka startKafka;

    @Autowired
    DependentServiceAvailabilityUtil dependentServiceAvailabilityUtil;

    @Autowired
    private DependentServiceAvailabilitySftpServer dependentServiceAvailabilitySftpServer;

    @Autowired
    private DependentServiceAvailabilityDataCatalog dependentServiceAvailabilityDataCatalog;

    @Autowired
    private DependentServiceAvailabilityBDR dependentServiceAvailabilityBDR;

    @Autowired
    private DependentServiceAvailabilityConnectedSystems dependentServiceAvailabilityConnectedSystems;

    @Autowired
    private DependentServiceAvailabilityKafka dependentServiceAvailabilityKafka;

    @Autowired
    private KafkaBootstrapSupplier kafkaBootstrapSupplier;


    public Startup() {
        //No Args Constructor
    }

    @EventListener(
            value = ApplicationReadyEvent.class,
            condition = "@environment.getActiveProfiles()[0] != 'NoAsync'")
    @Async
    public void startup() throws UnsatisfiedExternalDependencyException {
        startAndConfigureExternalComponents();
    }

    /**
     * Start components needed for the application after string boot is up and running.
     *
     */
    public boolean startAndConfigureExternalComponents() throws UnsatisfiedExternalDependencyException {
        log.info("Performing Post Startup Dependency Checks");
        if (configureExternalDependencies()) {
            log.info("All External Dependency Checks and Configuration was Successful, Will attempt to start Kafka Listener");
            return startKafkaListener();
        }
        return false;
    }

    boolean checkSftpServerAvailableAndConnection() {
        if (!environment.acceptsProfiles(Profiles.of("test"))) {
            log.info("Checking if SFTP Server is online...");
            if (dependentServiceAvailabilitySftpServer.checkHealth()) {
                log.info("SFTP Server ONLINE");
                return true;
            }
            return false;
        }
        return true;
    }

    private boolean configureExternalDependencies() throws UnsatisfiedExternalDependencyException {
        //Data Catalog Needs to be populated first for BDR Access Points to be available
        if (dependentServiceAvailabilityDataCatalog.checkHealth() && dependentServiceAvailabilityBDR.checkHealth() && startupUtil.setupBDRBucketUsingDataCatalog()) {
            log.info("CREATED BDR Bucket successfully");
        } else {
            log.error("FAILED to create BDR Bucket");
            return false;
        }
        if (dependentServiceAvailabilityConnectedSystems.checkHealth() && connectedSystemsRetriever.checkSubsystemDetailsAvailable()) {
            log.info("Received subsystem details successfully, checking connection to SFTP server");
            if (!checkSftpServerAvailableAndConnection()) {
                log.error("FAILED to connect to SFTP Server");
                return false;
            }
        } else {
            log.error("FAILED to receive subsystem details, unable to connect to SFTP Server");
            return false;
        }
        //Bootstrap supplier is initialized before any kafka producer or consumer is started just to avoid their restarts
        configureKafkaBootstrapSupplier();
        if (dependentServiceAvailabilityDataCatalog.checkHealth()
                && startupUtil.setupFileFormatInDataCatalog()) {
            log.info("Registered in data catalog successfully.");
        } else {
            log.error("FAILED to register in data catalog.");
            return false;
        }
        if (dependentServiceAvailabilityKafka.checkHealth() && outputTopicController.buildAndCreateTopic()) {
            log.info("CREATED Output Topic successfully.");
        } else {
            log.error("FAILED to setup output topic.");
            return false;
        }
        return true;
    }

    private void configureKafkaBootstrapSupplier() throws UnsatisfiedExternalDependencyException {
        startupUtil.setMessageBusModel();
        String kafkaBootstrapSource = "Default Kafka Endpoint";
        log.info("Used {} to obtain Kafka Bootstrap endpoint(s): {}", kafkaBootstrapSource, kafkaBootstrapSupplier.get());
    }

    private boolean startKafkaListener() {
        if (startKafka.startKafkaListener(LISTENER_ID)) {
            log.info("Kafka Listener started successfully.");
            return true;
        } else {
            log.error("FAILED to start Kafka Listener.");
            return false;
        }
    }
}
