/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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
package com.ericsson.oss.adc.sftp.filetrans.bdr;

import com.ericsson.oss.adc.sftp.filetrans.configuration.properties.DataServiceProperties;
import com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import org.awaitility.Durations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import static com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel.FileRepoType.S3;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka
@Testcontainers
public class ScheduledDeleteOnMinioTest {

    private static final String accessKey = "AKIAIOSFODNN7EXAMPLE";
    private static final String secretKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
    private static final Integer defaultPort = 9000;

    @Autowired
    private DataServiceProperties dataServiceProperties;

    @Autowired
    private DataCatalogBasedBdrEndpointSupplier dataCatalogBasedBDREndpointSupplier;

    @SpyBean
    private ScheduledMinIoFileCleanUp minioFileCleanUp;

    @Container
    private static final GenericContainer<?> minioContainer =
            new GenericContainer<>(DockerImageName.parse("quay.io/minio/minio"))
                    .withExposedPorts(defaultPort)
                    .withEnv("MINIO_ACCESS_KEY", accessKey)
                    .withEnv("MINIO_SECRET_KEY", secretKey)
                    .withCommand("server", "/data");


    private MinioClient minioClient;

    @Test
    @DisplayName("Given that there is a file loaded into minio with an almost immediate expiry, subsequent trigger of cleanup task should delete it")
    public void verifyFilesAreDeleted() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String bucketName = dataServiceProperties.getDataServiceName();
        minioContainer.start();
        /*
              Given
              1. Minio Container running
              2. The datacatalog supplier used by scheduled task points to minio container
              3. Sample file uploaded into container with almost immediate expiry
         */
        if (minioContainer.isRunning()) {

            String endpoint = String.format("http:\\%s:%s", minioContainer.getContainerIpAddress(), minioContainer.getMappedPort(defaultPort));

            MockMultipartFile mockMultipartFile
                    = new MockMultipartFile(
                    "file",
                    "hello.txt",
                    MediaType.TEXT_PLAIN_VALUE,
                    "Hello, World!".getBytes()
            );

            BulkDataRepositoryModel bulkDataRepositoryDTOS3 = BulkDataRepositoryModel.builder()
                    .id(1L)
                    .name("testBDR1")
                    .clusterName("testCluster")
                    .nameSpace("testNS")
                    .accessEndpoints(new ArrayList<>(Collections.singletonList(endpoint)))
                    .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
                    .fileRepoType(S3)
                    .build();
            //Putting an explicit init here since the actual initialization in Startup shall fail since datacatalog and BDR is not mocked
            dataCatalogBasedBDREndpointSupplier.initBdrEndPointDetailsFromDataCatalog(bulkDataRepositoryDTOS3.getAccessEndpoints());

            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();


            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());

            UUID fileId = UUID.randomUUID();
            PutObjectArgs uploadObject = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileId.toString())
                    .stream(mockMultipartFile.getInputStream(), mockMultipartFile.getSize(), -1)
                    .contentType(mockMultipartFile.getContentType())
                    .build();

            minioClient.putObject(uploadObject);
            /*
                 When
                     scheduled job is invoked subsequently
             */
            /*
                By the time test container starts up and test file is added to minio , the scheduled task is already few times.
                The following code gets the number of times scheduled task is already invoked before test execution
                reaches this point.
             */
            Collection<Invocation> invocations = Mockito.mockingDetails(minioFileCleanUp).getInvocations();
            int numberOfTimesCleanupInvoked = invocations.size();
            //ideally it could have been one. But the wait exits almost immediately the method is invoked
            //before cleanup happens. Hence it waits for 2 more invocations.
            int expectedTotalInvocations = numberOfTimesCleanupInvoked + 2;

            await().atMost(Durations.FIVE_SECONDS)
                    .untilAsserted(() -> verify(minioFileCleanUp, atLeast(expectedTotalInvocations)).scheduleFileCleanUp());

            Iterable<Result<Item>> existingFiles = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucketName)
                            .recursive(true)
                            .build());
            //then
            // the file is removed by the scheduled job
            assertFalse(existingFiles.iterator().hasNext());
        }

    }
}
