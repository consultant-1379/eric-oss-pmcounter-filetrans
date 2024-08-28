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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.adc.sftp.filetrans.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.adc.sftp.filetrans.availability.UnsatisfiedExternalDependencyException;
import com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel;
import com.ericsson.oss.adc.sftp.filetrans.model.ConnectionPropertiesModel;
import com.ericsson.oss.adc.sftp.filetrans.model.SubsystemModel;

@Service
@Scope("singleton")
@Slf4j
public class ConnectedSystemsRetriever {


    @Value("${connected.systems.uri}")
    private String uri;

    @Value("${subsystem.name}")
    private String subsystemName;

    @Autowired
    DataCatalogService  dataCatalogService;

    private final List<String> accessEndpoints = new ArrayList<>();

    private final Map<String, SubsystemModel> subsystemsByNameMap =  new HashMap<>();

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ENMScriptingVMLoadBalancer loadBalancer;

    private boolean isInfiniteRetry = true;

    @Value("${connected.systems.availability.retry_interval}")
    private int retryInterval;


    @Value("${connected.systems.availability.retry_attempts}")
    private int retryAttempts;

    /**
     * Gets the connected systems access points.
     * This method should only be called after the beans have been successfully initialized,
     * since retry will not work if called before
     */
    public boolean checkSubsystemDetailsAvailable() {
        final RetryListener retryListener = Utils.getRetryListener();
        final RetryTemplate template = RetryTemplate.builder()
                .maxAttempts(retryAttempts)
                .fixedBackoff(retryInterval)
                .retryOn(UnsatisfiedExternalDependencyException.class)
                .withListener(retryListener)
                .build();
        do {
            try {
                return template.execute(retryContext -> areSubsystemDetailsPopulated());
            } catch (final UnsatisfiedExternalDependencyException exception) {
                log.error("Retry attempts: {}. Exception error trying to fetch Connected Systems details.", retryAttempts, exception);
            }
        } while (isInfiniteRetry);

        return false;
    }


    public boolean areSubsystemDetailsPopulated() throws UnsatisfiedExternalDependencyException {
        String errorMessage = MessageFormat.format("ERROR retrieving {0} Subsystem Details. Subsystem details could not be found.", subsystemName);
        try {

            final Map<String, SubsystemModel> subsystemDetails = getSubsystemDetails();
            if (subsystemDetails == null || subsystemDetails.isEmpty() || subsystemDetails.get(subsystemName) == null) {
                accessEndpoints.clear();
                final List<String> newAccessPoints = getConnectedSystemsAccessPoints();
                if (newAccessPoints != null && !newAccessPoints.isEmpty()) {
                    addAccessEndpoints(newAccessPoints);
                }
                throw new UnsatisfiedExternalDependencyException(errorMessage);
            } else {
                isInfiniteRetry = false;
                return true;
            }
        } catch (Exception exception) {
            throw new UnsatisfiedExternalDependencyException(errorMessage, exception);
        }
    }

    public List<String> getConnectedSystemsAccessPoints() {
        final ResponseEntity<BulkDataRepositoryModel[]> responseEntity = dataCatalogService.getAllBulkDataRepositories();
        final List<String> connectedSystemsAccessPoints = new ArrayList<>();
        if (responseEntity != null) {
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                for (final BulkDataRepositoryModel bdrDTO : responseEntity.getBody()) {
                    if (bdrDTO.getFileRepoType() != null && bdrDTO.getFileRepoType().equals(BulkDataRepositoryModel.FileRepoType.SFTP)
                            && bdrDTO.getAccessEndpoints() != null && !bdrDTO.getAccessEndpoints().isEmpty()) {
                        connectedSystemsAccessPoints.addAll(bdrDTO.getAccessEndpoints());
                        log.info("Access Points updated for {} Connected Systems from Data Catalog: {}", bdrDTO.getName(), bdrDTO.getAccessEndpoints());
                    }
                }
            }
        }
        else {
            log.error("FAILED to retrieve access end points, no BulkDataRepository found");
        }
        return connectedSystemsAccessPoints;
    }

    public Map<String, SubsystemModel> getSubsystemDetails() {
        if (!accessEndpoints.isEmpty()) {
            try {
                final HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                final HttpEntity<Object> entity = new HttpEntity<>(headers);
                final ResponseEntity<SubsystemModel[]> response = restTemplate.exchange(accessEndpoints.get(0) + uri, HttpMethod.GET, entity, SubsystemModel[].class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    return getConnectionPropierties(response);
                }
            } catch (final Exception exception) {
                log.error("FAILED to retrieve ENM details, connected system  {}{} request ", accessEndpoints.get(0) , uri, exception);
            }
        }
        else {
            log.error("FAILED to request subsystem details, access endpoint not available");
        }
        return Collections.emptyMap();
    }

    private Map<String, SubsystemModel> populateSubsystemByNameMap(final List<SubsystemModel> subsystemList) {
        for (final SubsystemModel subsystem: subsystemList) {
            subsystemsByNameMap.put(subsystem.getName(), subsystem);
        }
        addVMsToLoadBalancer();
        return subsystemsByNameMap;
    }


    private void addVMsToLoadBalancer() {
        log.info("populateSubsystemByNameMap, subsystemName {} ", subsystemName);
        if (subsystemsByNameMap.get(subsystemName) != null ) {
            if (!subsystemsByNameMap.get(subsystemName).getConnectionProperties().isEmpty()) {
                final List<ConnectionPropertiesModel> connProperties = subsystemsByNameMap.get(subsystemName).getConnectionProperties();
                if (connProperties.get(0).getScriptingVMs() != null) {
                    loadBalancer.setScriptingVMs(connProperties.get(0).getScriptingVMs());
                }
            } else {
                log.error("Connection properties for {} is empty ", subsystemName);
            }
        } else {
            log.error("FAILED to add VM to LoadBalancer. Subsystem map does not contain {} ", subsystemName);
        }
    }

    private Map<String, SubsystemModel> getConnectionPropierties(final ResponseEntity<SubsystemModel[]> response) {
        HttpStatus httpStatus = (HttpStatus) response.getStatusCode();
        log.info("Successfully executed request {}{}, response {} ", accessEndpoints.get(0), uri, httpStatus);

        if (httpStatus.equals(HttpStatus.OK) && response.getBody() != null) {
            final List<SubsystemModel> subsystemList = Arrays.asList(response.getBody());
            if (!subsystemList.isEmpty()) {
                return populateSubsystemByNameMap(subsystemList);
            }
        }
        return Collections.emptyMap();
    }

    public ConnectionPropertiesModel getConnectionPropertiesBySubsystemsName() {
        if (subsystemsByNameMap.isEmpty() || subsystemsByNameMap.get(getSubsystemName()) != null
                || subsystemsByNameMap.get(getSubsystemName()).getConnectionProperties()!= null
                || subsystemsByNameMap.get(getSubsystemName()).getConnectionProperties().isEmpty()) {
            getSubsystemDetails();
        }
        return subsystemsByNameMap.get(getSubsystemName()).getConnectionProperties().get(0);
    }

    public void addAccessEndpoints(final List<String> endpoint){
        accessEndpoints.addAll(endpoint);
    }

    //Needed for Test
    public Map<String, SubsystemModel> getSubsystemsByNameMap() {
        return subsystemsByNameMap;
    }

    //Needed for Test
    public String getSubsystemName() {
        return subsystemName;
    }

    public void setDataCatalogService(final DataCatalogService dataCatalogService) {
        this.dataCatalogService = dataCatalogService;
    }

}