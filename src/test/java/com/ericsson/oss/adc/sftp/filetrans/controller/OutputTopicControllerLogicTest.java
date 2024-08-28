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
package com.ericsson.oss.adc.sftp.filetrans.controller;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * The Class OutputTopicControllerLogicTest.
 * This test the logic of the OutputTopicController with out starting Spring.
 *
 */


class OutputTopicControllerLogicTest {

    /**
     * test create Topic Fails If Kafka Exception Thrown.
     *
     * @throws KafkaException
     *             the kafka exception
     */
    @Test
    @DisplayName("Expected outputTopicController createTopic to FAIL when KafkaException thrown.")
    void test_createTopicFailIfKafkaExceptionThrown() throws KafkaException {
        final KafkaAdmin ka = mock(KafkaAdmin.class);
        final OutputTopicController outputTopicController = new OutputTopicController();
        outputTopicController.setKafkaAdmin(ka);

        final NewTopic outputTopic = TopicBuilder.name("MoreMuck").partitions(1).replicas(0).build();
        doThrow(new KafkaException("Error occurred")).when(ka).createOrModifyTopics(outputTopic);

        assertThrows(KafkaException.class, () -> outputTopicController.createTopic(outputTopic));
    }

}
