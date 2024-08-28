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
package com.ericsson.oss.adc.sftp.filetrans.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.ericsson.oss.adc.sftp.filetrans.model.ConnectionPropertiesModel;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UtilsTest {

    @Test
    @Order(1)
    @DisplayName("Expected Pass, good sleep.")
    void test_sleep() {
        final int waitTimeMs = 1000;
        final long then = System.currentTimeMillis();
        final boolean result = Utils.waitRetryInterval(waitTimeMs);
        final long now = System.currentTimeMillis();
        Assertions.assertTrue(result, "Expected Utils: Wait Retry Interval to sleep and return true, returned false.");
        Assertions.assertTrue((now - then) >= waitTimeMs,
                "Expected Utils: Wait Retry Interval to sleep for " + waitTimeMs + " mS. Actually waited for " + (now - then) + " mS");
    }

    @Test
    @Order(2)
    @DisplayName("Expected Fail, bad sleep.")
    void test_sleep_with_kids() {
        final boolean result = Utils.waitRetryInterval(-1);
        Assertions.assertFalse(result, "Expected Utils: Wait Retry Interval to sleep and return false (Interrupted), returned true.");
    }

    @Test
    @Order(3)
    @DisplayName("Expect isValidConnectionProperties, with null port to return false.")
    void test_isValidConnectionPropertiesPortIsNUll() {
        final ConnectionPropertiesModel connectionProperties = TestUtils.getConnectedSystemsDto();
        connectionProperties.setSftpPort(null);
        final boolean result = Utils.isValidConnectionProperties(connectionProperties);
        Assertions.assertFalse(result, "Expected Utils: isValidConnectionProperties with port == null to return false.");
    }

    @Test
    @Order(4)
    @DisplayName("Expect isValidConnectionProperties, with invalid username to return false.")
    void test_isValidConnectionPropertiesInvalidUsername() {
        final ConnectionPropertiesModel connectionProperties = TestUtils.getConnectedSystemsDto();
        connectionProperties.setUsername(null);
        final boolean result = Utils.isValidConnectionProperties(connectionProperties);
        Assertions.assertFalse(result, "Expected Utils: isValidConnectionProperties with username == null to return false.");
    }

    @Test
    @Order(5)
    @DisplayName("Expect isValidConnectionProperties, with invalid password to return false.")
    void test_isValidConnectionPropertiesInvalidPassword() {
        final ConnectionPropertiesModel connectionProperties = TestUtils.getConnectedSystemsDto();
        connectionProperties.setPassword(null);
        final boolean result = Utils.isValidConnectionProperties(connectionProperties);
        Assertions.assertFalse(result, "Expected Utils: isValidConnectionProperties with password == null to return false.");
    }

    @Test
    @Order(6)
    @DisplayName("Expect isValidConnectionProperties, with invalid port to return false.")
    void test_isValidConnectionPropertiesInvalidPort() {
        final ConnectionPropertiesModel connectionProperties = TestUtils.getConnectedSystemsDto();
        connectionProperties.setSftpPort("-1");
        final boolean result = Utils.isValidConnectionProperties(connectionProperties);
        Assertions.assertFalse(result, "Expected Utils: isValidConnectionProperties with port == -1 to return false.");
    }

    @Test
    @Order(7)
    @DisplayName("Expect isValidConnectionProperties, with valid username to return true.")
    void test_isValidConnectionPropertiesValidUsername() {
        final ConnectionPropertiesModel connectionProperties = TestUtils.getConnectedSystemsDto();
        connectionProperties.setUsername("user");
        final boolean result = Utils.isValidConnectionProperties(connectionProperties);
        Assertions.assertTrue(result, "Expected Utils: isValidConnectionProperties with username == user to return true.");
    }

    @Test
    @Order(8)
    @DisplayName("Expect isValidConnectionProperties, with valid password to return true.")
    void test_isValidConnectionPropertiesValidPassword() {
        final ConnectionPropertiesModel connectionProperties = TestUtils.getConnectedSystemsDto();
        connectionProperties.setPassword("pw");
        final boolean result = Utils.isValidConnectionProperties(connectionProperties);
        Assertions.assertTrue(result, "Expected Utils: isValidConnectionProperties with password == pw to return true.");
    }

    @Test
    @Order(9)
    @DisplayName("Expect isValidConnectionProperties, with valid port to return true.")
    void test_isValidConnectionPropertiesValidPort() {
        final ConnectionPropertiesModel connectionProperties = TestUtils.getConnectedSystemsDto();
        connectionProperties.setSftpPort("1");
        final boolean result = Utils.isValidConnectionProperties(connectionProperties);
        Assertions.assertTrue(result, "Expected Utils: isValidConnectionProperties with port == 1 to return true.");
    }

    @Test
    @Order(10)
    @DisplayName("Expect isValidConnectionProperties, with invalid username (empty string) to return false.")
    void test_isValidConnectionPropertiesInvalidUsernameEmptyString() {
        final ConnectionPropertiesModel connectionProperties = TestUtils.getConnectedSystemsDto();
        connectionProperties.setUsername("");
        final boolean result = Utils.isValidConnectionProperties(connectionProperties);
        Assertions.assertFalse(result, "Expected Utils: isValidConnectionProperties with username == '' to return false.");
    }

    @Test
    @Order(11)
    @DisplayName("Expect setRetryForever to Return false when AvailabilityRetryCountMax Is not MaxInteger.")
    void test_setRetryForeverReturnsTrueWhenAvailabilityRetryCountMaxIsNOTMaxInteger() {
        final boolean result = Utils.setRetryForever(Integer.MAX_VALUE - 1);
        Assertions.assertFalse(result, "Expected Utils: Expect isetRetryForever to Return false When AvailabilityRetryCountMax is not MaxInteger.");
    }

    @Test
    @Order(12)
    @DisplayName("Expect setRetryForever to Return true when AvailabilityRetryCountMax Is MaxInteger.")
    void test_setRetryForeverReturnsFalseWhenAvailabilityRetryCountMaxIsMaxInteger() {
        final boolean result = Utils.setRetryForever(Integer.MAX_VALUE);
        Assertions.assertTrue(result, "Expected Utils: Expect isetRetryForever to Return true When AvailabilityRetryCountMax is not MaxInteger.");
    }
}
