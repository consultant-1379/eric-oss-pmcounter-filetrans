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

@ConfigurationProperties(prefix = "dmm.data-catalog")
@Getter
@Setter
public class DataCatalogProperties {

    private String baseUrl;
    private String basePort;
    private String dataServiceUri;
    private String fileFormatUri;
    private String fileFormatUriV2;
    private String messageBusUri;
    private String bulkDataRepositoryUri;
    private String messageBusName;
    private String messageBusNamespace;

    public String getHost() {
        return getBaseUrl() + getBasePort();
    }

    public String getDataServicePath() {
        return getHost() + getDataServiceUri();
    }


    public String getFileFormatPath() {
        return getHost() + getFileFormatUri();
    }

}
