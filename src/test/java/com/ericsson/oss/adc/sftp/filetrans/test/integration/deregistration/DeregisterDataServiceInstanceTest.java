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
package com.ericsson.oss.adc.sftp.filetrans.test.integration.deregistration;

import com.ericsson.oss.adc.sftp.filetrans.deregistration.DeregisterStatus;
import com.ericsson.oss.adc.sftp.filetrans.deregistration.DeregistrationController;
import com.ericsson.oss.adc.sftp.filetrans.test.integration.IntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class DeregisterDataServiceInstanceTest extends IntegrationTest {

    private static final String DEREGISTER_STATUS_PROPERTY = "deregisterStatus";

    @Autowired
    private MockMvc mockMvc;

    private ResultActions response;

    @Test
    void GIVEN_noContentResponseFromDataCatalog_WHEN_deleteDataServiceInstance_THEN_okResponse_and_successStatus() throws Exception {
        // given
        mockRestServer.mockDeleteResponse(Matchers.containsString(dataServiceProperties.getDataServiceInstanceName()), HttpStatus.NO_CONTENT);

        // when
        response = deregisterService();

        // then
        response.andExpect(status().isOk());
        checkResponseBodyContainsProperty(DEREGISTER_STATUS_PROPERTY, DeregisterStatus.SUCCESS.getExitStatus());
    }

    @Test
    void GIVEN_notFoundResponseFromDataCatalog_WHEN_deleteDataServiceInstance_THEN_okResponse_and_successStatus() throws Exception {
        // given
        mockRestServer.mockDeleteResponse(Matchers.containsString(dataServiceProperties.getDataServiceInstanceName()), HttpStatus.NOT_FOUND);

        // when
        response = deregisterService();

        // then
        response.andExpect(status().isOk());
        checkResponseBodyContainsProperty(DEREGISTER_STATUS_PROPERTY, DeregisterStatus.SUCCESS.getExitStatus());
    }

    @Test
    void GIVEN_dataCatalogErrorResponse_WHEN_deleteDataServiceInstance_THEN_sameResponse_and_failureStatus() throws Exception {
        // given
        HttpStatus dataCatalogResponseStatus = HttpStatus.SERVICE_UNAVAILABLE;
        mockRestServer.mockDeleteResponse(Matchers.containsString(dataServiceProperties.getDataServiceInstanceName()), dataCatalogResponseStatus);

        // when
        response = deregisterService();

        // then
        response.andExpect(status().is(dataCatalogResponseStatus.value()));
        checkResponseBodyContainsProperty(DEREGISTER_STATUS_PROPERTY, DeregisterStatus.FAILURE.getExitStatus());
    }

    @Test
    void GIVEN_unhandledException_WHEN_deleteDataServiceInstance_THEN_internalErrorResponse_and_exceptionStatus() throws Exception {
        // given
        mockRestServer.mockException(Matchers.containsString(dataServiceProperties.getDataServiceInstanceName()), HttpMethod.DELETE);

        // when
        response = deregisterService();

        // then
        response.andExpect(status().isInternalServerError());
        checkResponseBodyContainsProperty(DEREGISTER_STATUS_PROPERTY, DeregisterStatus.EXCEPTION.getExitStatus());
    }

    private ResultActions deregisterService() throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.delete(DeregistrationController.DEREGISTER_DATA_SERVICE_INSTANCE_URI));
    }

    private <T> void checkResponseBodyContainsProperty(String propertyName, T propertyValue) throws Exception {
        response.andExpect(jsonPath("$." + propertyName).value(propertyValue));
    }

}
