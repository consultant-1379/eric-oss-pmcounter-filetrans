#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/custom_ruleset.yaml"

try {
    stage('Custom Helm Install') {

        withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
            sh "${bob} -r ${ruleset} helm-dry-run"
        }

        script {
            withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
                if (env.HELM_UPGRADE == "true") {
                    echo "HELM_UPGRADE is set to true:"
                    sh "${bob} -r ${ruleset} helm-upgrade"
                } else {
                    echo "HELM_UPGRADE is NOT set to true:"
                    sh "${bob} -r ${ruleset} helm-install"
                }
            }
        }

        withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
            sh "${bob} -r ${ruleset} healthcheck"
        }
    }
} catch (e) {
    withCredentials([usernamePassword(credentialsId: 'SERO_ARTIFACTORY', usernameVariable: 'SERO_ARTIFACTORY_REPO_USER', passwordVariable: 'SERO_ARTIFACTORY_REPO_PASS')]) {
        sh "${bob} -r ${ruleset} collect-k8s-logs || true"
    }
    archiveArtifacts allowEmptyArchive: true, artifacts: 'k8s-logs/*'
    sh "${bob} -r ${ruleset} delete-namespace"
    throw e
} finally {
    withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
        sh "${bob} -r ${ruleset} kaas-info || true"
    }
    archiveArtifacts allowEmptyArchive: true, artifacts: 'build/kaas-info.log'
}

if (env.RELEASE) {
    try {
        stage('K8S Restart Pod') {
            sh "${bob} -r ${ruleset} k8s-restart-pod"
            archiveArtifacts "doc/Characteristic_Test_Report/characteristic_test_report.md"
        }
    } catch (e) {
        withCredentials([usernamePassword(credentialsId: 'SERO_ARTIFACTORY', usernameVariable: 'SERO_ARTIFACTORY_REPO_USER', passwordVariable: 'SERO_ARTIFACTORY_REPO_PASS')]) {
            sh "${bob} -r ${ruleset} collect-k8s-logs || true"
        }
        archiveArtifacts allowEmptyArchive: true, artifacts: 'k8s-logs/*'
        sh "${bob} -r ${ruleset} delete-namespace"
        throw e
    } finally {
        withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
            sh "${bob} -r ${ruleset} kaas-info || true"
        }
        archiveArtifacts allowEmptyArchive: true, artifacts: 'build/kaas-info.log'
    }
}
