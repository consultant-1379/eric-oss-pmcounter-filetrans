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

package com.ericsson.oss.adc.sftp.filetrans;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ericsson.oss.adc.sftp.filetrans.configuration.properties.DataCatalogProperties;
import com.ericsson.oss.adc.sftp.filetrans.configuration.properties.DataServiceProperties;
import com.ericsson.oss.adc.sftp.filetrans.rest.RestTemplateFacade;
import com.ericsson.oss.adc.sftp.filetrans.service.DataCatalogService;
import com.ericsson.oss.adc.sftp.filetrans.service.ENMScriptingVMLoadBalancer;
import com.ericsson.oss.adc.sftp.filetrans.util.RestExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;

import com.ericsson.oss.adc.sftp.filetrans.model.ConnectionPropertiesModel;
import com.ericsson.oss.adc.sftp.filetrans.model.SubsystemModel;
import com.ericsson.oss.adc.sftp.filetrans.service.ConnectedSystemsRetriever;
import com.google.gson.Gson;
import org.springframework.web.client.RestTemplate;

//IDUN-63054 : remove explicit class declaration on non e2e test cases.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {"connected.systems.uri=subsystem-manager/v1/subsystems",
"subsystem.name=ten"}, classes = {ConnectedSystemsRetriever.class,
        DataCatalogService.class,
        DataCatalogProperties.class,
        DataServiceProperties.class,
        SubsystemModel.class,
        ConnectionPropertiesModel.class,
        RestExecutor.class,
        RestTemplateFacade.class,
        RestTemplate.class,
        ENMScriptingVMLoadBalancer.class})
@AutoConfigureStubRunner(repositoryRoot = "https://arm.seli.gic.ericsson.se/artifactory/proj-service-orchestrator-release-local",
stubsMode = StubRunnerProperties.StubsMode.REMOTE,
ids = "com/ericsson/oss/orchestration/so/service/core:subsystem-management-service:+:stubs:9600")
public class ConnectedSystemsContractTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectedSystemsContractTest.class);

    @Autowired
    ConnectedSystemsRetriever connectedSystemsRetriever;

    @Test
    @DisplayName("Verify that Contract Stub is Correctly Retrieved")
    public void verifyContractStubIsCorrectlyRetrieved(){
        connectedSystemsRetriever.addAccessEndpoints(new ArrayList<>(Collections.singletonList("http://localhost:9600/")));
        // Note: will need to update this test to add in scriptingVm when connected systems update the stub: getConnectionProps.json
        final String expectedJsonString = "[{\"id\":\"10\",\"subsystemTypeId\":\"2\",\"name\":\"ten\",\"url\":\"https://test.subsystem-10/\",\"operationalState\":\"REACHABLE\",\"connectionProperties\":[{\"id\":3,\"subsystemId\":10,\"name\":\"connection3\",\"tenant\":\"master\",\"username\":\"ecmadmin\",\"password\":\"CloudAdmin123\",\"encryptedKeys\":[\"password\"],\"subsystemUsers\":[{\"id\":4,\"connectionPropsId\":3},{\"id\":15,\"connectionPropsId\":3},{\"id\":2,\"connectionPropsId\":3}]}],\"vendor\":\"subsystem-vendor\",\"subsystemType\":{\"id\":2,\"type\":\"NFVO\"},\"adapterLink\":\"eric-eo-ecm-adapter\"},"
                + "{\"id\":\"15\",\"subsystemTypeId\":\"5\",\"name\":\"fifteen\",\"url\":\"https://test.subsystem-15/\",\"operationalState\":\"REACHABLE\",\"connectionProperties\":[{\"id\":4,\"subsystemId\":15,\"name\":\"connection4\",\"tenant\":\"master\",\"username\":\"ecmadmin\",\"password\":\"CloudAdmin123\",\"encryptedKeys\":[\"password\"],\"subsystemUsers\":[{\"id\":4,\"connectionPropsId\":3},{\"id\":15,\"connectionPropsId\":3},{\"id\":2,\"connectionPropsId\":3}]}],\"vendor\":\"subsystem-vendor\",\"subsystemType\":{\"id\":5,\"type\":\"PhysicalDevice\"},\"adapterLink\":\"eric-eo-ecm-adapter\"},"
                + "{\"id\":\"2\",\"subsystemTypeId\":\"5\",\"name\":\"two\",\"url\":\"https://test.subsystem-2/\",\"operationalState\":\"REACHABLE\",\"connectionProperties\":[{\"id\":5,\"subsystemId\":2,\"name\":\"connection5\",\"tenant\":\"tenant1\",\"username\":\"ecmadmin\",\"password\":\"CloudAdmin123\",\"encryptedKeys\":[\"password\"],\"subsystemUsers\":[{\"id\":4,\"connectionPropsId\":3},{\"id\":15,\"connectionPropsId\":3},{\"id\":2,\"connectionPropsId\":3}]}],\"vendor\":\"subsystem-vendor\",\"subsystemType\":{\"id\":5,\"type\":\"PhysicalDevice\"},\"adapterLink\":\"eric-eo-ecm-adapter\"},"
                + "{\"id\":\"1\",\"subsystemTypeId\":\"1\",\"name\":\"one\",\"url\":\"https://test.subsystem-1/\",\"operationalState\":\"REACHABLE\",\"connectionProperties\":[{\"id\":7,\"subsystemId\":1,\"name\":\"connection7\",\"username\":\"ecmadmin\",\"password\":\"CloudAdmin123\",\"scriptingVMs\":\"scp-1-scripting,scp-2-scripting,scp-3-scripting\",\"encryptedKeys\":[\"password\"],\"sftpPort\":\"22\",\"subsystemUsers\":[{\"id\":9,\"connectionPropsId\":7},{\"id\":3,\"connectionPropsId\":7},{\"id\":1,\"connectionPropsId\":7}]}],\"vendor\":\"subsystem-vendor\",\"subsystemType\":{\"id\":1,\"type\":\"DomainManager\"}},"
                + "{\"id\":\"5\",\"subsystemTypeId\":\"1\",\"name\":\"five\",\"url\":\"https://test.subsystem-5/\",\"operationalState\":\"REACHABLE\",\"connectionProperties\":[{\"id\":7,\"subsystemId\":1,\"name\":\"connection9\",\"username\":\"ecmadmin\",\"password\":\"CloudAdmin123\",\"encryptedKeys\":[\"password\"],\"subsystemUsers\":[{\"id\":9,\"connectionPropsId\":7},{\"id\":3,\"connectionPropsId\":7},{\"id\":1,\"connectionPropsId\":7}]}],\"vendor\":\"subsystem-vendor\",\"subsystemType\":{\"id\":8,\"type\":\"DomainOrchestrator\"}}]";
        final Map<String, SubsystemModel> subsystemDTOMap = connectedSystemsRetriever.getSubsystemDetails();
        final SubsystemModel[] expectedArray = new Gson().fromJson(expectedJsonString, SubsystemModel[].class);
        final Map<String, SubsystemModel> expectedJsonMap = new HashMap<>();

        for(final SubsystemModel subsystemModel : expectedArray){
            expectedJsonMap.put(subsystemModel.getName(), subsystemModel);
        }

        // to aid debug of connected systems stub issues, compare the responses individually
        assertEquals(expectedJsonMap.size(), subsystemDTOMap.size(), "Expected Connected Systems Retriever to get correct number of responses");

        for (final Entry<String, SubsystemModel> expectedSubsystemDTOEntry : expectedJsonMap.entrySet()) {
            final String expectedName = expectedSubsystemDTOEntry.getKey();
            final SubsystemModel expectedSubsystemModel = expectedSubsystemDTOEntry.getValue();
            assertTrue(subsystemDTOMap.containsKey(expectedName),
                    "Missing entry in actual resposne map; Expected the reponse '" + expectedName + "' to be in the actual response");
            final SubsystemModel actualSubsystemModel = subsystemDTOMap.get(expectedName);
            printSubsystemModelDetails(expectedSubsystemModel, actualSubsystemModel);
            assertThat(expectedSubsystemModel).usingRecursiveComparison().isEqualTo(actualSubsystemModel);
        }
        assertThat(expectedJsonMap).usingRecursiveComparison().isEqualTo(subsystemDTOMap);
    }

    private void printSubsystemModelDetails(final SubsystemModel expectedSubsystemModel, final SubsystemModel actualSubsystemModel) {
        LOGGER.info("Comparing : expected '{}' with actual '{}'", expectedSubsystemModel.getName(), actualSubsystemModel.getName());
        LOGGER.info("Comparing : expectedSubsystemModel id '{}', actualSubsystemModel id = '{}'", expectedSubsystemModel.getId(),
                actualSubsystemModel.getId());
        LOGGER.info("Comparing : expectedSubsystemModel Name '{}', actualSubsystemModel Name = '{}'", expectedSubsystemModel.getName(),
                actualSubsystemModel.getName());
        for (final ConnectionPropertiesModel ecdto : expectedSubsystemModel.getConnectionProperties()) {
            LOGGER.info("expectedSubsystemModel ConnectionPropertiesModel '{}' ", ecdto.toString());
        }
        for (final ConnectionPropertiesModel acdto : actualSubsystemModel.getConnectionProperties()) {
            LOGGER.info("actualSubsystemModel ConnectionPropertiesModel '{}'", acdto.toString());
        }
    }
}
