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
package com.ericsson.oss.adc.sftp.filetrans.rest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataCatalogUriBuilder {


    // Params
    private static final String NAME_PARAM = "name";
    private static final String DATA_SERVICE_NAME_PARAM = "dataServiceName";
    private static final String DATA_SERVICE_INSTANCE_NAME_PARAM = "dataServiceInstanceName";

    private final String basePath;
    private final StringBuilder uri;

    protected DataCatalogUriBuilder(String basePath) {
        this.basePath = basePath;
        uri = new StringBuilder();
    }

    // -- Core -- //

    public static DataCatalogUriBuilder base(String basePath) {
        return new DataCatalogUriBuilder(basePath);
    }

    public String build() {
        String resultUri = basePath + uri;
        log.trace("Built URI: '{}'", resultUri);
        return resultUri;
    }

    // -- Parameter Setters -- //

    public DataCatalogUriBuilder name(String name) {
        return appendParam(NAME_PARAM, name);
    }

    public DataCatalogUriBuilder dataService(String dataService) {
        return appendParam(DATA_SERVICE_NAME_PARAM, dataService);
    }

    public DataCatalogUriBuilder dataServiceInstance(String dataServiceInstance) {
        return appendParam(DATA_SERVICE_INSTANCE_NAME_PARAM, dataServiceInstance);
    }

    // -- Utilities -- //

    private DataCatalogUriBuilder appendParam(String paramName, String paramValue) {
        uri.append(resolveRequestParamDelimiter())
                .append(paramName).append("=").append(paramValue);
        return this;
    }

    private String resolveRequestParamDelimiter() {
        return uri.lastIndexOf("?") != -1 ? "&" : "?";
    }

}