#!/usr/bin/env groovy

def bob = 'python3 bob/bob2.0/bob.py'
def bob_mimer = 'python3 bob/bob2.0/bob.py -r ruleset2.0.mimer.yaml'
def LOCKABLE_RESOURCE_LABEL = "kaas"

pipeline {
    agent {
        node {
            label NODE_LABEL
        }
    }

    options {
        timestamps()
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '50', artifactNumToKeepStr: '50'))
    }

    environment {
        RELEASE = "true"
        TEAM_NAME = "Phoenix"
        KUBECONFIG = "${WORKSPACE}/.kube/config"
        MAVEN_CLI_OPTS = "-Duser.home=${env.HOME} -B -s ${env.WORKSPACE}/settings.xml"
        VHUB_API_TOKEN = credentials('vhub-api-key-id')
        GERRIT_CHANGE_URL = "${GERRIT_CHANGE_URL}"
    }

    // Stage names (with descriptions) taken from ADP Microservice CI Pipeline Step Naming Guideline: https://confluence.lmera.ericsson.se/pages/viewpage.action?pageId=122564754
    stages {
        stage('Prepare') {
            steps {
                checkout([$class: 'GitSCM',
                    branches: [
                        [name: "master"]
                    ],
                    extensions: [
                        [$class: 'CleanBeforeCheckout'],
                        [$class: 'WipeWorkspace'],
                        [$class: 'SubmoduleOption',
                            disableSubmodules: false,
                            parentCredentials: true,
                            recursiveSubmodules: true,
                            reference: '',
                            trackingSubmodules: false],
                        ],
                        userRemoteConfigs: [
                            [url: '${GERRIT_CENTRAL}/OSS/com.ericsson.oss.adc/eric-oss-pmcounter-filetrans']
                        ]
                    ])
                    sh "${bob} --help"
            }
        }

        stage('Clean') {
            steps {
                echo 'Inject settings.xml into workspace:'
                configFileProvider([configFile(fileId: "${env.SETTINGS_CONFIG_FILE_NAME}", targetLocation: "${env.WORKSPACE}")]) {}
                archiveArtifacts allowEmptyArchive: true, artifacts: 'ruleset2.0.yaml, publish.Jenkinsfile'
                sh "${bob} clean"
            }
        }

        stage('Init') {
            steps {
                sh "${bob} init-drop"
                archiveArtifacts 'artifact.properties'
                script {
                    authorName = sh(returnStdout: true, script: 'git show -s --pretty=%an')
                    currentBuild.displayName = currentBuild.displayName + ' / ' + authorName
                }
            }
        }

        stage('Lint') {
            steps {
                parallel(
                    "lint markdown": {
                        sh "${bob} lint:markdownlint lint:vale"
                    },
                    "lint helm": {
                        sh "${bob} lint:helm"
                    },
                    "lint helm design rule checker": {
                        sh "${bob} lint:helm-chart-check"
                    },
                    "lint code": {
                        sh "${bob} lint:license-check"
                    },
                    "lint metrics": {
                        sh "${bob} lint:metrics-check"
                    }
                )
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: '**/design-rule-check-report.*'
                }
            }
        }

        stage('Build') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                    sh "${bob} build"
                }
            }
        }

        stage('Test') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                    sh "${bob} test"
                }
            }
        }

        stage('SonarQube') {
            when {
                expression { env.SQ_ENABLED == "true" }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                    withSonarQubeEnv("${env.SQ_SERVER}") {
                        sh "${bob} sonar-enterprise-release"
                    }
                }
            }
        }
        stage('Cbos version') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                       sh "${bob} cbos-version"
                       sh "${bob} update-cbos-version"
                    }
                }
            }
        }
        stage('Image') {
            steps {
                sh "${bob} image"
                sh "${bob} image-dr-check"
                archiveArtifacts "doc/Characteristic_Test_Report/characteristic_test_report.md"
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: '**/image-design-rule-check-report*'
                }
            }
        }

        stage('Package') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                    sh "${bob} package-local"
                    sh "${bob} package-jars"
                }
            }
        }

        stage('K8S Resource Lock') {
            options {
                lock(label: LOCKABLE_RESOURCE_LABEL, variable: 'RESOURCE_NAME', quantity: 1)
            }
            environment {
                K8S_CLUSTER_ID = sh(script: "echo \${RESOURCE_NAME} | cut -d'_' -f1", returnStdout: true).trim()
                K8S_NAMESPACE = sh(script: "echo \${RESOURCE_NAME} | cut -d',' -f1 | cut -d'_' -f2", returnStdout: true).trim()
            }

            stages {
                stage('Helm Install') {
                    steps {
                        echo "Inject kubernetes config file (${env.K8S_CLUSTER_ID}) based on the Lockable Resource name: ${env.RESOURCE_NAME}"
                        configFileProvider([configFile(fileId: "${env.K8S_CLUSTER_ID}", targetLocation: "${env.KUBECONFIG}")]) {}
                        echo "The namespace (${env.K8S_NAMESPACE}) is reserved and locked based on the Lockable Resource name: ${env.RESOURCE_NAME}"

                        sh "${bob} helm-dry-run"
                        sh "${bob} create-namespace"

                        script {
                            if (env.HELM_UPGRADE == "true") {
                                echo "HELM_UPGRADE is set to true:"
                                withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                                    sh "${bob} helm-upgrade"
                                }
                            } else {
                                echo "HELM_UPGRADE is NOT set to true:"
                                sh "${bob} helm-install"
                            }
                            archiveArtifacts "doc/Characteristic_Test_Report/characteristic_test_report.md"
                        }

                        sh "${bob} healthcheck"
                    }
                    post {
                        always {
                            sh "${bob} kaas-info || true"
                            archiveArtifacts allowEmptyArchive: true, artifacts: 'build/kaas-info.log'
                        }
                        unsuccessful {
                            withCredentials([usernamePassword(credentialsId: 'SERO_ARTIFACTORY', usernameVariable: 'SERO_ARTIFACTORY_REPO_USER', passwordVariable: 'SERO_ARTIFACTORY_REPO_PASS')]) {
                                sh "${bob} collect-k8s-logs || true"
                            }
                            archiveArtifacts allowEmptyArchive: true, artifacts: "k8s-logs/*"
                            sh "${bob} delete-namespace"
                        }
                    }
                }
                stage('K8S Restart Pod') {
                    steps {
                        echo "Inject kubernetes config file (${env.K8S_CLUSTER_ID}) based on the Lockable Resource name: ${env.RESOURCE_NAME}"
                        configFileProvider([configFile(fileId: "${env.K8S_CLUSTER_ID}", targetLocation: "${env.KUBECONFIG}")]) {}

                        sh "${bob} k8s-restart-pod"
                        archiveArtifacts "doc/Characteristic_Test_Report/characteristic_test_report.md"
                    }
                    post {
                        always {
                            sh "${bob} kaas-info || true"
                            archiveArtifacts allowEmptyArchive: true, artifacts: 'build/kaas-info.log'
                        }
                        unsuccessful {
                            withCredentials([usernamePassword(credentialsId: 'SERO_ARTIFACTORY', usernameVariable: 'SERO_ARTIFACTORY_REPO_USER', passwordVariable: 'SERO_ARTIFACTORY_REPO_PASS')]) {
                                sh "${bob} collect-k8s-logs || true"
                            }
                            archiveArtifacts allowEmptyArchive: true, artifacts: "k8s-logs/*"
                            sh "${bob} delete-namespace"
                        }
                    }
                }
                stage('Vulnerability Analysis') {
                    steps {
                        parallel(
                            "Hadolint": {
                                script {
                                    sh "${bob} hadolint-scan"
                                    echo "Evaluating Hadolint Scan Resultcodes..."
                                    sh "${bob} evaluate-design-rule-check-resultcodes"
                                    archiveArtifacts "build/va-reports/hadolint-scan/**.*"
                                }
                            },
                            "Kubehunter": {
                                script {
                                    configFileProvider([configFile(fileId: "${K8S_CLUSTER_ID}", targetLocation: "${env.KUBECONFIG}")]) {}
                                    sh 'echo "System: [$system]"'
                                    sh 'echo "Kubeconfig: [$KUBECONFIG]"'
                                    sh "${bob} kubehunter-scan"
                                    archiveArtifacts "build/va-reports/kubehunter-report/**/*"
                                }
                            },
                            "Kubeaudit": {
                                script {
                                    sh "${bob} kube-audit"
                                    archiveArtifacts "build/va-reports/kube-audit-report/**/*"
                                }
                            },
                            "Kubsec": {
                                script {
                                    sh "${bob} kubesec-scan"
                                    archiveArtifacts "build/va-reports/kubesec-reports/*"
                                }
                            },
                            "Trivy": {
                                script {
                                    sh "${bob} trivy-inline-scan"
                                    archiveArtifacts "build/va-reports/trivy-reports/**.*"
                                    archiveArtifacts 'trivy_metadata.properties'
                                }
                            },
                           "X-Ray": {
                                script {
                                    sleep(60)
                                    withCredentials([usernamePassword(credentialsId: 'XRAY_SELI_ARTIFACTORY', usernameVariable: 'XRAY_USER', passwordVariable: 'XRAY_APIKEY')]) {
                                        sh "${bob} fetch-xray-report"
                                    }
                                    archiveArtifacts 'build/va-reports/xray-reports/xray_report.json'
                                    archiveArtifacts 'build/va-reports/xray-reports/raw_xray_report.json'
                                }
                            },
                            "Anchore-Grype": {
                                script {
                                    sh "${bob} anchore-grype-scan"
                                    archiveArtifacts "build/va-reports/anchore-reports/**.*"
                                }
                            }
                        )
                    }
                    post {
                        unsuccessful {
                            withCredentials([usernamePassword(credentialsId: 'SERO_ARTIFACTORY', usernameVariable: 'SERO_ARTIFACTORY_REPO_USER', passwordVariable: 'SERO_ARTIFACTORY_REPO_PASS')]) {
                                sh "${bob} collect-k8s-logs || true"
                            }
                            archiveArtifacts allowEmptyArchive: true, artifacts: 'k8s-logs/**/*.*'
                        }
                        cleanup {
                            sh "${bob} delete-namespace"
                            sh "${bob} cleanup-anchore-trivy-images"
                            sh "rm -f ${env.KUBECONFIG}"
                        }
                    }
                }
            }
        }
        stage('Generate Vulnerability report V2.0'){
            steps {
                sh "${bob} generate-VA-report-V2:no-upload" // TODO IDUN-22106 this needs to be "upload" when the service has been registered in VHUB
                archiveArtifacts allowEmptyArchive: true, artifacts: 'build/va-reports/Vulnerability_Report_2.0.md'
            }
        }

        stage ('FOSSA Analyze') {
            steps {
                withCredentials([string(credentialsId: 'FOSSA_API_token', variable: 'FOSSA_API_KEY')]){
                    sh "${bob} fossa-analyze"
                }
            }
        }

        stage ('FOSSA Fetch Report') {
           
            steps {
                withCredentials([string(credentialsId: 'FOSSA_API_token', variable: 'FOSSA_API_KEY')]){
                    sh "${bob} fossa-scan-status-check"
                    sh "${bob} fetch-fossa-report-attribution"
                    archiveArtifacts '*fossa-report.json'
                }
            }
        }

        stage ('FOSSA Dependency Validate') {
            when {
                expression { env.FOSSA_ENABLED == "true" }
            }
            steps {
                withCredentials([string(credentialsId: 'FOSSA_API_token', variable: 'FOSSA_API_KEY')]){
                    sh "${bob} dependency-validate"
                }
              }
        }

        stage('Generate') {
            steps {
                sh "${bob} generate-docs"
                archiveArtifacts "build/doc/**/*.*"
            }
        }

        stage('Marketplace Documents upload') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS'),
                string(credentialsId: 'SFTP_FT_MARKETPLACE_TOKEN', variable: 'MARKETPLACE_TOKEN')]) {
                    sh "${bob} generate-doc-zip-package"
                    archiveArtifacts "build/doc-marketplace/**"
              //    archiveArtifacts "build/doc-svl-replacement/**"
                    sh "${bob} marketplace-upload:upload-doc-to-arm-released"
                    sh "${bob} marketplace-upload:refresh-adp-portal-marketplace"
                }
            }
        }

        stage('Publish') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS'),
                usernamePassword(credentialsId: 'GERRIT_PASSWORD', usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD')]) {
                    sh "${bob} publish"
                    sh "${bob} create-git-tag"
                    sh "git pull"
                }
            }
            post {
                cleanup {
                    sh "${bob} delete-images"
                }
            }
        }
    }
    post {
        success {
            withCredentials([usernamePassword(credentialsId: 'GERRIT_PASSWORD', usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD')])
            {
                bumpVersion()
            }
            script {
                sh "${bob} helm-chart-check-report-warnings"
                sendHelmDRWarningEmail()
                modifyBuildDescription()
            }
        }
    }
}

def modifyBuildDescription() {

    def CHART_NAME = "eric-oss-sftp-filetrans"
    def DOCKER_IMAGE_NAME = "eric-oss-sftp-filetrans"

    def VERSION = readFile('.bob/var.version').trim()

    def CHART_DOWNLOAD_LINK = "https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-drop-helm/${CHART_NAME}/${CHART_NAME}-${VERSION}.tgz"
    def DOCKER_IMAGE_DOWNLOAD_LINK = "https://armdocker.rnd.ericsson.se/artifactory/docker-v2-global-local/proj-eric-oss-drop/${CHART_NAME}/${VERSION}/"

    currentBuild.description = "Helm Chart: <a href=${CHART_DOWNLOAD_LINK}>${CHART_NAME}-${VERSION}.tgz</a><br>Docker Image: <a href=${DOCKER_IMAGE_DOWNLOAD_LINK}>${DOCKER_IMAGE_NAME}-${VERSION}</a><br>Gerrit: <a href=${env.GERRIT_CHANGE_URL}>${env.GERRIT_CHANGE_URL}</a> <br>"
}

def sendHelmDRWarningEmail() {
    def val = readFile '.bob/var.helm-chart-check-report-warnings'
    if (val.trim().equals("true")) {
        echo "WARNING: One or more Helm Design Rules have a WARNING state. Review the Archived Helm Design Rule Check Report: design-rule-check-report.html"
        manager.addWarningBadge("One or more Helm Design Rules have a WARNING state. Review the Archived Helm Design Rule Check Report: design-rule-check-report.html")
        echo "Sending an email to Helm Design Rule Check distribution list: ${env.HELM_DR_CHECK_DISTRIBUTION_LIST}"
        try {
            mail to: "${env.HELM_DR_CHECK_DISTRIBUTION_LIST}",
            from: "${env.GERRIT_PATCHSET_UPLOADER_EMAIL}",
            cc: "${env.GERRIT_PATCHSET_UPLOADER_EMAIL}",
            subject: "[${env.JOB_NAME}] One or more Helm Design Rules have a WARNING state. Review the Archived Helm Design Rule Check Report: design-rule-check-report.html",
            body: "One or more Helm Design Rules have a WARNING state. <br><br>" +
            "Please review Gerrit and the Helm Design Rule Check Report: design-rule-check-report.html: <br><br>" +
            "&nbsp;&nbsp;<b>Gerrit master branch:</b> https://gerrit-gamma.gic.ericsson.se/gitweb?p=${env.GERRIT_PROJECT}.git;a=shortlog;h=refs/heads/master <br>" +
            "&nbsp;&nbsp;<b>Helm Design Rule Check Report:</b> ${env.BUILD_URL}artifact/.bob/design-rule-check-report.html <br><br>" +
            "For more information on the Design Rules and ADP handling process please see: <br>" +
            "&nbsp;&nbsp; - <a href='https://confluence.lmera.ericsson.se/display/AA/Helm+Chart+Design+Rules+and+Guidelines'>Helm Design Rule Guide</a><br>" +
            "&nbsp;&nbsp; - <a href='https://confluence.lmera.ericsson.se/display/ACD/Design+Rule+Checker+-+How+DRs+are+checked'>More Details on Design Rule Checker</a><br>" +
            "&nbsp;&nbsp; - <a href='https://confluence.lmera.ericsson.se/display/AA/General+Helm+Chart+Structure'>General Helm Chart Structure</a><br><br>" +
            "<b>Note:</b> This mail was automatically sent as part of the following Jenkins job: ${env.BUILD_URL}",
            mimeType: 'text/html'
        } catch(Exception e) {
            echo "Email notification was not sent."
            print e
        }
    }
}

/*  increase pom & prefix version - patch number
    e.g.  1.0.0 -> 1.1.0
*/
def bumpVersion() {
    env.oldPatchVersionPrefix = readFile ".bob/var.version"
    env.VERSION_PREFIX_CURRENT = env.oldPatchVersionPrefix.trim()
    // increase patch number to version_prefix
    sh 'docker run --rm -v $PWD/VERSION_PREFIX:/app/VERSION -w /app --user $(id -u):$(id -g) armdocker.rnd.ericsson.se/proj-eric-oss-drop/utilities/bump minor'

    env.versionPrefix = readFile "VERSION_PREFIX"
    env.newPatchVersionPrefix = env.versionPrefix.trim() + "-SNAPSHOT"
    env.VERSION_PREFIX_UPDATED = env.newPatchVersionPrefix.trim()

    echo "pom version has been bumped from ${VERSION_PREFIX_CURRENT} to ${VERSION_PREFIX_UPDATED}"

    sh """
        sed -i '0,/${VERSION_PREFIX_CURRENT}/s//${VERSION_PREFIX_UPDATED}/' pom.xml
        git add VERSION_PREFIX
        git add pom.xml
        git commit -m "Automatically updating VERSION_PREFIX to ${versionPrefix}"
        git push origin HEAD:master
    """
}
