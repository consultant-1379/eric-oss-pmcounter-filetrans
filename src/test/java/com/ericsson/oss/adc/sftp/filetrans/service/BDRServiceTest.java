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

import static com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel.FileRepoType.S3;
import static com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel.FileRepoType.SFTP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.ericsson.oss.adc.sftp.filetrans.bdr.DataCatalogBasedBdrEndpointSupplier;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel;
import com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil;

import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.UploadObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.messages.Bucket;
import io.minio.messages.Contents;
import io.minio.messages.Item;


@SpringBootTest(classes = {BDRService.class, BDRServiceTest.class, SFTPProcessingMetricsUtil.class})
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BDRServiceTest {

    private static final String FILE_1_TXT = "file1.xml";
    private static final String FILE_2_TXT = "file2.xml";
    private static final String BUCKET_NAME = "testbucket";
    private static final byte[] TEXT_DATA_BYTES = "text data".getBytes();

    @Autowired
    private BDRService bdrService;

    @MockBean
    private DataCatalogBasedBdrEndpointSupplier dataCatalogBasedBDREndpointSupplier;

    @MockBean
    private DataCatalogService dataCatalogService;

    @MockBean
    SFTPProcessingMetricsUtil metrics;

    @Mock
    private final MinioClient minioClient = mock(MinioClient.class);

    @Mock
    List<String> list = mock(ArrayList.class);

    ObjectWriteResponse objectWriteResponse = mock(ObjectWriteResponse.class);
    private final BulkDataRepositoryModel[] connectedSystemsBdrDTOs = new BulkDataRepositoryModel[2];

    private final BulkDataRepositoryModel connectedSystemsBdrDTO2 = BulkDataRepositoryModel.builder()
            .name("testBDR1")
            .clusterName("testCluster")
            .nameSpace("testNS")
            .accessEndpoints(new ArrayList<>(Collections.singletonList("http://endpoint1:1234")))
            .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
            .fileRepoType(S3)
            .build();

    private final BulkDataRepositoryModel connectedSystemsBdrDTO1 = BulkDataRepositoryModel.builder()
            .name("testBDR2")
            .clusterName("testCluster")
            .nameSpace("testNS")
            .accessEndpoints(new ArrayList<>(Collections.singletonList("http://endpoint1:1234")))
            .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
            .fileRepoType(SFTP)
            .build();

    @BeforeEach
    public void init(){
        connectedSystemsBdrDTOs[0] = connectedSystemsBdrDTO1;
        connectedSystemsBdrDTOs[1] = connectedSystemsBdrDTO2;
        ReflectionTestUtils.setField(bdrService, "numConnectionAttemptsMax", 3);
        ReflectionTestUtils.setField(bdrService, "retryIntervalMs", 100);
    }

    @Test
    @Order(1)
    public void testBDRCreateBucketFailScenaio() {
        final ResponseEntity<BulkDataRepositoryModel[]> connectedSystemsBdrDTOResponseEntity = new ResponseEntity<>(connectedSystemsBdrDTOs, HttpStatus.OK);
        Mockito.when(dataCatalogService.getAllBulkDataRepositories()).thenReturn(connectedSystemsBdrDTOResponseEntity);
        Mockito.when(dataCatalogBasedBDREndpointSupplier.get()).thenReturn("http://endpoint1:1234");
        bdrService.initMinioClient("key","seckey");
        assertFalse(bdrService.createBucket(BUCKET_NAME));
    }

    @Test
    public void testBDRCreateBucket() throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, ErrorResponseException {
        ReflectionTestUtils.setField(bdrService, "minioClient", minioClient);
        doNothing().when(minioClient).makeBucket(Mockito.any(MakeBucketArgs.class));
        assertTrue(bdrService.createBucket(BUCKET_NAME));
        when(minioClient.listBuckets()).thenReturn(Arrays.asList(new Bucket()));
        assertThat(bdrService.getBucketsList()).hasSize(1);
        Mockito.verify(minioClient, Mockito.times(1)).makeBucket(Mockito.any(MakeBucketArgs.class));
    }

    @Test
    public void testBDRCreateBucketIfExists() throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, ErrorResponseException {
        ReflectionTestUtils.setField(bdrService, "minioClient", minioClient);
        when(minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build())).thenReturn(true);
        assertTrue(bdrService.createBucket(BUCKET_NAME));
        when(minioClient.listBuckets()).thenReturn(Arrays.asList(new Bucket()));
        assertThat(bdrService.getBucketsList()).hasSize(1);
        Mockito.verify(minioClient, Mockito.times(1)).bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build());
    }

    @Test
    public void testBDRGetBucketListFailure() throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, ErrorResponseException {
        ReflectionTestUtils.setField(bdrService, "minioClient", minioClient);
        doThrow(new IOException("testBDRGetBucketListFailure")).when(minioClient).listBuckets();
        assertThat(bdrService.getBucketsList()).hasSize(0);
        Mockito.verify(minioClient, Mockito.times(1)).listBuckets();
    }

    @Test
    public void testBDRListObjectsFailure() throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, ErrorResponseException {
        ReflectionTestUtils.setField(bdrService, "minioClient", minioClient);
        final List<Result<Item>> resultItemList = new ArrayList<>();
        final Contents contents = new Contents(FILE_1_TXT);
        final Result<Item> result = new Result<Item>(contents);
        resultItemList.add(result);

        ReflectionTestUtils.setField(bdrService, "objectsList", list);
        when(minioClient.listObjects(Mockito.any(ListObjectsArgs.class))).thenReturn(resultItemList);
        when(list.add(Mockito.any(String.class))).thenAnswer(invocation -> { throw new IOException(); });
        assertThat(bdrService.listObjects(BUCKET_NAME, BDRService.NO_PREFIX)).hasSize(0);
        Mockito.verify(minioClient, Mockito.times(1)).listObjects(Mockito.any(ListObjectsArgs.class));
    }

    @Test
    public void testBDRListObjectsEmpty()  {
        ReflectionTestUtils.setField(bdrService, "minioClient", null);
        assertThat(bdrService.listObjects(BUCKET_NAME, BDRService.NO_PREFIX)).hasSize(0);
    }



    @Test
    public void testGetMinioClientNotNull() {
        ReflectionTestUtils.setField(bdrService,"minioClient",minioClient);
        MinioClient miClient = (MinioClient)ReflectionTestUtils.getField(bdrService,"minioClient");
        assertTrue(miClient != null);
    }

    @Test
    public void testBDRCreateBucketThrowEx() {
        ReflectionTestUtils.setField(bdrService, "minioClient", minioClient);
        final byte[] objectBytes = "MINIO Object test store ".getBytes();
        bdrService.uploadObject(BUCKET_NAME, "test", objectBytes);
        final List<Result<Item>> resultItemList = new ArrayList<>();
        when(minioClient.listObjects(Mockito.any(ListObjectsArgs.class))).thenReturn(resultItemList);
        assertThat(bdrService.listObjects(BUCKET_NAME, BDRService.NO_PREFIX)).hasSize(0);
    }

    @Test
    public void testBDRFileUpload() throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, ErrorResponseException {
        ReflectionTestUtils.setField(bdrService, "minioClient", minioClient);
        when(minioClient.uploadObject(Mockito.any(UploadObjectArgs.class))).thenReturn(objectWriteResponse);
        final List<Result<Item>> resultItemList = new ArrayList<>();
        final Contents contents = new Contents(FILE_1_TXT);
        final Result<Item> result = new Result<Item>(contents);
        resultItemList.add(result);

        List<String> initializedList = new ArrayList<>();
        ReflectionTestUtils.setField(bdrService, "objectsList", initializedList);

        when(minioClient.listObjects(Mockito.any(ListObjectsArgs.class))).thenReturn(resultItemList);
        final ObjectWriteResponse objectInfo = new ObjectWriteResponse(null, BUCKET_NAME, "region", FILE_1_TXT, "etag", "versionId");
        when(minioClient.putObject(Mockito.any(PutObjectArgs.class))).thenReturn(objectInfo);
        final boolean isUploaded = bdrService.uploadObject(BUCKET_NAME, FILE_1_TXT, TEXT_DATA_BYTES);
        Assertions.assertTrue(isUploaded, "Expected BDR Service to upload file");
        assertThat(bdrService.listObjects(BUCKET_NAME, BDRService.NO_PREFIX)).anyMatch(name -> name.equals(FILE_1_TXT));
        Mockito.verify(minioClient, Mockito.times(1)).putObject(Mockito.any(PutObjectArgs.class));

    }

    @Test
    public void testBDRFileUploadFailure() throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, ErrorResponseException {
        ReflectionTestUtils.setField(bdrService, "minioClient", minioClient);
        doThrow(new IOException("testBDRFileUploadFailure")).when(minioClient).putObject(Mockito.any(PutObjectArgs.class));
        final List<Result<Item>> resultItemList = new ArrayList<>();
        when(minioClient.listObjects(Mockito.any(ListObjectsArgs.class))).thenReturn(resultItemList);

        assertFalse(bdrService.uploadObject(BUCKET_NAME, FILE_2_TXT, TEXT_DATA_BYTES));
        assertThat(bdrService.listObjects(BUCKET_NAME, BDRService.NO_PREFIX)).hasSize(0);
        Mockito.verify(minioClient, Mockito.times(3)).putObject(Mockito.any(PutObjectArgs.class));
    }
}
