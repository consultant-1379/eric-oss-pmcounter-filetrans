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
import com.ericsson.oss.adc.sftp.filetrans.model.FileFormatModel;
import com.ericsson.oss.adc.sftp.filetrans.model.MessageBusModel;
import com.ericsson.oss.adc.sftp.filetrans.rest.RestTemplateFacade;
import com.ericsson.oss.adc.sftp.filetrans.util.RestExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel.FileRepoType.SFTP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;


/**
 * The Class DataCatalogServiceContractTest.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {"spring.kafka.topics.enm_id="},
        classes = {
                DataCatalogService.class,
                DataCatalogProperties.class,
                DataServiceProperties.class,
                RestExecutor.class,
                RestTemplate.class,
                RestTemplateFacade.class,
                ObjectMapper.class,
        })
@AutoConfigureStubRunner(repositoryRoot = "https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-release-local",
        stubsMode = StubRunnerProperties.StubsMode.REMOTE,
        ids = "com.ericsson.oss.dmi:eric-oss-data-catalog:+:stubs:9590")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataCatalogServiceContractTest {

    @Autowired
    private DataCatalogService dataCatalogService;

    @Autowired
    private ObjectMapper objectMapper;


    private FileFormatApiModel fileFormatApiModel;

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



    @BeforeAll
    void initDtosFromJson() throws IOException {
        fileFormatApiModel = objectMapper.readValue(
                new FileReader(getClass().
                        getClassLoader().
                        getResource("models/sampleFileFormatV2.json").
                        getFile()),
                FileFormatApiModel.class);
    }


    /**
     * Test create for all data catalog when data already exists in data catalog.
     */
    @Test
    @DisplayName("Test creation of Data Catalog using PUT - V2")
    void test_registerFileFormatV2InDataCatalogUsingPUT() {
        final HttpStatus httpStatus = (HttpStatus) dataCatalogService.registerFileFormatUsingPut(fileFormatApiModel).getStatusCode();
        assertEquals(HttpStatus.NOT_FOUND, httpStatus);
    }

    /**
     * Test get message bus by params.
     *
     * The test is currently failing in the master branch probably due to some issue from the data catalog side.
     * Would be uncommented when its fixed.
     */
    /*@Test
    @DisplayName("Should successfully retrieve Message Bus entry")
    void test_getMessageBusByParams() {
        final ResponseEntity<MessageBusModel[]> getResponse = dataCatalogService.getMessageBusByParams("name", "nameSpace");
        assertEquals(messageBus.getName(), getResponse.getBody()[0].getName());
    }*/


    /**
     * Test get connected systems from data catalog.
     */
    @Test
    void test_getConnectedSystemsFromDataCatalog() {
        final ResponseEntity<BulkDataRepositoryModel[]> getResponse = dataCatalogService.getAllBulkDataRepositories();

        if (getResponse.getBody() != null) {
            for (final BulkDataRepositoryModel bulkDataRepositoryModel : getResponse.getBody()) {
                if (bulkDataRepositoryModel.getFileRepoType().equals(SFTP)) {
                    assertEquals(new ArrayList<>(Collections.singletonList("http://endpoint1:1234/")), bulkDataRepositoryModel.getAccessEndpoints());
                }
            }
        }
    }

    /**
     * Test rest executor post for entity for invalid input.
     */
    @Test
    @DisplayName("TEST: Verify DataCatalogService's Rest Executor PostForEntity returns SERVICE_UNAVAILABLE for invalid url")
    void test_restExecutorPostForEntityForInvalidInput() {
        final String url = "http://unknown/catalog/v1/file-format/";
        final String logMessage = "FileFormat";
        final Map<String, Object> httpBody = generateFileFormatHttpBody();
        final Class<?> responseType = FileFormatModel.class;
        final ResponseEntity<FileFormatModel> resp = (ResponseEntity<FileFormatModel>) dataCatalogService.restExecutorPostForEntity(url, logMessage,
                httpBody, responseType);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, resp.getStatusCode());
    }

    /**
     * Test request post notification structure and rest executor post for entity with json processing exception thrown.
     *
     * @throws JsonProcessingException the json processing exception
     */
    @Test
    @DisplayName("TEST: Verify DataCatalogService's Request Post Notification Structure returns 'null' HttpEntity, if a 'JsonProcessingException' is thrown, and "
            + "Rest Executor PostForEntity returns BAD_REQUEST as a result of this exception.")
    void test_requestPostNotificationStructureAndRestExecutorPostForEntityWithJsonProcessingExceptionThrown() throws JsonProcessingException {
        final ObjectMapper objectMapper = mock(ObjectMapper.class);
        final String logMessage = "FileFormat";
        final Map<String, Object> httpBody = null;
        doThrow(new JsonProcessingExceptionImpl("Error occurred")).when(objectMapper).writeValueAsString(httpBody);
        final HttpEntity<String> httpEntity = dataCatalogService.requestPostNotificationStructure(objectMapper, httpBody, logMessage);
        assertTrue(httpEntity == null);

        final String url = "http://unknown/catalog/v1/file-format/";
        final Class<?> responseType = FileFormatModel.class;
        final ResponseEntity<FileFormatModel> resp = (ResponseEntity<FileFormatModel>) dataCatalogService.restExecutorPostForEntity(url, logMessage,
                httpBody, responseType, objectMapper);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    /**
     * Test request post notification structure happ case.
     *
     * @throws JsonProcessingException the json processing exception
     */
    @Test
    @DisplayName("TEST: Verify DataCatalogService's Request Post Notification Structure returns valid HttpEntity for valid inputs")
    void test_requestPostNotificationStructureHappyCase() throws JsonProcessingException {
        final String logMessage = "FileFormat";
        final Map<String, Object> httpBody = generateFileFormatHttpBody();
        final String expectedHttpEntity = "<{\"dataEncoding\":\"XML\",\"dataProviderTypeId\":4,\"dataCollectorId\":null,\"notificationTopicId\":1,\"bulkDataRepositoryId\":1,\"specificationReference\":\"\",\"reportOutputPeriodList\":[15]},[Content-Type:\"application/json\"]>";
        final HttpEntity<String> httpEntity = dataCatalogService.requestPostNotificationStructure(httpBody, logMessage);
        assertEquals(expectedHttpEntity, httpEntity.toString());
    }

    private Map<String, Object> generateFileFormatHttpBody() {
        final Map<String, Object> fileFormatBody = new LinkedHashMap<>(7);
        fileFormatBody.put("dataEncoding", "XML");
        fileFormatBody.put("dataProviderTypeId", 4);
        fileFormatBody.put("dataCollectorId", null);
        fileFormatBody.put("notificationTopicId", 1);
        fileFormatBody.put("bulkDataRepositoryId", 1);
        fileFormatBody.put("specificationReference", "");
        final long[] longArr = {15};
        fileFormatBody.put("reportOutputPeriodList", longArr);
        return fileFormatBody;
    }


}
