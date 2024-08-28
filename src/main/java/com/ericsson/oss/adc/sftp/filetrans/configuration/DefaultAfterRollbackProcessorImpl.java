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
package com.ericsson.oss.adc.sftp.filetrans.configuration;

import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_INPUT_KAFKA_MESSAGES_REPLAYED_TOTAL;
import static com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil.NUM_TRANSACTIONS_ROLLEDBACK_TOTAL;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.AfterRollbackProcessor;
import org.springframework.kafka.listener.ContainerProperties.EOSMode;
import org.springframework.kafka.listener.DefaultAfterRollbackProcessor;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.lang.Nullable;
import com.ericsson.oss.adc.sftp.filetrans.util.SFTPProcessingMetricsUtil;

@Slf4j
public class DefaultAfterRollbackProcessorImpl<K, V> extends DefaultAfterRollbackProcessor<K, V> implements AfterRollbackProcessor<K, V> {

    @Autowired
    SFTPProcessingMetricsUtil metrics;

    public DefaultAfterRollbackProcessorImpl() {
        // No Args Constructor.
    }

    @Override
    public void process(final List<ConsumerRecord<K, V>> records, final Consumer<K, V> consumer, @Nullable final MessageListenerContainer container,
                        final Exception exception, final boolean recoverable, final EOSMode eosMode) {
        log.info("------------------------- Rolling Back Transaction for this Batch -------------------------");
        int index = 0;
        for (final ConsumerRecord<K, V> record : records) {
            metrics.incrementCounterByName(NUM_INPUT_KAFKA_MESSAGES_REPLAYED_TOTAL);
            ++index;
            log.info("Record # {}, offset = {}, key = {}, value = {}", index, record.offset(), record.key(), record.value());
        }
        metrics.incrementCounterByName(NUM_TRANSACTIONS_ROLLEDBACK_TOTAL);
        log.info("Transactions rolled back (total): {}", (int) metrics.getCounterValueByName(NUM_TRANSACTIONS_ROLLEDBACK_TOTAL));
        log.info("---------------------------------------------------------------------------------------\n\n");

        super.process(records, consumer, container, exception, recoverable, eosMode);
    }
}
