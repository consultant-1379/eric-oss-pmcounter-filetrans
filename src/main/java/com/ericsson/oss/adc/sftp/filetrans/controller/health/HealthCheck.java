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

package com.ericsson.oss.adc.sftp.filetrans.controller.health;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@ConfigurationProperties(prefix = "info.app")
@Component
@Data
@Slf4j
public final class HealthCheck implements HealthIndicator {

    private String errorMessage;
    private String serviceName;

    @Override
    public Health health() {
        if (errorMessage != null) {
            return Health.down()
                    .withDetail(serviceName + " is DOWN. Health check failure.", errorMessage)
                    .build();
        }

        log.debug("{} is UP and healthy.", serviceName);
        return Health.up().build();
    }

}
