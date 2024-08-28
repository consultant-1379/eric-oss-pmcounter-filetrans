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

package com.ericsson.oss.adc.sftp.filetrans.util;



import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Generic REST utility to class to handle REST exchanges, exception handling, and logging
 */
@Deprecated
@Component
@Slf4j
public class RestExecutor {

    @Autowired
    private RestTemplate restTemplate;


    private HttpEntity<Object> requestStructure(){
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(headers);
    }

    private HttpEntity<Object> requestStructureWithAuthentication(final String username, final String password){
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(username, password);
        return new HttpEntity<>(headers);
    }

    public <T> ResponseEntityWrapper<T> exchange(final String url, final String logMessage, final HttpMethod httpMethod, final Class<T> responseType) {
        log.info("{} : {}", logMessage, url);
        final ResponseEntityWrapper<T> responseEntityWrapper = ResponseEntityWrapper.<T>builder().build();
        try {
            final ResponseEntity<T> response = restTemplate.exchange(url, httpMethod, requestStructure(), responseType);
            responseEntityWrapper.setResponseEntity(response);
        }
        catch (HttpStatusCodeException exception) {
            log.error("FAILED to {} : {} , Error Msg: {}", httpMethod.name(), url, exception.getMessage());
            responseEntityWrapper.setResponseEntity(new ResponseEntity<>(exception.getStatusCode()));
        }
        return responseEntityWrapper;
    }

    public <T> ResponseEntityWrapper<T> exchangeWithAuthentication(final String username, final String password, final String url, final String logMessage,
                                                                   final HttpMethod httpMethod, final Class<T> responseType) {
        log.info("{} : {}", logMessage, url);
        final ResponseEntityWrapper<T> responseEntityWrapper = ResponseEntityWrapper.<T>builder().build();
        try {
            final ResponseEntity<T> response = restTemplate.exchange(url, httpMethod, requestStructureWithAuthentication(username, password), responseType);
            responseEntityWrapper.setResponseEntity(response);
        } catch (final RestClientException error) {
            responseEntityWrapper.setResponseEntity(new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE));
            log.error("FAILED to GET with Authentication {} , Error Msg: {}", url, error.getMessage());
            log.debug("Full Trace - FAILED to GET , URL : {}, Error Trace: ", url, error);
        }
        return responseEntityWrapper;

    }

    public <T> ResponseEntityWrapper<T> postForEntity(final String url, final String logMessage, final HttpEntity<String> httpEntity,
                                                      final Class<T> responseType) {
        log.info("POST for '{}' using {}", logMessage, url);
        final ResponseEntityWrapper<T> responseEntityWrapper = ResponseEntityWrapper.<T>builder().build();
        try {
            final ResponseEntity<T> response = restTemplate.postForEntity(url, httpEntity, responseType);
            responseEntityWrapper.setResponseEntity(response);
        } catch (final HttpClientErrorException exception) {
            responseEntityWrapper.setResponseEntity(new ResponseEntity<>(exception.getStatusCode()));
            if (exception.getStatusCode() == HttpStatus.CONFLICT) {
                log.info("FAILED to POST : resource already exists : {},  message : {}", url, exception.getMessage());
                return responseEntityWrapper;
            }
            logPostFailure(url, exception);

        } catch (final RestClientException error) {
            responseEntityWrapper.setResponseEntity(new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE));
            logPostFailure(url, error);
        }
        return responseEntityWrapper;
    }

    private void logPostFailure(final String url, final Exception exception) {
        log.error("FAILED to POST : {}, Error message : {}", url, exception.getMessage());
        log.debug("Full Trace - FAILED to POST , URL : {}, Error Trace : ", url, exception);
    }

}