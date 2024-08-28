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
package com.ericsson.oss.adc.sftp.filetrans.kafka;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Component
public class KafkaBootstrapSupplier implements Supplier<String> {

    @Value("${spring.kafka.bootstrap-servers}")
    private String defaultKafkaEndpoint;

    @Setter
    private List<String> messageBusAccessEndpoints = new ArrayList<>();

    @Override
    public String get() {
        return messageBusAccessEndpoints.isEmpty() ? defaultKafkaEndpoint : String.join(",",messageBusAccessEndpoints);
    }

}

