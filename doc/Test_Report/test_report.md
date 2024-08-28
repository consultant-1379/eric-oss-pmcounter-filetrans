<!--Document Template information:
Prepared:***
Approved:***
Document Name:test-report
Document Number:152 83-APR 201 535/1-n
-->

# SFTP File Transfer Test Report

---

## Abstract

This document lists the results for tests run against this service manually and on the CI pipeline.

The API spec, Application developer guide and Service user guide can be found in the [SFTP File Transfer API Documentation](https://adp.ericsson.se/marketplace/sftp-file-transfer/documentation/development/dpi/service-user-guide).

The report follows the tests outlined in the Test Specification.

---

## Unit Tests

These tests are run on the CI pipeline for this microservice. There are two pipelines and both of them run these tests
- [PreCodeReview (PCR)](https://fem1s11-eiffel216.eiffel.gic.ericsson.se:8443/jenkins/job/eric-oss-pmcounter-filetrans_PreCodeReview/) 
- [Publish](https://fem1s11-eiffel216.eiffel.gic.ericsson.se:8443/jenkins/job/eric-oss-pmcounter-filetrans_Publish/)

#### Test Environment

More information [here](https://fem1s11-eiffel216.eiffel.gic.ericsson.se:8443/jenkins/)

|       Name       | Value                                   |
|:----------------:|-----------------------------------------|
|     Hardware     | Jenkins Worker Node (fem1s11-eiffel216) |
| Operating System | SUSE Linux Enterprise Server 15 SP3.    |

#### Test Object(s)

|         Service          |  Version  |
|:------------------------:|:---------:|
| eric-oss-sftp-filetrans  | 1.101.0-1 |

#### Test Execution

- Triggered at every commit (and its patchsets) - PCR
- Triggered after merging code into master - Publish
  

#### Test Results

|         Test Case         | Test Case Stage | Test Result | Comment                                                                                                                 |
|:-------------------------:|:---------------:|:-----------:|-------------------------------------------------------------------------------------------------------------------------|
| Java JUnit and Stub tests |      Test       |   266/266   | The JUnit tests verify class behaviour and Stubs test expected integration based on stubs provided by dependent service |
|       Docker Build        |      Image      |     1/1     | Tests if the docker image can be built                                                                                  |
|         Helm Lint         |      Lint       |     1/1     | Tests if the chart follows best practices based on the Helm DR checker                                                  |
|       Helm Package        |     Package     |     1/1     | Tests if the chart can be packaged into a .tgz file                                                                     |

---

## Application Staging Tests

#### Test Environment

More information [here](https://ews.rnd.gic.ericsson.se/cd.php?cluster=hart107)

|    Name     |              Value              |
|:-----------:|:-------------------------------:|
|  Hardware   |        OSS BCSS Test Env        |
|  Software   |   1.24.2-kaas.1 [ccd:2.23.0]    |
| K8S Version |             v1.24.2             |
| K8S Cluster | 1 Master Node + 15 Worker Nodes |
|   Capacity  |    16 vCPUs per Worker node     |


#### Test Object(s)

|      Service       |           Version            |
|:------------------:|:----------------------------:|
| eric-eiae-helmfile | eric-eiae-helmfile-2.19.0-88 |

#### Test Execution

- Triggered after a successful Publish job run
  

#### Test Results

|                                                                Test Case                                                                 | Test Result | 
|:----------------------------------------------------------------------------------------------------------------------------------------:|:-----------:|
| Verify the SFTP File Transfer service receives 10K notifications and successfully downloads and transfers all files to BDR object Store  |     1/1     | 
|                               Verify the SFTP File Transfer service instance registration in Data Catalog.                               |     1/1     | 
| Verify the SFTP File Transfer service instance registration in Data Catalog for multiple instances of the same datatype and schema-name. |     1/1     |
|  Verify end-to-end functionality from ENM File Notification Service to 3GPP PM XML RAN parser flow when Servicemesh and TLS is enabled.  |     1/1     | 
|        Verify end-to-end functionality from ENM File Notification Service to 3GPP PM XML RAN parser flow when TLS is not enabled.        |     1/1     |
---

## Product Staging Tests

Verification of end-to-end flow using real ENM.

#### Test Environment


#### Test Object(s)

|      Service       |           Version            |
|:------------------:|:----------------------------:|
| eric-eiae-helmfile | eric-eiae-helmfile-2.19.0-88 |

#### Test Execution

- Triggered after a successful Application Staging run.
  

#### Test Results

|    Test Case    | Test Result | Comment              |
|:---------------:|:-----------:|----------------------|
| Test to verify that EIC is processing PM stats data [PE-006] |     1/1     |  |    

---

## Performance Tests

Verified at Application Staging & Product Staging level.
Application Staging verified with 10K files within 9 mins using stubbed ENM.
Product Staging verification of 10K files witin 9 mins using stubbed and real ENM.

<!-- #### Test Environment

| Name        | Value   |
| :---------: | :-----: |
| Hardware    |         |
| Software    |         |
| K8S Version |         |
| K8S Cluster |         |
| Capacity    |         |

#### Test Object(s)

| Service | Version |
| :-----: | :-----: |
| eric-oss-sftp-filetrans |  |


#### Test Execution

- 

#### Test Results -->

---

## Deployment Tests
Verified in Application Staging and also on pooled environment.

#### Test Environment
Pooled environment.

#### Test Object(s)

|         Service         |  Version  |
|:-----------------------:|:---------:|
| eric-oss-sftp-filetrans | 1.101.0-1 |

#### Test Execution

- Automated in the App staging pipeline.
- Manual - Uninstall of the service removes data registered in Data-Catalog.

#### Test Results

|                                     Test Case                                     |                                                                                                                                                                                                                                                                                     Test Result                                                                                                                                                                                                                                                                                     | 
|:---------------------------------------------------------------------------------:|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
|                                   Instantiation                                   | Covered by the pipelines. See dashboard https://cicd-ng.web.adp.gic.ericsson.se/view/6/dashboard/32 .<br/> Initial service installation and upgrade are verified with all mandatory parameters specified, the service does not have optional parameters.<br/> Deployment with dependent services available is verified in [App-Staging. adc-e2e-cicd · Pipeline Executions · Execution Details: (ericsson.se)](https://spinnaker.rnd.gic.ericsson.se/#/applications/adc-e2e-cicd/executions/?stage=0&step=0)<br/> Dependent Services will not be available in micro-service CI pipeline. |
|                       Scale-out (increase no of instances)	                       |                                                                                                                                                                                                                                                                   Covered by the pipelines.                                                                                                                                                                                                                                                                  |
|  Successful Uninstall - The service instance is de-registered in Data Catalog  	  |                                                                                                                                                                                                                                                                                       SUCCESS                                                                                                                                                                                                                                                                                       |
| Uninstall fails when errors are encountered on de-registration from Data Catalog	 |                                                                                                                                                                                                                                                                                       SUCCESS                                                                                                                                                                                                                                                                                       |
|           Service can be uninstalled using helm parameter '--no-hooks'	           |                                                                                                                                                                                                                                                                                       SUCCESS                                                                                                                                                                                                                                                                                       |

---

## Upgrade Tests

#### Test Environment

Verified in the pipelines.

#### Test Object(s)

|               Service                |  Version  |
|:------------------------------------:|:---------:|
|       eric-oss-sftp-filetrans        | 1.101.0-1 |

#### Test Execution

- Manual on pooled environment
- Automated in the App staging/Product Staging pipelines.

#### Test Results

|              Test Case              | Test Result | Comment |
|:-----------------------------------:|:-----------:|---------|
|               Upgrade               |   SUCCESS   |         |
| Multiple Upgrades to latest version |   SUCCESS   |         |
| In Service Software Upgrade (ISSU)  |   SUCCESS   |         |
|         Downgrade/Rollback          |   SUCCESS   |         |
|          Long Jump Upgrade          |   SUCCESS   |         |

---
## Robustness Tests

#### Test Environment

Verified on pooled environment.

#### Test Object(s)

|         Service         |  Version  |
|:-----------------------:|:---------:|
| eric-oss-sftp-filetrans | 1.101.0-1 |


#### Test Execution

- Manual

#### Test Results

|             Test Case              | Test Result | Comment                                                                                                                                                       |
|:----------------------------------:|:-----------:|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
|    Robustness, Service Restart     |   SUCCESS   |                                                                                                                                                               |
| Liveness and Readiness probes test |   SUCCESS   | The liveness and readiness probes are unit tested. And there is also a test in the CI that deploys the service and tests that it comes up into a ready state. |
|    SIGTERM and SIGKILL handling    |     N/A     | Microservice is stateless                                                                                                                                     |
|        Move between workers        |     N/A     | Microservice is stateless                                                                                                                                     |


---

## Characteristics Tests

#### Test Environment

Verified in the CI pipelines.

#### Test Object(s)

|         Service         |  Version  |
|:-----------------------:|:---------:|
| eric-oss-sftp-filetrans | 1.101.0-1 |

#### Test Execution

- Manual
- Automated

#### Test Results

|                                  Test Case                                   |                     Test Result                      | Comment                                  |
|:----------------------------------------------------------------------------:|:----------------------------------------------------:|------------------------------------------|
|                        Startup time (to fully ready)                         |                   Default (3m 14s)                   |                                          |
|                        Restart time (to fully ready)                         |                   Default (3m 14s)                   |                                          |
|                        Upgrade time (to fully ready)                         |                        3m 14s                        |                                          |
|                       Downgrade time (to fully ready)                        |                         N/A                          |                                          |
|             Loss of service duration (during upgrade/downgrade)              |                          0s                          | No data is lost during upgrade/rollback. |
|                                  Image size                                  |                   Default (427 MB)                   |                                          |
|                    Microservice memory footprint required                    |           468MB no load / 473MB under load           |                                          |
|                     Microservice CPU footprint required                      | Processing 1400 files. Max is 41mCPU. Idle is 12mCPU |                                          |
|         Some kind of meaningful latency or throughput for your “API”         |                          -                           |                                          |

---
