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
package com.ericsson.oss.adc.sftp.filetrans;


import com.ericsson.oss.adc.sftp.filetrans.controller.BDRComponent;
import com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel;
import com.ericsson.oss.adc.sftp.filetrans.model.DataCategoryModel;
import com.ericsson.oss.adc.sftp.filetrans.model.DataProviderTypeModel;
import com.ericsson.oss.adc.sftp.filetrans.model.DataServiceModel;
import com.ericsson.oss.adc.sftp.filetrans.model.DataServiceInstanceModel;
import com.ericsson.oss.adc.sftp.filetrans.model.DataSpaceModel;
import com.ericsson.oss.adc.sftp.filetrans.model.DataTypeModel;
import com.ericsson.oss.adc.sftp.filetrans.model.FileFormatApiModel;
import com.ericsson.oss.adc.sftp.filetrans.model.FileFormatModel;
import com.ericsson.oss.adc.sftp.filetrans.model.MessageBusModel;
import com.ericsson.oss.adc.sftp.filetrans.model.NotificationTopicModel;
import com.ericsson.oss.adc.sftp.filetrans.model.SupportedPredicateParameterModel;
import com.ericsson.oss.adc.sftp.filetrans.test.integration.setup.rest.MockRestServerFacade;
import com.ericsson.oss.adc.sftp.filetrans.util.StartupUtil;
import com.ericsson.oss.adc.sftp.filetrans.util.TestUtils;
import com.ericsson.oss.adc.sftp.filetrans.util.Utils;
import com.google.gson.Gson;
import io.restassured.internal.util.IOUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.ExpectedCount;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel.FileRepoType.S3;
import static com.ericsson.oss.adc.sftp.filetrans.model.BulkDataRepositoryModel.FileRepoType.SFTP;
import static com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer.withSftpServer;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * The Class StartupIntegrationTest.
 * <p>
 * The purpose of this test is to test the startup Logic especially the dependency checks on the dependent services.
 */

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = "file-notification-service--sftp-filetrans--enm1", brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@ActiveProfiles("NoAsync")
class StartupIntegrationTest {

    private static final String HEALTH_PATH = "actuator/health";

    private static final String BDR_HEALTH_PATH = "/minio/health/live";

    private static final String DATA_CATALOG_BDR_URL = "http://localhost:9590/catalog/v1/bulk-data-repository/";

    private static final String CONNECTED_SYSTEM_URL = "http://eric-eo-subsystem-management/";

    private static final String CONNECTED_SYSTEMS_END_POINT = "subsystem-manager/v1/subsystems/";

    private static final String MESSAGE_BUS_URL = "http://localhost:9590/catalog/v1/message-bus?name=null&nameSpace=null";

    private static final String DATA_CATALOG_V1_FILE_FORMAT_URL = "http://localhost:9590/catalog/v1/file-format";

    protected static final int PORT = 1234;

    protected static final String USER = "user";

    protected static final String PASSWORD = "password";

    @Value("${dmm.data-catalog.base-url}")
    private String dataCatalogUri;

    @Value("${dmm.data-catalog.base-port}")
    private String dataCatalogPort;

    @Value("${bdr.hostname}")
    private String bdrHostname;

    @Autowired
    Startup startup;

    @Autowired
    StartupUtil startupUtil;

    @MockBean
    BDRComponent bdrComponent;

    @MockBean
    StartKafka startKafta;

    @Autowired
    private MockRestServerFacade mockRestServer;

    private final BulkDataRepositoryModel[] bdrModels = new BulkDataRepositoryModel[2];

    private final MessageBusModel[] messageBusModels = new MessageBusModel[1];

    private final BulkDataRepositoryModel bulkDataRepositoryModelS3 = BulkDataRepositoryModel.builder()
            .id(1L)
            .name("testBDR1")
            .clusterName("testCluster")
            .nameSpace("testNS")
            .accessEndpoints(new ArrayList<>(Collections.singletonList("http://eric-data-object-storage-mn:9000")))
            .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
            .fileRepoType(S3)
            .build();

    private final BulkDataRepositoryModel bulkDataRepositoryModelSFTP = BulkDataRepositoryModel.builder()
            .id(2L)
            .name("testBDR1")
            .clusterName("testCluster")
            .nameSpace("testNS")
            .accessEndpoints(new ArrayList<>(Collections.singletonList("http://eric-eo-subsystem-management/")))
            .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
            .fileRepoType(SFTP)
            .build();

    private final MessageBusModel messageBus = MessageBusModel.builder()
            .id(1L)
            .name("name")
            .clusterName("clusterName")
            .nameSpace("nameSpace")
            .accessEndpoints(new ArrayList<>(Collections.singletonList("PLAINTEXT://localhost:9092")))
            .notificationTopicIds(new ArrayList<>(Collections.singletonList(1L)))
            .messageStatusTopicIds(new ArrayList<>(Collections.singletonList(1L)))
            .messageDataTopicIds(new ArrayList<>(Collections.singletonList(1L)))
            .build();

    private FileFormatApiModel fileFormatApiModel = FileFormatApiModel.builder()
            .id(1L)
            .dataServiceInstance(DataServiceInstanceModel.builder()
                    .id(1L)
                    .dataService(DataServiceModel.builder()
                            .id(1L)
                            .dataServiceName("ran-pm-counter-sftp-file-transfer")
                            .dataServiceInstance(new ArrayList<>())
                            .predicateParameter(new ArrayList<>())
                            .fileFormatList(new ArrayList<>())
                            .build())
                    .dataServiceInstanceName("null-pm-counter-sftp-file-transfers--enm1")
                    .controlEndPoint("")
                    .consumedDataSpace("PM_COUNTERS")
                    .consumedDataCategory("enm1")
                    .consumedDataProvider("")
                    .consumedSchemaName("core")
                    .consumedSchemaVersion("1")
                    .build())
            .dataSpace(new DataSpaceModel(""))
            .dataService(DataServiceModel.builder()
                    .id(1L)
                    .dataServiceName("ran-pm-counter-sftp-file-transfer")
                    .dataServiceInstance(new ArrayList<>())
                    .predicateParameter(new ArrayList<>())
                    .fileFormatList(new ArrayList<>())
                    .build())
            .supportedPredicateParameter(SupportedPredicateParameterModel.builder()
                    .id(1L)
                    .parameterName("nodeName")
                    .dataServiceId(1)
                    .isPassedToConsumedService(true)
                    .build())
            .dataCategory(new DataCategoryModel())
                    .dataProviderType(DataProviderTypeModel.builder()
                    .id(1L)
                    .dataSpace(new DataSpaceModel(""))
                    .fileFormatIds(new ArrayList<>())
                    .messageSchemaIds(new ArrayList<>())
                    .providerVersion("")
                    .dataCategory("PM_COUNTERS")
                    .dataCategoryType(new DataCategoryModel())
                    .providerTypeId("enmFileNotificationService")
                    .build())
            .notificationTopic(NotificationTopicModel.builder()
                    .id(1L)
                    .name("NotificationTopic1")
                    .specificationReference("NotificationTopicSpecRef")
                    .encoding("JSON")
                    .dataProviderTypeId(1L)
                    .dataProviderType(DataProviderTypeModel.builder()
                            .id(1L)
                            .dataSpace(new DataSpaceModel())
                            .fileFormatIds(new ArrayList<>())
                            .messageSchemaIds(new ArrayList<>())
                            .providerVersion("")
                            .dataCategory("PM_COUNTERS")
                            .dataCategoryType(new DataCategoryModel())
                            .providerTypeId("enmFileNotificationService")
                            .build())
                    .dataCategoryType(new DataCategoryModel())
                    .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
                    .messageBusId(3L)
                    .messageBus(MessageBusModel.builder()
                            .id(1L)
                            .name("name")
                            .clusterName("clusterName")
                            .nameSpace("nameSpace")
                            .accessEndpoints(new ArrayList<>(Arrays.asList("http://endpoint1:1234/", "eric-oss-dmm-data-message-bus-kf-client:9092", "http://localhost:9092")))
                            .notificationTopicIds(new ArrayList<>(Collections.singletonList(1L)))
                            .messageStatusTopicIds(new ArrayList<>(Collections.singletonList(1L)))
                            .messageDataTopicIds(new ArrayList<>(Collections.singletonList(1L)))
                            .build())
                    .build())
            .fileFormat(FileFormatModel.builder()
                    .bulkDataRepositoryId(1)
                    .dataEncoding("XML")
                    .dataServiceId(1)
                    .id(1L)
                    .notificationTopicId(1)
                    .reportOutputPeriodList(new ArrayList<>())
                    .specificationReference("")
                    .bulkDataRepository(BulkDataRepositoryModel.builder()
                            .id(1L)
                            .name("testBDR")
                            .clusterName("testCluster")
                            .nameSpace("testNS")
                            .accessEndpoints(new ArrayList<>(Collections.singletonList("http://endpoint1:1234/")))
                            .fileFormatIds(new ArrayList<>(Collections.singletonList(1L)))
                            .fileRepoType(S3)
                            .build())
                    .build())
            .dataType(DataTypeModel.builder()
                    .id(1L)
                    .mediumId(0)
                    .mediumType("file")
                    .schemaName("COREBDR")
                    .schemaVersion("1.0.0")
                    .isExternal(false)
                    .consumedDataSpace("")
                    .consumedDataCategory("")
                    .consumedDataProvider("PM_COUNTERS")
                    .consumedSchemaName("FLS")
                    .consumedSchemaVersion("1")
                    .build())
            .bulkDataRepository(new BulkDataRepositoryModel())
            .reportOutputPeriodList(new ArrayList<>())
            .specificationReference("")
            .dataEncoding("JSON")
            .build();

    @BeforeEach
    public void setup() {
        mockRestServer.resetServer();
        bdrModels[0] = bulkDataRepositoryModelS3;
        bdrModels[1] = bulkDataRepositoryModelSFTP;
        messageBusModels[0] = messageBus;
        Utils.waitRetryInterval(1000);
    }

    @Test
    @Order(1)
    @DisplayName("Verify file is downloaded when successful connections is made")
    public void StartupTest() throws Exception {

        withSftpServer(server -> {
            byte[] connectedSystemsResource = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("IntegrationGetSubsystemsResponse.json"));
            server.setPort(PORT);
            server.addUser(USER, PASSWORD);

            final Gson gson = new Gson();


            //Data Catalog
            mockRestServer.mockGetOkResponse(Matchers.containsString(MESSAGE_BUS_URL), gson.toJson(messageBusModels));

            //BDR Create Bucket
            when(bdrComponent.createBucket(Mockito.anyString())).thenReturn(true);

            //Data Catalog Health Check
            mockRestServer.mockGetOkResponse(Matchers.containsString(dataCatalogUri + TestUtils.addTrailingSlash(dataCatalogPort) + HEALTH_PATH),
                    ExpectedCount.manyTimes(), buildHealthCheckResponseBody(true));

            //BDR Health Check
            mockRestServer.mockGetOkResponse(Matchers.containsString(DATA_CATALOG_BDR_URL),
                    ExpectedCount.manyTimes(), gson.toJson(bdrModels));
            mockRestServer.mockGetOkResponse(Matchers.containsString(bdrHostname + BDR_HEALTH_PATH), buildHealthCheckResponseBody(true));

            //Connected Systems
            mockRestServer.mockGetOkResponse(Matchers.containsString(CONNECTED_SYSTEM_URL + HEALTH_PATH), buildHealthCheckResponseBody(true));
            mockRestServer.mockGetOkResponse(Matchers.containsString(CONNECTED_SYSTEM_URL + CONNECTED_SYSTEMS_END_POINT),
                    ExpectedCount.manyTimes(), new ByteArrayResource(connectedSystemsResource));


            //V2 tests ---
            mockRestServer.mockGetOkResponse(Matchers.containsString(MESSAGE_BUS_URL), gson.toJson(messageBusModels));
            mockRestServer.mockPutOkResponse(Matchers.containsString(DATA_CATALOG_V1_FILE_FORMAT_URL), gson.toJson(fileFormatApiModel));

            when(startKafta.startKafkaListener(Mockito.anyString())).thenReturn(true);
            startupUtil.setMessageBusModel();
            assertTrue(startup.startAndConfigureExternalComponents());
            Mockito.verify(startKafta, times(1)).startKafkaListener(Mockito.anyString());
        });
    }

    private String buildHealthCheckResponseBody(final boolean up) {
        final String body = "{\"status\":\"%1$s\",\"groups\":[\"liveness\",\"readiness\"]}";
        if (up) {
            return String.format(body, "UP");
        }
        return String.format(body, "DOWN");
    }
}
