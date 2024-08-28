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
package com.ericsson.oss.adc.sftp.filetrans.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectionPropertiesModel {
    
    private Long id;
    private Long subsystemId;
    private String name;
    private String tenant;
    private String username;
    private String password;
    private String scriptingVMs;
    private String sftpPort;
    private List<String> encryptedKeys;
    private List<SubsystemUsersModel> subsystemUsers;


    public List<String> getScriptingVMs() {
        final String delimiter = ",";
        if(scriptingVMs == null){
            return Collections.emptyList();
        }
        return Arrays.asList(scriptingVMs.split(delimiter));
    }

}
