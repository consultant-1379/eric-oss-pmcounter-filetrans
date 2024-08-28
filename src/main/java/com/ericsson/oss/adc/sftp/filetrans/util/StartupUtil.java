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

package com.ericsson.oss.adc.sftp.filetrans.util;


import com.ericsson.oss.adc.sftp.filetrans.availability.RetryForeverHandler;
import com.ericsson.oss.adc.sftp.filetrans.availability.UnsatisfiedExternalDependencyException;
import com.ericsson.oss.adc.sftp.filetrans.bdr.DataCatalogBasedBdrEndpointSupplier;
import com.ericsson.oss.adc.sftp.filetrans.configuration.properties.DataCatalogProperties;
import com.ericsson.oss.adc.sftp.filetrans.configuration.properties.DataServiceProperties;
import com.ericsson.oss.adc.sftp.filetrans.controller.BDRComponent;
import com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel;
import com.ericsson.oss.adc.sftp.filetrans.model.DataCategoryModel;
import com.ericsson.oss.adc.sftp.filetrans.model.DataProviderTypeModel;
import com.ericsson.oss.adc.sftp.filetrans.model.DataServiceModel;
import com.ericsson.oss.adc.sftp.filetrans.model.DataServiceInstanceModel;
import com.ericsson.oss.adc.sftp.filetrans.model.DataSpaceModel;
import com.ericsson.oss.adc.sftp.filetrans.model.DataTypeModel;
import com.ericsson.oss.adc.sftp.filetrans.model.FileFormatApiModel;
import com.ericsson.oss.adc.sftp.filetrans.model.FileFormatModel;
import com.ericsson.oss.adc.sftp.filetrans.model.MessageBusModel;
import com.ericsson.oss.adc.sftp.filetrans.model.NotificationTopicModel;
import com.ericsson.oss.adc.sftp.filetrans.model.SupportedPredicateParameterModel;
import com.ericsson.oss.adc.sftp.filetrans.service.ConnectedSystemsRetriever;
import com.ericsson.oss.adc.sftp.filetrans.service.DataCatalogService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Component()
@Slf4j
public class StartupUtil extends RetryForeverHandler {

    @Autowired
    DataCatalogService dataCatalogService;

    @Autowired
    DataCatalogProperties dataCatalogProperties;

    @Autowired
    DataServiceProperties dataServiceProperties;

    @Autowired
    BDRComponent bdrComponent;

    @Autowired
    ConnectedSystemsRetriever connectedSystemsRetriever;

    @Autowired
    BDRS3BucketNameValidator bdrs3BucketNameValidator;

    @Autowired
    private DataCatalogBasedBdrEndpointSupplier dataCatalogBasedBDREndpointSupplier;

    @Value("${subsystem.name}")
    private String subsystemName;

    @Value("${spring.kafka.topics.input.name}")
    private String inputTopicName;

    @Value("${spring.kafka.topics.enm_id}")
    private String enmID;

    @Value("${dmm.data-catalog.availability.retry_interval}")
    private int availabilityBackoffTimeInMs;

    @Value("${dmm.data-catalog.availability.retry_attempts}")
    private int availabilityRetryCountMax;

    private String createdBucketName;

    private MessageBusModel messageBusModel;
    private NotificationTopicModel notificationTopic;
    private FileFormatApiModel fileFormatApiModel;


    public boolean setupFileFormatInDataCatalog() {
        Utils.setRetryForever(availabilityRetryCountMax);
        log.info("Setup Data Objects Using File Format registration in Data Catalog: retry attempts {} with backoff {}", availabilityRetryCountMax,
                availabilityBackoffTimeInMs);
        final RetryTemplate template = RetryTemplate.builder().maxAttempts(availabilityRetryCountMax).fixedBackoff(availabilityBackoffTimeInMs)
                .retryOn(UnsatisfiedExternalDependencyException.class).withListener(Utils.getRetryListener()).build();

        return retrySetupDataObjectUsingFileFormatRegistrationInDataCatalog(template);
    }

    public boolean setupBDRBucketUsingDataCatalog() {
        Utils.setRetryForever(availabilityRetryCountMax);
        log.info("Setup BDR Bucket Using Data Catalog: retry attempts {} with backoff {}", availabilityRetryCountMax,
                availabilityBackoffTimeInMs);
        final RetryTemplate template = RetryTemplate.builder().maxAttempts(availabilityRetryCountMax).fixedBackoff(availabilityBackoffTimeInMs)
                .retryOn(UnsatisfiedExternalDependencyException.class).withListener(Utils.getRetryListener()).build();
        return retrySetupBDRBucket(template);
    }

    boolean retrySetupDataObjectUsingFileFormatRegistrationInDataCatalog(final RetryTemplate template) {
        retryForeverAttemptNo = 0;
        do {
            attemptNo = 0;
            retryForeverAttemptNo++;
            try {
                return template.execute(retryContext -> registerEntireFileFormatOrDataServiceInstanceInDataCatalog());

            } catch (final UnsatisfiedExternalDependencyException exception) {
                log.error("Hit max retry attempts {}", availabilityRetryCountMax, exception);
            }
            log.warn("Retry forever is '{}'", retryForever);
            //Allow retryForever to be false for testing.
        } while (retryForever); // exhausted retries
        return false;
    }

    boolean retrySetupBDRBucket(final RetryTemplate template) {
        retryForeverAttemptNo = 0;
        do {
            attemptNo = 0;
            retryForeverAttemptNo++;
            try {
                return template.execute(retryContext -> createBDRBucketUsingDataCatalogAccessEndPoints());
            } catch (final UnsatisfiedExternalDependencyException exception) {
                log.error("Hit max retry attempts {}", availabilityRetryCountMax, exception);
            }
            log.warn("Retry forever is '{}'", retryForever);
            //Allow retryForever to be false for testing.
        } while (retryForever); // exhausted retries
        return false;
    }

    boolean registerEntireFileFormatOrDataServiceInstanceInDataCatalog() throws UnsatisfiedExternalDependencyException {
        attemptNo++;
        final boolean objectsSetUp = setFileFormatInDataCatalog();
        boolean fileFormatRegistered = registerFileFormatUsingPut(fileFormatApiModel);
        log.info("All registered in Data Catalog Successfully");
        return objectsSetUp && fileFormatRegistered;
    }


    protected boolean setFileFormatInDataCatalog() throws UnsatisfiedExternalDependencyException {
        final BulkDataRepositoryModel bulkDataRepositoryModel = getAccessEndPointsFromDataCatalog(dataCatalogService.getAllBulkDataRepositories());
        if (bulkDataRepositoryModel != null) {

            notificationTopic = NotificationTopicModel.builder()
                    .name(dataServiceProperties.getDataServiceName())
                    .encoding(dataServiceProperties.getNotificationTopic().getDataEncoding())
                    .messageBus(messageBusModel)
                    .messageBusId(messageBusModel.getId())
                    .fileFormatIds(new ArrayList<>())
                    .specificationReference(dataServiceProperties.getNotificationTopic().getSpecificationReference())
                    .build();

            DataTypeModel dataType = DataTypeModel.builder()
                    .schemaName(dataServiceProperties.getDataType().getSchemaName())
                    .mediumType(dataServiceProperties.getDataType().getMediumType())
                    .schemaVersion(dataServiceProperties.getDataType().getSchemaVersion())
                    .isExternal(false).build();

            DataServiceModel dataService = DataServiceModel.builder()
                    .dataServiceName(dataServiceProperties.getDataServiceName()).build();

            DataServiceInstanceModel dataServiceInstance = DataServiceInstanceModel.builder()
                    .dataServiceInstanceName(dataServiceProperties.getDataServiceInstanceName())
                    .controlEndPoint(dataServiceProperties.getDataServiceInstance().getControlEndPoint())
                    .consumedDataSpace(dataServiceProperties.getDataServiceInstance().getConsumedDataSpace())
                    .consumedDataCategory(dataServiceProperties.getDataServiceInstance().getConsumedDataCategory())
                    .consumedDataProvider(dataServiceProperties.getDataServiceInstance().getConsumedDataProvider())
                    .consumedSchemaName(dataServiceProperties.getDataServiceInstance().getConsumedSchemaName()).dataService(dataService)
                    .consumedDataProvider(dataServiceProperties.getDataServiceInstance().getConsumedDataProvider())
                    .consumedSchemaVersion(dataServiceProperties.getDataServiceInstance().getConsumedSchemaVersion()).build();

            DataCategoryModel dataCategory = DataCategoryModel.builder()
                    .dataCategoryName(dataServiceProperties.getDataCategory().getName()).build();

            DataSpaceModel dataSpaceModel = new DataSpaceModel(dataServiceProperties.getDataSpace().getName());
            DataProviderTypeModel dataProviderType = DataProviderTypeModel.builder()
                    .providerTypeId(dataServiceProperties.getDataProvider().getName())
                    .dataCategory(dataServiceProperties.getDataCategory().getName())
                    .providerVersion(dataServiceProperties.getDataProvider().getVersion())
                    .dataSpace(dataSpaceModel)
                    .build();

            FileFormatModel fileFormatModel = FileFormatModel.builder()
                    .dataEncoding(dataServiceProperties.getFileFormat().getEncoding())
                    .specificationReference(dataServiceProperties.getNotificationTopic().getSpecificationReference())
                    .bulkDataRepositoryId(bulkDataRepositoryModel.getId())
                    .reportOutputPeriodList(List.of(0L)).build();

            SupportedPredicateParameterModel supportedPredicateParameter = SupportedPredicateParameterModel.builder()
                    .parameterName(dataServiceProperties.getSupportedPredicateParameter().getParameterName())
                    .isPassedToConsumedService(Boolean.parseBoolean(dataServiceProperties.getSupportedPredicateParameter().getPassedToConsumedService())).build();

            this.fileFormatApiModel = FileFormatApiModel.builder()
                    .dataSpace(dataSpaceModel).dataService(dataService)
                    .dataServiceInstance(dataServiceInstance)
                    .supportedPredicateParameter(supportedPredicateParameter)
                    .dataCategory(dataCategory)
                    .dataProviderType(dataProviderType)
                    .notificationTopic(notificationTopic)
                    .fileFormat(fileFormatModel)
                    .dataType(dataType).build();

            return true;
        }
        throw new UnsatisfiedExternalDependencyException("Unable to set up objects");
    }

    protected boolean createBDRBucketUsingDataCatalogAccessEndPoints() throws UnsatisfiedExternalDependencyException {
        attemptNo++;
        final BulkDataRepositoryModel bulkDataRepositoryModel = getAccessEndPointsFromDataCatalog(dataCatalogService.getAllBulkDataRepositories());
        if (bulkDataRepositoryModel != null) {
            dataCatalogBasedBDREndpointSupplier.initBdrEndPointDetailsFromDataCatalog(bulkDataRepositoryModel.getAccessEndpoints());
            log.info("Initialized Supplier with BDR Endpoint: {}", dataCatalogBasedBDREndpointSupplier.get());
            final List<String> accessEndPointList = bulkDataRepositoryModel.getAccessEndpoints();
            if (createBdrBucket(accessEndPointList)) {
                return true;
            }
        }
        throw new UnsatisfiedExternalDependencyException("Unable to get access points from Data Catalog for BDR");
    }


    public boolean createBdrBucket(final List<String> accessEndPointList) {
        String validatedBucketName;
        String bucketName = dataServiceProperties.getDataServiceName();
        if (bucketName != null) {
            if (accessEndPointList != null && !accessEndPointList.isEmpty()) {
                validatedBucketName = bdrs3BucketNameValidator.validateUnderS3Standards(bucketName);
                log.info("Configured subsystem name - {} , validated subsystem name according to S3 Standards- {}", bucketName, validatedBucketName);
                if (bdrComponent.createBucket(validatedBucketName)) {
                    createdBucketName = validatedBucketName;
                    return true;
                }
                log.error("FAILED to create bucket {} after validation", validatedBucketName);
            }
        } else {
            log.error(" FAILED to create bucket. Bucket name is NULL.");
        }
        return false;
    }

    protected boolean registerFileFormatUsingPut(final FileFormatApiModel fileFormat) throws UnsatisfiedExternalDependencyException {
        final ResponseEntity<Optional<FileFormatApiModel>> responseEntity = dataCatalogService.registerFileFormatUsingPut(fileFormat);

        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            log.info("Successfully Registered FileFormat using PUT: '{}'", responseEntity.getStatusCode());
            return true;
        } else if (responseEntity.getStatusCode() == HttpStatus.CONFLICT) {
            log.info("FileFormatV2 was already successfully registered: '{}'", responseEntity.getStatusCode());
            return true;
        }
        throw new UnsatisfiedExternalDependencyException(getExceptionMessage("Register FileFormatV2", responseEntity));
    }


    protected BulkDataRepositoryModel getAccessEndPointsFromDataCatalog(final ResponseEntity<BulkDataRepositoryModel[]> responseEntity) {
        BulkDataRepositoryModel bulkDataRepositoryDTOS3BDR = null;
        if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
            for (final BulkDataRepositoryModel bdrDTO : responseEntity.getBody()) {
                if (bdrDTO.getFileRepoType() != null && bdrDTO.getFileRepoType().equals(BulkDataRepositoryModel.FileRepoType.SFTP) && !bdrDTO.getAccessEndpoints().isEmpty()) {
                    connectedSystemsRetriever.addAccessEndpoints(bdrDTO.getAccessEndpoints());
                    log.info("Updated Connected System Details {} {}", bdrDTO.getName(), bdrDTO.getAccessEndpoints());
                } else if (bdrDTO.getFileRepoType() != null && bdrDTO.getFileRepoType().equals(BulkDataRepositoryModel.FileRepoType.S3) && !bdrDTO.getAccessEndpoints().isEmpty()) {
                    bulkDataRepositoryDTOS3BDR = bdrDTO;
                    log.info("Received BDR details S3 {} {}", bdrDTO.getName(), bdrDTO.getAccessEndpoints());
                }
            }
        }
        return bulkDataRepositoryDTOS3BDR;
    }

    public void setMessageBusModel() throws UnsatisfiedExternalDependencyException {
        // Retry until message bus details are fetched. This will indicate that data-catalog is available.
        final ResponseEntity<MessageBusModel[]> responseEntity = dataCatalogService.getMessageBusByParams(dataCatalogProperties.getMessageBusName(),
                dataCatalogProperties.getMessageBusNamespace());
        if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.hasBody()) {
            final MessageBusModel[] messageBusModelList = responseEntity.getBody();
            if (messageBusModelList != null && messageBusModelList.length >= 1) {
                messageBusModel = messageBusModelList[0];
                return;
            }
        }
        throw new UnsatisfiedExternalDependencyException(
                "Cannot get MessageBusDTO from Data Catalog, response code is '" + responseEntity.getStatusCode().value() + ", response body: " +
                        responseEntity.getBody());
    }

    private <T> String getExceptionMessage(final String taskToDo, final ResponseEntity<T> responseEntity) {
        final String acceptableResponses = HttpStatus.OK + ", " + HttpStatus.CONFLICT;
        final int actualResponse = responseEntity.getStatusCode().value();
        return String.format("Cannot %s, expected response codes of %s, received response code of %d", taskToDo, acceptableResponses,
                actualResponse);
    }

    // For test from here.
    public int getAvailabilityBackoffTimeInMs() {
        return availabilityBackoffTimeInMs;
    }

    public String getCreatedBucketName() {
        return createdBucketName;
    }


}
