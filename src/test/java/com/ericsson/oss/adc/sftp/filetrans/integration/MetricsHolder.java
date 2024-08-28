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
package com.ericsson.oss.adc.sftp.filetrans.integration;

/**
 * The Class MetricsHolder.
 */
public class MetricsHolder {
    private double numInputKafkaMessagesReceivedTotal;
    private double numSuccessfulFileTransferTotal;
    private double processedCounterFileDataVolumeTotal;
    private double numFailedFileTransferTotal;
    private double numSuccessfulBdrUploadsTotal;
    private double processedBdrFileDataVolumeTotal;
    private double numFailedBdrUploadsTotal;
    private double numOutputKafkaMessagesProducedSuccessfullyTotal;
    private double numOutputKafkaMessagesFailedTotal;
    private double numInputKafkaMessagesReplayedTotal;
    private double numTransactionsRolledbackTotal;

    /**
     * Instantiates a new metrics holder.
     */
    public MetricsHolder() {
        // No Args Constructor
    }

    /**
     * Instantiates a new metrics holder.
     *
     * @param numInputKafkaMessagesReceivedTotal
     *            the num input kafka messages received total
     * @param numSuccessfulFileTransferTotal
     *            the num successful file transfer total
     * @param processedCounterFileDataVolumeTotal
     *            the processed counter file data volume total
     * @param numFailedFileTransferTotal
     *            the num failed file transfer total
     * @param numSuccessfulBdrUploadsTotal
     *            the num successful bdr uploads total
     * @param processedBdrFileDataVolumeTotal
     *            the processed bdr file data volume total
     * @param numFailedBdrUploadsTotal
     *            the num failed bdr uploads total
     * @param numOutputKafkaMessagesProducedSuccessfullyTotal
     *            the num output kafka messages produced total
     * @param numOutputKafkaMessagesFailedTotal
     *            the num output kafka messages failed total
     * @param numInputKafkaMessagesReplayedTotal
     *            the num input kafka messages replayed total
     * @param numTransactionsRolledbackTotal
     *            the num transactions rolledback total
     */
    public MetricsHolder(final double numInputKafkaMessagesReceivedTotal, final double numSuccessfulFileTransferTotal,
                         final double processedCounterFileDataVolumeTotal, final double numFailedFileTransferTotal,
                         final double numSuccessfulBdrUploadsTotal, final double processedBdrFileDataVolumeTotal,
                         final double numFailedBdrUploadsTotal, final double numOutputKafkaMessagesProducedSuccessfullyTotal,
                         final double numOutputKafkaMessagesFailedTotal,
                         final double numInputKafkaMessagesReplayedTotal, final double numTransactionsRolledbackTotal) {
        super();
        this.numInputKafkaMessagesReceivedTotal = numInputKafkaMessagesReceivedTotal;
        this.numSuccessfulFileTransferTotal = numSuccessfulFileTransferTotal;
        this.processedCounterFileDataVolumeTotal = processedCounterFileDataVolumeTotal;
        this.numFailedFileTransferTotal = numFailedFileTransferTotal;
        this.numSuccessfulBdrUploadsTotal = numSuccessfulBdrUploadsTotal;
        this.processedBdrFileDataVolumeTotal = processedBdrFileDataVolumeTotal;
        this.numFailedBdrUploadsTotal = numFailedBdrUploadsTotal;
        this.numOutputKafkaMessagesProducedSuccessfullyTotal = numOutputKafkaMessagesProducedSuccessfullyTotal;
        this.numOutputKafkaMessagesFailedTotal = numOutputKafkaMessagesFailedTotal;
        this.numInputKafkaMessagesReplayedTotal = numInputKafkaMessagesReplayedTotal;
        this.numTransactionsRolledbackTotal = numTransactionsRolledbackTotal;
    }

    public double getNumInputKafkaMessagesReceivedTotal() {
        return numInputKafkaMessagesReceivedTotal;
    }

    public void setNumInputKafkaMessagesReceivedTotal(final double numInputKafkaMessagesReceivedTotal) {
        this.numInputKafkaMessagesReceivedTotal = numInputKafkaMessagesReceivedTotal;
    }

    public double getNumSuccessfulFileTransferTotal() {
        return numSuccessfulFileTransferTotal;
    }

    public void setNumSuccessfulFileTransferTotal(final double numSuccessfulFileTransferTotal) {
        this.numSuccessfulFileTransferTotal = numSuccessfulFileTransferTotal;
    }

    public double getProcessedCounterFileDataVolumeTotal() {
        return processedCounterFileDataVolumeTotal;
    }

    public void setProcessedCounterFileDataVolumeTotal(final double processedCounterFileDataVolumeTotal) {
        this.processedCounterFileDataVolumeTotal = processedCounterFileDataVolumeTotal;
    }

    public double getNumFailedFileTransferTotal() {
        return numFailedFileTransferTotal;
    }

    public void setNumFailedFileTransferTotal(final double numFailedFileTransferTotal) {
        this.numFailedFileTransferTotal = numFailedFileTransferTotal;
    }

    public double getNumSuccessfulBdrUploadsTotal() {
        return numSuccessfulBdrUploadsTotal;
    }

    public void setNumSuccessfulBdrUploadsTotal(final double numSuccessfulBdrUploadsTotal) {
        this.numSuccessfulBdrUploadsTotal = numSuccessfulBdrUploadsTotal;
    }

    public double getProcessedBdrFileDataVolumeTotal() {
        return processedBdrFileDataVolumeTotal;
    }

    public void setProcessedBdrFileDataVolumeTotal(final double processedBdrFileDataVolumeTotal) {
        this.processedBdrFileDataVolumeTotal = processedBdrFileDataVolumeTotal;
    }

    public double getNumFailedBdrUploadsTotal() {
        return numFailedBdrUploadsTotal;
    }

    public void setNumFailedBdrUploadsTotal(final double numFailedBdrUploadsTotal) {
        this.numFailedBdrUploadsTotal = numFailedBdrUploadsTotal;
    }

    public double getNumOutputKafkaMessagesProducedSuccessfullyTotal() {
        return numOutputKafkaMessagesProducedSuccessfullyTotal;
    }

    public void setNumOutputKafkaMessagesProducedTotal(final double numOutputKafkaMessagesProducedSuccessfullyTotal) {
        this.numOutputKafkaMessagesProducedSuccessfullyTotal = numOutputKafkaMessagesProducedSuccessfullyTotal;
    }

    public double getNumInputKafkaMessagesReplayedTotal() {
        return numInputKafkaMessagesReplayedTotal;
    }

    public double getNumTransactionsRolledbackTotal() {
        return numTransactionsRolledbackTotal;
    }

    public double getNumOutputKafkaMessagesFailedTotal() {
        return numOutputKafkaMessagesFailedTotal;
    }

    @Override
    public String toString() {
        return "MetricsHolder [numInputKafkaMessagesReceivedTotal=" + numInputKafkaMessagesReceivedTotal + ", numSuccessfulFileTransferTotal="
                + numSuccessfulFileTransferTotal + ", processedCounterFileDataVolumeTotal=" + processedCounterFileDataVolumeTotal
                + ", numFailedFileTransferTotal=" + numFailedFileTransferTotal + ", numSuccessfulBdrUploadsTotal=" + numSuccessfulBdrUploadsTotal
                + ", processedBdrFileDataVolumeTotal=" + processedBdrFileDataVolumeTotal + ", numFailedBdrUploadsTotal=" + numFailedBdrUploadsTotal
                + ", numOutputKafkaMessagesProducedSuccessfullyTotal=" + numOutputKafkaMessagesProducedSuccessfullyTotal
                + ", numOutputKafkaMessagesFailedTotal=" + numOutputKafkaMessagesFailedTotal + ", numInputKafkaMessagesReplayedTotal="
                + numInputKafkaMessagesReplayedTotal + ", numTransactionsRolledbackTotal=" + numTransactionsRolledbackTotal + "]";
    }
}
