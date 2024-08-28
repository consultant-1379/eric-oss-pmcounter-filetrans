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
package com.ericsson.oss.adc.sftp.filetrans.controller;

import com.ericsson.oss.adc.sftp.filetrans.service.BDRService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@Slf4j
public class BDRComponent {

    @Autowired
    private BDRService bdrService;

    public boolean createBucket(final String bucketName) {
        if (this.bdrService.createBucket(bucketName)) {
            log.info("CREATED the MinIO Bucket: '{}'.", bucketName);
            return true;
        }

        log.error("FAILED to create the MinIO bucket: '{}'.", bucketName);
        return false;
    }

    public List<String> listBuckets() {
        return this.bdrService.getBucketsList();
    }

    public boolean uploadObject(final String bucketName, final String objectName, final byte[] inputBytes) {
        return (this.bdrService.uploadObject(bucketName, objectName, inputBytes));
    }

    public List<String> listObjects(final String bucketName, final String prefix) {
        return (this.bdrService.listObjects(bucketName, prefix));
    }

}
