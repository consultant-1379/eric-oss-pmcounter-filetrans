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
        TEAM_NAME = "Phoenix"
        KUBECONFIG = "${WORKSPACE}/.kube/config"
        MAVEN_CLI_OPTS = "-Duser.home=${env.HOME} -B -s ${env.WORKSPACE}/settings.xml"
        VHUB_API_TOKEN = credentials('vhub-api-key-id')
        HADOLINT_ENABLED = "true"
        KUBEHUNTER_ENABLED = "true"
        KUBEAUDIT_ENABLED = "true"
        KUBESEC_ENABLED = "true"
        TRIVY_ENABLED = "true"
        XRAY_ENABLED = "true"
        ANCHORE_ENABLED = "true"
        FOSSA_ENABLED = "true"
    }

    // Stage names (with descriptions) taken from ADP Microservice CI Pipeline Step Naming Guideline: https://confluence.lmera.ericsson.se/pages/viewpage.action?pageId=122564754
    stages {
        stage('Prepare') {
            steps {
                checkout([$class: 'GitSCM',
                    branches: [
                        [name: "${GERRIT_PATCHSET_REVISION}"]
                    ],
                    extensions: [
                        [$class: 'SubmoduleOption',
                            disableSubmodules: false,
                            parentCredentials: true,
                            recursiveSubmodules: true,
                            reference: '',
                            trackingSubmodules: false],
                            [$class: 'CleanBeforeCheckout']
                        ],
                    userRemoteConfigs: [
                        [url: '${GERRIT_MIRROR}/OSS/com.ericsson.oss.adc/eric-oss-pmcounter-filetrans']
                    ]
                ])
                /* End of generated snippet */
                sh "${bob} --help"
            }
        }

        stage('Clean') {
            steps {
                echo 'Inject settings.xml into workspace:'
                configFileProvider([configFile(fileId: "${env.SETTINGS_CONFIG_FILE_NAME}", targetLocation: "${env.WORKSPACE}")]) {}
                archiveArtifacts allowEmptyArchive: true, artifacts: 'ruleset2.0.yaml, precodereview.Jenkinsfile'
                sh "${bob} clean"
            }
        }

        stage('Init') {
            steps {
                sh "${bob} init-precodereview"
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

        stage('SonarQube Analysis') {
            when {
                expression { env.SQ_ENABLED == "true" }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                    withSonarQubeEnv("${env.SQ_SERVER}") {
                        sh "${bob} sonar-enterprise-pcr"
                    }
                }
            }
        }

        stage('SonarQube Quality Gate') {
            when {
                expression { env.SQ_ENABLED == "true" }
            }
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitUntil {
                        withSonarQubeEnv("${env.SQ_SERVER}") {
                            script {
                                return getQualityGate()
                            }
                        }
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
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: '**/image-design-rule-check-report*'
                }
            }
        }

        stage('Package') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                        sh "${bob} package"
                        sh "${bob} package-jars"
                    }
                }
            }
            post {
                cleanup {
                    sh "${bob} delete-images:delete-internal-image"
                }
            }
        }

        stage('K8S Resource Lock') {
            options {
                lock(label: LOCKABLE_RESOURCE_LABEL, variable: 'RESOURCE_NAME', quantity: 1)
            }
            environment {
                K8S_CLUSTER_ID = sh(script: "echo \${RESOURCE_NAME} | cut -d'_' -f1", returnStdout: true).trim()
                K8S_NAMESPACE = sh(script: "echo \${RESOURCE_NAME} | cut -d'_' -f1", returnStdout: true).trim()
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
                stage('Vulnerability Analysis') {
                    steps {
                        parallel(
                            "Hadolint": {
                                script {
                                    if (env.HADOLINT_ENABLED == "true") {
                                        sh "${bob} hadolint-scan"
                                        echo "Evaluating Hadolint Scan Resultcodes..."
                                        sh "${bob} evaluate-design-rule-check-resultcodes"
                                        archiveArtifacts "build/va-reports/hadolint-scan/**.*"
                                    }
                                }
                            },
                            "Kubehunter": {
                                script {
                                    if (env.KUBEHUNTER_ENABLED == "true") {
                                        configFileProvider([configFile(fileId: "${K8S_CLUSTER_ID}", targetLocation: "${env.KUBECONFIG}")]) {}
                                        sh 'echo "System: [$system]"'
                                        sh 'echo "Kubeconfig: [$KUBECONFIG]"'
                                        sh "${bob} kubehunter-scan"
                                        archiveArtifacts "build/va-reports/kubehunter-report/**/*"
                                    }
                                }
                            },
                            "Kubeaudit": {
                                script {
                                    if (env.KUBEAUDIT_ENABLED == "true") {
                                        sh "${bob} kube-audit"
                                        archiveArtifacts "build/va-reports/kube-audit-report/**/*"
                                    }
                                }
                            },
                            "Kubsec": {
                                script {
                                    if (env.KUBESEC_ENABLED == "true") {
                                        sh "${bob} kubesec-scan"
                                        archiveArtifacts 'build/va-reports/kubesec-reports/*'
                                    }
                                }
                            },
                            "Trivy": {
                                script {
                                    if (env.TRIVY_ENABLED == "true") {
                                        sh "${bob} trivy-inline-scan"
                                        archiveArtifacts 'build/va-reports/trivy-reports/**.*'
                                        archiveArtifacts 'trivy_metadata.properties'
                                    }
                                }
                            },
                            "X-Ray": {
                                script {
                                    if (env.XRAY_ENABLED == "true") {
                                        sleep(60)
                                        withCredentials([usernamePassword(credentialsId: 'XRAY_SELI_ARTIFACTORY', usernameVariable: 'XRAY_USER', passwordVariable: 'XRAY_APIKEY')]) {
                                            sh "${bob} fetch-xray-report"
                                        }
                                        archiveArtifacts 'build/va-reports/xray-reports/xray_report.json'
                                        archiveArtifacts 'build/va-reports/xray-reports/raw_xray_report.json'
                                    }
                                }
                            },
                            "Anchore-Grype": {
                                script {
                                    if (env.ANCHORE_ENABLED == "true") {
                                        sh "${bob} anchore-grype-scan"
                                        archiveArtifacts 'build/va-reports/anchore-reports/**.*'
                                    }
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
                sh "${bob} generate-VA-report-V2:no-upload"
                archiveArtifacts allowEmptyArchive: true, artifacts: 'build/va-reports/Vulnerability_Report_2.0.md'
            }
        }

        stage ('FOSSA Analyze') {
            when {
                expression { env.FOSSA_ENABLED == "true" }
            }
            steps {
                withCredentials([string(credentialsId: 'FOSSA_API_token', variable: 'FOSSA_API_KEY')]){
                    sh "${bob} fossa-analyze"
                }
            }
        }

        stage ('FOSSA Fetch Report') {
            when {
                expression { env.FOSSA_ENABLED == "true" }
            }
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
                withCredentials([string(credentialsId: 'munin_token', variable: 'MUNIN_TOKEN')]) {
                    sh "${bob} dependency-validate"
                }
            }
        }

        stage ('FOSS Validation for Mimer') {
              when {
                 expression { env.MUNIN_UPDATE_ENABLED == "true" }
              }
              steps {
                 withCredentials([string(credentialsId: 'munin_token', variable: 'MUNIN_TOKEN')]) {
                     sh "${bob_mimer} munin-update-version"
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
                    //  archiveArtifacts "build/doc-svl-replacement/**"
                        sh "${bob} marketplace-upload:upload-doc-to-arm-dev"
                        sh "${bob} marketplace-upload:refresh-adp-portal-marketplace"
                }
            }
        }
    }
    post {
        success {
            script {
                sh "${bob} helm-chart-check-report-warnings"
                addHelmDRWarningIcon()
                modifyBuildDescription()
            }
        }
    }
}

def modifyBuildDescription() {

    def CHART_NAME = "eric-oss-sftp-filetrans"
    def DOCKER_IMAGE_NAME = "eric-oss-sftp-filetrans"

    def VERSION = readFile('.bob/var.version').trim()

    def CHART_DOWNLOAD_LINK = "https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-ci-internal-helm/${CHART_NAME}/${CHART_NAME}-${VERSION}.tgz"
    def DOCKER_IMAGE_DOWNLOAD_LINK = "https://armdocker.rnd.ericsson.se/artifactory/docker-v2-global-local/proj-eric-oss-ci-internal/${CHART_NAME}/${VERSION}/"

    currentBuild.description = "Helm Chart: <a href=${CHART_DOWNLOAD_LINK}>${CHART_NAME}-${VERSION}.tgz</a><br>Docker Image: <a href=${DOCKER_IMAGE_DOWNLOAD_LINK}>${DOCKER_IMAGE_NAME}-${VERSION}</a><br>Gerrit: <a href=${env.GERRIT_CHANGE_URL}>${env.GERRIT_CHANGE_URL}</a> <br>"
}

def addHelmDRWarningIcon() {
    def val = readFile '.bob/var.helm-chart-check-report-warnings'
    if (val.trim().equals("true")) {
        echo "WARNING: One or more Helm Design Rules have a WARNING state. Review the Archived Helm Design Rule Check Report: design-rule-check-report.html"
        manager.addWarningBadge("One or more Helm Design Rules have a WARNING state. Review the Archived Helm Design Rule Check Report: design-rule-check-report.html")
    } else {
        echo "No Helm Design Rules have a WARNING state"
    }
}

def getQualityGate() {
    echo "Wait for SonarQube Analysis is done and Quality Gate is pushed back:"
    try {
        timeout(time: 30, unit: 'SECONDS') {
            qualityGate = waitForQualityGate()
        }
    } catch(Exception e) {
        return false
    }

    echo 'If Analysis file exists, parse the Dashboard URL:'
    if (fileExists(file: 'target/sonar/report-task.txt')) {
        sh 'cat target/sonar/report-task.txt'
        def props = readProperties file: 'target/sonar/report-task.txt'
        env.DASHBOARD_URL = props['dashboardUrl']
    }

    if (qualityGate.status.replaceAll("\\s","") == 'IN_PROGRESS') {
        return false
    }

    if (!env.GERRIT_HOST) {
        env.GERRIT_HOST = "gerrit-gamma.gic.ericsson.se"
    }

    if (qualityGate.status.replaceAll("\\s","") != 'OK') {
        env.SQ_MESSAGE="'"+"SonarQube Quality Gate Failed: ${DASHBOARD_URL}"+"'"
        if (env.GERRIT_CHANGE_NUMBER) {
            sh '''
                ssh -p 29418 ${GERRIT_HOST} gerrit review --label 'SQ-Quality-Gate=-1' --message ${SQ_MESSAGE} --project ${GERRIT_PROJECT} ${GERRIT_PATCHSET_REVISION}
            '''
        }
        manager.addWarningBadge("Pipeline aborted due to Quality Gate failure, see SonarQube Dashboard for more information.")
        error "Pipeline aborted due to quality gate failure!\n Report: ${env.DASHBOARD_URL}\n Pom might be incorrectly defined for code coverage: https://confluence-oss.seli.wh.rnd.internal.ericsson.com/pages/viewpage.action?pageId=309793813"
    } else {
        env.SQ_MESSAGE="'"+"SonarQube Quality Gate Passed: ${DASHBOARD_URL}"+"'"
        if (env.GERRIT_CHANGE_NUMBER) { // If Quality Gate Passed
            sh '''
                ssh -p 29418 ${GERRIT_HOST} gerrit review --label 'SQ-Quality-Gate=+1' --message ${SQ_MESSAGE} --project ${GERRIT_PROJECT} ${GERRIT_PATCHSET_REVISION}
            '''
        }
    }
    return true
}
