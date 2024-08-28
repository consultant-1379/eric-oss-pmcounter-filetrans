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
package com.ericsson.oss.adc.sftp.filetrans.availability;

public class RetryForeverHandler {
    protected int attemptNo;
    protected boolean retryForever = true;
    protected int retryForeverAttemptNo;

    public int getAttemptNo() {
        return attemptNo;
    }

    public int getRetryForeverAttemptNo() {
        return retryForeverAttemptNo;
    }

    public void setRetryForever(final boolean retryForever) {
        this.retryForever = retryForever;
    }

}
