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
package com.ericsson.oss.adc.sftp.filetrans.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileFormatApiModel {

    private Long id;
    private DataServiceInstanceModel dataServiceInstance;
    private DataSpaceModel dataSpace;
    private DataServiceModel dataService;
    private SupportedPredicateParameterModel supportedPredicateParameter;
    private DataCategoryModel dataCategory;
    private DataProviderTypeModel dataProviderType;
    private NotificationTopicModel notificationTopic;
    private FileFormatModel fileFormat;
    private DataTypeModel dataType;
    private BulkDataRepositoryModel bulkDataRepository;
    private List<Long> reportOutputPeriodList;
    private String specificationReference;
    private String dataEncoding;

}