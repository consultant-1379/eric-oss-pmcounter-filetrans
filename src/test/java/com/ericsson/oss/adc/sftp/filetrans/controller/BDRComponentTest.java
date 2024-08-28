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

package com.ericsson.oss.adc.sftp.filetrans.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ericsson.oss.adc.sftp.filetrans.service.BDRService;

/**
 * The Class BDRControllerTest.
 */

@SpringBootTest(classes = {BDRComponent.class})
public class BDRComponentTest {

    @Autowired
    private BDRComponent bdrComponent;

    @MockBean
    BDRService bdrService;

    final private String BUCKET_NAME = "testbucket";
    final private String FILE_NAME = "testobject";

    /**
     * Test BDR create bucket.
     */
    @Test
    public void testBDRCreateBucket() {
        when(bdrService.createBucket(anyString())).thenReturn(true);
        when(bdrService.getBucketsList()).thenReturn(Arrays.asList(BUCKET_NAME));
        bdrComponent.createBucket(BUCKET_NAME);
        assertThat(bdrComponent.listBuckets()).anyMatch(name -> name.equals(BUCKET_NAME));
    }

    /**
     * Test upload object.
     */
    @Test
    public void testUploadObject() {
        when(bdrService.listObjects(anyString(), anyString())).thenReturn(Arrays.asList(FILE_NAME));
        when(bdrService.uploadObject(anyString(), anyString(), Mockito.any(byte[].class))).thenReturn(true);
        final String fileContent = "test contest";
        bdrComponent.uploadObject(BUCKET_NAME, FILE_NAME, fileContent.getBytes());
        assertThat(bdrComponent.listObjects(BUCKET_NAME, BDRService.NO_PREFIX)).anyMatch(name -> name.equals(FILE_NAME));
    }

}
