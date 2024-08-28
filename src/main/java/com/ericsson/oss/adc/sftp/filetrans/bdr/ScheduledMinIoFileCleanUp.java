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
import com.ericsson.oss.adc.sftp.filetrans.service.BDRService;
import io.minio.Result;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.StreamSupport;

@Component
@Slf4j
public class ScheduledMinIoFileCleanUp {
    @Value("${bdr.accesskey}")
    private String bdrAccessKey;
    @Value("${bdr.secretkey}")
    private String bdrSecretKey;

    @Autowired
    private DataServiceProperties dataServiceProperties;

    @Autowired
    private DataCatalogBasedBdrEndpointSupplier dataCatalogBasedBDREndpointSupplier;

    @Autowired
    private BDRService bdrService;

    @Value("${bdr.filePersistenceDuration}")
    private String filePersistenceDuration;

    @Scheduled(fixedDelayString = "${bdr.fileDeleteRate}", initialDelayString = "${bdr.fileDeleteInitialDelay}")
    public void scheduleFileCleanUp() {
        String bucketName = dataServiceProperties.getDataServiceName();

        log.info("Deleting Objects from Bucket Name '{}'", bucketName);

        Iterable<Result<Item>> existingFiles = bdrService.retrieveBucketInformation(bucketName, BDRService.NO_PREFIX, BDRService.RECURSIVE);

        long seconds = Long.parseLong(filePersistenceDuration) / 1000;
        //ZonedDateTime.now() can return a time in different timezone
        // which may vary from BDR timezone.
        //Hence explicitly setting all calculation to UTC timezone.
        ZoneId london = ZoneId.of("Europe/London");
        ZonedDateTime cutoffTime = ZonedDateTime.now(london).minusSeconds(seconds);

        log.info("All files created prior to '{}' UTC shall be deleted : " , cutoffTime);

        List<DeleteObject> filesToBeDeleted = new LinkedList<>();
        for (Result<Item> file : existingFiles) {
            try {
                log.debug("Processing File '{}' created at '{}' UTC :", file.get().objectName(), file.get().lastModified());
                //converting modified time zone from BDR to UTC for comparison
                ZonedDateTime fileCreatedTime = file.get().lastModified().withZoneSameInstant(ZoneOffset.UTC);
                if (fileCreatedTime.isBefore(cutoffTime)) {
                    filesToBeDeleted.add(new DeleteObject(file.get().objectName()));
                }
            } catch (Exception exception) {
                log.warn("Error reading file info", exception);
            }
        }
        log.info("Submitting Files for Deletion. Number of Files to be Deleted: {}", filesToBeDeleted.size());

        Iterable<Result<DeleteError>> deletionResults = bdrService.removeObjects(bucketName, filesToBeDeleted);

        long countOfFilesErrored = StreamSupport.stream(deletionResults.spliterator(), false).count();
        log.info("Count of Files Deleted Successfully: {}", filesToBeDeleted.size() - countOfFilesErrored);
        log.info("Count of Files Errored : {}", countOfFilesErrored);
    }
}
