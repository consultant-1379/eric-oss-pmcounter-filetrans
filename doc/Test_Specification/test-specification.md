<!--Document Template information:
Prepared:***
Approved:***
Document Name:test-specification
Document Number:n/152 41-APR 201 xxx/1
-->

**SFTP File Transfer Test Specification**

**Abstract**

This document describes the test cases used to verify the SFTP File Transfer service:

- Source code
- Dockerfile
- Helm chart
- Integration with ENM File Notification Service, DMM Data Catalog, Connected Systems, DMM KF Operator SZ, Object Store MN Service and Ericsson Network Manager (ENM).

# 1. Revision Information

| **Revision** | **Date**   | **Description**                                                | **Prepared By** |
|--------|------------|----------------------------------------------------------------|-----------------|
| A      | 2022-09-12 | First Revision                                                 | Team Phoenix    |
| B      | 2023-02-01 | Updated to include Data registration for DC-C tests            | Team Phoenix    |
| C      | 2023-04-04 | Test to verify Secure Access to Object Store MN Service added. | Team Phoenix    |

# 2. Introduction

This document contains the Test Specification for the SFTP File Transfer service.

# 3. Functional Test

## 3.1 Junit

Verify the functionality of individual classes within the SFTP File Transfer service.

Verify the collective functionality of the integrated classes.

| **Use Case**                                                                                               |
|------------------------------------------------------------------------------------------------------------|
| SFTP Transfer of LTE and NR PM Counter Files (Compressed/Uncompressed) and Storage in Bulk Data Repository |

## 3.2 Contract Test

Verify the integration of the SFTP File Transfer service with the following dependent services:

- Data Catalog
- Connected Systems

# 4. Compliance Test

## 4.1 Lint

Perform lint scan of the SFTP File Transfer service and verify it complies with enforced design rules.

## 4.2 SonarQube

Perform SonarQube scan and gating of the SFTP File Transfer service and verify it complies with the coding guidelines and best practices.

# 5. System Test

## 5.1 Application Staging

- Verify the SFTP File Transfer service receives 10K notifications and successfully downloads and transfers all files to BDR object Store.
- Verify the SFTP File Transfer service instance registration in Data Catalog.
- Verify the SFTP File Transfer service instance registration in Data Catalog for multiple instances of the same datatype and schema-name.
- Verify end-to-end functionality from ENM File Notification Service to 3GPP PM XML RAN parser
  flow when Servicemesh and TLS is enabled.

## 5.2 Product Staging

No specific product staging tests for this service. 
The service is verified as part of the end-to-end flow from ENM File Notification Service to 3GPP PM XML RAN parser using real ENM.

# 6. Deployment Test

## 6.1 Initial Install

Automated initial install test:

- Verify the SFTP File Transfer service is successfully installed on the microservice CI with mandatory parameters.
- Verify the SFTP File Transfer is successfully installed on the Application Staging with mandatory parameters.

Manual initial install test:

- Verify the SFTP File Transfer service is successfully installed with optional parameters

## 6.2 Upgrade

Automated upgrade test:

- Verify the SFTP File Transfer service is successfully upgraded in Application Staging environment.

For manual upgrade test, the following paths are verified:

- Single upgrade to latest version
- Multiple upgrades to latest version
- Long Jump Upgrade
- In Service Software Upgrade (perform upgrade with traffic to verify service availability and ensure the ISSU expectations are met).

## 6.3 Rollback

Manual rollback test:

- Verify rollback of the SFTP File Transfer service to previous version.

## 6.3 Uninstall

Manual uninstall tests:

- Verify uninstall of the SFTP File Transfer service is successful and the service instance is de-registered in Data Catalog.
- Verify uninstall of the SFTP File Transfer service fails when errors are encountered on de-registration from Data Catalog.
- Verify that it is possible to successfully uninstall the SFTP File Transfer service using helm parameter '--no-hooks'

# 7. Robustness Test

- Verify the SFTP File Transfer service does not start processing files until all dependent services are available.
- Verify the SFTP File Transfer service does not start processing files if any of the dependent services are un-available/not installed. Service resumes when service becomes available, no files are lost.
- Verify the SFTP File Transfer service resumes processing files following a restart and that no files are lost
- Verify the SFTP File Transfer service comes back up seamlessly when the service and its dependent services are restarted simultaneously, no files are lost.
- Verify the SFTP File Transfer service handles scenario where the service is restarted during processing and dependent services are not immediately available.
- Verify the SFTP File Transfer service handles the scenario where data is mis-configured in dependent services. Service resumes once configuration is corrected, no files are lost.

# 8. Performance Test

Verify end-to-end processing of 10K files is complete within 9 minutes.

# 9. Scalability Test

Manual scalability testing performed:


# 10. Characteristics

Measured the following characteristics on a KaaS environment

- Deployment time
- Restart time
- Upgrade time
- Loss of service time during upgrade / rollback
- Docker image size
- Microservice memory footprint required to achieve SLO
- Microservice CPU footprint required to achieve SLO
- Throughput of the service, ie time taken to process files, (Bytes/sec).