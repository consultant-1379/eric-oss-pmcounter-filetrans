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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
@Slf4j
public class SFTPProcessingMetricsUtil {
    private static final String ERIC_OSS_SFTP_FILETRANS = "eric.oss.sftp.filetrans:";
    public static final String NUM_INPUT_KAFKA_MESSAGES_RECEIVED_TOTAL = "num.input.kafka.messages.received.total";
    public static final String NUM_INPUT_KAFKA_MESSAGES_REPLAYED_TOTAL = "num.input.kafka.messages.replayed.total";
    public static final String NUM_TRANSACTIONS_ROLLEDBACK_TOTAL = "num.transactions.rolledback.total";
    public static final String NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL = "num.successful.file.transfer.total";
    public static final String NUM_FAILED_FILE_TRANSFER_TOTAL = "num.failed.file.transfer.total";
    public static final String NUM_SUCCESSFUL_BDR_UPLOADS_TOTAL = "num.successful.bdr.uploads.total";
    public static final String NUM_FAILED_BDR_UPLOADS_TOTAL = "num.failed.bdr.uploads.total";
    public static final String NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY = "num.output.kafka.messages.produced.successfully";
    public static final String NUM_OUTPUT_KAFKA_MESSAGES_FAILED_TOTAL = "num.output.kafka.messages.failed.total";
    public static final String PROCESSED_BDR_DATA_VOLUME_TOTAL = "processed.bdr.data.volume.total";
    public static final String PROCESSED_COUNTER_FILE_DATA_VOLUME_TOTAL = "processed.counter.file.data.volume.total";
    public static final String PROCESSED_COUNTER_FILES_TIME_TOTAL = "processed.counter.file.time.total";

    //batch sftp and bdr time metrics
    public static final String BATCH_SFTP_DOWNLOAD_TIME_TOTAL = "batch.sftp.download.time.total";
    public static final String BATCH_BDR_UPLOAD_TIME_TOTAL = "batch.bdr.upload.time.total";


    private final ConcurrentHashMap<String, Counter> counterMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> gaugeMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Timer> timerMap = new ConcurrentHashMap<>();
    final MeterRegistry meterRegistry;
    public SFTPProcessingMetricsUtil(final MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        counterMap.put(NUM_INPUT_KAFKA_MESSAGES_RECEIVED_TOTAL, meterRegistry.counter(ERIC_OSS_SFTP_FILETRANS + NUM_INPUT_KAFKA_MESSAGES_RECEIVED_TOTAL));
        counterMap.put(NUM_INPUT_KAFKA_MESSAGES_REPLAYED_TOTAL,
                meterRegistry.counter(ERIC_OSS_SFTP_FILETRANS + NUM_INPUT_KAFKA_MESSAGES_REPLAYED_TOTAL));
        counterMap.put(NUM_TRANSACTIONS_ROLLEDBACK_TOTAL, meterRegistry.counter(ERIC_OSS_SFTP_FILETRANS + NUM_TRANSACTIONS_ROLLEDBACK_TOTAL));
        counterMap.put(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL, meterRegistry.counter(ERIC_OSS_SFTP_FILETRANS + NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL));
        counterMap.put(NUM_FAILED_FILE_TRANSFER_TOTAL, meterRegistry.counter(ERIC_OSS_SFTP_FILETRANS + NUM_FAILED_FILE_TRANSFER_TOTAL));
        counterMap.put(NUM_SUCCESSFUL_BDR_UPLOADS_TOTAL, meterRegistry.counter(ERIC_OSS_SFTP_FILETRANS + NUM_SUCCESSFUL_BDR_UPLOADS_TOTAL));
        counterMap.put(NUM_FAILED_BDR_UPLOADS_TOTAL, meterRegistry.counter(ERIC_OSS_SFTP_FILETRANS + NUM_FAILED_BDR_UPLOADS_TOTAL));
        counterMap.put(NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY,
                meterRegistry.counter(ERIC_OSS_SFTP_FILETRANS + NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY));
        counterMap.put(NUM_OUTPUT_KAFKA_MESSAGES_FAILED_TOTAL,
                meterRegistry.counter(ERIC_OSS_SFTP_FILETRANS + NUM_OUTPUT_KAFKA_MESSAGES_FAILED_TOTAL));
        gaugeMap.put(PROCESSED_BDR_DATA_VOLUME_TOTAL, meterRegistry.gauge(ERIC_OSS_SFTP_FILETRANS + PROCESSED_BDR_DATA_VOLUME_TOTAL, new AtomicLong(0)));
        gaugeMap.put(PROCESSED_COUNTER_FILE_DATA_VOLUME_TOTAL, meterRegistry.gauge(ERIC_OSS_SFTP_FILETRANS + PROCESSED_COUNTER_FILE_DATA_VOLUME_TOTAL, new AtomicLong(0)));
        timerMap.put(PROCESSED_COUNTER_FILES_TIME_TOTAL, meterRegistry.timer(ERIC_OSS_SFTP_FILETRANS + PROCESSED_COUNTER_FILES_TIME_TOTAL));

        // using gauges instead of timer as they need to be reset with each batch
        gaugeMap.put(BATCH_SFTP_DOWNLOAD_TIME_TOTAL, meterRegistry.gauge(ERIC_OSS_SFTP_FILETRANS + BATCH_SFTP_DOWNLOAD_TIME_TOTAL, new AtomicLong(0)));
        gaugeMap.put(BATCH_BDR_UPLOAD_TIME_TOTAL, meterRegistry.gauge(ERIC_OSS_SFTP_FILETRANS + BATCH_BDR_UPLOAD_TIME_TOTAL, new AtomicLong(0)));

    }
    public boolean incrementCounterByName(final String counterName) {
        if (!counterMap.containsKey(counterName)) {
            log.error("Counter {} doesn't exist", counterName);
            return false;
        } else {
            counterMap.get(counterName).increment();
        }
        return true;
    }

    public boolean incrementCounterByValue(final String counterName, final double incrementValue) {
        if (!counterMap.containsKey(counterName)) {
            log.error("Counter {} doesn't exist", counterName);
            return false;
        } else {
            counterMap.get(counterName).increment(incrementValue);
        }
        return true;
    }

    public boolean addToGaugeByName(final String gaugeName, final long value) {
        if (!gaugeMap.containsKey(gaugeName)) {
            log.error("Gauge {} doesn't exist", gaugeName);
            return false;
        }
        else{
            gaugeMap.get(gaugeName).addAndGet(value);
        }
        return true;
    }

    public boolean resetGaugeByName(final String gaugeName) {
        if (!gaugeMap.containsKey(gaugeName)) {
            log.error("Gauge {} doesn't exist", gaugeName);
            return false;
        }
        else {
            gaugeMap.get(gaugeName).getAndSet(0L);
        }
        return true;
    }

    public boolean addCounterByName(final String counterName) {
        if (counterMap.containsKey(counterName)) {
            log.error("Counter {} already exists", counterName);
            return false;
        }
        log.info("Counter {} does not exist, adding it now as {}{}", counterName, ERIC_OSS_SFTP_FILETRANS, counterName);
        counterMap.put(counterName, meterRegistry.counter(ERIC_OSS_SFTP_FILETRANS + counterName));
        return true;
    }

    public double getCounterValueByName(final String counterName) {
        if (!counterMap.containsKey(counterName)) {
            log.error("Counter {} doesn't exist", counterName);
            return 0;
        } else {
            return counterMap.get(counterName).count();
        }
    }

    public double getGaugeValueByName(final String gaugeName) {
        if (!gaugeMap.containsKey(gaugeName)) {
            log.error("Gauge {} doesn't exist", gaugeName);
            return 0;
        } else {
            return gaugeMap.get(gaugeName).get();
        }
    }

    public long getTimerValueByName(final String counterName) {
        if (!timerMap.containsKey(counterName)) {
            log.error("Timer {} doesn't exist", counterName);
            return 0;
        } else {
            return timerMap.get(counterName).count();
        }
    }

    public Timer getTimer(final String timerName) {
        return timerMap.get(timerName);
    }

    public void recordTimer(final String timerName, final long startTime) {
        if (!timerMap.containsKey(timerName)) {
            log.error("Timer {} doesn't exist", timerName);
        } else {
            final long diff = System.currentTimeMillis() - startTime;
            timerMap.get(timerName).record(diff, TimeUnit.MILLISECONDS);
            log.debug("Timer: Took {} ms to process", diff);
        }
    }

    public void printAllCounterValues(final String stage) {
            log.info("--------------------------------------------- METRIC VALUES : {} -------------------------------------------------", stage);
            log.info("Successful File Transfers (total): {}", getCounterValueByName(NUM_SUCCESSFUL_FILE_TRANSFER_TOTAL));
            log.info("FAILED File Transfers (total): {}", getCounterValueByName(NUM_FAILED_FILE_TRANSFER_TOTAL));
            log.info("Kafka INPUT Messages received (total): {}", getCounterValueByName(NUM_INPUT_KAFKA_MESSAGES_RECEIVED_TOTAL));
            log.info("Kafka INPUT Messages relayed (total): {}", getCounterValueByName(NUM_INPUT_KAFKA_MESSAGES_REPLAYED_TOTAL));
            log.info("Kafka OUTPUT Messages produced (total): {}", getCounterValueByName(NUM_OUTPUT_KAFKA_MESSAGES_PRODUCED_SUCCESSFULLY));
            log.info("Kafka OUTPUT Messages FAILED (total): {}", getCounterValueByName(NUM_OUTPUT_KAFKA_MESSAGES_FAILED_TOTAL));
            log.info("Transactions rolled back (total): {}", getCounterValueByName(NUM_TRANSACTIONS_ROLLEDBACK_TOTAL));
            log.info("Successful BDR uploads (total): {}", getCounterValueByName(NUM_SUCCESSFUL_BDR_UPLOADS_TOTAL));
            log.info("FAILED BDR uploads (total): {}", getCounterValueByName(NUM_FAILED_BDR_UPLOADS_TOTAL));
            log.info("Total volume of BDR data processed (bytes): {}", getGaugeValueByName(PROCESSED_BDR_DATA_VOLUME_TOTAL));
            log.info("Total volume of counter file data processed (bytes): {}", getGaugeValueByName(PROCESSED_COUNTER_FILE_DATA_VOLUME_TOTAL));
            log.info("Total time to process counter files (ms): {}", getTimer(PROCESSED_COUNTER_FILES_TIME_TOTAL).totalTime(TimeUnit.MILLISECONDS));
            log.info("------------------------------------------------------------------------------------------------------------------");
    }
}
