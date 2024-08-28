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
package com.ericsson.oss.adc.sftp.filetrans.test.integration;

import com.ericsson.oss.adc.sftp.filetrans.CoreApplication;
import com.ericsson.oss.adc.sftp.filetrans.configuration.properties.DataCatalogProperties;
import com.ericsson.oss.adc.sftp.filetrans.configuration.properties.DataServiceProperties;
import com.ericsson.oss.adc.sftp.filetrans.test.integration.setup.rest.MockRestServerFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

/**
 * Use as the foundation for every Spring Boot test.
 */
@SpringBootTest(classes = CoreApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka
public abstract class IntegrationTest {

    @Autowired
    protected MockRestServerFacade mockRestServer;
    @Autowired
    protected DataCatalogProperties dataCatalogProperties;
    @Autowired
    protected DataServiceProperties dataServiceProperties;

    @BeforeEach
    @AfterEach
    public void resetRestServer() {
        mockRestServer.resetServer();
    }

}