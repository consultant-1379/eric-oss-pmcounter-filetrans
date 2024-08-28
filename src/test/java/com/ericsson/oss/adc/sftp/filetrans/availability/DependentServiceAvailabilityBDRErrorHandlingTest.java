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

package com.ericsson.oss.adc.sftp.filetrans.availability;

import com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel;
import com.ericsson.oss.adc.sftp.filetrans.service.BDRService;
import com.ericsson.oss.adc.sftp.filetrans.service.DataCatalogService;
import com.ericsson.oss.adc.sftp.filetrans.util.RestExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;

import static com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel.FileRepoType.S3;
import static com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel.FileRepoType.SFTP;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.atLeast;

@SpringBootTest(classes = {DependentServiceAvailabilityBDR.class, BDRService.class, DataCatalogService.class, BulkDataRepositoryModel.class, RestExecutor.class})
@AutoConfigureWebClient(registerRestTemplate = true)
public class DependentServiceAvailabilityBDRErrorHandlingTest {
    @Autowired
    DependentServiceAvailabilityBDR dependentServiceAvailabilityBDR;

    @MockBean
    private BDRService bdrServiceMock;

    @MockBean
    private DataCatalogService dataCatalogServiceMock;

    @Mock
    private BulkDataRepositoryModel bulkDataRepositoryModelMock;
    @Test
    @DisplayName("When bulk data repository is null in data catalog, availability should be false")
    public void check_health_check_when_bulk_data_repository_null() {
        Mockito.when(dataCatalogServiceMock.getAllBulkDataRepositories()).thenReturn(null);
        assertFalse(dependentServiceAvailabilityBDR.checkHealth());
        Mockito.verify(dataCatalogServiceMock, atLeast(3)).getAllBulkDataRepositories();
    }

    @Test
    @DisplayName("When access endpoints in bulk data repository is null in data catalog, availability should be false")
    public void check_health_check_when_access_endpoints_null() {
        Mockito.when(dataCatalogServiceMock.getAllBulkDataRepositories()).thenReturn(null);
        assertFalse(dependentServiceAvailabilityBDR.checkHealth());

        final BulkDataRepositoryModel[] connectedSystemsBdrDTOs = new BulkDataRepositoryModel[2];
        connectedSystemsBdrDTOs[0] = BulkDataRepositoryModel.builder()
                .name("testBDR2")
                .clusterName("testCluster")
                .nameSpace("testNS")
                .accessEndpoints(new ArrayList<>(Collections.singletonList("http://endpoint1:1234")))
                .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
                .fileRepoType(S3)
                .build();

        connectedSystemsBdrDTOs[1] = BulkDataRepositoryModel.builder()
                .name("testBDR1")
                .clusterName("testCluster")
                .nameSpace("testNS")
                .accessEndpoints(new ArrayList<>(Collections.singletonList("http://endpoint1:1234")))
                .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
                .fileRepoType(SFTP)
                .build();

        final ResponseEntity<BulkDataRepositoryModel[]> connectedSystemsBdrDTOResponseEntity = new ResponseEntity<>(connectedSystemsBdrDTOs,
                HttpStatus.OK);
        Mockito.when(dataCatalogServiceMock.getAllBulkDataRepositories()).thenReturn(connectedSystemsBdrDTOResponseEntity);
        Mockito.when(bdrServiceMock.getAccessEndPointsFromDataCatalog(Mockito.any(ResponseEntity.class))).thenReturn(null);
        assertFalse(dependentServiceAvailabilityBDR.checkHealth());
        Mockito.verify(bdrServiceMock, atLeast(3)).getAccessEndPointsFromDataCatalog(Mockito.any(ResponseEntity.class));
    }

    @Test
    @DisplayName("When access endpoints in bulk data repository is null in data catalog, availability should be false")
    public void check_health_check_when_access_endpoints_null2() {
        Mockito.when(dataCatalogServiceMock.getAllBulkDataRepositories()).thenReturn(null);
        assertFalse(dependentServiceAvailabilityBDR.checkHealth());

        final BulkDataRepositoryModel[] connectedSystemsBdrDTOs = new BulkDataRepositoryModel[2];
        connectedSystemsBdrDTOs[0] = BulkDataRepositoryModel.builder()
                .name("testBDR2")
                .clusterName("testCluster")
                .nameSpace("testNS")
                .accessEndpoints(new ArrayList<>(Collections.singletonList("http://endpoint1:1234")))
                .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
                .fileRepoType(S3)
                .build();

        connectedSystemsBdrDTOs[1] = BulkDataRepositoryModel.builder()
                .name("testBDR1")
                .clusterName("testCluster")
                .nameSpace("testNS")
                .accessEndpoints(new ArrayList<>(Collections.singletonList("http://endpoint1:1234")))
                .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
                .fileRepoType(SFTP)
                .build();

        final ResponseEntity<BulkDataRepositoryModel[]> connectedSystemsBdrDTOResponseEntity = new ResponseEntity<>(connectedSystemsBdrDTOs,
                HttpStatus.OK);
        Mockito.when(dataCatalogServiceMock.getAllBulkDataRepositories()).thenReturn(connectedSystemsBdrDTOResponseEntity);
        Mockito.when(bdrServiceMock.getAccessEndPointsFromDataCatalog(Mockito.any(ResponseEntity.class))).thenReturn(bulkDataRepositoryModelMock);
        Mockito.when(bulkDataRepositoryModelMock.getAccessEndpoints()).thenReturn(new ArrayList<>());
        assertFalse(dependentServiceAvailabilityBDR.checkHealth());
    }
}
