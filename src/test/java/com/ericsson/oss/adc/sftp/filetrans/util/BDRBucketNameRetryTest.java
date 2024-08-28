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

import com.ericsson.oss.adc.sftp.filetrans.bdr.DataCatalogBasedBdrEndpointSupplier;
import com.ericsson.oss.adc.sftp.filetrans.configuration.properties.DataCatalogProperties;
import com.ericsson.oss.adc.sftp.filetrans.configuration.properties.DataServiceProperties;
import com.ericsson.oss.adc.sftp.filetrans.controller.BDRComponent;
import com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel;
import com.ericsson.oss.adc.sftp.filetrans.rest.RestTemplateFacade;
import com.ericsson.oss.adc.sftp.filetrans.service.ConnectedSystemsRetriever;
import com.ericsson.oss.adc.sftp.filetrans.service.DataCatalogService;
import com.ericsson.oss.adc.sftp.filetrans.service.ENMScriptingVMLoadBalancer;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;


import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;

import static com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel.FileRepoType.S3;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@SpringBootTest(classes = {StartupUtil.class, BDRS3BucketNameValidator.class, BDRComponent.class, DataCatalogService.class, RestExecutor.class, BDRProperties.class, DataCatalogProperties.class,
        ConnectedSystemsRetriever.class, ENMScriptingVMLoadBalancer.class, DataCatalogBasedBdrEndpointSupplier.class, RestTemplateFacade.class})
@AutoConfigureWebClient(registerRestTemplate = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BDRBucketNameRetryTest {

    private static final String DATA_CATALOG_BDR_URL = "http://localhost:9590/catalog/v1/bulk-data-repository/";

    @Autowired
    @InjectMocks
    StartupUtil startupUtil;

    @Autowired
    BDRS3BucketNameValidator bdrs3BucketNameValidator;

    @MockBean
    BDRComponent bdrComponent;

    @SpyBean
    DataServiceProperties dataServiceProperties;

    @Autowired
    RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private final BulkDataRepositoryModel[] bdrDTOs = new BulkDataRepositoryModel[1];

    private final BulkDataRepositoryModel bulkDataRepositoryDTOS3 = BulkDataRepositoryModel.builder()
            .name("testBDR1")
            .clusterName("testCluster")
            .nameSpace("testNS")
            .accessEndpoints(new ArrayList<>(Collections.singletonList("http://eric-data-object-storage-mn:9000")))
            .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
            .fileRepoType(S3)
            .build();

    @BeforeEach
    public void init() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        bdrDTOs[0] = bulkDataRepositoryDTOS3;
    }

    @Test
    public void testBucketCreationFailure() {
        when(dataServiceProperties.getDataServiceName()).thenReturn("..InvalidNamE..");
        final ArrayList<String> accessEndPointList = new ArrayList<>();
        accessEndPointList.add("http://endpoint1:1234/");
        when(bdrComponent.createBucket(Mockito.anyString())).thenReturn(false);
        assertFalse(startupUtil.createBdrBucket(accessEndPointList));
    }

    @Test
    public void testBucketCreationSuccess() {
        when(dataServiceProperties.getDataServiceName()).thenReturn("..InvalidNamE..");
        final ArrayList<String> accessEndPointList = new ArrayList<>();
        accessEndPointList.add("http://endpoint1:1234/");

        when(bdrComponent.createBucket(Mockito.anyString())).thenReturn(true);
        assertTrue(startupUtil.createBdrBucket(accessEndPointList));
    }

    @Test
    public void testWhenCreateBucketFails() throws Exception {
        when(dataServiceProperties.getDataServiceName()).thenReturn("..InvalidNamE..");

        final Gson gson = new Gson();
        mockServer.expect(ExpectedCount.manyTimes(),
                        requestTo(new URI(DATA_CATALOG_BDR_URL)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(gson.toJson(bdrDTOs)));

        when(bdrComponent.createBucket(Mockito.anyString())).thenReturn(false).thenReturn(false).thenReturn(true);
        assertTrue(startupUtil.setupBDRBucketUsingDataCatalog());
        Mockito.verify(bdrComponent, times(3)).createBucket(Mockito.anyString());
        final Object bucketName = startupUtil.getCreatedBucketName();
        assertNotEquals("..InvalidNamE..", bucketName.toString());
    }
    
}
