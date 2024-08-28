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

import com.ericsson.oss.adc.sftp.filetrans.service.BDRService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;


import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BDRS3BucketNameValidatorTest {

    @InjectMocks
    private BDRS3BucketNameValidator bucketNameValidator;

    @Spy
    private BDRService bdrService;

    @Mock
    SFTPProcessingMetricsUtil metrics;

    @Mock
    private final Random randomNumber = mock(Random.class);

    @Mock
    private final MinioClient minioClient = mock(MinioClient.class);

    @BeforeAll
    public void init() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(bdrService, "numConnectionAttemptsMax", 3);
        ReflectionTestUtils.setField(bdrService, "retryIntervalMs", 100);
    }

    @Test
    public void testMinimumLengthFollowingS3Standards() {
        assertEquals("er-bucket",bucketNameValidator.lengthFollowingS3Standards("er",randomNumber));
    }

    @Test
    public void testMaximumLengthFollowingS3Standards() {
        when(randomNumber.nextInt(anyInt())).thenReturn(1);
        assertEquals("eric-oss-pmcounter-filetrans/src/test/java/com/ericsson/oss/ad1",bucketNameValidator.lengthFollowingS3Standards("eric-oss-pmcounter-filetrans/src/test/java/com/ericsson/oss/ad/sftp/filetrans/util",randomNumber));
    }

    @Test
    public void testIPAddressWithConsecutiveDots() {
        assertEquals("43-25-23-42",bucketNameValidator.validateUnderS3Standards("43..25..23..42"));
    }

    @Test
    public void testLowerCaseLetters() {
        when(randomNumber.nextInt(anyInt())).thenReturn(7);
        assertEquals("7..invalidname..7",bucketNameValidator.startAndEndWithLetterOrLowerCaseAlphabet("..InvalidNamE..",randomNumber));
    }

    @Test
    public void testRemovingUnwantedCharacters() {
        assertEquals("invalid--.-name",bucketNameValidator.validateUnderS3Standards("xn--Invalid//..£Name-s3alias"));
    }


    @Test
    public void testSameNameReturnedWhenValidBucketNameIsPassed() {
        assertEquals("valid-name.0",bucketNameValidator.validateUnderS3Standards("valid-name.0"));
    }

    @Test
    public void testBucketCreationWithInvalidAndValidatedName() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        doNothing().when(minioClient).makeBucket(Mockito.any(MakeBucketArgs.class));
        when(minioClient.bucketExists(new BucketExistsArgs())).thenReturn(false);
        ReflectionTestUtils.setField(bdrService, "minioClient", minioClient);
        assertFalse(bdrService.createBucket(".invalidName."));
        assertTrue(bdrService.createBucket(bucketNameValidator.validateUnderS3Standards(".invalidName.")));
    }

    @Test
    public void testNameWithConsecutiveDots() {
        assertEquals("a.d.d.",bucketNameValidator.removeConsecutiveDots("a..d.....d..."));
    }

    @Test
    public void testSameNameReturnedWhenValidatedMultipleTimes() {
        assertEquals("1.enm1prog.---2",bucketNameValidator.validateUnderS3Standards("1.ENM1Prog...--_2"));
        assertEquals("1.enm1prog.---2",bucketNameValidator.validateUnderS3Standards("1.enm1prog.---2"));
    }

}
