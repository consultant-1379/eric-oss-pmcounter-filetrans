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
package com.ericsson.oss.adc.sftp.filetrans.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ENMScriptingVMLoadBalancer {

    private String currentHost;

    private List<String> allAvailableScriptingVMs =  new ArrayList<>();

    private List<String> onlineAvailableScriptingVMs =  new ArrayList<>();

    public List<String> getAllAvailableScriptingVMs() {
        return new ArrayList<>(allAvailableScriptingVMs);
    }

    public List<String> getAllOnlineScriptingVMs() {
        return new ArrayList<>(onlineAvailableScriptingVMs);
    }

    public String getRandomOnlineScriptingVMs() {
        final SecureRandom rand = new SecureRandom();
        final int randomIndex = rand.nextInt(onlineAvailableScriptingVMs.size());
        currentHost = onlineAvailableScriptingVMs.get(randomIndex);
        return currentHost;
    }

    public String getCurrentConnectedScriptingVMs() {
        return currentHost;
    }

    public void setScriptingVMs(final List<String> scriptingVmList) {
        allAvailableScriptingVMs = new ArrayList<>(scriptingVmList);
        onlineAvailableScriptingVMs =  new ArrayList<>(scriptingVmList);
    }

    public void resetAllAvailableScriptingVMs() {
        onlineAvailableScriptingVMs =  new ArrayList<>(allAvailableScriptingVMs);
    }

    public void clearAllAvailableScriptingVMsMaps() {
        onlineAvailableScriptingVMs.clear();
        allAvailableScriptingVMs.clear();
    }

    public List<String> removeVMFromOnlineScriptingVMs(final String scriptingVM) {
        if (scriptingVM == null) {
            int i = 0;
            for (i = 0; i < onlineAvailableScriptingVMs.size(); i++) {
                if (onlineAvailableScriptingVMs.get(i) == null) {
                    break;
                }
            }
            if (i < onlineAvailableScriptingVMs.size()) {
                onlineAvailableScriptingVMs.remove(i);
            }
        }
        else {
            onlineAvailableScriptingVMs.remove(scriptingVM);
        }
        return new ArrayList<>(onlineAvailableScriptingVMs);
    }
}
