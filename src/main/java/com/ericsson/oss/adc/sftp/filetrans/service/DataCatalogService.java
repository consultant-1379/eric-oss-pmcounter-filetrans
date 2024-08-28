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


import com.ericsson.oss.adc.sftp.filetrans.configuration.properties.DataCatalogProperties;
import com.ericsson.oss.adc.sftp.filetrans.configuration.properties.DataServiceProperties;
import com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel;
import com.ericsson.oss.adc.sftp.filetrans.model.FileFormatApiModel;
import com.ericsson.oss.adc.sftp.filetrans.model.MessageBusModel;
import com.ericsson.oss.adc.sftp.filetrans.rest.DataCatalogUriBuilder;
import com.ericsson.oss.adc.sftp.filetrans.rest.RestTemplateFacade;
import com.ericsson.oss.adc.sftp.filetrans.util.ResponseEntityWrapper;
import com.ericsson.oss.adc.sftp.filetrans.util.RestExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;


/**
 * Service for interacting with DMM service and consuming the rest end-points
 * we will get the access point as a response to which we configure our microservice as a consumer.
 */
@Service
@Slf4j
public class DataCatalogService {

    private static final String LOG_MESSAGE = "[Requesting {0} from data-catalog]";
    private static final String LOG_MESSAGE_REGISTER = "[Register {0} on data-catalog]";

    @Value("${dmm.data-catalog.base-url}")
    private String dataCatalogBaseUrl;
    @Value("${dmm.data-catalog.base-port}")
    private String dataCatalogBasePort;
    @Value("${dmm.data-catalog.message-bus-uri}")
    private String messageBusUri;
    @Value("${dmm.data-catalog.bulk-data-repository-uri}")
    private String bulkDataRepositoryUri;

    @Autowired
    private DataCatalogProperties dataCatalogProperties;
    @Autowired
    private DataServiceProperties dataServiceProperties;
    @Autowired
    private RestTemplateFacade restTemplateFacade;
    @Autowired
    private RestExecutor restExecutor;

    public ResponseEntity<BulkDataRepositoryModel[]> getAllBulkDataRepositories() {
        final String url = MessageFormat.format("{0}{1}{2}", dataCatalogBaseUrl, dataCatalogBasePort, bulkDataRepositoryUri);
        log.info("[Requesting Bulk Data Repository(All) from data-catalog using: ] GET Request: {}", url);
        final ResponseEntityWrapper<BulkDataRepositoryModel[]> responseEntityWrapper = restExecutor.exchange(url,
                MessageFormat.format(LOG_MESSAGE, "Bulk Data Repository(All)"), HttpMethod.GET, BulkDataRepositoryModel[].class);
        return responseEntityWrapper.getResponseEntity();
    }

    public ResponseEntity<MessageBusModel[]> getMessageBusByParams(final String messageBusName, final String messageBusNameSpace) {
        final String url = MessageFormat.format("{0}{1}{2}?name={3}&nameSpace={4}", dataCatalogBaseUrl, dataCatalogBasePort, messageBusUri, messageBusName, messageBusNameSpace);
        log.info("[Requesting Message Bus by params from data-catalog using: ] GET Request: {}", url);
        final ResponseEntityWrapper<MessageBusModel[]> responseEntityWrapper = restExecutor.exchange(url,
                MessageFormat.format(LOG_MESSAGE, "Message Bus by params"), HttpMethod.GET, MessageBusModel[].class);
        return responseEntityWrapper.getResponseEntity();
    }

    public ResponseEntity<Void> deleteDataServiceInstance(String dataServiceName, String dataServiceInstanceName) {
        log.info("DELETING Data Service Instance: '{}' from Data Catalog ...", dataServiceInstanceName);
        String dataServiceInstanceUrl = DataCatalogUriBuilder.base(dataCatalogProperties.getDataServicePath())
                .dataService(dataServiceName)
                .dataServiceInstance(dataServiceInstanceName)
                .build();

        return restTemplateFacade.delete(dataServiceInstanceUrl);
    }

    public ResponseEntity<Optional<FileFormatApiModel>> registerFileFormatUsingPut(final FileFormatApiModel fileFormat) {
        log.info("CREATING/UPDATING File Format (ID: {}) in Data Catalog ...", fileFormat.getId());

        String fileFormatUrl = DataCatalogUriBuilder.base(dataCatalogProperties.getFileFormatPath())
                .build();

        HttpEntity<String> httpRequest = requestPutNotificationStructure(generateFileFormatPutHttpBody(fileFormat), "File Format API");
        return restTemplateFacade.put(fileFormatUrl, httpRequest, FileFormatApiModel.class);
    }

    <T> ResponseEntity<T> restExecutorPostForEntity(final String url, final String logMessage, final Map<String, Object> httpBody,
                                                    final Class<T> responseType) {
        return restExecutorPostForEntity(url, logMessage, httpBody, responseType, new ObjectMapper());
    }

    <T> ResponseEntity<T> restExecutorPostForEntity(final String url, final String logMessage, final Map<String, Object> httpBody,
                                                    final Class<T> responseType, final ObjectMapper objectMapper) {
        log.info("Rest Executor for '{}' : POST Request: url = {}, httpBody = {}, responseType = {}", logMessage, url, httpBody, responseType);
        final HttpEntity<String> httpEntity = requestPostNotificationStructure(objectMapper, httpBody, logMessage);
        if (httpEntity != null) {
            final ResponseEntityWrapper responseEntityWrapper = restExecutor.postForEntity(url, MessageFormat.format(LOG_MESSAGE_REGISTER, logMessage),
                    httpEntity, responseType);
            return responseEntityWrapper.getResponseEntity();
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    public HttpEntity<String> requestPostNotificationStructure(final Map<String, Object> httpBody, final String type) {
        return requestPostNotificationStructure(new ObjectMapper(), httpBody, type);

    }

    public HttpEntity<String> requestPostNotificationStructure(final ObjectMapper objectMapper, final Map<String, Object> httpBody,
                                                               final String type) {
        try {
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            final String jsonData = objectMapper.writeValueAsString(httpBody);
            return new HttpEntity<>(jsonData, headers);
        } catch (final JsonProcessingException jsonProcessingException) {
            log.error("FAILED to create the {} due to incorrect JSON {}", type, jsonProcessingException.getMessage());
            return null;
        }
    }

    public HttpEntity<String> requestPutNotificationStructure(Map<String, Object> httpBody, String type) {
        try {
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            final String jsonData = new ObjectMapper().writeValueAsString(httpBody);
            return new HttpEntity<>(jsonData, headers);
        } catch (final JsonProcessingException jsonProcessingException) {
            log.error("FAILED to create the {} due to incorrect JSON {}", type, jsonProcessingException.getMessage());
            return null;
        }
    }

    private Map<String, Object> generateFileFormatPutHttpBody(final FileFormatApiModel fileFormat) {
        final Map<String, Object> fileFormatBody = new LinkedHashMap<>(9);

        if (fileFormat.getId() != null) {
            fileFormatBody.put("id", fileFormat.getId());
        }

        final Map<String, Object> dataSpace = new LinkedHashMap<>(1);
        dataSpace.put("name", fileFormat.getDataSpace().getName());
        fileFormatBody.put("dataSpace", dataSpace);

        final Map<String, Object> dataService = new LinkedHashMap<>(1);
        dataService.put("dataServiceName", fileFormat.getDataService().getDataServiceName());
        fileFormatBody.put("dataService", dataService);

        final Map<String, Object> dataServiceInstance = new LinkedHashMap<>(7);
        dataServiceInstance.put("dataServiceInstanceName", fileFormat.getDataServiceInstance().getDataServiceInstanceName());
        dataServiceInstance.put("controlEndPoint", fileFormat.getDataServiceInstance().getControlEndPoint());
        dataServiceInstance.put("consumedDataSpace", fileFormat.getDataServiceInstance().getConsumedDataSpace());
        dataServiceInstance.put("consumedDataCategory", fileFormat.getDataServiceInstance().getConsumedDataCategory());
        dataServiceInstance.put("consumedDataProvider", fileFormat.getDataServiceInstance().getConsumedDataProvider());
        dataServiceInstance.put("consumedSchemaName", fileFormat.getDataServiceInstance().getConsumedSchemaName());
        dataServiceInstance.put("consumedSchemaVersion", fileFormat.getDataServiceInstance().getConsumedSchemaVersion());
        fileFormatBody.put("dataServiceInstance", dataServiceInstance);

        final Map<String, Object> supportedPredicateParameter = new LinkedHashMap<>(2);
        supportedPredicateParameter.put("isPassedToConsumedService", fileFormat.getSupportedPredicateParameter().isPassedToConsumedService());
        supportedPredicateParameter.put("parameterName", fileFormat.getSupportedPredicateParameter().getParameterName());
        fileFormatBody.put("supportedPredicateParameter", supportedPredicateParameter);

        final Map<String, Object> dataCategory = new LinkedHashMap<>(1);
        dataCategory.put("dataCategoryName", fileFormat.getDataCategory().getDataCategoryName());
        fileFormatBody.put("dataCategory", dataCategory);

        final Map<String, Object> providerType = new LinkedHashMap<>(2);
        providerType.put("dataProviderName", fileFormat.getDataProviderType().getProviderTypeId());
        fileFormatBody.put("dataProviderType", providerType);

        final Map<String, Object> notificationTopic = new LinkedHashMap<>(3);
        notificationTopic.put("messageBusId", fileFormat.getNotificationTopic().getMessageBusId());
        notificationTopic.put("name", fileFormat.getNotificationTopic().getName());
        notificationTopic.put("encoding", fileFormat.getNotificationTopic().getEncoding());
        notificationTopic.put("specificationReference", fileFormat.getNotificationTopic().getSpecificationReference());

        fileFormatBody.put("notificationTopic", notificationTopic);

        final Map<String, Object> fileFormatMap = new LinkedHashMap<>(4);
        fileFormatMap.put("bulkDataRepositoryId", fileFormat.getFileFormat().getBulkDataRepositoryId());
        fileFormatMap.put("reportOutputPeriodList", fileFormat.getFileFormat().getReportOutputPeriodList());
        fileFormatMap.put("dataEncoding", fileFormat.getFileFormat().getDataEncoding());
        fileFormatMap.put("specificationReference", fileFormat.getFileFormat().getSpecificationReference());
        fileFormatBody.put("fileFormat", fileFormatMap);

        final Map<String, Object> dataType = new LinkedHashMap<>(4);

        dataType.put("mediumType", fileFormat.getDataType().getMediumType());
        dataType.put("schemaName", fileFormat.getDataType().getSchemaName());
        dataType.put("schemaVersion", fileFormat.getDataType().getSchemaVersion());
        dataType.put("isExternal", fileFormat.getDataType().isExternal());
        fileFormatBody.put("dataType", dataType);

        return fileFormatBody;
    }
}

