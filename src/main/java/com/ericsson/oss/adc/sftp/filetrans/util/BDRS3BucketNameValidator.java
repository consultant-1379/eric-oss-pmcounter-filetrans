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

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Validate the bucket name according to the Amazon S3 Standards and manipulating it to adhere to it.
 * https://docs.aws.amazon.com/AmazonS3/latest/userguide//bucketnamingrules.html
 */
@Component
public class BDRS3BucketNameValidator {

    private static final String IP_ADDRESS_PATTERN = "(\\d+\\.){3}\\d+";

    private static final String ALLOWED_CHARACTERS_PATTERN = "[a-z0-9-.]";

    private static final String START_WITH_DIGIT_LETTER_PATTERN = "[a-z0-9].*";

    private static final String END_WITH_DIGIT_LETTER_PATTERN = ".*[a-z0-9]$";

    private static final String INVALID_PREFIX = "xn--";

    private static final String INVALID_SUFFIX = "-s3alias";

    private static final String NAME_FOR_BELOW_MINIMUM_LENGTH = "-bucket";

    private static final String CONSECUTIVE_DOTS = "..";

    private static final int MINIMUM_S3_BUCKET_NAME_LENGTH = 3;

    private static final int MAXIMUM_S3_BUCKET_NAME_LENGTH = 63;

    private static final int MAXIMUM_RANDOM_DIGIT = 9;

    private static final int MINIMUM_RANDOM_DIGIT = 0;

    private static final String DOT = ".";

    private static final String HYPHEN = "-";

    public String validateUnderS3Standards(final String bucketName) {
        final Random random = new Random();

        final String prefixSuffixChecked = removePrefixAndSuffixIfInvalid(bucketName);
        final String lengthChecked = lengthFollowingS3Standards(prefixSuffixChecked,random);
        final String unwantedCharactersChecked = removeUnwantedCharacters(lengthChecked);
        final String consecutiveDotsChecked = removeConsecutiveDots(unwantedCharactersChecked);
        final String lengthCheckAgain = lengthFollowingS3Standards(consecutiveDotsChecked, random);
        return checkIfIPAddress(lengthCheckAgain);
    }

    public final String lengthFollowingS3Standards(String bucketName, final Random random) {
        if (bucketName.length() < MINIMUM_S3_BUCKET_NAME_LENGTH) {
            bucketName = bucketName + NAME_FOR_BELOW_MINIMUM_LENGTH;
        }

        else if (bucketName.length() > MAXIMUM_S3_BUCKET_NAME_LENGTH) {
            bucketName = bucketName.substring(0, MAXIMUM_S3_BUCKET_NAME_LENGTH);
            final String alteredName = startAndEndWithLetterOrLowerCaseAlphabet(bucketName, random);

            if (alteredName.length() > MAXIMUM_S3_BUCKET_NAME_LENGTH) {
                return alteredName.substring(0, MAXIMUM_S3_BUCKET_NAME_LENGTH - 1) + alteredName.charAt(MAXIMUM_S3_BUCKET_NAME_LENGTH);
            }
        }
        return startAndEndWithLetterOrLowerCaseAlphabet(bucketName, random);
    }

    public final String startAndEndWithLetterOrLowerCaseAlphabet(String bucketName, final Random random) {
        bucketName = bucketName.toLowerCase(Locale.ENGLISH);

        final int randomNumber = random.nextInt((MAXIMUM_RANDOM_DIGIT - MINIMUM_RANDOM_DIGIT) + 1) + MINIMUM_RANDOM_DIGIT;

        if (!Pattern.matches(START_WITH_DIGIT_LETTER_PATTERN, bucketName)) {
            bucketName = randomNumber + bucketName;
        }

        if (!Pattern.matches(END_WITH_DIGIT_LETTER_PATTERN, bucketName)) {
            bucketName = bucketName + randomNumber;
        }
        return bucketName;
    }

    public final String removeUnwantedCharacters(String bucketName) {
        for (final String eachChar :bucketName.split("")) {
            if (!Pattern.matches(ALLOWED_CHARACTERS_PATTERN, eachChar)) {
                bucketName = bucketName.replace(eachChar, HYPHEN);
            }
        }
        return bucketName;
    }

    public final String removeConsecutiveDots(String bucketName) {
        while (bucketName.contains(CONSECUTIVE_DOTS)) {
            final int indexOfDot = bucketName.indexOf(CONSECUTIVE_DOTS);
            bucketName = bucketName.substring(0, indexOfDot) + bucketName.substring(indexOfDot + 1);
        }
        return bucketName;
    }

    public final String removePrefixAndSuffixIfInvalid(String bucketName) {
        if (bucketName.startsWith(INVALID_PREFIX)) {
            bucketName = bucketName.replace(INVALID_PREFIX, "");
        }

        if (bucketName.endsWith(INVALID_SUFFIX)) {
            bucketName = bucketName.replace(INVALID_SUFFIX, "");
        }
        return bucketName;
    }

    public final String checkIfIPAddress(final String bucketName) {
        if (Pattern.matches(IP_ADDRESS_PATTERN, bucketName)) {
            return bucketName.replace(DOT, HYPHEN);
        }
        return bucketName;
    }
}
