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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.ericsson.oss.adc.sftp.filetrans.availability.DependentServiceAvailabilityBDR;
import com.ericsson.oss.adc.sftp.filetrans.availability.DependentServiceAvailabilityConnectedSystems;
import com.ericsson.oss.adc.sftp.filetrans.availability.DependentServiceAvailabilityDataCatalog;
import com.ericsson.oss.adc.sftp.filetrans.availability.DependentServiceAvailabilityKafka;
import com.ericsson.oss.adc.sftp.filetrans.availability.DependentServiceAvailabilitySftpServer;
import com.ericsson.oss.adc.sftp.filetrans.availability.DependentServiceAvailabilityUtil;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DependentServiceAvailabilityUtilTest {

    @InjectMocks
    DependentServiceAvailabilityUtil dependentServiceAvailabilityUtil;

    @Mock
    private DependentServiceAvailabilityDataCatalog dSADataCatalog;

    @Mock
    private DependentServiceAvailabilityBDR dSABDR;

    @Mock
    private DependentServiceAvailabilityConnectedSystems dSAConnectedSystems;

    @Mock
    private DependentServiceAvailabilityKafka dSAKafka;

    @Mock
    private DependentServiceAvailabilitySftpServer dSASftp;


    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testHealth() {
        Mockito.when(dSADataCatalog.checkHealth()).thenReturn(true);
        Mockito.when(dSABDR.checkHealth()).thenReturn(true);
        Mockito.when(dSAKafka.checkHealth()).thenReturn(true);
        Mockito.when(dSASftp.checkHealth()).thenReturn(true);
        //Note: Connected systems already enabled in test/resources/application.yaml
        assertTrue(dependentServiceAvailabilityUtil.areAllDependentServicesAvailable());
        assertTrue(dSASftp.checkHealth());
    }
}
