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


package com.ericsson.oss.adc.sftp.filetrans.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dmm.data-catalog.data-service")
@Getter
@Setter
public class DataServiceProperties {

    private String nameSuffix;
    private DataSpace dataSpace;
    private DataServiceInstance dataServiceInstance;
    private DataCategory dataCategory;
    private DataProvider dataProvider;
    private NotificationTopic notificationTopic;
    private SupportedPredicateParameter supportedPredicateParameter;
    private FileFormat fileFormat;
    private DataType dataType;

    @Getter
    @Setter
    public static class DataSpace {
        private String name;
    }

    @Getter
    @Setter
    public static class DataServiceInstance {
        private String controlEndPoint;
        private String consumedDataSpace;
        private String consumedDataCategory;
        private String consumedDataProvider;
        private String consumedSchemaName;
        private String consumedSchemaVersion;
    }

    @Getter
    @Setter
    public static class DataCategory {
        private String name;
    }

    @Getter
    @Setter
    public static class DataProvider {
        private String name;
        private String version;
    }

    @Getter
    @Setter
    public static class NotificationTopic {
        private String specificationReference;
        private String dataEncoding;
    }

    @Getter
    @Setter
    public static class SupportedPredicateParameter {
        private String passedToConsumedService;
        private String parameterName;
    }

    @Getter
    @Setter
    public static class FileFormat {
        private String specificationReference;
        private String encoding;
    }

    @Getter
    @Setter
    public static class DataType {
        private String mediumType;
        private String schemaName;
        private String schemaVersion;
    }


    /**
     * Returns the qualified Data Service Name created in Data Catalog.
     * Qualified means that the Data Service Name is prefixed with the respective Data Service Data-type Schema Name.
     */
    public String getDataServiceName(){
        return getDataType().getSchemaName() + "-" + getNameSuffix();
    }

    /**
     * Returns the qualified Data Service Instance name.
     * Qualified means  that the Data Service Name is prefixed with the respective Data Service Data-type Schema Name and the Data Service Name.
     */
    public String getDataServiceInstanceName() {
        return getDataServiceName() + "--" + getDataServiceInstance().getConsumedDataProvider();
    }
}
