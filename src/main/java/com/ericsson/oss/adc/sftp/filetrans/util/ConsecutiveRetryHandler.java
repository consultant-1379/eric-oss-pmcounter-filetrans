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

import com.ericsson.oss.adc.sftp.filetrans.availability.DependentServiceAvailabilityUtil;
import com.ericsson.oss.adc.sftp.filetrans.controller.health.HealthCheck;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ConsecutiveRetryHandler {


    @Value("${spring.kafka.topics.input.name}")
    private String inputTopicName;

    @Value("${spring.kafka.topics.enm_id}")
    private String enmID;

    @Autowired
    private HealthCheck healthCheck;

    @Autowired
    DependentServiceAvailabilityUtil dependentServiceAvailabilityUtil;

    private int numOfRetries = 0;

    private long numOfBackOffs = 0;

    @Value("${retryhandler.back_off_period_ms}")
    private int backOffPeriodMilliseconds;

    @Value("${retryhandler.max_num_of_retries}")
    private int maxNumOfRetries;


    public void incrementNumOfFails() {
        numOfRetries++;
        log.info("Consecutive retry fails: {}", numOfRetries);
        if (numOfRetries >= maxNumOfRetries) {
            log.error("Will now back off for {}. Consecutive number of backoff is now {}", backOffPeriodMilliseconds, numOfBackOffs);
            healthCheck.setErrorMessage("Service Unavailable");
            initializeBackOff();
        }
    }

    public void reset() {
        numOfRetries = 0;
        healthCheck.setErrorMessage(null);
    }

    public void initializeBackOff() {
        numOfBackOffs++;
        Utils.waitRetryInterval(backOffPeriodMilliseconds);
        while(!dependentServiceAvailabilityUtil.areAllDependentServicesAvailable()) {
            Utils.waitRetryInterval(backOffPeriodMilliseconds);
        }
        reset();
    }

    // Used for Test
    public long getNumOfBackOffs(){
        return numOfBackOffs;
    }
    public int getNumOfRetries(){
        return numOfRetries;
    }
}
