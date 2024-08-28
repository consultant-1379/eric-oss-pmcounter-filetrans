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

package com.ericsson.oss.adc.sftp.filetrans.util;

import com.ericsson.oss.adc.sftp.filetrans.CoreApplication;
import com.ericsson.oss.adc.sftp.filetrans.availability.UnsatisfiedExternalDependencyException;
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
import com.ericsson.oss.adc.sftp.filetrans.service.DataCatalogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.retry.support.RetryTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel.FileRepoType.S3;
import static com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel.FileRepoType.SFTP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

/**
 * The Class StartupUtilTest.
 */

@SpringBootTest(classes = {CoreApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StartupUtilTestV2 {

    private static final int EXPECTED_RETRY_COUNT_MAX = 3;
    private static final int EXPECTED_BACKOFF_TIMEOUT_MS = 1000;

    @Autowired
    @InjectMocks
    StartupUtil startupUtil;

    @MockBean
    DataCatalogService dataCatalogService;

    @Autowired
    DataCatalogProperties dataCatalogProperties;

    @Autowired
    DataServiceProperties dataServiceProperties;

    @MockBean
    BDRComponent bdrComponent;

    @MockBean
    BDRProperties bdrProperties;

    DataSpaceModel dataSpace4GModel;

    DataSpaceModel dataSpace5GModel;

    String dataEncoding;

    String specificationReference;

    String encoding;


    private final MessageBusModel messageBus = MessageBusModel.builder()
            .id(1L)
            .name("name")
            .clusterName("clusterName")
            .nameSpace("nameSpace")
            .accessEndpoints(new ArrayList<>(Collections.singletonList("http://endpoint1:1234/")))
            .notificationTopicIds(new ArrayList<>(Collections.singletonList(1L)))
            .messageStatusTopicIds(new ArrayList<>(Collections.singletonList(1L)))
            .messageDataTopicIds(new ArrayList<>(Collections.singletonList(1L)))
            .build();

    private final BulkDataRepositoryModel bulkDataRepositoryModel = BulkDataRepositoryModel.builder()
            .id(1l)
            .name("testBDR")
            .clusterName("testCluster")
            .nameSpace("testNameSpace")
            .accessEndpoints(new ArrayList<>(Collections.singletonList("http://testBdr:1234/")))
            .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
            .fileRepoType(S3)
            .build();

    private final BulkDataRepositoryModel connectedSystemsBdrModel2 = BulkDataRepositoryModel.builder()
            .id(2l)
            .name("testBDR1")
            .clusterName("testCluster")
            .nameSpace("testNS")
            .accessEndpoints(new ArrayList<>(Collections.singletonList("http://endpoint1:1234")))
            .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
            .fileRepoType(SFTP)
            .build();

    private final BulkDataRepositoryModel connectedSystemsBdrModel1 = BulkDataRepositoryModel.builder()
            .id(3l)
            .name("testBDR2")
            .clusterName("testCluster")
            .nameSpace("testNS")
            .accessEndpoints(new ArrayList<>(Collections.singletonList("http://endpoint1:1234")))
            .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
            .fileRepoType(S3)
            .build();

    private final SupportedPredicateParameterModel supportPredicateParamater = SupportedPredicateParameterModel.builder()
            .id(1L)
            .parameterName("nodeName")
            .dataServiceId(1)
            .isPassedToConsumedService(false)
            .build();

    private final FileFormatModel fileFormatFromDataCatalog = FileFormatModel.builder()
            .bulkDataRepositoryId(1)
            .dataEncoding("XML")
            .dataServiceId(1)
            .id(1L)
            .notificationTopicId(1)
            .reportOutputPeriodList(new ArrayList<>())
            .specificationReference("")
            .bulkDataRepository(BulkDataRepositoryModel.builder()
                    .id(1L)
                    .name("testName")
                    .clusterName("testCluster")
                    .nameSpace("testNS")
                    .accessEndpoints(new ArrayList<>(Collections.singletonList("http://endpoint1:1234/")))
                    .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
                    .fileRepoType(SFTP)
                    .build())
            .build();

    private SupportedPredicateParameterModel[] predicatedParamaterList = new SupportedPredicateParameterModel[1];

    private FileFormatModel[] fileFormatList = new FileFormatModel[1];

    private final BulkDataRepositoryModel[] connectedSystemsBdrModels = new BulkDataRepositoryModel[2];


    /**
     * Inits the.
     */
    @BeforeEach
    public void init() {
        dataEncoding = "XML";
        specificationReference = "";
        encoding = "JSON";
        dataSpace4GModel = new DataSpaceModel("4G");
        dataSpace5GModel = new DataSpaceModel("5G");
        connectedSystemsBdrModels[0] = connectedSystemsBdrModel1;
        connectedSystemsBdrModels[1] = connectedSystemsBdrModel2;
        predicatedParamaterList[0] = supportPredicateParamater;
        fileFormatList[0] = fileFormatFromDataCatalog;

    }

    /**
     * Test all objects setup correctly.
     *
     * @throws UnsatisfiedExternalDependencyException the unsatisfied external dependency exception
     */
    @Test
    @Order(1)
    public void test_allObjectsSetupCorrectly() throws UnsatisfiedExternalDependencyException {
        setupHttpOkResponseTestCase(HttpStatus.OK);
        startupUtil.setMessageBusModel();
        assertTrue(startupUtil.setFileFormatInDataCatalog());
        assertTrue(startupUtil.registerEntireFileFormatOrDataServiceInstanceInDataCatalog());
    }


    /**
     * This test will test the happy case for startupUtil.setupObjects() & test the negative case for startupUtil.setupObjects()
     * where by bulkDataRepositoryDTO is null.
     *
     * @throws JsonProcessingException the json processing exception
     */
    @Test
    @Order(3)
    public void test_allObjectsSetupFailScenario() throws JsonProcessingException {
        setupHttpConflictResponseTestCase();
        final ResponseEntity<BulkDataRepositoryModel[]> connectedSystemsBdrDTOResponseEntityNotOK = new ResponseEntity<>(connectedSystemsBdrModels,
                HttpStatus.SERVICE_UNAVAILABLE);
        final ResponseEntity<BulkDataRepositoryModel[]> connectedSystemsBdrDTOResponseEntity = new ResponseEntity<>(connectedSystemsBdrModels,
                HttpStatus.OK);

        when(dataCatalogService.getAllBulkDataRepositories()).thenReturn(connectedSystemsBdrDTOResponseEntity)
                .thenReturn(connectedSystemsBdrDTOResponseEntityNotOK);

        // Happy case.
        try {
            startupUtil.setMessageBusModel();
            assertTrue(startupUtil.setFileFormatInDataCatalog());
        } catch (final UnsatisfiedExternalDependencyException e) {
            fail("Did not expect exception to be thrown for happy case test");
        }

        // test bulkDataRepositoryDTO == null throws exception;
        assertThrows(UnsatisfiedExternalDependencyException.class, () -> startupUtil.setFileFormatInDataCatalog(),
                "Expected setupDataCatalogObjectsForFileFormatRegistration() to throw UnsatisfiedExternalDependencyException when (mocked) bulkDataRepositoryDTO is null");
    }

    /**
     * Test setup data object using data catalog no retry happy case.
     */
    @Test
    @Order(4)
    public void test_setupDataObjectUsingDataCatalog_noRetryHappyCase() throws UnsatisfiedExternalDependencyException {
        setupHttpOkResponseTestCase(HttpStatus.OK);
        assertTrue(startupUtil.setupFileFormatInDataCatalog());
        startupUtil.setMessageBusModel();
        assertEquals(1, startupUtil.getAttemptNo(), "Expect only one attempt for Happy Case");
        assertEquals(1, startupUtil.getRetryForeverAttemptNo(), "Expect only one attempt (retryForeverAttemptNo) for Happy Case");
    }

    /**
     * Test set Message when message bus DTO throws exception.
     */
    @Test
    @Order(5)
    public void test_setMessage_retryGetMessageBusDTOThrowsException() throws UnsatisfiedExternalDependencyException {
        setupHttpOkResponseTestCase(HttpStatus.OK);
        startupUtil.setMessageBusModel();
        final ResponseEntity<MessageBusModel[]> responseEntity = new ResponseEntity<>(ArrayUtils.toArray(messageBus), HttpStatus.CONFLICT);
        when(
                dataCatalogService.getMessageBusByParams(dataCatalogProperties.getMessageBusName(), dataCatalogProperties.getMessageBusNamespace()))
                .thenReturn(responseEntity);
        Exception exception = assertThrows(UnsatisfiedExternalDependencyException.class, () -> {
            startupUtil.setMessageBusModel();
        });
        String expectedMessage = "Cannot get MessageBusDTO from Data Catalog";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }


    /**
     * Test setup data object using data catalog retry setup objects throws exception.
     */
    @Test
    @Order(6)
    public void test_setupDataObjectUsingDataCatalog_retrySetupObjectsThrowsException() {
        setupHttpOkResponseTestCase(HttpStatus.OK);
        final ResponseEntity<BulkDataRepositoryModel[]> connectedSystemsBdrDTOResponseEntityNotOK = new ResponseEntity<>(connectedSystemsBdrModels,
                HttpStatus.SERVICE_UNAVAILABLE);
        when(dataCatalogService.getAllBulkDataRepositories()).thenReturn(connectedSystemsBdrDTOResponseEntityNotOK);

        assertFalse(startupUtil.setupFileFormatInDataCatalog());
        assertRetries();
    }

    /**
     * Test setup data object using data catalog retry register file format throws exception.
     */
    @Test
    @Order(7)
    public void test_setupDataObjectUsingDataCatalog_retryRegisterFileFormatThrowsException() throws UnsatisfiedExternalDependencyException {
        setupHttpOkResponseTestCase(HttpStatus.OK);
        final DataProviderTypeModel dataProviderTypeModel = DataProviderTypeModel.builder()
                .providerTypeId(dataServiceProperties.getDataProvider().getName())
                .dataCategory(dataServiceProperties.getDataCategory().getName())
                .providerVersion(dataServiceProperties.getDataProvider().getVersion())
                .dataSpace(dataSpace4GModel)
                .build();
        final NotificationTopicModel notificationTopicModel = NotificationTopicModel.builder()
                .name(dataServiceProperties.getDataServiceName())
                .encoding(encoding)
                .messageBus(messageBus)
                .messageBusId(messageBus.getId())
                .fileFormatIds(new ArrayList<>())
                .specificationReference(dataServiceProperties.getNotificationTopic().getSpecificationReference())
                .build();

        final FileFormatModel fileFormatModel = FileFormatModel.builder().dataEncoding(dataEncoding).specificationReference(specificationReference)
                .bulkDataRepository(this.bulkDataRepositoryModel).build();
        final DataCategoryModel dataCategoryModel = new DataCategoryModel();
        final DataServiceModel dataServiceModel = new DataServiceModel();
        final DataServiceInstanceModel dataServiceInstanceModel = new DataServiceInstanceModel();
        final SupportedPredicateParameterModel supportedPredicateParameterModel = new SupportedPredicateParameterModel();
        final DataSpaceModel dataSpaceModel = new DataSpaceModel();
        final DataTypeModel dataTypeModel = new DataTypeModel();

        final FileFormatApiModel fileFormatApiModel = FileFormatApiModel.builder()
                .dataSpace(dataSpaceModel)
                .dataService(dataServiceModel).dataServiceInstance(dataServiceInstanceModel)
                .supportedPredicateParameter(supportedPredicateParameterModel)
                .dataCategory(dataCategoryModel)
                .dataProviderType(dataProviderTypeModel)
                .notificationTopic(notificationTopicModel)
                .fileFormat(fileFormatModel).dataType(dataTypeModel).build();

        final ResponseEntity<Optional<FileFormatApiModel>> responseEntityFileFormatDTO = new ResponseEntity<>(Optional.of(fileFormatApiModel), HttpStatus.SERVICE_UNAVAILABLE);

        final FileFormatApiModel fileFormat5GDTO = FileFormatApiModel.builder()
                .dataSpace(dataSpaceModel)
                .dataService(dataServiceModel).dataServiceInstance(dataServiceInstanceModel)
                .supportedPredicateParameter(supportedPredicateParameterModel)
                .dataCategory(dataCategoryModel)
                .dataProviderType(dataProviderTypeModel)
                .notificationTopic(notificationTopicModel)
                .fileFormat(fileFormatModel).dataType(dataTypeModel).build();
        final ResponseEntity<Optional<FileFormatApiModel>> responseEntityFileFormat5GDTO = new ResponseEntity<>(Optional.of(fileFormat5GDTO), HttpStatus.SERVICE_UNAVAILABLE);
        startupUtil.setMessageBusModel();
        when(dataCatalogService.registerFileFormatUsingPut(any(FileFormatApiModel.class))).thenReturn(responseEntityFileFormatDTO, responseEntityFileFormat5GDTO);
        assertFalse(startupUtil.setupFileFormatInDataCatalog());
        assertRetries();
    }

    /**
     * Test setup data object using data catalog retry forever.
     */
    @Test
    @Timeout(5)
    @Order(8)
    void test_setupDataObjectUsingDataCatalog_retryForever() throws UnsatisfiedExternalDependencyException {
        setupHttpOkResponseTestCase(HttpStatus.OK);
        final RetryTemplate template = RetryTemplate.builder().maxAttempts(1).fixedBackoff(100).retryOn(UnsatisfiedExternalDependencyException.class)
                .withListener(Utils.getRetryListener()).build();

        final ResponseEntity<BulkDataRepositoryModel[]> connectedSystemsBdrDTOResponseEntityNotOK = new ResponseEntity<>(connectedSystemsBdrModels,
                HttpStatus.SERVICE_UNAVAILABLE);
        final ResponseEntity<BulkDataRepositoryModel[]> connectedSystemsBdrDTOResponseEntityOK = new ResponseEntity<>(connectedSystemsBdrModels,
                HttpStatus.OK);
        when(dataCatalogService.getAllBulkDataRepositories()).thenReturn(connectedSystemsBdrDTOResponseEntityNotOK)
                .thenReturn(connectedSystemsBdrDTOResponseEntityOK);
        startupUtil.setRetryForever(true);
        startupUtil.setMessageBusModel();
        final boolean result = startupUtil.retrySetupDataObjectUsingFileFormatRegistrationInDataCatalog(template);
        assertTrue(result, "Expected setupDataObjectUsingDataCatalog with retry forever to pass");
        assertEquals(1, startupUtil.getAttemptNo(), "Expect only one attempt for 'retry forever' case (3 retries/exit to while/reset attemptNo)");
        assertEquals(2, startupUtil.getRetryForeverAttemptNo(), "Expect two 'retryForever' attempts for 'retry forever' case");
    }


    /**
     * Test set Message when message bus DTO with no response body throws exception.
     */
    @Test
    @Order(10)
    public void test_setMessage_retryGetMessageBusDTOWithNoResponseBodyThrowsException() throws UnsatisfiedExternalDependencyException {
        setupHttpOkResponseTestCase(HttpStatus.OK);
        startupUtil.setMessageBusModel();
        final ResponseEntity<MessageBusModel[]> responseEntityMesssageBusDtoNull = new ResponseEntity<>(null, HttpStatus.OK);
        when(dataCatalogService.getMessageBusByParams(dataCatalogProperties.getMessageBusName(), dataCatalogProperties.getMessageBusNamespace()))
                .thenReturn(responseEntityMesssageBusDtoNull);

        Exception exception = assertThrows(UnsatisfiedExternalDependencyException.class, () -> {
            startupUtil.setMessageBusModel();
        });
        String expectedMessage = "Cannot get MessageBusDTO from Data Catalog";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));

        final ResponseEntity<MessageBusModel[]> responseEntityMesssageBusDtoEmpty = new ResponseEntity<>(ArrayUtils.toArray(), HttpStatus.OK);
        when(dataCatalogService.getMessageBusByParams(dataCatalogProperties.getMessageBusName(), dataCatalogProperties.getMessageBusNamespace()))
                .thenReturn(responseEntityMesssageBusDtoEmpty);
        Exception exception2 = assertThrows(UnsatisfiedExternalDependencyException.class, () -> {
            startupUtil.setMessageBusModel();
        });
        String expectedMessage2 = "Cannot get MessageBusDTO from Data Catalog";
        String actualMessage2 = exception.getMessage();
        assertTrue(actualMessage2.contains(expectedMessage2));
    }

    /**
     * Test setup data object using data catalog Create Bucket failure.
     */
    @Test
    @Order(11)
    public void test_setupDataObjectUsingDataCatalog_createBucketFailure() {
        setupHttpOkResponseTestCase(HttpStatus.OK);
        when(bdrComponent.createBucket(anyString())).thenReturn(false);
        assertFalse(startupUtil.setupBDRBucketUsingDataCatalog());
        assertRetries();
    }

    private void assertRetries() {
        assertEquals(EXPECTED_RETRY_COUNT_MAX, startupUtil.getAttemptNo(),
                "Expected " + EXPECTED_RETRY_COUNT_MAX + " Retries, read from values.yaml");
        assertEquals(EXPECTED_BACKOFF_TIMEOUT_MS, startupUtil.getAvailabilityBackoffTimeInMs(),
                "Expected " + EXPECTED_BACKOFF_TIMEOUT_MS + " ms for backoff Time, read from values.yaml");
        assertEquals(1, startupUtil.getRetryForeverAttemptNo(), "Expect only one attempt (retryForeverAttemptNo)");
    }


    public void setupHttpOkResponseTestCase(final HttpStatus httpStatus) {

        final DataProviderTypeModel dataProviderTypeModel = DataProviderTypeModel.builder()
                .providerTypeId(dataServiceProperties.getDataProvider().getName())
                .dataCategory(dataServiceProperties.getDataCategory().getName())
                .providerVersion(dataServiceProperties.getDataProvider().getVersion())
                .dataSpace(dataSpace4GModel)
                .build();
        final NotificationTopicModel notificationTopicModel = NotificationTopicModel.builder()
                .name(dataServiceProperties.getDataServiceName())
                .encoding(encoding)
                .messageBus(messageBus)
                .messageBusId(messageBus.getId())
                .fileFormatIds(new ArrayList<>())
                .specificationReference(dataServiceProperties.getNotificationTopic().getSpecificationReference())
                .build();

        final FileFormatModel fileFormatModel = FileFormatModel.builder().dataEncoding(dataEncoding).specificationReference(specificationReference)
                .bulkDataRepository(this.bulkDataRepositoryModel).build();
        final DataCategoryModel dataCategoryModel = new DataCategoryModel();
        final DataServiceModel dataServiceModel = new DataServiceModel();
        final DataServiceInstanceModel dataServiceInstanceModel = new DataServiceInstanceModel();
        final SupportedPredicateParameterModel supportedPredicateParameterModel = new SupportedPredicateParameterModel();
        final DataSpaceModel dataSpaceModel = new DataSpaceModel();
        final DataTypeModel dataTypeModel = new DataTypeModel();

        final FileFormatApiModel fileFormatApiModel = FileFormatApiModel.builder()
                .dataSpace(dataSpaceModel)
                .dataService(dataServiceModel).dataServiceInstance(dataServiceInstanceModel)
                .supportedPredicateParameter(supportedPredicateParameterModel)
                .dataCategory(dataCategoryModel)
                .dataProviderType(dataProviderTypeModel)
                .notificationTopic(notificationTopicModel)
                .fileFormat(fileFormatModel).dataType(dataTypeModel).build();

        final ResponseEntity<Optional<FileFormatApiModel>> responseEntityFileFormatAPIDTO = new ResponseEntity<>(Optional.of(fileFormatApiModel), httpStatus);

        final ResponseEntity<MessageBusModel[]> responseEntityMessageBusDTO = new ResponseEntity<>(ArrayUtils.toArray(messageBus), HttpStatus.OK); //OK Only

        final ResponseEntity<BulkDataRepositoryModel[]> connectedSystemsBdrDTOResponseEntity = new ResponseEntity<>(connectedSystemsBdrModels,
                HttpStatus.OK); //OK Only

        when(dataCatalogService.registerFileFormatUsingPut(any(FileFormatApiModel.class))).thenReturn(responseEntityFileFormatAPIDTO);
        when(bdrProperties.getName()).thenReturn("testBDR");
        when(bdrProperties.getNamespace()).thenReturn("testNS");
        when(
                dataCatalogService.getMessageBusByParams(anyString(), anyString()))
                .thenReturn(responseEntityMessageBusDTO);

        when(dataCatalogService.getAllBulkDataRepositories()).thenReturn(connectedSystemsBdrDTOResponseEntity);
        when(bdrComponent.createBucket(anyString())).thenReturn(true);
        startupUtil.setRetryForever(false);
    }

    private void setupHttpConflictResponseTestCase() {
        connectedSystemsBdrModels[0] = connectedSystemsBdrModel1;
        connectedSystemsBdrModels[1] = connectedSystemsBdrModel2;
        final NotificationTopicModel notificationTopicModel = NotificationTopicModel.builder()
                .name(dataServiceProperties.getDataServiceName())
                .encoding(encoding)
                .messageBus(messageBus)
                .messageBusId(messageBus.getId())
                .fileFormatIds(new ArrayList<>())
                .specificationReference(dataServiceProperties.getNotificationTopic().getSpecificationReference())
                .build();
        final List<NotificationTopicModel> notificationTopicModelsList = new ArrayList<>();
        notificationTopicModelsList.add(notificationTopicModel);

        final ResponseEntity<MessageBusModel[]> responseEntity = new ResponseEntity<>(ArrayUtils.toArray(messageBus), HttpStatus.OK);
        final ResponseEntity<BulkDataRepositoryModel[]> connectedSystemsBdrDTOResponseEntity = new ResponseEntity<>(connectedSystemsBdrModels,
                HttpStatus.OK);

        when(dataCatalogService.getAllBulkDataRepositories()).thenReturn(connectedSystemsBdrDTOResponseEntity);
        when(bdrProperties.getName()).thenReturn("testBDR");
        when(bdrProperties.getNamespace()).thenReturn("testNS");
        when(dataCatalogService.getMessageBusByParams(anyString(),anyString()))
                .thenReturn(responseEntity);
        when(bdrComponent.createBucket(anyString())).thenReturn(true);
    }
}