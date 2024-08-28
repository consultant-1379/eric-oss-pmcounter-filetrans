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
package com.ericsson.oss.adc.sftp.filetrans.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

import com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The Class BDRServiceBasicTest.
 * This class is used for testing unit logic in the BDRService class that does not need the spring framework.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BDRServiceBasicTest {
    private static final String TEST_STRING_50 = "TEST_TEST_TEST_TEST_TEST_TEST_TEST_TEST_TEST_TEST_";
    private static final String BUCKET_NAME = "enm1";
    private static final String OBJECT_NAME = "XML";
    private static final long PART_SIZE_AUTO_DETECT = -1L;

    private final int numConnectionAttemptsMax = 1;
    private final int retryIntervalMs = 1000;
    private final byte[] objectBytes = TEST_STRING_50.getBytes();

    private final SFTPProcessingMetricsUtil metrics = new SFTPProcessingMetricsUtil(new SimpleMeterRegistry());
    final MinioClient minioClient = mock(MinioClient.class);

    final MinioClient minioClientReal = MinioClient.builder().endpoint("test").build();

    /**
     * Verify BDR service upload object happy case.
     *
     * @throws InvalidKeyException
     *             the invalid key exception
     * @throws ErrorResponseException
     *             the error response exception
     * @throws InsufficientDataException
     *             the insufficient data exception
     * @throws InternalException
     *             the internal exception
     * @throws InvalidResponseException
     *             the invalid response exception
     * @throws NoSuchAlgorithmException
     *             the no such algorithm exception
     * @throws ServerException
     *             the server exception
     * @throws XmlParserException
     *             the xml parser exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    @DisplayName("Verify BDRService Upload Object - happy case.")
    void verifyBDRServiceUploadObject_happyCase() throws InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException,
        InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
        final CommonObjects co = new CommonObjects(numConnectionAttemptsMax);
        when(minioClient.putObject(Mockito.any(PutObjectArgs.class))).thenReturn(co.objectInfo);
        ReflectionTestUtils.setField(co.bdrService, "minioClient", minioClient);

        final boolean isUploaded = co.bdrService.uploadObject(BUCKET_NAME, OBJECT_NAME, objectBytes);
        assertTrue("Expected BDR upload to be successful for happ case test", isUploaded);
    }

    /**
     * Verify BDR service upload object exception thrown.
     *
     * @throws InvalidKeyException
     *             the invalid key exception
     * @throws ErrorResponseException
     *             the error response exception
     * @throws InsufficientDataException
     *             the insufficient data exception
     * @throws InternalException
     *             the internal exception
     * @throws InvalidResponseException
     *             the invalid response exception
     * @throws NoSuchAlgorithmException
     *             the no such algorithm exception
     * @throws ServerException
     *             the server exception
     * @throws XmlParserException
     *             the xml parser exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    @DisplayName("Verify BDRService Upload Object fails when exception thrown.")
    void verifyBDRServiceUploadObject_exceptionThrown() throws InvalidKeyException, ErrorResponseException, InsufficientDataException,
        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
        final CommonObjects co = new CommonObjects(numConnectionAttemptsMax);
        when(minioClient.putObject(Mockito.any(PutObjectArgs.class))).thenReturn(co.objectInfo);
        ReflectionTestUtils.setField(co.bdrService, "minioClient", minioClient);

        final boolean isUploaded = co.bdrService.uploadObject(BUCKET_NAME, OBJECT_NAME, null);
        assertFalse("Expected BDR upload to fail when bytes to upload is null", isUploaded);
    }

    /**
     * Verify BDR service upload object zero bytes.
     *
     * @throws InvalidKeyException
     *             the invalid key exception
     * @throws ErrorResponseException
     *             the error response exception
     * @throws InsufficientDataException
     *             the insufficient data exception
     * @throws InternalException
     *             the internal exception
     * @throws InvalidResponseException
     *             the invalid response exception
     * @throws NoSuchAlgorithmException
     *             the no such algorithm exception
     * @throws ServerException
     *             the server exception
     * @throws XmlParserException
     *             the xml parser exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    @DisplayName("Verify BDRService Upload Object fails when object bytes to upload is zero.")
    void verifyBDRServiceUploadObject_zeroBytes() throws InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException,
        InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
        final CommonObjects co = new CommonObjects(numConnectionAttemptsMax);
        when(minioClient.putObject(Mockito.any(PutObjectArgs.class))).thenReturn(co.objectInfo);
        ReflectionTestUtils.setField(co.bdrService, "minioClient", minioClient);

        final boolean isUploaded = co.bdrService.uploadObject(BUCKET_NAME, OBJECT_NAME, new byte[] {});
        assertFalse("Expected BDR upload to fail when bytes to upload is zero", isUploaded);
    }

    /**
     * Verify BDR service upload object do upload is false.
     *
     * @throws InvalidKeyException
     *             the invalid key exception
     * @throws ErrorResponseException
     *             the error response exception
     * @throws InsufficientDataException
     *             the insufficient data exception
     * @throws InternalException
     *             the internal exception
     * @throws InvalidResponseException
     *             the invalid response exception
     * @throws NoSuchAlgorithmException
     *             the no such algorithm exception
     * @throws ServerException
     *             the server exception
     * @throws XmlParserException
     *             the xml parser exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    @DisplayName("Verify BDRService Upload Object fails when doUpload returns false.")
    void verifyBDRServiceUploadObject_doUploadIsFalse() throws InvalidKeyException, ErrorResponseException, InsufficientDataException,
        InternalException, InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
        final CommonObjects co = new CommonObjects(numConnectionAttemptsMax);
        when(minioClient.putObject(Mockito.any(PutObjectArgs.class))).thenReturn(null);
        ReflectionTestUtils.setField(co.bdrService, "minioClient", minioClient);

        final boolean isUploaded = co.bdrService.uploadObject(BUCKET_NAME, OBJECT_NAME, new byte[] {});
        assertFalse("Expected BDR upload to fail when doUpload returns false", isUploaded);
    }

    /**
     * Verify BDR service do upload happy case.
     *
     * @throws InvalidKeyException
     *             the invalid key exception
     * @throws ErrorResponseException
     *             the error response exception
     * @throws InsufficientDataException
     *             the insufficient data exception
     * @throws InternalException
     *             the internal exception
     * @throws InvalidResponseException
     *             the invalid response exception
     * @throws NoSuchAlgorithmException
     *             the no such algorithm exception
     * @throws ServerException
     *             the server exception
     * @throws XmlParserException
     *             the xml parser exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    @DisplayName("Verify BDRService doUpload Object - happy case.")
    void verifyBDRServiceDoUpload_happyCase() throws InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException,
    InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
        final CommonObjects co = new CommonObjects(numConnectionAttemptsMax);
        when(minioClient.putObject(Mockito.any(PutObjectArgs.class))).thenReturn(co.objectInfo);
        ReflectionTestUtils.setField(co.bdrService, "minioClient", minioClient);

        final boolean isUploaded = co.bdrService.doUpload(OBJECT_NAME, objectBytes, BUCKET_NAME, co.byteArrayInputStream);
        co.byteArrayInputStream.close();
        assertTrue("Expected BDR doUpload to pass - happy case", isUploaded);
    }

    /**
     * Verify BDR service do upload num connection attempts max exceeded.
     *
     * @throws InvalidKeyException
     *             the invalid key exception
     * @throws ErrorResponseException
     *             the error response exception
     * @throws InsufficientDataException
     *             the insufficient data exception
     * @throws InternalException
     *             the internal exception
     * @throws InvalidResponseException
     *             the invalid response exception
     * @throws NoSuchAlgorithmException
     *             the no such algorithm exception
     * @throws ServerException
     *             the server exception
     * @throws XmlParserException
     *             the xml parser exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    @DisplayName("Verify BDRService doUpload Object fails when numConnectionAttemptsMax exceeded.")
    void verifyBDRServiceDoUpload_numConnectionAttemptsMaxExceeded()
            throws InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException,
            InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
        final CommonObjects co = new CommonObjects(0);
        when(minioClient.putObject(Mockito.any(PutObjectArgs.class))).thenReturn(co.objectInfo);
        ReflectionTestUtils.setField(co.bdrService, "minioClient", minioClient);

        final boolean isUploaded = co.bdrService.doUpload(OBJECT_NAME, objectBytes, BUCKET_NAME, co.byteArrayInputStream);
        co.byteArrayInputStream.close();
        assertFalse("Expected BDR doUpload to fail when numConnectionAttemptsMax exceeded", isUploaded);
    }

    /**
     * Verify BDR service do upload exception thrown.
     *
     * @throws InvalidKeyException
     *             the invalid key exception
     * @throws ErrorResponseException
     *             the error response exception
     * @throws InsufficientDataException
     *             the insufficient data exception
     * @throws InternalException
     *             the internal exception
     * @throws InvalidResponseException
     *             the invalid response exception
     * @throws NoSuchAlgorithmException
     *             the no such algorithm exception
     * @throws ServerException
     *             the server exception
     * @throws XmlParserException
     *             the xml parser exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    @DisplayName("Verify BDRService doUpload Object fails when exception thrown.")
    void verifyBDRServiceDoUpload_exceptionThrown() throws InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException,
        InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
        final CommonObjects co = new CommonObjects(numConnectionAttemptsMax);
        when(minioClient.putObject(Mockito.any(PutObjectArgs.class)))
            .thenThrow(new IllegalArgumentException("TEST-verifyBDRServiceDoUpload_exceptionThrown"));
        ReflectionTestUtils.setField(co.bdrService, "minioClient", minioClient);

        final boolean isUploaded = co.bdrService.doUpload(OBJECT_NAME, objectBytes, BUCKET_NAME, co.byteArrayInputStream);
        co.byteArrayInputStream.close();
        assertFalse("Expected BDR doUpload to fail when exception thrown", isUploaded);
    }

    /**
     * Verify BDR service do upload put object fails.
     *
     * @throws InvalidKeyException
     *             the invalid key exception
     * @throws ErrorResponseException
     *             the error response exception
     * @throws InsufficientDataException
     *             the insufficient data exception
     * @throws InternalException
     *             the internal exception
     * @throws InvalidResponseException
     *             the invalid response exception
     * @throws NoSuchAlgorithmException
     *             the no such algorithm exception
     * @throws ServerException
     *             the server exception
     * @throws XmlParserException
     *             the xml parser exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    @DisplayName("Verify BDRService doUpload Object fails when minioClient.putObject fails to uplaod object.")
    void verifyBDRServiceDoUpload_putObjectFails() throws InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException,
        InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
        final CommonObjects co = new CommonObjects(numConnectionAttemptsMax);
        when(minioClient.putObject(Mockito.any(PutObjectArgs.class))).thenReturn(null);
        ReflectionTestUtils.setField(co.bdrService, "minioClient", minioClient);

        final boolean isUploaded = co.bdrService.doUpload(OBJECT_NAME, objectBytes, BUCKET_NAME, co.byteArrayInputStream);
        co.byteArrayInputStream.close();
        assertFalse("Expected BDR doUpload to fail when minioClient.putObject fails to uplaod object", isUploaded);
    }


    /**
     * Verify BDR service byteInputStream's index is reset when failure to upload
     *
     * @throws InvalidKeyException
     *             the invalid key exception
     * @throws ErrorResponseException
     *             the error response exception
     * @throws InsufficientDataException
     *             the insufficient data exception
     * @throws InternalException
     *             the internal exception
     * @throws InvalidResponseException
     *             the invalid response exception
     * @throws NoSuchAlgorithmException
     *             the no such algorithm exception
     * @throws ServerException
     *             the server exception
     * @throws XmlParserException
     *             the xml parser exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    @DisplayName("Verify ByteArrayStream is reset when BDRService doUpload fails to upload object")
    void verifyByteArrayStreamIsResetWhenBDRServiceDoUploadFails() throws IOException {
        final CommonObjects co = new CommonObjects(1);

        final int beforeUploadBytesAvailable = co.byteArrayInputStream.available();
        ReflectionTestUtils.setField(co.bdrService, "minioClient", minioClientReal);
        co.bdrService.doUpload(OBJECT_NAME, objectBytes, BUCKET_NAME, co.byteArrayInputStream);
        final int afterUploadBytesAvailable = co.byteArrayInputStream.available();
        co.byteArrayInputStream.close();

        assertEquals(beforeUploadBytesAvailable, afterUploadBytesAvailable);
    }

    private class CommonObjects {
        PutObjectArgs putObjectArgs;
        BDRService bdrService;
        ByteArrayInputStream byteArrayInputStream;
        ObjectWriteResponse objectInfo;

        CommonObjects(final int connectionAttemptsMax) {
            this.bdrService = new BDRService(metrics, connectionAttemptsMax, retryIntervalMs);

            this.byteArrayInputStream = new ByteArrayInputStream(objectBytes);
            this.putObjectArgs = PutObjectArgs.builder().bucket(BUCKET_NAME).object(OBJECT_NAME)
                .stream(this.byteArrayInputStream, objectBytes.length, PART_SIZE_AUTO_DETECT).build();
            this.objectInfo = new ObjectWriteResponse(null, BUCKET_NAME, "region", OBJECT_NAME, "etag", "versionId");
        }
    }
}
