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
package com.ericsson.oss.adc.sftp.filetrans.test.integration.setup.rest;

import org.hamcrest.Matcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

@Component
public class MockRestServerFacade {

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockRestServiceServer;

    public void resetServer() {
        mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);
    }

    // -- Core -- //

    public void mockResponse(Matcher<String> urlMatcher, ExpectedCount expectedCount, HttpMethod method, HttpStatus responseStatus, String responseBody) {
        mockRestServiceServer.expect(expectedCount, requestTo(urlMatcher))
                .andExpect(method(method))
                .andRespond(MockRestResponseCreators.withStatus(responseStatus)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseBody));
    }

    public void mockResponse(Matcher<String> urlMatcher, HttpMethod method, HttpStatus responseStatus, String responseBody) {
        mockResponse(urlMatcher, ExpectedCount.once(), method, responseStatus, responseBody);
    }

    public void mockResponse(Matcher<String> urlMatcher, HttpMethod method, HttpStatus responseStatus) {
        mockRestServiceServer.expect(requestTo(urlMatcher))
                .andExpect(method(method))
                .andRespond(MockRestResponseCreators.withStatus(responseStatus)
                        .contentType(MediaType.APPLICATION_JSON));
    }

    public void mockOkResponse(Matcher<String> urlMatcher, HttpMethod method, String responseBody) {
        mockResponse(urlMatcher, method, HttpStatus.OK, responseBody);
    }

    // -- GET Responses -- //

    public void mockGetResponse(Matcher<String> urlMatcher, HttpStatus responseStatus, String responseBody) {
        mockResponse(urlMatcher, HttpMethod.GET, responseStatus, responseBody);
    }

    public void mockGetOkResponse(Matcher<String> urlMatcher, ExpectedCount expectedCount, String responseBody) {
        mockResponse(urlMatcher, expectedCount, HttpMethod.GET, HttpStatus.OK, responseBody);
    }

    public void mockGetOkResponse(Matcher<String> urlMatcher, String responseBody) {
        mockOkResponse(urlMatcher, HttpMethod.GET, responseBody);
    }

    public void mockGetOkResponse(Matcher<String> urlMatcher, ExpectedCount expectedCount, Resource responseBody) {
        mockRestServiceServer.expect(expectedCount, requestTo(urlMatcher))
                .andExpect(method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseBody));
    }

    // -- POST Responses -- //

    public void mockPostResponse(Matcher<String> urlMatcher, HttpStatus responseStatus, String responseBody) {
        mockResponse(urlMatcher, HttpMethod.POST, responseStatus, responseBody);
    }

    public void mockPostOkResponse(Matcher<String> urlMatcher, ExpectedCount expectedCount, String responseBody) {
        mockResponse(urlMatcher, expectedCount, HttpMethod.POST, HttpStatus.OK, responseBody);
    }

    public void mockPostOkResponse(Matcher<String> urlMatcher, String responseBody) {
        mockOkResponse(urlMatcher, HttpMethod.POST, responseBody);
    }

    // -- PUT Responses -- //

    public void mockPutResponse(Matcher<String> urlMatcher, HttpStatus responseStatus, String responseBody) {
        mockResponse(urlMatcher, HttpMethod.PUT, responseStatus, responseBody);
    }

    public void mockPutOkResponse(Matcher<String> urlMatcher, String responseBody) {
        mockResponse(urlMatcher, HttpMethod.PUT, HttpStatus.OK, responseBody);
    }

    // -- DELETE Responses -- //

    public void mockDeleteResponse(Matcher<String> urlMatcher, HttpStatus status) {
        mockResponse(urlMatcher, HttpMethod.DELETE, status);
    }

    // -- Exception Responses -- //

    public void mockException(Matcher<String> urlMatcher, HttpMethod method) {
        mockRestServiceServer.expect(requestTo(urlMatcher))
                .andExpect(method(method))
                .andRespond(MockRestResponseCreators.withException(new IOException("Mocked Exception")));
    }

}