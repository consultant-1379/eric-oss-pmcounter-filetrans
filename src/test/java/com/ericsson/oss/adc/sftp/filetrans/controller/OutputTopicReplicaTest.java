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

import org.apache.kafka.clients.admin.TopicDescription;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest(properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@EmbeddedKafka(count = 5, brokerProperties = {"min.insync.replicas=4"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext
public class OutputTopicReplicaTest {

    @Autowired
    OutputTopicController outputTopicController;

    @Autowired
    KafkaAdmin kafkaAdmin;

    @Test
    @Order(0)
    void test_setup() throws InterruptedException {
        // Adding a sleep to allow EmbeddedKafka to fully start up. Was seeing issues with leader_not_available.
        TimeUnit.SECONDS.sleep(3);
    }

    @Test
    @Order(1)
    @DisplayName("Tests number of replicas when less than Min ISR")
    void test_number_of_replicas_when_less_than_Min_ISRs() {
        final String testTopic = "testTopic1";
        // USE ONLY THIS SETTER INSIDE TEST CASES
        outputTopicController.setOutputTopicName(testTopic);
        outputTopicController.setReplicas((short) 1);
        outputTopicController.buildAndCreateTopic();
        Assert.assertEquals(4, outputTopicController.getMinIsrReplicas(0));
        Assert.assertEquals(4, getNumberOfReplicasByTopicName(testTopic));
    }

    @Test
    @Order(2)
    @DisplayName("Tests number of replicas when greater than Min ISR")
    void test_number_of_replicas_when_greater_than_Min_ISRs() {
        final String testTopic = "testTopic2";
        // USE ONLY THIS SETTER INSIDE TEST CASES
        outputTopicController.setOutputTopicName(testTopic);
        outputTopicController.setReplicas((short) 5);
        outputTopicController.buildAndCreateTopic();
        Assert.assertEquals(4, outputTopicController.getMinIsrReplicas(0));
        Assert.assertEquals(5, getNumberOfReplicasByTopicName(testTopic));
    }

    @Test
    @Order(3)
    @DisplayName("Test get minimum ISRs returns 0 with incorrect broker id")
    void test_min_isr_when_incorrect_broker_id() {
        Assert.assertEquals(0, outputTopicController.getMinIsrReplicas(-1));
    }

    private int getNumberOfReplicasByTopicName(final String topicName) {
        final Map<String, TopicDescription> topics = kafkaAdmin.describeTopics(topicName);
        return topics.get(topicName).partitions().get(0).replicas().size();
    }
}

