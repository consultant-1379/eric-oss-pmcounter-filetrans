**SFTP File Transfer 1.0.0 PRI**

APR 201 535/1, R1A\

SFTP File Transfer 1.87.0-1

**Contents**

[1 Reason for Revision](#reason-for-revision)

[//]: # (* [1.1 Reason for Major Version step]&#40;#reason-for-major-version-step&#41;)

[2 Evidence of Conformity with the Acceptance Criteria](#evidence-of-conformity-with-the-acceptance-criteria)

[3 Technical Solution](#technical-solution)

* [3.1 Implemented Requirements](#implemented-requirements)

* [3.2 Implemented additional features](#implemented-additional-features)

* [3.3 Implemented API Changes](#implemented-api-changes)

* [3.4 SW Library](#sw-library)

* [3.5 Reusable Images](#reusable-images)

* [3.6 Impact on Users: Abrupt NBC](#impact-on-users-abrupt-nbc)

* [3.7 Impact on Users: NUC/NRC](#impact-on-users-nucnrc)

* [3.8 Impact on Users: NBC (Deprecation Ended)](#impact-on-users-nbc-deprecation-ended)

* [3.9 Impact on Users: Started/Ongoing Deprecations](#impact-on-users-startedongoing-deprecations)

* [3.10 Corrected Trouble Reports](#corrected-trouble-reports)

* [3.11 Restrictions and Limitations](#restrictions-and-limitations)

[4 Product Deliverables](#product-deliverables)

* [4.1 Software Products](#software-products)

* [4.2 New and Updated 2PP/3PP](#new-and-updated-2pp3pp)

* [4.3 Helm Chart Link](#helm-chart-link)

* [4.4 Related Documents](#related-documents)

[5 Product Documentation](#product-documentation)

* [5.1 Developer Product Information](#developer-product-information)

* [5.2 Customer Product Information](#customer-product-information)

[6 Deployment Information](#deployment-information)

* [6.1 Deployment Instructions](#deployment-instructions)

* [6.2 Upgrade Information](#upgrade-information)

[7 Verification Status](#verification-status)

* [7.1 Stakeholder Verification](#stakeholder-verification)

[8 Support](#support)

[9 References](#references)

Revision History

| **Revision** | **Date**   | **Reason for Revision**                                      |
|--------------|------------|--------------------------------------------------------------|
| PA1          | 2022-09-27 | First Draft                                                  |
| PA2          | 2022-10-13 | Minor updates                                                |
| PB1          | 2022-11-28 | Update for Service Mesh                                      |
| PC1          | 2023-02-01 | Update for DC-C Data Registration                            |
| PD1          | 2023-04-04 | Update for Secure Access to BDR Storage for PM file transfer |
# Reason for Revision

SFTP File Transfer service 1.87.0-1 introduces SIP-TLS certificates to facilitate secure communication from Sftp to Object Storage.

<!---
(In case of Emergency Package (EP), report “This is an emergency
correction release meant for limited use. Details on the restrictions
and limitations for this release are reported in [section 3.11](#Restrictions-and-Limitations).”)
-->

[//]: # (## Reason for Major Version step)

[//]: # ()
[//]: # (This is the first release of the ADC SFTP File Transfer service.)

[//]: # (<!---)

[//]: # (&#40;This section shall be created only in case of releases that introduce a)

[//]: # (Major Version step to clarify the reason for it.&#41;)

[//]: # ()
[//]: # (Following changes in this release introduced a backwards incompatibility)

[//]: # (that required a major version step:)

[//]: # ()
[//]: # (&#40;Provide a list of NBC/NUC/NRC/Abrupt NBC JIRA items that caused the)

[//]: # (major version step. Free text items for causes that are not tracked in)

[//]: # (JIRA items.&#41;)

[//]: # ()
[//]: # (- [IDUN-00]&#40;https://jira.link&#41;: Service API NBC XXXX)

[//]: # ()
[//]: # (- [IDUN-01]&#40;https://jira.link&#41;: Impacts on upgrade &#40;NUC&#41;)

[//]: # ()
[//]: # (- [IDUN-02]&#40;https://jira.link&#41;: Impacts on Rollback &#40;NRC&#41;)

[//]: # ()
[//]: # (- \<Free text for other reasons not tracked as JIRA items\>)

[//]: # (-->)

# Evidence of Conformity with the Acceptance Criteria

The release criteria have been fulfilled.

The release decision has been taken by the approval of this document.

The release decision has been taken by the approver of this document.

<!---
This release has been done in accordance with the \<Link to the PRA
Checklist in full released and checked revision\>.

Free text
(If there are no deviations state “The release criteria have been
fulfilled”)
(If there are deviations state:
“The release criteria have been fulfilled except for:

- Reason A

- Reason B

“)

-->
# Technical Solution

## Implemented Requirements

| Requirement ID (MR/JIRA ID) | Heading/DESCRIPTION                               |
|-----------------------------|---------------------------------------------------|
| IDUN-4752                   | Secure access to BDR storage for PM file transfer |


<!---
All implemented requirements in this chapter have Feature Maturity
Stable. Requirements with Feature Maturity lower than Stable are listed
in chapter [3.11.5](#Features-not-ready-for-commercial-use).

(If no requirement implemented write: No requirement implemented in this
release)
-->

## Implemented additional features

No additional features implemented in this release

<!---
| Jira-id   | jira Heading/DESCRIPTION |
|-----------|--------------------------|
| IDUN-00 | Description                |

All implemented additional features in this chapter have Feature
Maturity Stable. Additional features with Feature Maturity lower than
Stable are listed in chapter [3.11.5](#Features-not-ready-for-commercial-use).

(If no feature implemented write: No additional features implemented in
this release)
-->
## Implemented API Changes

No API exists for this product.
<!---
(If no CAF product exists for the service write "No API exists for this
product")

### \<API Name – This subchapter is added for each CAF product\>

Free text
(Describe the change, including any changes in API Maturity. By default,
if there are no changes, please state “No API change implemented in this
release”)

(The “API Documentation” table shall always be included if a CAF product
is provided and contain the highest version with API Maturity Stable.)

Stable API Documentation:

| Document ID           | Title       | Rev     | Support/Other |
|-----------------------|-------------|---------|---------------|
| n/155 19-CAF xxx xx/x | Description | \<rev\> | EriDoc        |

(If any API versions are included in the release with an API Maturity
lower than Stable they shall be listed in the “Non-stable API
documentation” table.)

(The API Maturity changes table shall be omitted if there are no
included API version with API Maturity lower than Stable.)

Non-stable API Documentation:

| Document ID           | Title       | Rev     | Maturity |
|-----------------------|-------------|---------|----------|
| n/155 19-CAF xxx xx/x | Description | \<rev\> | Beta     |
| n/155 19-CAF xxx xx/x | Description | \<rev\> | Alpha    |

-->
## SW Library
No SW library product for this service.
<!---
**(This section is for SW libraries provided by this service and NOT for
the ones it includes as 2PPs (that shall instead be reported in
[chapter 4.2](#new-and-updated-2pp3pp)). If there is no CXA library product provided by the
service, please state “No SW library product for this service”)**

### This release has been verified to be compatible from the following versions of each SW library product up to the latest versions

| SW library Name | Product Number | Oldest compatible version |
|-----------------|----------------|---------------------------|
| SW library java | CXA 301 1234   | 1.0.0                     |
| SW library c++  | CXA 301 5678   | 1.0.0                     |

### \<SW library Name Changes– This subchapter is added for each CXA product\>

Free text

(Describe the change and refer to the Application Developers Guide for
details. By default, if there are no changes, please state “No SW
library change implemented in this release”)
-->
## Reusable Images

No reusable images products for this service.
<!---
(This section is for Reusable images provided by this service and NOT
for the ones it includes as 2PPs (that shall instead be reported in
[chapter 4.2](#new-and-updated-2pp3pp)). If there is no 2PP reusable image provided by the service,
please state “No reusable images products for this service”)

### \<Reusable Image Name Changes– This subchapter is added for each reusable image CXU product\>

Free text

(Describe the change and refer to the Application Developers Guide for
details. By default, if there are no changes, please state “No change in
reusable image implemented in this release”)

(If there are compatibility aspects to consider for the reusable images
describe them here in free text or table format if possible. Possible
examples of compatibility aspects are: oldest reusable image version
compatible with the service, oldest compatible version of a third
service interacting with reusable image, etc.).
-->
## Impact on Users: Abrupt NBC

No Abrupt NBC introduced in this release.

<!---
(If no abrupt NBC is introduced by the service, please state “No Abrupt
NBC introduced in this release”)

The tables below describe the abrupt non-backward compatible changes
(Abrupt NBC) introduced in this service version. These functions have
been modified and require their users to adapt in this version of the
service.

| [ADPPRG-7008](https://eteamproject.internal.ericsson.com/browse/ADPPRG-7008) | \<Summary\>                            |
|------------------------------------------------------------------------------|----------------------------------------|
| Impact on other ADP Service Components                                       | Description (multiple lines if needed) |
| Impact on interface users                                                    | Description (multiple lines if needed) |
| Impact on end-customers                                                      | Description (multiple lines if needed) |

Free text

(The table and the free text are repeated for each Abrupt NBC)
-->
## Impact on Users: NUC/NRC

No NUC/NRC introduced in this release.

<!---
(If no abrupt NUC/NRC is introduced by the service, please state “No
NUC/NRC introduced in this release”)

The tables below describe the Non-upgradeable (NUC) and Non-rollbackable
(NRC) changes introduced in this service version.

| [ADPPRG-7008](https://eteamproject.internal.ericsson.com/browse/ADPPRG-7008) | \<Summary\>                            |
|------------------------------------------------------------------------------|----------------------------------------|
| Impact on end-customers                                                      | Description (multiple lines if needed) |

Free text

(The table and the free text are repeated for each NUC/NRC)
-->
## Impact on Users: NBC (Deprecation Ended)

No NBC introduced in this release.

<!---
(If no NBC is introduced by the service, please state “No NBC introduced
in this release”)

The tables below describe the non-backward compatible changes (NBC)
introduced in this service version. These functions have been deprecated
and are now removed in this version and can no longer be used.

| [ADPPRG-7008](https://eteamproject.internal.ericsson.com/browse/ADPPRG-7008) | \<Summary\>                            |
|------------------------------------------------------------------------------|----------------------------------------|
| Impact on other ADP Service Components                                       | Description (multiple lines if needed) |
| Impact on interface users                                                    | Description (multiple lines if needed) |
| Impact on end-customers                                                      | Description (multiple lines if needed) |

Free text

(The table and the free text are repeated for each NBC)
-->
## Impact on Users: Started/Ongoing Deprecations

No NBC introduced in started/ongoing deprecations.

<!---
The tables below describe the started deprecations for this service. As
a user of a deprecated function, please ensure to stop using the
deprecated function as early as possible. For more details, see the JIRA
ticket(s).

| [ADPPRG-7008](https://eteamproject.internal.ericsson.com/browse/ADPPRG-7008) | \<Summary\>                            |
|------------------------------------------------------------------------------|----------------------------------------|
| Start Date                                                                   | 2020-01-01                             |
| End Date                                                                     | 2020-05-02                             |
| Impact on other ADP Service Components                                       | Description (multiple lines if needed) |
| Impact on interface users                                                    | Description (multiple lines if needed) |
| Impact on end-customers                                                      | Description (multiple lines if needed) |

Free text

(The table and the free text are repeated for each Deprecation)
-->

## Corrected Trouble Reports

The table below lists the Trouble Reports that is reported in the
[JIRA](https://eteamproject.internal.ericsson.com/projects/ADPPRG) and
is corrected in SFTP File Transfer service:

| **TR ID**  | **TR HEADING**                                                                                   |
|------------|--------------------------------------------------------------------------------------------------|
| IDUN-59474 | The notification name has the schemaname additionally appended                                   |
| IDUN-60314 | [MM-ADC]: SFTP Netpol is not working for multiple SFTP instances                                 |
| IDUN-61846 | [MM-ADC]: SFTP : Test cases failed with "FAILED to create Output Topic"error in the App Staging  | 
| IDUN-62616 | eric-oss-sftp-filetrans in ContainerStatusUnknown state.                                         | 
| IDUN-63577 | Change FileFormat values registered by SFTP file transfer                                        | 
| IDUN-64169 | ScheduledTask to delete PM Stat files earlier than 24 hours is not getting triggered             | 
| IDUN-64208 | ADC - SFTP : Test cases failed with "FAILED to create Output Topic"error in the App Staging      | 
| IDUN-64965 | EIC Release / MANA Lab -- No documentation exists for configuring ADC SFTP File Transfer Service | 

### Corrected Vulnerability Trouble Reports

No vulnerability trouble reports fixed in this release.

<!---
The table below lists the Vulnerability Trouble Reports that is reported
in the
[JIRA](https://eteamproject.internal.ericsson.com/projects/ADPPRG) and
is corrected in SFTP File Transfer service:

(If no fix implemented write: No vulnerability trouble reports fixed in
this release)

| Vulnerability ID(s) | Vulnerability Description | TR ID     |
|---------------------|---------------------------|-----------|
| CVE-00              |                           |           |
| CVE-11              | Summary                   | IDUN-00   |
-->
## Restrictions and Limitations

<!---
Free text

(Report here general restrictions or limitations to the usage of this
release: for instance, if the release is limited to specific
Customer/Applications for which the release provides an emergency
correction (EP scenario). Leave empty and jump to next subsection if
nothing to report here.)
-->
### Exemptions

No exemption is present in this release.
<!---
Free text

(All active exemptions as one of the following kinds, which are valid
this release of the product, should be described in this chapter:

1.GPR Security and Privacy exemptions granted by [ADP Security and
Privacy GPRs Exemption
Board](https://confluence.lmera.ericsson.se/display/ASPGEB/ADP+Security+and+Privacy+GPRs+Exemption+Board)

2\.[Permanent ADP DR
exemptions](https://confluence.lmera.ericsson.se/pages/viewpage.action?spaceKey=AA&title=Recorded+DR+Exemption+Decisions)
granted by ADP AC

3\.[Temporary ADP DR
exemptions](https://confluence.lmera.ericsson.se/display/AGW/DR+checker+-+exemptions+follow+up)
granted by ADP program)

(Description of the exemption shall be done to a proper detail level.
For example, for non-root DR exemption, description should include what
capabilities and/or privileges are included.)

(If no exemption is present, please state “No exemption is present in
this release.”)
-->

### Open Trouble Reports
No open Trouble Reports.
<!---
The table below lists the Trouble Reports that are currently open in
SFTP File Transfer X.Y.Z.

(If no feature implemented write: All work items mentioned in commits
since the last PRA tag are in closed status.)

| **TR ID** | **TR Heading** | **Priority** |
|-----------|----------------|--------------|
| IDUN-00   | Description    | **A**        |

-->

### Backward incompatibilities
No NBC introduced in this release.
<!---
(If no NBC is introduced by this service release, please state “No NBC
introduced in this release”, otherwise report the text below)
For the list of non-backward compatible changes (NBC) introduced in this
release refer to [chapters 3.6](#impact-on-users-abrupt-nbc) and [chapter 3.8](#impact-on-users-nbc-deprecation-ended).
-->
### Unsupported Upgrade/Rollback paths

No NUC/NRC introduced in this release.

<!---
(If the service is introducing or has introduced in previous releases a
NUC/NRC that limit the possible upgrade/rollback paths for the service,
report, in a table format, the oldest service version for which upgrade
and rollback are supported. See example below)

| **Oldest version for which upgrade to this release is supported** | **Oldest version for which rollback from this release is supported** |
|-------------------------------------------------------------------|----------------------------------------------------------------------|
| 1.0.0                                                             | **1.5.0**                                                            |

(If no NUC/NRC is introduced by this service release, please state “No
NUC/NRC introduced in this release”, otherwise report the text below)
For the list of Non-Upgradeable (NUC) and Non-Rollbackable (NRC) changes
introduced in this release refer to [chapter 3.7](#impact-on-users-nucnrc).
-->
### Features not ready for commercial use

No Feature with Feature Maturity Alpha/Beta included in this release.

<!---
(If no Feature is delivered by the service with Feature Maturity Alpha
or Beta, please write “No Feature with Feature Maturity Alpha/Beta
included in this release”)

The table below lists the included features that are not yet ready for
commercial use and thus have Feature Maturity Alpha or Beta. These
features should not be used in any commercial or otherwise sensitive
deployment unless a proper risk analysis has been performed as their
function is not yet final. Alpha features shall be disabled by default,
Beta features may be enabled by default. The default state of each
feature can be found in the Helm chart for this release.

| Requirement ID (MR/JIRA ID) | DESCRIPTION | feature maturity |
|-----------------------------|-------------|------------------|
| IDUN-00                     | Description | Alpha            |
| IDUN-01                     | Description | Beta             |
-->
# Product Deliverables

## Software Products

The following table shows the software products of this release.

| Product Type | Name                    | Product ID   | New Version |
|--------------|-------------------------|--------------|-------------|
| HELM chart   | SFTP File Transfer HELM | CXU 101 1655 | 1.87.0-1    |
| Docker image | SFTP File Transfer      | CXD 101 395  | 1.87.0-1    |
| Code Source  | SFTP File Transfer      | CAV 101 0489 | 1.87.0-1    |


## New and Updated 2PP/3PP
Please refer to SVL for a complete list of all 2PP/3PPs, SFTP File Transfer [SVL](To be added).
<!---
(This section shall report all new and updated 2PPs and 3PPs integrated
by the service. This includes SW libraries and reusable images embedded
as 2PP in the service.)

(NOTE: Either PRIM or Mimer product ID can be used in the list depending
on the PLM System used by the Service.)

The following 2PP/3PP’s are new or updated:
-->
| Name                        | Product ID       | Old version | New version               |
|-----------------------------|------------------|-------------|---------------------------|
| CBOS                        | APR 901 0622     | 5.12.0-13   | Automated, latest version |


## Helm Chart Link

The following table shows the repository manager links for this release:

| Release                          | Helm package link                                                                                                                                                    |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| SFTP File transfer HELM 1.87.0-1 | [SFTP File Transfer Helm Package](https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-drop-helm/eric-oss-sftp-filetrans/eric-oss-sftp-filetrans-1.87.0-1.tgz) |

## Related Documents

All the documents can be accessible from Marketplace, see chapter “Product Documentation” for details.

<!---
 The following list contains all the document deliverables. The
 documents are stored in Eridoc. All the documents can be accessible
 from Marketplace, see chapter “Product Documentation” for details.
 Revisions to be stated in letters, e.g A, B, C, etc.

| Document ID             | Title                                         | Rev |
|-------------------------|-----------------------------------------------|-----|
| n/198 17-APR 201 535/1  | Application Developers Guide                  | B   |
| n/1553-APR 201 535/1    | User Guide                                    | B   |
| n/152 41-APR 201 535/1  | Test Specification                            | B   |
| n/006 64-APR 201 535 /1 | Risk assessment and Privacy Impact Assessment | E   |
| n/1597-APR 201 535/1    | Vulnerability Analysis                        | C   |
| 152 83-APR 201 535/1    | Test Report                                   | B   |
-->
# Product Documentation

## Developer Product Information

The Developer Product Information (DPI) documentation can be accessed
using the following link:[ADP Market Place](https://adp.ericsson.se/marketplace/sftp-file-transfer/documentation/development/dpi/).


## Customer Product Information

This service does not provide any CPI content.

<!---
(If no CPI provided at all for this service write: This service does not
provide any CPI content.)

The service provides reusable Customer Product Information (CPI) content
that can be reused in ADP application CPI libraries. This content is
published in an ADP specific ELEX library to show how the information
could be reused. It helps applications to produce their own CPI
libraries based on the reusable content. The ELEX library can be
accessed using the following link:

<http://calstore.internal.ericsson.com/elex?LI=EN/LZN7950007*>.

The ADP example documents in this ELEX library are only used to
illustrate how CPI topics from DITA CMS DRM "ADP Content for Reuse"
could be reused in ADP application documents. The ADP example documents
must in general not be published in CPI libraries of ADP applications.
How ADP CPI is to be reused by ADP applications is described in the ELEX
library, see HOW TO REUSE OVERVIEW
(<http://calstore.internal.ericsson.com/elex?LI=EN/LZN7950007*&FB=0_0_0&FN=1_1551-LZA9018693Uen.*.html>).
-->

# Deployment Information

The SFTP File Transfer Service can be deployed in a Kubernetes PaaS
environment.

## Deployment Instructions

The target group for the deployment instructions is only application
developers and application integrators. Deployment instruction can be
found in [SFTP File Transfer User
Guide](https://adp.ericsson.se/marketplace/sftp-file-transfer/documentation/development/dpi/).

## Upgrade Information

This is a new product.

# Verification Status

The verification status is described in the [SFTP File Transfer Test
Report](https://adp.ericsson.se/marketplace/sftp-file-transfer/documentation/development/dpi/).

## Stakeholder Verification

It is verified in ADP APP staging. See [CI/CD
Dashboard](https://cicd-ng.web.adp.gic.ericsson.se/view/6/dashboard/22) for further
understanding.

Note: that result showing in this CI/CD Dashboard pipeline is always
result of latest build and is not this specific PRA release.

# Support

For support use the [Generic Services Support
JIRA](https://jira-oss.seli.wh.rnd.internal.ericsson.com/secure/RapidBoard.jspa?rapidView=7862&view=planning.nodetail&issueLimit=100)
project, please also see the SFTP File Transfer Service troubleshooting
guidelines in Marketplace where you will find more detailed support
information.

# References

1. [JIRA](https://jira-oss.seli.wh.rnd.internal.ericsson.com/secure/RapidBoard.jspa?rapidView=7862&view=planning.nodetail&issueLimit=100)

2. [Marketplace](https://adp.ericsson.se/marketplace/sftp-file-transfer)