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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Optional;

@Component
@Slf4j
public class RestTemplateFacade {

    @Autowired
    private RestTemplate restTemplate;

    public <T> ResponseEntity<Optional<T>> post(String uri, T requestBody, Class<T> responseType) {
        log.info("Processing a POST request: '{}', HTTP request body: {} ...", uri, requestBody);

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<T> httpRequest = new HttpEntity<>(requestBody, headers);

        try {
            T responseObject = restTemplate.postForObject(URI.create(uri), httpRequest, responseType);
            log.info("Request processed successfully. Object CREATED: {}", responseObject);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Optional.ofNullable(responseObject));

        } catch (HttpStatusCodeException restException) {
            logHttpErrorResponse(restException, uri, HttpMethod.POST);
            return ResponseEntity.status(restException.getStatusCode())
                    .body(Optional.empty());
        }
    }

    public <T> ResponseEntity<Optional<T>> get(String uri, Class<T> responseType) {
        log.info("Processing a GET request: '{}' ...", uri);

        try {
            T responseObject = restTemplate.getForObject(URI.create(uri), responseType);
            log.info("Request processed successfully. Object READ: {}", responseObject);
            return ResponseEntity.ok(Optional.ofNullable(responseObject));

        } catch (HttpStatusCodeException restException) {
            logHttpErrorResponse(restException, uri, HttpMethod.GET);
            return ResponseEntity.status(restException.getStatusCode())
                    .body(Optional.empty());
        }
    }

    public <T> ResponseEntity<Optional<T>> put(String uri, HttpEntity<String> request, Class<T> responseType) {
        log.info("Processing a PUT request: '{}', HTTP request body: {} ...", uri, request.getBody());

        try {
            ResponseEntity<T> responseEntity = restTemplate.exchange(uri, HttpMethod.PUT, request, responseType);
            log.info("Request processed successfully. Object UPDATED.");
            return ResponseEntity.status(responseEntity.getStatusCode()) // Could be 200 (OK) or 201 (CREATED)
                    .body(Optional.ofNullable(responseEntity.getBody()));

        } catch (HttpStatusCodeException restException) {
            logHttpErrorResponse(restException, uri, HttpMethod.PUT);
            return ResponseEntity.status(restException.getStatusCode())
                    .body(Optional.empty());
        }
    }

    public ResponseEntity<Void> delete(String uri) {
        log.info("Processing a DELETE request: '{}' ...", uri);

        try {
            restTemplate.delete(URI.create(uri));
            log.info("Request processed successfully. Object DELETED.");
            return ResponseEntity.noContent().build();

        } catch (HttpStatusCodeException restException) {
            logHttpErrorResponse(restException, uri, HttpMethod.DELETE);
            return ResponseEntity.status(restException.getStatusCode())
                    .build();
        }
    }

    private void logHttpErrorResponse(HttpStatusCodeException restException, String uri, HttpMethod httpMethod) {
        log.error("Error while processing the {} request: '{}'. Error message: {}", httpMethod, uri, restException.getMessage());
        log.debug("Exception stack trace ...", restException);
    }

}