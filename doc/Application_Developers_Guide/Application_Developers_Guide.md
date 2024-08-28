# SFTP File Transfer Service Application Developer Guide

## Table Of Contents

1. [Introduction](#Introduction)
2. [Prerequisites to run SFTP File Transfer Service](#Prerequisites to run SFTP File Transfer Service)
3. [PreConfiguration of External Dependencies](#PreConfiguration of External Dependencies)
    1. [DMM Data Catalog](#DMM Data Catalog)
    2. [Connected Systems Service](#Connected Systems Service)
4. [PM Counter File Available Notification](#PM Counter File Available Notification)
    1. [PM Counter File Available Notification Headers](#PM Counter File Available Notification Headers)
    2. [PM Counter File Available Notification Trouble Shooting](#PM Counter File Available Notification Trouble Shooting)
5. [PM Counter File Stored on Bulk Data Repository Notification](#PM Counter File Stored on Bulk Data Repository Notification)
    1. [PM Counter File Stored on Bulk Data Repository Notification Headers](#PM Counter File Stored on Bulk Data Repository Notification Headers)
    2. [PM Counter File Stored on Bulk Data Repository Notification Trouble Shooting](#PM Counter File Stored on Bulk Data Repository Notification Trouble Shooting)
6. [Appendix](#Appendix)
7. [References](#References)

## Introduction

This guide is intended for services that want to use the interfaces provided by the SFTP File Transfer Service.

The service provides the following interfaces:

- Input Kafka Topic Notification - PM Counter File Available Notification.
- Output Kafka Topic Notification - PM Counter File Stored in Bulk Data Repository Notification.

The service does not expose any REST API.

## Prerequisites to run SFTP File Transfer Service

The SFTP File Transfer Service has dependency on the following services:

- DMM Message Bus Kafka
- DMM Data-Catalog
- Object Storage MN Service
- Connected Systems Service
- ENM System
- File Notification Service
- Service Mesh

**DMM Message Bus Kafka**

The Input and Output Topics require a Kafka server to host them.

Kafka is part of EIAP DMM – Data Management & Movement - implemented
by [MessageBus KF microservice](#https://adp.ericsson.se/marketplace/message-bus-kf)

**DMM Data-Catalog**

SFTP File Transfer Service requires the kafka and the Object Storage MN connection information.

The data catalog is a repository mapping of data produced by EIAP platform components to data locations, namely

- Bulk Data Repository locations and file notification topics for files
- Message bus KF clusters and topics for streams

**Object Storage MN Service**

SFTP File Transfer Service copies and store the files in a Bulk Data Repository, this is provided by Object Store MN.

The Object Storage MN Service provides persistent object storage with data encryption. The Object Storage MN Service is
based on 3rd Party Minio and delivered as a microservice that can be accessed through Amazon S3-compatible APIs.

The PM Counter Files are transferred over SFTP from the ENM system and stored in the Bulk Data Repository Store,(MinIO)
in Object Storage MN Service.

The PM Counter files are deleted automatically after an expiry of 1 hour.

SFTP File Transfer Service allows a manual created secret name to be specified which is expected to have 4 keys,
where sftp_access_key and sftp_secret_key are used to create a service account for the SFTP File Transfer Service with
read write access on the bucket and, parser_access_key and parser_secret_key for the downstream service ie. pm-counter
parser to read to a read files from bucket. The secret is created only once in a cluster and then reused.

**Connected Systems Service**

The SFTP File Transfer Service requires connection and access details for the ENM system.

The Connected Systems Service allows users to create and configure external systems.

The Connected System Service provides SFTP credentials for the ENM system.

**ENM System**

ENM System from where the files are to be transferred.

**Service Mesh**

Optionally secure communication over mTLS HTTPS can be enabled with other services in the mesh (enabled at global level)
.

## PreConfiguration of External Dependencies

This chapter aims to describe the relation between the SFTP File Transfer Service and entities that need to be
pre-configured in the dependent Services.

The identity of the source of the data, the ENM System must be given when deploying the SFTP File Transfer Service.

The PM Counter Files are transferred and stored in the Bulk Data Repository Store in a MinIO bucket identified by the
ENM System identity, [subsystem_name].

### DMM Data Catalog

The SFTP File Transfer Service requires the following data to be modelled in Data Catalog.

Applications using the SFTP File Transfer Service must model this data in Data Catalog using the DMM Data Catalog Client
API.

| **Entity**                    | **Description**                                                                                                                                                                                    | 
|-------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Data Space                    | A container for domain-related data. The SFTP File Transfer Service handles PM Counter files related to 4G and 5G domains only. It also doesn't differentiate between 4G and 5G Domain.            | 
| Data Provider Type            | A specific type of data source, e.g. ENM.                                                                                                                                                          |
| Data Category                 | DataCategory to which the dataProviderType is associated, e.g. PM_COUNTERS.                                                                                                                        |
| Data Type                     | DataType is a unique combination of data space, data category and data provider type. Datatype can be file based or stream based. It can optionally have a schema name and version                 |
| Data Service Instance         | Represents a single deployment of SFTP File Transfer Service that consumes a specific Datatype                                                                                                     |
| Data Service                  | DataService is a logical grouping of DataServiceInstances which produce the same Datatype                                                                                                          |
| Supported Predicate Parameter | Predicate to restrict the output. SFTP File transfer cannot restrict its output.But it relays the predicate of upstream services.                                                                  |
| File Format                   | Represents file based datatype. A specific file format of a specific Data Provider Type, Data Category and Data space, e.g. XML.                                                                   |
| Message Bus                   | Message bus entity holds details of cluster, namespace, assess end point (bootstrap server).                                                                                                       |
| Bulk Data Repository          | The access end point for the Bulk Data Repository Store, (hostname connection details). A separate Bulk Data Repository must be modelled for the access end point for the Connected SystemService. |
| Notification Topic            | The topic used for  PM Counter File Available notifications                                                                                                                                        |

Each Deployment of SFTP File Transfer Service will register itself as a 'Data Service Instance' with following detail in
data catalog:

| **Entity**                    | **Value**                                                                                                  | **Description**                                                                                                                                            |
|-------------------------------|------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Consumed Datatype             | DataSpace="",DataCategory=PM_COUNTERS,DataProviderType=[enm_id],schemaName=fls                             | The consumed datatype shall match the produced datatype of FNS. FNS produces separate datatypes for each enm.                                              |
| Produced Datatype             | DataSpace="",DataCategory=PM_COUNTERS,DataProviderType=enmFileNotificationService,schemaName=[ran or core] | SFTP can produce two datatypes one for the ran and one for core differentiated through the schemaname. Dataspace is empty since it can be either 5G and 4G |
| Data Service Name             | [produced-schemaName]-pm-counter-sftp-file-transfer                                                        | DataServices producing ran datatypes shall have ran in their name and the ones producing core shall have core in their name                                |
| Data Service Instance Name    | [produced-schemaName]-pm-counter-sftp-file-transfer--[provider of consumed datatype]                       |                                                                                                                                                            |
| Supported Predicate Parameter | nodeName                                                                                                   | FNS supports predicate nodeName                                                                                                                            |
| File Format                   | XML.                                                                                                       |                                                                                                                                                            |
| Notification Topic            | [producedschemaname]-pm-counter-sftp-file-transfer                                                         | Same as Data Service Name                                                                                                                                  |

**Note:** The consumed and produced datatypes,supported predicate, file type, data service name and data service
instance name suffixes are configurable

**Note:** Since the dataservice name is used as bucket name , it needs to be S3 compliant.

### Connected Systems Service

The SFTP credentials for the ENM System, i.e. scriptingVMs, must be configured in the Connected Systems Service using
the Connected Systems API.

Up to 3 scripting VMs can be configured for an ENM system.

The ENM system is modelled under uri, subsystem-manager/v1/subsystems with the following parameters:

| **Entity**           | **Description**                                                                                                                             |
|----------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| name                 | This is the source of the files being transferred and corresponds to the subsystem_name used when deploying the SFTP File Transfer Service. |
| connectionProperties | This is used to hold SFTP credentials and access details for the ENM System.                                                                |

Example of configuration in Connected Systems for an ENM System 'enm1':

```json
{
  "id": "1",
  "subsystemTypeId": "1",
  "name": "enm1",
  "url": "https://test.subsystem-1/",
  "operationalState": "REACHABLE",
  "connectionProperties": [
    {
      "id": 7,
      "subsystemId": 1,
      "name": "connection7",
      "username": "ecmadmin",
      "password": "CloudAdmin123",
      "scriptingVMs": "scp-1-scripting,scp-2-scripting,scp-3-scripting",
      "sftpPort": "22",
      "encryptedKeys": [
        "password"
      ],
      "subsystemUsers": []
    }
  ],
  "vendor": "subsystem-vendor",
  "subsystemType": {
    "id": 1,
    "type": "DomainManager"
  }
}
```

## PM Counter File Available Notification

The PM Counter File Available notification uses the Kafka protocol.

The File Notification Service sends the PM Counter File Available Notification in JSON format to the "
file-notification-service--sftp-filetrans--[enm_id]" topic.

The SFTP File Transfer Service consumes the PM Counter File Available Notifications, transfers the PM Counter file from
the location specified in the notification and stores the PM Counter File in Bulk Data Repository.

### PM Counter File Available Notification

The PM Counter File Available notification must be compliant to the following schema. If not compliant the file transfer
will fail and an error will be logged.

Kafka producer parameters:

- **nodeName:** The name of the network element on the ENM System from where the file is collected.
- **nodeType:** The node type, e.g. RadioNode.
- **dataType:** The type of data e.g. PM_STATISTICAL.
- **fileLocation:**  The location of the file on the ENM System.
- **fileType:** The format of the file, e.g. XML.

Example PM Counter File Available Notification:

```json 
{
 "nodeName": "SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE03dg2ERBS00011",
 "dataType": "PM_STATISTICAL",
 "nodeType": "RadioNode",
 "fileLocation": "/ericsson/pmic1/XML/SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE03dg2ERBS00011/A20220406.1330+01001345+0100_SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE03dg2ERBS00011_statsfile.xml.gz"
 "fileType": "XML"
}
```

#### PM Counter File Available Notification Trouble Shooting

Errors are logged in the SFTP File Transfer Service log.

Types of errors that can occur:

- Unable to connect to ENM system
- File location incorrect
- File no longer available at specified location

## PM Counter File Stored on Bulk Data Repository Notification

The PM Counter File Stored on Bulk Data Repository Store notification uses the Kafka protocol.

The consumer of the PM Counter File Stored on Bulk Data Repository store notification should use the Kafka Consumer API
to receive the notification in JSON format from the topic which is the same as dataservice name.

The Kafka Consumer that reads from this topic must be configured to have its 'isolation.level' property set to '
read_committed'. This is to avoid the Kafka consumer of this topic reading uncommitted notifications.

The PM Counter File stored on Bulk Data Repository Store notification is sent when the file received in the PM Counter
File Available Notification has been successfully transferred from the ENM system and stored in Bulk Data Repository.

If any error occurs when uploading the file to Bulk Data Repository, the error is logged in the SFTP File Transfer
Service log and the notification is not sent.

### PM Counter File Stored on Bulk Data Repository Notification Headers

Kafka consumer parameters:

- **nodeName:** The name of the network element from where the file was collected.
- **fileLocation:** The location of the file on the Bulk Data Repository Store.
- **subSystemType:** The sub system type.
- **nodeType:** The node type, e.g. RadioNode.
- **dataType:** The type of data e.g. PM_STATISTICAL.
- **fileType:** The format of the file, e.g. XML.

Example of PM Counter File Stored on Bulk Data Repository Store Notification:

Headers:

```json
{
  "nodeType": "RadioNode",
  "nodeName": "SubNetwork=ONRM_ROOT_MO_R,SubNetwork=RAN,SubNetwork=QC,SubNetwork=SD,MeContext=eNB3330_statsfi86",
  "dataType": "PM_STATISTICAL",
  "subSystemType": "pENM",
  "fileType": "XML"
}
```

Payload:

```json
{
  "fileLocation": "enm1/ericsson/pmic2/xml/SubNetwork=ONRM_ROOT_MO_R,SubNetwork=RAN,SubNetwork=QC,SubNetwork=SD,MeContext=eNB3330_statsfi86/A20220504.1600-1615_SubNetwork=ONRM_ROOT_MO_R,SubNetwork=RAN,SubNetwork=QC,SubNetwork=SD,MeContext=eNB3330_statsfi86.xml"
}
```

#### PM Counter File Stored on Bulk Data Repository Notification Trouble Shooting

Any errors encountered on storing the PM Counter Files in Bulk Data Repository are logged to the SFTP File Transfer
Service log.

## Appendix

## References

[SFTP File Transfer Service User Guide][sftp]

[Service Mesh] [sm]

[Message Bus KF][mbkf]

[Data Catalog][dc]

[Connected Systems][cs]

[Object Store MN][osmn]

[ENM File Notification Service][efns]

[Kafka Configuration][kfc]


[sftp]: <https://adp.ericsson.se/marketplace/sftp-file-transfer/documentation/development/dpi/service-user-guide>

[sm]: <https://adp.ericsson.se/marketplace/servicemesh-controller>

[mbkf]: <https://adp.ericsson.se/marketplace/message-bus-kf>

[dc]: <https://adp.ericsson.se/marketplace/data-catalog>

[cs]: <https://adp.ericsson.se/marketplace/connected-systems>

[osmn]: <https://adp.ericsson.se/marketplace/object-storage-mn>

[efns]: <https://adp.ericsson.se/marketplace/enm-file-notification>

[kfc]: <https://kafka.apache.org/documentation/#configuration>
