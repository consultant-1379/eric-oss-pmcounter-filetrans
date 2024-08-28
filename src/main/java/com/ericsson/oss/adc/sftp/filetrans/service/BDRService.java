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

import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_FAILED_BDR_UPLOADS_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_SUCCESSFUL_BDR_UPLOADS_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.PROCESSED_BDR_DATA_VOLUME_TOTAL;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.ericsson.oss.adc.sftp.filetrans.bdr.DataCatalogBasedBdrEndpointSupplier;
import com.ericsson.oss.adc.sftp.filetrans.util.BDREndPointInitializedEvent;

import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.SetBucketLifecycleArgs;

import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Expiration;
import io.minio.messages.Item;
import io.minio.messages.LifecycleConfiguration;
import io.minio.messages.LifecycleRule;
import io.minio.messages.RuleFilter;
import io.minio.messages.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel;
import com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil;
import com.ericsson.oss.adc.sftp.filetrans.util.Utils;

import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;


@Service
@Slf4j
public class BDRService {

    private static final long PART_SIZE_AUTO_DETECT = -1L;

    @Autowired
    private DataCatalogBasedBdrEndpointSupplier dataCatalogBasedBDREndpointSupplier;

    @Autowired
    private DataCatalogService dataCatalogService;

    @Autowired
    private SFTPProcessingMetricsUtil metrics;

    private MinioClient minioClient = null;

    private List<String> objectsList = new ArrayList<>();

    @Value("${bdr.numberOfConnectionAttempts}")
    private int numConnectionAttemptsMax;

    @Value("${bdr.retryIntervalMs}")
    private int retryIntervalMs;

    @Value("${bdr.accesskey}")
    private String accesskey;

    @Value("${bdr.secretkey}")
    private String secretkey;

    public static final String NO_PREFIX = "";
    public static final boolean RECURSIVE = true;
    public static final boolean NON_RECURSIVE = false;

    @Autowired
    public BDRService() {
        //No Args constructor for Spring.
    }

    /**
     * Constructor for Unit test only.
     */
    public BDRService(final SFTPProcessingMetricsUtil metrics, final int numConnectionAttemptsMax, final int retryIntervalMs) {
        super();
        this.metrics = metrics;
        this.numConnectionAttemptsMax = numConnectionAttemptsMax;
        this.retryIntervalMs = retryIntervalMs;
    }


    @EventListener
    public void initMinioClient(BDREndPointInitializedEvent event) {
        if (minioClient == null && dataCatalogBasedBDREndpointSupplier.get() != null) {
            minioClient = MinioClient.builder().endpoint(event.getMinIOUrl()).credentials(accesskey, secretkey).build();
        }
    }

    public void initMinioClient(String accesskey, String secretkey) {
        if (minioClient == null && dataCatalogBasedBDREndpointSupplier.get() != null) {
            minioClient = MinioClient.builder().endpoint(dataCatalogBasedBDREndpointSupplier.get()).credentials(accesskey, secretkey).build();
        }
    }

    /**
     * REF : https://minio-java.min.io/io/minio/PutObjectArgs.Builder.html#stream-java.io.InputStream-long-long-
     */
    public boolean uploadObject(final String bucketName, final String objectName, final byte[] objectToWrite) {
        try (final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(objectToWrite)) {
            if (objectToWrite.length > 0) {
                log.debug("Starting upload of {} Object of length {} to bucket {}", objectName, objectToWrite.length, bucketName);
                return doUpload(objectName, objectToWrite, bucketName, byteArrayInputStream);
            }
        } catch (final Exception exception) {
            log.error("FAILED to upload {} Object, Not retrying as this is an exception on the input objectToWrite: ", objectName, exception);
        }
        log.error("FAILED to upload {} Object of length {} to bucket {}", objectName, (objectToWrite == null ? "null" : objectToWrite.length),
                bucketName);
        getMetrics().incrementCounterByName(NUM_FAILED_BDR_UPLOADS_TOTAL);
        return false;
    }

    // Avoids "//" in PUT to BDR e.g. bucketname//path/to/object
    private String cleanObjectNameForUpload(final String objectName) {
        if (objectName.charAt(0) == '/') {
            return objectName.replaceFirst("/", "");
        } else {
            return objectName;
        }
    }

    public boolean doUpload(final String objectName, final byte[] objectToWrite, final String bucketName,
                            final ByteArrayInputStream byteArrayInputStream) {
        if (minioClient != null) {
            for (int uploadAttempt = 1; uploadAttempt <= numConnectionAttemptsMax; uploadAttempt++) {
                final PutObjectArgs putObjectArgs = PutObjectArgs.builder().bucket(bucketName).object(cleanObjectNameForUpload(objectName))
                        .stream(byteArrayInputStream, objectToWrite.length, PART_SIZE_AUTO_DETECT).build();
                log.debug("Upload Attempt {}/{} of {}", uploadAttempt, numConnectionAttemptsMax, objectName);
                try {
                    final ObjectWriteResponse objectInfo = minioClient.putObject(putObjectArgs);
                    if (objectInfo != null) {
                        log.debug("Uploaded object {} of length {} to bucket {} successfully", objectName, objectToWrite.length, bucketName);
                        getMetrics().addToGaugeByName(PROCESSED_BDR_DATA_VOLUME_TOTAL, objectToWrite.length);
                        getMetrics().incrementCounterByName(NUM_SUCCESSFUL_BDR_UPLOADS_TOTAL);
                        return true;
                    } else {
                        log.error("FAILED to upload object {} : ObjectWriteResponse is null", objectName);
                    }

                } catch (final ErrorResponseException | IllegalArgumentException | InsufficientDataException |
                               InternalException | InvalidResponseException
                               | NoSuchAlgorithmException | ServerException | XmlParserException | InvalidKeyException |
                               IOException ex) {
                    log.error("FAILED to upload object {} : {}", objectName, ex.getMessage());
                }
                log.error("Upload Attempt FAILED {}/{} of {}, Backoff now for {} mS ", uploadAttempt, numConnectionAttemptsMax, objectName,
                        retryIntervalMs);
                Utils.waitRetryInterval(retryIntervalMs);
                byteArrayInputStream.reset();
            }
            log.info("Upload FAILED, RETRIES EXHAUSTED of {}", objectName);
            getMetrics().incrementCounterByName(NUM_FAILED_BDR_UPLOADS_TOTAL);
        }
        return false;
    }

    public boolean createBucket(final String bucketName) {
        if (minioClient != null) {
            for (int creationAttempt = 1; creationAttempt <= numConnectionAttemptsMax; creationAttempt++) {
                log.info("Create Bucket {}, attempt {}/{}", bucketName, creationAttempt, numConnectionAttemptsMax);
                try {
                    if (minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                        minioClient.setBucketLifecycle(
                                SetBucketLifecycleArgs.builder().bucket(bucketName).config(getLifeCycleConfiguration()).build());
                        log.warn("Bucket {} already exists. Available buckets {}; ", bucketName, getBucketsList());
                        return true;
                    }
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                    minioClient.setBucketLifecycle(
                            SetBucketLifecycleArgs.builder().bucket(bucketName).config(getLifeCycleConfiguration()).build());
                    return true;
                } catch (final ErrorResponseException | IllegalArgumentException | InsufficientDataException |
                               InternalException
                               | InvalidResponseException | NoSuchAlgorithmException
                               | ServerException | XmlParserException | IOException | InvalidKeyException ex) {
                    log.error("Bucket creation FAILED {} , {}", bucketName, ex.getMessage(), ex);
                    log.error("Retrying bucket creation..");
                    Utils.waitRetryInterval(retryIntervalMs);
                }
            }
            log.error("FAILED to create bucket {}, giving up", bucketName);
        }
        return false;
    }

    private LifecycleConfiguration getLifeCycleConfiguration() {
        final List<LifecycleRule> rules = new LinkedList<>();
        rules.add(
                new LifecycleRule(
                        Status.ENABLED,
                        null,
                        new Expiration((ZonedDateTime) null, 1, null),
                        new RuleFilter(""),
                        "Delete Objects in Bucket Older than 1 Day",
                        null,
                        null,
                        null));
        return new LifecycleConfiguration(rules);
    }

    public List<String> getBucketsList() {
        if (minioClient != null) {
            final List<String> listOfBuckets = new ArrayList<>();
            try {
                for (final Bucket bucket : minioClient.listBuckets()) {
                    listOfBuckets.add(bucket.name());
                }
            } catch (final ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException
                           | InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                           XmlParserException e) {
                log.error("List buckets call FAILED, {}", e.getMessage());
            }
            return listOfBuckets;
        }
        return Collections.<String>emptyList();
    }

    public BulkDataRepositoryModel getAccessEndPointsFromDataCatalog(final ResponseEntity<BulkDataRepositoryModel[]> responseEntity ) {
        BulkDataRepositoryModel bulkDataRepositoryModelS3BDR = null;
        if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
            for (final BulkDataRepositoryModel bdrDTO : responseEntity.getBody()) {
                if (bdrDTO.getFileRepoType() != null && bdrDTO.getFileRepoType().equals(BulkDataRepositoryModel.FileRepoType.S3)
                        && !bdrDTO.getAccessEndpoints().isEmpty()) {
                    bulkDataRepositoryModelS3BDR = bdrDTO;
                    log.info("MinIO client received BDR details S3 {} {}", bdrDTO.getName(), bdrDTO.getAccessEndpoints());
                }
            }
        }
        return bulkDataRepositoryModelS3BDR;
    }

    public Iterable<Result<Item>> retrieveBucketInformation(final String bucketName, final String prefix, final boolean isRecursive ) {
        initMinioClient(this.accesskey, this.secretkey);
        if (minioClient != null) {
            return minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(isRecursive)
                            .build());
        }
        return Collections.<Result<Item>>emptyList();
    }

    public Iterable<Result<DeleteError>> removeObjects(final String bucketName, final List<DeleteObject> files) {
        if (minioClient != null) {
            return minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(files)
                            .build());
        }
        return Collections.<Result<DeleteError>>emptyList();
    }

    public List<String> listObjects(String bucketName, final String prefix ) {
        if (minioClient != null) {
            final Iterable<Result<Item>> items = retrieveBucketInformation(bucketName, prefix, NON_RECURSIVE);
            for (final Result<Item> item : items) {
                try {
                    objectsList.add(item.get().objectName());
                } catch (final ErrorResponseException | InsufficientDataException | InternalException
                               | InvalidKeyException | InvalidResponseException | IOException
                               | NoSuchAlgorithmException | ServerException | XmlParserException e) {
                    log.error("FAILED to retrieve List of Objects in a bucket {}  , {}", bucketName, e.getMessage());
                }
            }
            return objectsList;
        }
        return Collections.<String>emptyList();
    }

    public SFTPProcessingMetricsUtil getMetrics() {
        return metrics;
    }
}
