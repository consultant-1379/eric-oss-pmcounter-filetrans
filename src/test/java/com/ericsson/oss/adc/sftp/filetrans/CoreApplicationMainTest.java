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
package com.ericsson.oss.adc.sftp.filetrans;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.aspectj.bridge.MessageUtil.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CoreApplicationMainTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreApplicationMainTest.class);

    /**
     * Test core application main fails with null arguments.
     */
    @Test
    @Order(1)
    @DisplayName("TEST: Verify CoreApplication Main Fails With Null Arguments")
    void test_CoreApplicationMainFailsWithNullArguments() {
        try {
            CoreApplication.main(null);
            fail("Expected CoreApplication main to fail with IllegalArgumentException as args cannot be null");
        } catch (final IllegalArgumentException illegalArgumentException) {
            LOGGER.info("CoreApplication Main test passes");
        } catch (final Exception e) {

            fail("Expected CoreApplication main to fail with IllegalArgumentException as args cannot be null", e);
        }
    }
}
