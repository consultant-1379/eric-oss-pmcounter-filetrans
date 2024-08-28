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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

import com.ericsson.oss.adc.sftp.filetrans.controller.InputTopicListener;
import com.ericsson.oss.adc.sftp.filetrans.model.ConnectionPropertiesModel;
import com.ericsson.oss.adc.sftp.filetrans.model.InputMessage;
import com.ericsson.oss.adc.sftp.filetrans.model.SubsystemUsersModel;
import com.github.stefanbirkner.fakesftpserver.lambda.FakeSftpServer;
import com.google.gson.Gson;

/**
 * The Class TestUtils.
 */
public class TestUtils {

    private static final String NO_KEY = "";
    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);
    private static final String NODE_NAME = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR102gNodeBRadio00010,ManagedElement=NR102gNodeBRadio00010";
    private static final String ENM_FILE_PATH = "/ericsson/pmic1/XML/SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR102gNodeBRadio00010,ManagedElement=NR102gNodeBRadio00010/";
    private static final String ENM_BAD_FILE_PATH = "/ericsson/bad/pmic1/XML/SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR102gNodeBRadio00010,ManagedElement=NR102gNodeBRadio00010/";
    private static final String FILE_NAME_TEMPLATE = "A20200721.1000+0100-1015+0100_SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR102gNodeBRadio00010,ManagedElement=NR102gNodeBRadio00010_statsfile.%s.xml.gz";
    private static final String NODE_TYPE = "RadioNode";
    private static final String DATA_TYPE = "4G";
    private static final String FILE_TYPE = "XML";
    private static int partition = 0;
    private static long offset;

    private static Gson gson = new Gson();

    @Value("${spring.kafka.topics.input.name}")
    private static String topicName;

    @Value("${spring.kafka.topics.enm_id}")
    private static String enmID;

    /**
     * Put filenames on input topic.
     *
     * @param server
     *            the server
     * @param inputTopicListener
     *            the input topic listener
     * @param numFilesToDownload
     *            the num files to download
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static final void putFilenamesOnInputTopic(final FakeSftpServer server, final InputTopicListener inputTopicListener,
                                                      final int numFilesToDownload)
                                                              throws IOException {
        final List<String> listFilenames = TestUtils.getFiles(ENM_FILE_PATH, numFilesToDownload);
        TestUtils.putFiles(server, listFilenames);
        final List<String> inputJsonListFiles = getInputJsonListFiles(listFilenames);
        final List<ConsumerRecord<String, String>> consumerRecords = createConsumerRecords(inputJsonListFiles);
        inputTopicListener.listen(consumerRecords);
    }

    /**
     * Put bad filenames on input topic.
     *
     * @param server
     *            the server
     * @param inputTopicListener
     *            the input topic listener
     * @param numFilesToDownload
     *            the num files to download
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static final void putBadFilenamesOnInputTopic(final FakeSftpServer server, final InputTopicListener inputTopicListener,
                                                         final int numFilesToDownload)
                                                                 throws IOException {
        final List<String> listFilenames = TestUtils.getFiles(ENM_FILE_PATH, numFilesToDownload);
        final List<String> listBadFilenames = TestUtils.getFiles(ENM_BAD_FILE_PATH, numFilesToDownload);
        TestUtils.putFiles(server, listFilenames);
        final List<String> inputJsonListFiles = getInputJsonListFiles(listBadFilenames);
        final List<ConsumerRecord<String, String>> consumerRecords = createConsumerRecords(inputJsonListFiles);
        inputTopicListener.listen(consumerRecords);
    }

    /**
     * Put filenames on input topic one null.
     *
     * @param server
     *            the server
     * @param inputTopicListener
     *            the input topic listener
     * @param numFilesToDownload
     *            the num files to download
     * @param nullIndex
     *            the null index
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static final void putFilenamesOnInputTopicOneNull(final FakeSftpServer server, final InputTopicListener inputTopicListener,
                                                             final int numFilesToDownload, final int nullIndex)
                                                                     throws IOException {
        final List<String> listFilenames = TestUtils.getFiles(ENM_FILE_PATH, numFilesToDownload);
        final List<String> listBadFilenames = TestUtils.getFiles(ENM_BAD_FILE_PATH, numFilesToDownload);
        TestUtils.putFiles(server, listFilenames);
        final List<String> inputJsonListFiles = getInputJsonListFiles(listBadFilenames);
        final List<ConsumerRecord<String, String>> consumerRecords = createConsumerRecords(inputJsonListFiles); //createConsumerRecordsOneNull(inputJsonListFiles, nullIndex);
        inputTopicListener.listen(consumerRecords);
    }

    /**
     * Put bad filenames on server.
     *
     * @param server
     *            the server
     * @param inputTopicListener
     *            the input topic listener
     * @param numFilesToDownload
     *            the num files to download
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static final void putBadFilenamesOnServer(final FakeSftpServer server, final InputTopicListener inputTopicListener,
                                                     final int numFilesToDownload)
                                                             throws IOException {
        final List<String> listFilenames = TestUtils.getFiles(ENM_FILE_PATH, numFilesToDownload);
        final List<String> listBadFilenames = TestUtils.getFiles(ENM_BAD_FILE_PATH, numFilesToDownload);
        TestUtils.putFiles(server, listBadFilenames);
        final List<String> inputJsonListFiles = getInputJsonListFiles(listFilenames);
        final List<ConsumerRecord<String, String>> consumerRecords = createConsumerRecords(inputJsonListFiles);
        inputTopicListener.listen(consumerRecords);
    }

    /**
     * Put filenames on input topic.
     *
     * @param server
     *            the server
     * @param kafkaTemplate
     *            the kafka template
     * @param topicName
     *            the topic name
     * @param numFilesToDownload
     *            the num files to download
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static final void putFilenamesOnInputTopic(final FakeSftpServer server, final KafkaTemplate<String, String> kafkaTemplate,
                                                      final String topicName, final int numFilesToDownload)
                                                              throws IOException {
        final List<String> listFilenames = TestUtils.getFiles(ENM_FILE_PATH, numFilesToDownload);
        TestUtils.putFiles(server, listFilenames);
        final List<String> inputJsonListFiles = getInputJsonListFiles(listFilenames);
        for (final String filename : inputJsonListFiles) {
            kafkaTemplate.send(topicName, filename);
        }
    }

    /**
     * Gets the input json list files one bad.
     *
     * @param filenames
     *            the filenames
     * @param badFileNo
     *            the bad file no
     *
     * @return the input json list files one bad
     */
    public static final List<String> getInputJsonListFilesOneBad(final List<String> filenames, final int badFileNo) {
        final List<String> inputListenerFileList = new ArrayList<>();
        int index = 0;
        for (final String filename : filenames) {
            final String inputStr;
            if (index == badFileNo) {
                inputStr = gson.toJson(InputMessage.builder()
                        .nodeName(null)
                        .fileLocation(filename)
                        .nodeType(NODE_TYPE)
                        .dataType(DATA_TYPE)
                        .fileType(FILE_TYPE)
                        .build());
            } else {
                inputStr = gson.toJson(InputMessage.builder()
                        .nodeName(NODE_NAME)
                        .fileLocation(filename)
                        .nodeType(NODE_TYPE)
                        .dataType(DATA_TYPE)
                        .fileType(FILE_TYPE)
                        .build());
            }
            LOGGER.info("Input Json List Files, {} Bad, Current file {}/{} : {} ", badFileNo, index, filenames.size() - 1, inputStr);
            inputListenerFileList.add(inputStr);
            index++;
        }
        LOGGER.info("Input Json ListFiles:  {} ", inputListenerFileList);
        return inputListenerFileList;

    }

    /**
     * Gets the input json list files.
     *
     * @param filenames
     *            the filenames
     *
     * @return the input json list files
     */
    public static final List<String> getInputJsonListFiles(final List<String> filenames) {
        final List<String> inputListenerFileList = new ArrayList<>(filenames.size());
        for (final String filename : filenames) {
            final String inputStr = gson.toJson(InputMessage.builder()
                    .nodeName(NODE_NAME)
                    .fileLocation(filename)
                    .nodeType(NODE_TYPE)
                    .dataType(DATA_TYPE)
                    .fileType(FILE_TYPE)
                    .build());
            LOGGER.info("Input Json List Files:  {} ", inputStr);
            inputListenerFileList.add(inputStr);
        }
        LOGGER.info("Input Json ListFiles:  {} ", inputListenerFileList);
        return inputListenerFileList;
    }

    /**
     * Put files.
     *
     * @param server
     *            the server
     * @param filenames
     *            the filenames
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static final void putFiles(final FakeSftpServer server, final List<String> filenames) throws IOException {
        final int index = 1;
        for (final String filename : filenames) {
            server.putFile(filename, "Content of file " + index, UTF_8);
        }
        LOGGER.info("Put {} Files:  {} ", filenames.size(), filenames);
    }

    /**
     * Gets the files.
     *
     * @param thePath
     *            the path
     * @param count
     *            the count
     *
     * @return the files
     */
    public static final List<String> getFiles(final String thePath, final int count) {
        final List<String> filenameList = new ArrayList<>();
        IntStream.range(1, count + 1).forEachOrdered(n -> {
            filenameList.add(thePath + String.format(FILE_NAME_TEMPLATE, Integer.toString(n)));
        });
        LOGGER.info("Prepared Files:  {} ", filenameList);
        return filenameList;
    }

    /**
     * Used to return a sample populated ConnectionPropertiesDTO
     *
     * Example ConnectionPropertiesDTO
     *
     * ConnectionProperties ConnectionPropertiesDTO [id=5, subsystemId=2, name=localhost, tenant=tenant1, username=user, password=password,
     * scriptingVMs=localhost,localhost,localhost, sftpPort=1234, encryptedKeys=null,
     * subsystemUsers=[com.ericsson.oss.adc.sftp.filetrans.model.SubsystemUsersDTO@1c31408f,
     * com.ericsson.oss.adc.sftp.filetrans.model.SubsystemUsersDTO@67b5a089, com.ericsson.oss.adc.sftp.filetrans.model.SubsystemUsersDTO@1bdf24c0]]
     */
    public static ConnectionPropertiesModel getConnectedSystemsDto() {
        final ConnectionPropertiesModel connectionPropertiesModel = ConnectionPropertiesModel.builder()
                .id(5L)
                .name("localHost")
                .subsystemId(2L)
                .tenant("tenant1")
                .username("user")
                .password("password")
                .sftpPort("1234")
                .scriptingVMs("localhost,localhost,localhost")
                .encryptedKeys(null)
                .build();

        final long[] ids = new long[] { 4L, 15L, 2L };
        final long[] propIds = new long[] { 3L, 3L, 3L };
        final List<SubsystemUsersModel> subsystemUsers = new ArrayList<>();
        final SubsystemUsersModel subsystemUsersModel = SubsystemUsersModel.builder().build();
        for (int i = 0; i < ids.length; i++) {
            subsystemUsersModel.setId(ids[i]);
            subsystemUsersModel.setConnectionPropsId(propIds[i]);
            subsystemUsers.add(subsystemUsersModel);
        }

        connectionPropertiesModel.setSubsystemUsers(subsystemUsers);
        return connectionPropertiesModel;
    }

    public static String addTrailingSlash(String uri) {
        if (!uri.endsWith("/")) { // if not ending / append one
            uri = uri + "/";
        }
        return uri;
    }

    private static List<ConsumerRecord<String, String>> createConsumerRecords(final List<String> values) {
        final List<ConsumerRecord<String, String>> consumerRecords = new ArrayList<>();
        for (final String value : values) {
            consumerRecords.add(createConsumerRecord(NO_KEY, value, offset++));
        }
        return consumerRecords;
    }

    private static ConsumerRecord<String, String> createConsumerRecord(final String key, final String value, final long offset) {
        return new ConsumerRecord<>(topicName + enmID, partition, offset, key, value);
    }
}
