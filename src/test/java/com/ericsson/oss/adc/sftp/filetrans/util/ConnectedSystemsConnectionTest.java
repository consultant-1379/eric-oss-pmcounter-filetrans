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


import static com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel.FileRepoType.SFTP;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.ericsson.oss.adc.sftp.filetrans.availability.UnsatisfiedExternalDependencyException;
import com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel;
import com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel.FileRepoType;
import com.ericsson.oss.adc.sftp.filetrans.service.ConnectedSystemsRetriever;
import com.ericsson.oss.adc.sftp.filetrans.service.DataCatalogService;

public class ConnectedSystemsConnectionTest {

    @Spy
    @InjectMocks
    ConnectedSystemsRetriever connectedSystemsRetrieverSpy;

    private final BulkDataRepositoryModel connectedSystemsBdrDTO1 = BulkDataRepositoryModel.builder()
            .name("testBDR1")
            .clusterName("testCluster")
            .nameSpace("testNS")
            .accessEndpoints(new ArrayList<>(Collections.singletonList("http://endpoint1:1234")))
            .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
            .fileRepoType(SFTP)
            .build();

    private final BulkDataRepositoryModel connectedSystemsBdrDTO2 = BulkDataRepositoryModel.builder()
            .name("testBDR2")
            .clusterName("testCluster")
            .nameSpace("testNS")
            .accessEndpoints(new ArrayList<>(Collections.singletonList("http://endpoint2:1234")))
            .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
            .fileRepoType(SFTP)
            .build();

    private final BulkDataRepositoryModel[] connectedSystemsBdrDTOArray = new BulkDataRepositoryModel[2];

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        // Spy doesn't set these up..
        ReflectionTestUtils.setField(connectedSystemsRetrieverSpy, "retryAttempts",3);
        ReflectionTestUtils.setField(connectedSystemsRetrieverSpy, "retryInterval",1000);
        ReflectionTestUtils.setField(connectedSystemsRetrieverSpy, "isInfiniteRetry",false);
    }

    @Test
    void checkSubsystemsAvailableWhenAreSubsystemDetailsEmptyIsTrue() throws UnsatisfiedExternalDependencyException {
        Mockito.doReturn(true).when(connectedSystemsRetrieverSpy).areSubsystemDetailsPopulated();
        assertTrue(connectedSystemsRetrieverSpy.checkSubsystemDetailsAvailable());
    }

    @Test
    void checkSubsystemsAvailableWhenCheckSubsystemDetailsEmptyThrowsException() throws UnsatisfiedExternalDependencyException {
        Mockito.doThrow(new UnsatisfiedExternalDependencyException("Test")).when(connectedSystemsRetrieverSpy).areSubsystemDetailsPopulated();
        assertFalse(connectedSystemsRetrieverSpy.checkSubsystemDetailsAvailable());
    }

    @Test
    void checkAreSubsystemDetailsEmptyThrowsExceptionWhenGetSubsystemDetailsIsEmpty() throws UnsatisfiedExternalDependencyException {
        Mockito.doReturn(Collections.emptyMap()).when(connectedSystemsRetrieverSpy).getSubsystemDetails();
        Mockito.doReturn(Collections.emptyList()).when(connectedSystemsRetrieverSpy).getConnectedSystemsAccessPoints();

        final DataCatalogService dataCatalogServiceMock = mock(DataCatalogService.class);
        connectedSystemsRetrieverSpy.setDataCatalogService(dataCatalogServiceMock);
        final ResponseEntity<BulkDataRepositoryModel[]> connectedSystemsBdrDTOResponseEntity = new ResponseEntity<>(connectedSystemsBdrDTOArray,
                HttpStatus.SERVICE_UNAVAILABLE);
        Mockito.when(dataCatalogServiceMock.getAllBulkDataRepositories()).thenReturn(connectedSystemsBdrDTOResponseEntity);

        Assert.assertThrows(UnsatisfiedExternalDependencyException.class, () -> connectedSystemsRetrieverSpy.areSubsystemDetailsPopulated());
    }

    @Test
    void checkGetConnectedSystemsAccessPointsHappyCase() {
        final DataCatalogService dataCatalogServiceMock = mock(DataCatalogService.class);
        connectedSystemsRetrieverSpy.setDataCatalogService(dataCatalogServiceMock);
        connectedSystemsBdrDTOArray[0] = connectedSystemsBdrDTO1;
        connectedSystemsBdrDTOArray[1] = connectedSystemsBdrDTO2;
        final List<String> expectedConnectedSystemsAccessPoints = new ArrayList<>();
        expectedConnectedSystemsAccessPoints.addAll(connectedSystemsBdrDTO1.getAccessEndpoints());
        expectedConnectedSystemsAccessPoints.addAll(connectedSystemsBdrDTO2.getAccessEndpoints());

        final ResponseEntity<BulkDataRepositoryModel[]> connectedSystemsBdrDTOResponseEntity = new ResponseEntity<>(connectedSystemsBdrDTOArray,
                HttpStatus.OK);
        Mockito.when(dataCatalogServiceMock.getAllBulkDataRepositories()).thenReturn(connectedSystemsBdrDTOResponseEntity);
        final List<String> actualConnectedSystemsAccessPoints = connectedSystemsRetrieverSpy.getConnectedSystemsAccessPoints();
        assertEquals(expectedConnectedSystemsAccessPoints, actualConnectedSystemsAccessPoints,
                "Expected Connected Access End Points to be setup correctly");
    }

    @Test
    void checkGetConnectedSystemsAccessPointsWithOnlyS3AccessPointsAvailable() {
        final DataCatalogService dataCatalogServiceMock = mock(DataCatalogService.class);
        connectedSystemsRetrieverSpy.setDataCatalogService(dataCatalogServiceMock);
        connectedSystemsBdrDTO1.setFileRepoType(FileRepoType.S3);
        connectedSystemsBdrDTO2.setFileRepoType(FileRepoType.S3);
        connectedSystemsBdrDTOArray[0] = connectedSystemsBdrDTO1;
        connectedSystemsBdrDTOArray[1] = connectedSystemsBdrDTO2;

        final ResponseEntity<BulkDataRepositoryModel[]> connectedSystemsBdrDTOResponseEntity = new ResponseEntity<>(connectedSystemsBdrDTOArray,
                HttpStatus.OK);
        Mockito.when(dataCatalogServiceMock.getAllBulkDataRepositories()).thenReturn(connectedSystemsBdrDTOResponseEntity);
        final List<String> actualConnectedSystemsAccessPoints = connectedSystemsRetrieverSpy.getConnectedSystemsAccessPoints();
        assertEquals( Collections.emptyList(), actualConnectedSystemsAccessPoints,
                "Expected Connected Access End Points to be empty if BulkDataRepositoryDTO only contains S3 Access Points");
    }

    @Test
    void checkGetConnectedSystemsAccessPointsWithNoAccessPointsAvailable() {
        final DataCatalogService dataCatalogServiceMock = mock(DataCatalogService.class);
        connectedSystemsRetrieverSpy.setDataCatalogService(dataCatalogServiceMock);
        connectedSystemsBdrDTO1.setAccessEndpoints(new ArrayList<>());
        connectedSystemsBdrDTO2.setAccessEndpoints(new ArrayList<>());
        connectedSystemsBdrDTOArray[0] = connectedSystemsBdrDTO1;
        connectedSystemsBdrDTOArray[1] = connectedSystemsBdrDTO2;

        final ResponseEntity<BulkDataRepositoryModel[]> connectedSystemsBdrDTOResponseEntity = new ResponseEntity<>(connectedSystemsBdrDTOArray,
                HttpStatus.OK);
        Mockito.when(dataCatalogServiceMock.getAllBulkDataRepositories()).thenReturn(connectedSystemsBdrDTOResponseEntity);
        final List<String> actualConnectedSystemsAccessPoints = connectedSystemsRetrieverSpy.getConnectedSystemsAccessPoints();
        assertEquals(Collections.emptyList(), actualConnectedSystemsAccessPoints,
                "Expected Connected Access End Points to be empty if BulkDataRepositoryDTO only contains No Access Points");
    }

    @Test
    void checkGetConnectedSystemsAccessPointsWithNullFileRepoType() {
        final DataCatalogService dataCatalogServiceMock = mock(DataCatalogService.class);
        connectedSystemsRetrieverSpy.setDataCatalogService(dataCatalogServiceMock);
        connectedSystemsBdrDTO1.setFileRepoType(null);
        connectedSystemsBdrDTO2.setFileRepoType(null);
        connectedSystemsBdrDTOArray[0] = connectedSystemsBdrDTO1;
        connectedSystemsBdrDTOArray[1] = connectedSystemsBdrDTO2;

        final ResponseEntity<BulkDataRepositoryModel[]> connectedSystemsBdrDTOResponseEntity = new ResponseEntity<>(connectedSystemsBdrDTOArray,
                HttpStatus.OK);
        Mockito.when(dataCatalogServiceMock.getAllBulkDataRepositories()).thenReturn(connectedSystemsBdrDTOResponseEntity);
        final List<String> actualConnectedSystemsAccessPoints = connectedSystemsRetrieverSpy.getConnectedSystemsAccessPoints();
        assertEquals(Collections.emptyList(), actualConnectedSystemsAccessPoints,
                "Expected Connected Access End Points to be empty if BulkDataRepositoryDTO only contains null FileRepoType");
    }

    @Test
    void checkGetConnectedSystemsAccessPointsWithNullAccessEndpoints() {
        final DataCatalogService dataCatalogServiceMock = mock(DataCatalogService.class);
        connectedSystemsRetrieverSpy.setDataCatalogService(dataCatalogServiceMock);
        connectedSystemsBdrDTO1.setAccessEndpoints(null);
        connectedSystemsBdrDTO2.setAccessEndpoints(null);
        connectedSystemsBdrDTOArray[0] = connectedSystemsBdrDTO1;
        connectedSystemsBdrDTOArray[1] = connectedSystemsBdrDTO2;

        final ResponseEntity<BulkDataRepositoryModel[]> connectedSystemsBdrDTOResponseEntity = new ResponseEntity<>(connectedSystemsBdrDTOArray,
                HttpStatus.OK);
        Mockito.when(dataCatalogServiceMock.getAllBulkDataRepositories()).thenReturn(connectedSystemsBdrDTOResponseEntity);
        final List<String> actualConnectedSystemsAccessPoints = connectedSystemsRetrieverSpy.getConnectedSystemsAccessPoints();
        assertEquals(Collections.emptyList(), actualConnectedSystemsAccessPoints,
                "Expected Connected Access End Points to be empty if BulkDataRepositoryDTO only contains null Access Endpoints");
    }

    @Test
    void checkGetConnectedSystemsAccessPointsWithNotOKHttpStatus() {
        final DataCatalogService dataCatalogServiceMock = mock(DataCatalogService.class);
        connectedSystemsRetrieverSpy.setDataCatalogService(dataCatalogServiceMock);

        final ResponseEntity<BulkDataRepositoryModel[]> connectedSystemsBdrDTOResponseEntity = new ResponseEntity<>(connectedSystemsBdrDTOArray,
                HttpStatus.SERVICE_UNAVAILABLE);
        Mockito.when(dataCatalogServiceMock.getAllBulkDataRepositories()).thenReturn(connectedSystemsBdrDTOResponseEntity);
        final List<String> actualConnectedSystemsAccessPoints = connectedSystemsRetrieverSpy.getConnectedSystemsAccessPoints();
        assertEquals(Collections.emptyList(), actualConnectedSystemsAccessPoints,
                "Expected Connected Access End Points to be empty if Http Status is NOT OK");
    }

    @Test
    void checkGetConnectedSystemsAccessPointsWithResponseEntityBodyNull() {
        final DataCatalogService dataCatalogServiceMock = mock(DataCatalogService.class);
        connectedSystemsRetrieverSpy.setDataCatalogService(dataCatalogServiceMock);

        final ResponseEntity<BulkDataRepositoryModel[]> connectedSystemsBdrDTOResponseEntity = new ResponseEntity<>(null,
                HttpStatus.OK);
        Mockito.when(dataCatalogServiceMock.getAllBulkDataRepositories()).thenReturn(connectedSystemsBdrDTOResponseEntity);
        final List<String> actualConnectedSystemsAccessPoints = connectedSystemsRetrieverSpy.getConnectedSystemsAccessPoints();
        assertEquals(Collections.emptyList(), actualConnectedSystemsAccessPoints,
                "Expected Connected Access End Points to be empty if responseEntity body is null");
    }

    @Test
    void checkGetConnectedSystemsAccessPointsWithNoBulkDataRepositories() {
        final DataCatalogService dataCatalogServiceMock = mock(DataCatalogService.class);
        connectedSystemsRetrieverSpy.setDataCatalogService(dataCatalogServiceMock);

        Mockito.when(dataCatalogServiceMock.getAllBulkDataRepositories()).thenReturn(null);
        final List<String> actualConnectedSystemsAccessPoints = connectedSystemsRetrieverSpy.getConnectedSystemsAccessPoints();
        assertEquals(Collections.emptyList(), actualConnectedSystemsAccessPoints,
                "Expected Connected Access End Points to be empty if there are no BulkDAtaRepositories");
    }

    @Test
    void checkAreSubsystemDetailsEmptyThrowsExceptionWhenConnectedSystemsAccessPointsIsNotEmpty() throws UnsatisfiedExternalDependencyException {
        Mockito.doReturn(Collections.emptyMap()).when(connectedSystemsRetrieverSpy).getSubsystemDetails();

        final List<String> connectedSystemsAccessPoints = new ArrayList<>();
        connectedSystemsAccessPoints.addAll(connectedSystemsBdrDTO1.getAccessEndpoints());
        connectedSystemsAccessPoints.addAll(connectedSystemsBdrDTO2.getAccessEndpoints());
        Mockito.doReturn(connectedSystemsAccessPoints).when(connectedSystemsRetrieverSpy).getConnectedSystemsAccessPoints();

        Assert.assertThrows(UnsatisfiedExternalDependencyException.class, () -> connectedSystemsRetrieverSpy.areSubsystemDetailsPopulated());
    }
}
