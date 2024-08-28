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

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * The Class RestExecutorTest.
 */

@ExtendWith(MockitoExtension.class)
class RestExecutorTest {
    private static final String URL = "http://localhost:8080";
    private static final String INVALID_URL = "invalidURL";
    private static final String NAME = "name";
    private static final String PASSWORD = "password";

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private RestExecutor restExecutor;

    @Mock
    private ResponseEntity responseEntity;


    /**
     * Test rest executor exchange returns response entity.
     */
    @Test
    void testRestExecutorExchangeReturnsResponseEntity(){
        Mockito.when(restTemplate.exchange(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(Class.class))).thenReturn(responseEntity);
        Assert.assertSame(responseEntity, restExecutor.exchange(URL, "testExchange", HttpMethod.GET, ResponseEntity.class).getResponseEntity());
    }

    /**
     * Test rest executor exchange exception caught.
     */
    @Test
    void testRestExecutorExchangeExceptionCaught(){
        Assertions.assertDoesNotThrow(() ->
        restExecutor.exchange(
                INVALID_URL,
                "testRestExecutorExchangeExceptionCaught",
                HttpMethod.GET,
                ResponseEntity.class
                ));
    }

    /**
     * Test rest executor exchange with authentication returns response entity.
     */
    @Test
    void testRestExecutorExchangeWithAuthenticationReturnsResponseEntity(){
        Mockito.when(restTemplate.exchange(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.any(Class.class))).thenReturn(responseEntity);
        Assert.assertSame(responseEntity, restExecutor.exchangeWithAuthentication (NAME, PASSWORD, URL, "testExchange", HttpMethod.GET, ResponseEntity.class).getResponseEntity());
    }

    /**
     * Test rest executor create post entity.
     */
    @Test
    void testRestExecutorCreatePostEntity(){
        Mockito.when(restTemplate.postForEntity(
                ArgumentMatchers.anyString(),
                Mockito.isNull(),
                ArgumentMatchers.any(Class.class))).thenReturn(responseEntity);
        Assert.assertSame(responseEntity, restExecutor.postForEntity (URL, "testExchange", null, ResponseEntity.class).getResponseEntity());
    }

    /**
     * Test rest executor exchange with authentication exception caught.
     */
    @Test
    void testRestExecutorExchangeWithAuthenticationExceptionCaught(){
        Mockito.when(restTemplate.exchange(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(Class.class))).thenThrow(RestClientException.class);

        Assertions.assertDoesNotThrow(() ->
        restExecutor.exchangeWithAuthentication(
                NAME,
                PASSWORD,
                INVALID_URL,
                "testRestExecutorExchangeExceptionCaught",
                HttpMethod.GET,
                ResponseEntity.class
                ));
    }

    /**
     * Test rest executor throw exception.
     */
    @Test
    void testRestExecutorThrowException(){
        Mockito.when(restTemplate.postForEntity(
                ArgumentMatchers.anyString(),
                Mockito.isNull(),
                ArgumentMatchers.any(Class.class))).thenThrow(new RestClientException("Rest Exception"));
        Assert.assertSame(HttpStatus.SERVICE_UNAVAILABLE,
                restExecutor.postForEntity (URL, "testExchange", null, ResponseEntity.class).getResponseEntity().getStatusCode());
    }

}