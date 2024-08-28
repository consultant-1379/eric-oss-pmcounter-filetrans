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
package com.ericsson.oss.adc.sftp.filetrans.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import com.ericsson.oss.adc.sftp.filetrans.model.InputMessage;
import com.google.gson.Gson;

/**
 * Kafka consumer Test that listens on the file-notification-service--sftp-filetrans--enm1 for Kafka messages and asserts that they are read
 * successfully.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}", "subsystem.name=enm1", "dmm.data-catalog.base-url=http://localhost:",
        "dmm.data-catalog.base-port=9590", "dmm.data-catalog.notification-topic-uri=/catalog/v1/notification-topic/",
        "dmm.data-catalog.file-format-uri-v2=/catalog/v2/file-format/", "dmm.data-catalog.file-format-uri=/catalog/v1/file-format/",
        "dmm.data-catalog.message-bus-uri=/catalog/v1/message-bus/", "dmm.data-catalog.bulk-data-repository-uri=/catalog/v1/bulk-data-repository/",
        "dmm.data-catalog.data-provider-type-5G=5G", "dmm.data-catalog.data-provider-type-4G=4G", "dmm.data-catalog.data-space=enm",
        "dmm.data-catalog.data-category=PM_COUNTERS", "connected.systems.uri=subsystem-manager/v1/subsystems/" })
@EmbeddedKafka(partitions = 1, topics = { "file-notification-service--sftp-filetrans--enm1", "sftp-filetrans--enm1" }, brokerProperties = {
        "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1", "replica.fetch.min.bytes=10240",
        "replica.fetch.wait.max.ms=300000", "replica.socket.timeout.ms=300000", "replica.lag.time.max.ms=300000" })
public class KafkaInputConsumerTest {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaInputConsumerTest.class);
    private static final Gson gson = new Gson();
    private Producer<String, String> producer;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;
    @Autowired
    private InputTopicListener inputTopicListener;

    @Value("${spring.kafka.topics.input.name}")
    private String TOPIC;
    @Value("${spring.kafka.topics.enm_id}")
    private String enmID;

    @BeforeEach
    public void init() throws Exception {
        createProducer();
    }

    @Test
    public void consumeFileNotificationMessage() throws Exception {
        final InputMessage inputMessage = InputMessage.builder()
                .nodeName("SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR102gNodeBRadio00010,ManagedElement=NR102gNodeBRadio00010")
                .fileLocation("/ericsson/pmic1/XML/SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR102gNodeBRadio00010,ManagedElement=NR102gNodeBRadio00010/"
                        + "A20200721.1000+0100-1015+0100_SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR102gNodeBRadio00010,ManagedElement=NR102gNodeBRadio00010_statsfile.xml.gz")
                .nodeType("testNode")
                .dataType("testData")
                .fileType("testFile")
                .build();
        final String jsonInputTopicPayload = gson.toJson(inputMessage);
        final ProducerRecord<String, String> producerRecord = new ProducerRecord<>(TOPIC + enmID, "", jsonInputTopicPayload);
        producer.send(producerRecord);

        inputTopicListener.getLatch().await(10000, TimeUnit.MILLISECONDS);
        assertThat(inputTopicListener.getLatch().getCount()).isEqualTo(0);
    }

    @Test
    public void testScheduledMetricsPrint() {
        inputTopicListener.scheduledSendMetrics();
    }

    private void createProducer() {
        final Map<String, Object> configs = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        producer = new DefaultKafkaProducerFactory<>(configs, new StringSerializer(), new StringSerializer()).createProducer();
    }
}
